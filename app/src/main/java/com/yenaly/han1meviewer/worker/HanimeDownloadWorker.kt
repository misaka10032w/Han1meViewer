package com.yenaly.han1meviewer.worker

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yenaly.han1meviewer.DOWNLOAD_NOTIFICATION_CHANNEL
import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.HFileManager
import com.yenaly.han1meviewer.HFileManager.createVideoName
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.network.ServiceCreator
import com.yenaly.han1meviewer.logic.state.DownloadState
import com.yenaly.han1meviewer.util.HImageMeower
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.await
import com.yenaly.yenaly_libs.utils.createFileIfNotExists
import com.yenaly.yenaly_libs.utils.saveTo
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/08/06 006 11:42
 */
class HanimeDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams), WorkerMixin {

    data class Args(
        val quality: String?,
        val downloadUrl: String?,
        val videoType: String?,
        val hanimeName: String,
        val videoCode: String,
        val coverUrl: String,
    ) {
        companion object {
            fun fromEntity(entity: HanimeDownloadEntity): Args {
                return Args(
                    quality = entity.quality,
                    downloadUrl = entity.videoUrl,
                    videoType = entity.suffix,
                    hanimeName = entity.title,
                    videoCode = entity.videoCode,
                    coverUrl = entity.coverUrl,
                )
            }
        }
    }

