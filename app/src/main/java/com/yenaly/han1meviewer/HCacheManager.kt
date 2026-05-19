package com.yenaly.han1meviewer

import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys
import com.yenaly.han1meviewer.util.SafFileManager
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
    @OptIn(ExperimentalSerializationApi::class)
    @WorkerThread
    fun saveHanimeVideoInfo(context: Context, videoCode: String, info: HanimeVideo) {
        val folder = HFileManager.getDownloadVideoFolder(context, videoCode) // 已封装 SAF/普通路径
        val cacheFile = File(folder, CACHE_INFO_FILE)
        val cacheUri = SafFileManager.getDownloadVideoFileUri(context, videoCode, CACHE_INFO_FILE)

        try {
            if (cacheUri != null) {
                // --- SAF 写入 ---
                context.contentResolver.openOutputStream(cacheUri, "rwt")?.use { os ->
                    HJson.encodeToStream(info, os)
                    Log.d("FileSave", "✅ SAF write completed")
                } ?: throw IOException("无法打开 SAF Uri 输出流: $cacheUri")
            } else {
                Log.d("FileSave", "📝 Using regular file write method")
                // --- 普通文件写入 ---
                cacheFile.atomicWrite { outputStream ->
                    HJson.encodeToStream(info, outputStream)
                    Log.d("FileSave", "✅ Regular write completed")
                }
            }

            Log.i("FileSave", "✅ Save video info OK: ${cacheFile.absolutePath}\n${info}")

        } catch (e: IOException) {
            val errorMsg = e.message.orEmpty()
            val isPathRelatedError = (errorMsg.contains("ENOENT") ||
                    errorMsg.contains("EEXIST") ||
                    errorMsg.contains("Permission denied") ||
                    errorMsg.contains("failed"))
            val shouldSwitch = isPathRelatedError && !Preferences.isUsePrivateStorage

            if (shouldSwitch) {
                notify(context)
                Log.w("FileSave", "⛔ 写入失败 (${e.message})，切换为私有路径")
                Preferences.preferenceSp.edit {
                    putBoolean(SettingsPreferenceKeys.USE_PRIVATE_STORAGE, true)
                }
                return saveHanimeVideoInfo(context, videoCode, info) // ⬅️ 重试一次
            }

            Log.e("FileSave", "❌ Save video info failed: ${cacheFile.absolutePath}", e)
            throw e
        } catch (e: Exception) {
            Log.e("FileSave", "❌ Unexpected error: ${cacheFile.absolutePath}", e)
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
            .setTitle(context.getString(R.string.save_failed_title))
            .setMessage(context.getString(R.string.save_failed_message))
            .setPositiveButton(context.getString(R.string.understood), null)
            .show()
    }

    // 缓解写入冲突 (仅 File 模式下用)
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
                val cacheUri = SafFileManager.getDownloadVideoFileUri(context, videoCode, CACHE_INFO_FILE)
                val info = kotlin.runCatching {
                    when {
                        cacheUri != null -> context.contentResolver.openInputStream(cacheUri)?.use {
                            HJson.decodeFromStream<HanimeVideo?>(it)
                        }
                        cacheFile.exists() -> cacheFile.inputStream().use {
                            HJson.decodeFromStream<HanimeVideo?>(it)
                        }
                        else -> null
                    }
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
