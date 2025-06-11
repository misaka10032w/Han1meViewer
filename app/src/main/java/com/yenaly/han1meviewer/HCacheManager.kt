package com.yenaly.han1meviewer

import android.content.Context
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.yenaly.han1meviewer.HFileManager.getDownloadVideoFolder
import com.yenaly.han1meviewer.HFileManager.setUsePrivateDownloadFolder
import com.yenaly.han1meviewer.HFileManager.shouldUsePrivateDownloadFolder
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.utils.application
import com.yenaly.yenaly_libs.utils.browse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @since 2025/3/5 20:11
 */
object HCacheManager {

    private const val CACHE_INFO_FILE = "info.json"

    /**
     * 保存 HanimeVideo 信息，用于下载后直接在 APP 内观看
     */
    val crashlytics = FirebaseCrashlytics.getInstance()
    @OptIn(ExperimentalSerializationApi::class)
    @WorkerThread
    fun saveHanimeVideoInfo(context: Context, videoCode: String, info: HanimeVideo) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        val folder = getDownloadVideoFolder(context, videoCode)
        val file = File(folder, CACHE_INFO_FILE)

        crashlytics.setCustomKey("video_info", info.toString())
        crashlytics.setCustomKey("file_path", file.absolutePath)

        try {
            file.atomicWrite { outputStream ->
                HJson.encodeToStream(info, outputStream)
            }
            Log.i("FileSave", "✅ Save video info OK: ${file.absolutePath}")
        } catch (e: IOException) {
            val errorMsg = e.message.orEmpty()
            val isPathRelatedError = (errorMsg.contains("ENOENT") ||
                    errorMsg.contains("EEXIST") ||
                    errorMsg.contains("Permission denied")||
                    errorMsg.contains("failed"))
            val shouldSwitch = isPathRelatedError && !shouldUsePrivateDownloadFolder(context)

            if (shouldSwitch) {
                notify(context)
                Log.w("FileSave", "⛔ 写入失败 (${e.message})，切换为私有路径")
                setUsePrivateDownloadFolder(context, true)
                return saveHanimeVideoInfo(context, videoCode, info) // ⬅️ 重试一次
            }
            Log.e("FileSave", "❌ Save video info failed: ${file.absolutePath}", e)
            crashlytics.log("Save failed: ${e.message}")
            crashlytics.recordException(e)
            throw e

        } catch (e: Exception) {
            Log.e("FileSave", "❌ Unexpected error: ${file.absolutePath}", e)
            crashlytics.log("Unexpected error: ${e.message}")
            crashlytics.recordException(e)
            throw e
        }
    }
    fun notify(context: Context) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showSwitchDialog(context)
        } else {
            MainScope().launch {
                showSwitchDialog(context)
            }
        }
    }
    private fun showSwitchDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("保存失败")
            .setMessage("由于外部存储权限或其他原因，视频信息文件保存失败。\n已自动切换到应用私有路径。\n如果你是卸载重新" +
                    "安装的应用可尝试清理Download/Han1meViewer目录后在设置切换下载位置再试，否则将继续使用私有目录，并请在卸" +
                    "载或清除数据之前移出文件！")
            .setPositiveButton("知道了", null)
            .show()
    }
//    fun saveHanimeVideoInfo(context: Context, videoCode: String, info: HanimeVideo) {
//        Log.i("saveHanimeVideoInfo","info:$info\nvideocode:$videoCode")
//        val folder = HFileManager.getDownloadVideoFolder(context, videoCode)
//        val file = File(folder, CACHE_INFO_FILE)
//        crashlytics.setCustomKey("video_info", info.toString())
//        crashlytics.setCustomKey("file_path", file.absolutePath)
//        try{
////        file.createFileIfNotExists()
////        HJson.encodeToStream(info, file.outputStream())
//            file.atomicWrite { outputStream ->
//                HJson.encodeToStream(info, outputStream)
//            }
//            Log.i("FileSave", "Save video info OK: ${file.absolutePath}")
//        }catch (e:Exception){
//            Log.e("FileSave", "Failed to save video info: ${file.absolutePath}", e)
//            crashlytics.log("Failed to write file: ${e.message}")
//            crashlytics.recordException(e)
//            throw e
//        }
//    }

    //缓解写入冲突
    private fun File.atomicWrite(block: (OutputStream) -> Unit) {
        parentFile?.mkdirs()
        val tempFile = File("$absolutePath.tmp")
        if (tempFile.exists()) tempFile.delete()
        if (this.exists()) this.delete()
        try {
            FileOutputStream(tempFile,false).use { fos ->
                block(fos)
                fos.fd.sync()
            }
            if (!tempFile.renameTo(this)) {
                tempFile.copyTo(this, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }



    /**
     * 加载 HanimeVideo 信息，用于下载后直接在 APP 内观看
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun loadHanimeVideoInfo(context: Context, videoCode: String): Flow<HanimeVideo?> {
        return flow {
            val entity = DatabaseRepo.HanimeDownload.find(videoCode)
            if (entity != null) {
                val folder = HFileManager.getDownloadVideoFolder(context, videoCode)
                val cacheFile = File(folder, CACHE_INFO_FILE)
                val info = kotlin.runCatching {
                    if (cacheFile.exists()) HJson.decodeFromStream<HanimeVideo?>(cacheFile.inputStream()) else null
                }.getOrNull()
                emit(
                    info?.copy(
                        videoUrls = linkedMapOf(
                            entity.quality to HanimeLink(
                                entity.videoUri, HFileManager.DEF_VIDEO_TYPE
                            )
                        ),
                        coverUrl = entity.coverUri ?: entity.coverUrl
                    )
                )
            } else {
                emit(null)
            }
        }.flowOn(Dispatchers.IO)
    }
}