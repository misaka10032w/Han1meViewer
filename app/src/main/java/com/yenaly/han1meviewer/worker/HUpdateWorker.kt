package com.yenaly.han1meviewer.worker

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.FILE_PROVIDER_AUTHORITY
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.UPDATE_NOTIFICATION_CHANNEL
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.logic.network.HUpdater
import com.yenaly.han1meviewer.util.installApkPackage
import com.yenaly.han1meviewer.util.runSuspendCatching
import com.yenaly.han1meviewer.util.updateFile
import com.yenaly.yenaly_libs.utils.showShortToast
import java.io.File
import java.util.Locale
import kotlin.random.Random

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/22 022 21:27
 */
class HUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams), WorkerMixin {
    companion object {
        const val TAG = "HUpdateWorker"

        const val DOWNLOAD_LINK = "download_link"
        const val NODE_ID = "node_id"
        const val UPDATE_APK = "update_apk"

        /**
         * This function is used to enqueue a download task
         */
        fun enqueue(context: Context, latest: Latest) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val data = workDataOf(
                DOWNLOAD_LINK to latest.downloadLink,
                NODE_ID to latest.nodeId,
            )
            val req = OneTimeWorkRequestBuilder<HUpdateWorker>()
                .addTag(TAG)
                .setConstraints(constraints)
                .setInputData(data)
                .build()
            WorkManager.getInstance(context)
                .beginUniqueWork(TAG, ExistingWorkPolicy.REPLACE, req)
                .enqueue()
        }

        /**
         * This function is used to collect the output of the download task
         */
        suspend fun collectOutput(context: Context) = WorkManager.getInstance(context)
            .getWorkInfosByTagFlow(TAG)
            .collect { workInfos ->
                // 只有一個！
                val workInfo = workInfos.firstOrNull()
                workInfo?.let {
                    when (it.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val apkPath = it.outputData.getString(UPDATE_APK)
                            val file = apkPath?.toUri()?.toFile()
                            file?.let { context.installApkPackage(file) }
                        }

                        WorkInfo.State.FAILED -> {
                            showShortToast(R.string.update_failed)
                        }

                        else -> Unit
                    }
                }
            }
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    private val downloadLink by inputData(DOWNLOAD_LINK, EMPTY_STRING)
    private val nodeId by inputData(NODE_ID, EMPTY_STRING)
    private val downloadId = Random.nextInt()

    override suspend fun doWork(): Result {
        with(HUpdater) {
            val file = context.updateFile.apply { delete() }
            val inject = runSuspendCatching {
                setForeground(createForegroundInfo())
                file.injectUpdate(downloadLink) { progress, fileSize, downloadedSize ->
                    updateNotification(progress, fileSize, downloadedSize)
                }
            }
            if (inject.isSuccess) {
                val outputData = workDataOf(UPDATE_APK to file.toUri().toString())
                Preferences.updateNodeId = nodeId
                showInstallNotification(file)
                return Result.success(outputData)
            } else {
                inject.exceptionOrNull()?.printStackTrace()
                file.delete()
                return Result.failure()
            }
        }
    }

    private fun createNotification(progress: Int = 0, fileSizeMB: String = "0", downloadedSizeMB: String = "0", isPending: Boolean = true): Notification {
        return NotificationCompat.Builder(context, UPDATE_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher_new)
            .setOngoing(true)
            .setContentTitle(
                context.getString(
                    R.string.downloading_update_percent, progress
                ) + " ($downloadedSizeMB MB / $fileSizeMB MB)"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, isPending)
            .build()
    }

    private var lastNotifyTime = 0L  // 节流，通知更新太快Android会抛异常
    @SuppressLint("MissingPermission")
    private fun updateNotification(progress: Int, fileSize: Long, downloadedSize: Long) {
        val now = System.currentTimeMillis()
        if (progress == 100 || now - lastNotifyTime > 1000) {
            lastNotifyTime = now
            val fileSizeMB = String.format(Locale.US, "%.2f", fileSize.toDouble() / (1024 * 1024))
            val downloadedSizeMB = String.format(Locale.US, "%.2f", downloadedSize.toDouble() / (1024 * 1024))
            notificationManager.notify(
                downloadId,
                createNotification(
                    progress,
                    fileSizeMB,
                    downloadedSizeMB,
                    false
                )
            )
        }
    }

    private fun createForegroundInfo(progress: Int = 0): ForegroundInfo {
        return ForegroundInfo(
            downloadId,
            createNotification(progress, isPending = false),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else 0
        )
    }
    @SuppressLint("MissingPermission")
    private fun showInstallNotification(file: File) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            val uri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                file
            )
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, UPDATE_NOTIFICATION_CHANNEL)
            .setContentTitle(context.getString(R.string.download_complete))
            .setContentText(context.getString(R.string.click_to_install_update))
            .setSmallIcon(R.mipmap.ic_launcher_new)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.mipmap.ic_launcher_new,
                    context.getString(R.string.install),
                    pendingIntent
                ).build()
            )
            .build()
        notificationManager.notify(downloadId + 1, notification)
    }
}