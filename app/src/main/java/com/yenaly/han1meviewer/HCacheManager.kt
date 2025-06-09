package com.yenaly.han1meviewer

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.FileOutputStream
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
        val folder = HFileManager.getDownloadVideoFolder(context, videoCode)
        val file = File(folder, CACHE_INFO_FILE)
        try{
//        file.createFileIfNotExists()
//        HJson.encodeToStream(info, file.outputStream())
            file.atomicWrite { outputStream ->
                HJson.encodeToStream(info, outputStream)
            }
            Log.i("FileSave", "Save video info OK: ${file.absolutePath}")
        }catch (e:Exception){
            Log.e("FileSave", "Failed to save video info: ${file.absolutePath}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        }
    }

    //缓解写入冲突
    private fun File.atomicWrite(block: (OutputStream) -> Unit) {
        parentFile?.mkdirs()
        val tempFile = File("$absolutePath.tmp")
        try {
            FileOutputStream(tempFile).use { fos ->
                block(fos)
                fos.fd.sync()
            }
            tempFile.renameTo(this)
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