    companion object {
        const val TAG = "HanimeDownloadWorker"

        const val RESPONSE_INTERVAL = 500L

        const val BACKOFF_DELAY = 10_000L

        const val FAST_PATH_CANCEL = "fast_path_cancel"
        const val DELETE = "delete"
        const val QUALITY = "quality"
        const val DOWNLOAD_URL = "download_url"
        const val VIDEO_TYPE = "video_type"
        const val HANIME_NAME = "hanime_name"
        const val VIDEO_CODE = "video_code"
        const val COVER_URL = "cover_url"
        const val REDOWNLOAD = "redownload"
        const val IN_WAITING_QUEUE = "in_waiting_queue"
        // const val RELEASE_DATE = "release_date"
        // const val COVER_DOWNLOAD = "cover_download"

        const val PROGRESS = "progress"
        // const val FAILED_REASON = "failed_reason"

        /**
         * 方便统一管理下载 Worker 的创建
         */
        inline fun build(
            constraintsRequired: Boolean = true,
            action: OneTimeWorkRequest.Builder.() -> Unit = {}
        ): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build()
            return OneTimeWorkRequestBuilder<HanimeDownloadWorker>()
                .addTag(TAG)
                .let { builder ->
                    if (constraintsRequired) {
                        builder.setConstraints(constraints)
                    } else {
                        builder
                    }
                }.setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    BACKOFF_DELAY, TimeUnit.MILLISECONDS
                ).apply(action).build()
        }

        fun getRunningWorkInfoCount(context: Context): Flow<Int> {
            return WorkManager.getInstance(context)
                .getWorkInfosByTagFlow(TAG)
                .map { workInfos ->
                    workInfos.count {
                        it.state == WorkInfo.State.RUNNING
                    }
                }.distinctUntilChanged()
        }
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    private val hanimeName by inputData(HANIME_NAME, EMPTY_STRING)
    private val downloadUrl by inputData(DOWNLOAD_URL, EMPTY_STRING)
    private val videoType by inputData(VIDEO_TYPE, HFileManager.DEF_VIDEO_TYPE)
    private val quality by inputData(QUALITY, EMPTY_STRING)
    private val videoCode by inputData(VIDEO_CODE, EMPTY_STRING)
    private val coverUrl by inputData(COVER_URL, EMPTY_STRING)

    private val fastPathCancel by inputData(FAST_PATH_CANCEL, false)
    private val shouldDelete by inputData(DELETE, false)
    private val shouldRedownload by inputData(REDOWNLOAD, false)
    private val isInWaitingQueue by inputData(IN_WAITING_QUEUE, false)

    private val downloadId = Random.nextInt()

    private val mainScope = CoroutineScope(Dispatchers.Main.immediate)
    private val dbScope = CoroutineScope(Dispatchers.IO)

    override suspend fun doWork(): Result {
        if (fastPathCancel) return Result.success()
        setForeground(createForegroundInfo())
        return download()
    }

    private suspend fun createNewRaf(file: File): HanimeDownloadEntity? {
        return withContext(Dispatchers.IO) {
            var os: OutputStream? = null
            var raf: RandomAccessFile? = null
            var response: Response? = null
            var body: ResponseBody? = null
            try {
                // SAF 优先
                val safUri = SafFileManager.getDownloadVideoFileUri(context, videoCode, createVideoName(hanimeName, quality, videoType))
                Log.i(TAG,safUri.toString())
                if (safUri != null) {
                    os = context.contentResolver.openOutputStream(safUri, "rwt")
                } else {
                    file.createFileIfNotExists()
                    raf = RandomAccessFile(file, "rwd")
                }

                val request = Request.Builder().url(downloadUrl).get().build()
                response = ServiceCreator.downloadClient.newCall(request).await()
                if (response.isSuccessful) {
                    body = response.body
                    body?.let { responseBody ->
                        val len = responseBody.contentLength()
                        // 创建数据库记录
                        val entity = HanimeDownloadEntity(
                            coverUrl = coverUrl,
                            coverUri = null,
                            title = hanimeName,
                            addDate = System.currentTimeMillis(),
                            videoCode = videoCode,
                            videoUri = safUri?.toString() ?: file.toUri().toString(),
                            quality = quality,
                            videoUrl = downloadUrl,
                            length = len,
                            downloadedLength = 0,
                            state = DownloadState.Queued
                        )
                        DatabaseRepo.HanimeDownload.insert(entity)
                        // 预写入长度（只有 File 支持）
                        raf?.setLength(len)
                        return@withContext entity
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (file.exists() && file.length() == 0L) {
                    dbScope.launch {
                        HFileManager.getDownloadVideoFolder(context, videoCode).deleteRecursively()
                    }
                }
            } finally {
                os?.closeQuietly()
                raf?.closeQuietly()
                response?.closeQuietly()
                body?.closeQuietly()
            }
            null
        }
    }

    private suspend fun download(): Result {
        return withContext(Dispatchers.IO) {
            val file = HFileManager.getDownloadVideoFile(
                context=context, title = hanimeName, quality=quality, suffix = videoType, videoCode = videoCode
            )
            val safUri = SafFileManager.getDownloadVideoFileUri(context, videoCode, createVideoName(hanimeName, quality, videoType))
            // 检查是否需要重下载
            if (shouldRedownload || shouldDelete) {
                HFileManager.getDownloadVideoFolder(context,videoCode).deleteRecursively()
                DatabaseRepo.HanimeDownload.delete(videoCode)
                if (shouldDelete) {
                    return@withContext Result.success()
                }
            }
            val entity = DatabaseRepo.HanimeDownload.find(videoCode, quality) ?: run {
                createNewRaf(file)
                DatabaseRepo.HanimeDownload.find(videoCode, quality)
                    ?: return@withContext run {
                        Log.d(TAG, "entity is null, create new raf failed")
                        showFailureNotification(context.getString(R.string.get_file_info_failed))
                        mainScope.launch {
                            showShortToast(
                                context.getString(R.string.download_task_failed_s, hanimeName)
                            )
                        }
                        Result.failure()
                    }
            }

            if (entity.coverUri == null) {
                updateCoverImage(entity)
            }
            if (isInWaitingQueue) {
                DatabaseRepo.HanimeDownload.update(
                    entity.copy(state = DownloadState.Queued)
                )
                return@withContext Result.success()
            }

            var downloadedLength = entity.downloadedLength
            val needRange = downloadedLength > 0 && safUri == null // SAF 下不支持断点续传
            var raf: RandomAccessFile? = null
            var outputStream: OutputStream? = null
            var response: Response? = null
            var body: ResponseBody? = null
            var bodyStream: InputStream? = null

            var result: Result = Result.failure()

            try {
                if (safUri != null) {
                    outputStream = context.contentResolver.openOutputStream(safUri, "rwt")
                } else {
                    raf = RandomAccessFile(file, "rwd")
                    if (needRange) raf.seek(downloadedLength)
                }
                val requestBuilder = Request.Builder().url(downloadUrl).get()
                if (needRange) requestBuilder.header("Range", "bytes=$downloadedLength-")
                val request = requestBuilder.build()
                response = ServiceCreator.downloadClient.newCall(request).await()
                val canWrite = (safUri != null) || ((needRange && response.code == 206) || (!needRange && response.isSuccessful))
                if (!canWrite) {
                    showFailureNotification(response.message)
                    mainScope.launch { showShortToast(context.getString(R.string.download_task_failed_s, hanimeName)) }
                    return@withContext Result.failure(workDataOf(DownloadState.STATE to DownloadState.Failed.mask))
                }

                body = response.body
                bodyStream = body?.byteStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var len: Int = bodyStream?.read(buffer) ?: -1
                var delayTime = 0L

                while (len != -1) {
                    if (raf != null) {
                        raf.write(buffer, 0, len)
                    } else outputStream?.write(buffer, 0, len)
                    downloadedLength += len

                    if (System.currentTimeMillis() - delayTime > RESPONSE_INTERVAL) {
                        val progress = downloadedLength * 100 / entity.length
                        setProgress(workDataOf(PROGRESS to progress.toInt()))
                        updateDownloadNotification(progress.toInt())
                        DatabaseRepo.HanimeDownload.update(
                            entity.copy(downloadedLength = downloadedLength,
                                state = DownloadState.Downloading
                            )
                        )
                        delayTime = System.currentTimeMillis()
                    }
                    len = bodyStream?.read(buffer) ?: -1
                }

                showSuccessNotification()
                result = Result.success(
                    workDataOf(DownloadState.STATE to DownloadState.Finished.mask)
                )

            } catch (e: Exception) {
                result = if (e is CancellationException) {
                    cancelDownloadNotification()
                    Result.success(
                        workDataOf(DownloadState.STATE to DownloadState.Paused.mask)
                    )
                } else {
                    showFailureNotification(e.localizedMessage)
                    e.printStackTrace()
                    mainScope.launch {
                        showShortToast(e.localizedMessage)
                    }
                    Result.failure(
                        workDataOf(DownloadState.STATE to DownloadState.Failed.mask)
                    )
                }
            } finally {
                dbScope.launch {
                    val state = DownloadState.from(
                        result.outputData.getInt(DownloadState.STATE, DownloadState.Unknown.mask)
                    )
                    DatabaseRepo.HanimeDownload.update(
                        entity.copy(
                            state = state,
                            downloadedLength = downloadedLength
                        )
                    )
                }
                raf?.closeQuietly()
                outputStream?.closeQuietly()
                response?.closeQuietly()
                body?.closeQuietly()
                bodyStream?.closeQuietly()
            }
            return@withContext result
        }
    }

    private fun CoroutineScope.updateCoverImage(entity: HanimeDownloadEntity) {
        launch {
            val imgRes = HImageMeower.execute(entity.coverUrl)
            val (os, uri) = SafFileManager.openOutputStreamForCover(
                context, entity.videoCode, entity.title
            )
            val isSuccess = os?.use { out -> imgRes.drawable?.saveTo(out) == true } ?: false
            if (isSuccess && uri != null) {
                val coverUriStr = uri.toString()
                withContext(Dispatchers.IO) {
                    DatabaseRepo.HanimeDownload.update(
                        entity.copy(coverUri = coverUriStr)
                    )
                }
                entity.coverUri = coverUriStr
            }
        }
    }

    private fun createDownloadNotification(progress: Int = 0): Notification {
        return NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentTitle(context.getString(R.string.downloading_s, hanimeName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentText("$progress%")
            .setProgress(100, progress, false)
            .build()
    }

    private fun cancelDownloadNotification() {
        notificationManager.cancel(downloadId)
    }

    @SuppressLint("MissingPermission")
    private fun updateDownloadNotification(progress: Int) {
        notificationManager.notify(downloadId, createDownloadNotification(progress))
    }

    private fun createForegroundInfo(progress: Int = 0): ForegroundInfo {
        val notification = createDownloadNotification(progress)
        return ForegroundInfo(
            downloadId, notification,
            // #issue-34: 這裡的參數是為了讓 Android 14 以上的系統可以正常顯示前景通知
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else 0
        )
    }

    @SuppressLint("MissingPermission")
    private fun showSuccessNotification() {
        notificationManager.notify(
            downloadId, NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_baseline_check_circle_24)
                .setContentTitle(context.getString(R.string.download_task_completed))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentText(context.getString(R.string.download_completed_s, hanimeName))
                .build()
        )
    }

    @SuppressLint("MissingPermission")
    private fun showFileExistsFailureNotification(fileName: String) {
        notificationManager.notify(
            downloadId, NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_baseline_cancel_24)
                .setContentTitle(context.getString(R.string.this_data_exists))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(context.getString(R.string.download_failed_s_exists, fileName))
                .build()
        )
    }

    @SuppressLint("MissingPermission")
    private fun showFailureNotification(errMsg: String? = null) {
        notificationManager.notify(
            downloadId, NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_baseline_cancel_24)
                .setContentTitle(context.getString(R.string.download_task_failed))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentText(
                    context.getString(
                        R.string.download_task_failed_s_reason_s,
                        hanimeName, errMsg ?: context.getString(R.string.unknown_download_error)
                    )
                )
                .build()
        )
    }
}