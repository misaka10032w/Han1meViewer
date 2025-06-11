package com.yenaly.han1meviewer
import android.content.Context
import android.os.Environment
import android.util.Log
import com.yenaly.han1meviewer.util.HStorageModeManager
import com.yenaly.han1meviewer.util.HStorageModeManager.isUsingPrivateDownloadFolder
import java.io.File
import java.io.IOException

object HFileManager {

    const val HANIME_DOWNLOAD_FOLDER = "hanime_download"
    const val DEF_VIDEO_TYPE = "mp4"
    const val DEF_VIDEO_COVER_TYPE = "png"
    val illegalCharsRegex = Regex("""["*/:<>?\\|\x00-\x1F\x7F]""")


    /**
     * 获取 App 的下载主目录，如写入失败则切换为私有目录，不想写MediaStore，
     * 好烦，国产定制ROM也是了，滑为报No such file or directory，小米报File exists，NMD
     */
    fun getAppDownloadFolder(context: Context): File {
        return if (shouldUsePrivateDownloadFolder(context)) {
            // 私有路径
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APP_NAME).apply {
                makeFolderNoMedia()
            }
        } else {
            // 公共路径
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APP_NAME).apply {
                makeFolderNoMedia()
            }
        }
    }
    fun shouldUsePrivateDownloadFolder(context: Context): Boolean {
        return isUsingPrivateDownloadFolder(context)
    }

    fun setUsePrivateDownloadFolder(context: Context, usePrivate: Boolean) {
        HStorageModeManager.setUsePrivateDownloadFolder(context,usePrivate)
    }

    /**
     * 获取某视频的下载目录
     */
    fun getDownloadVideoFolder(context: Context, videoCode: String): File {
        val folder = File(getAppDownloadFolder(context), "$HANIME_DOWNLOAD_FOLDER/$videoCode")
        folder.makeFolderNoMedia()
        return folder
    }

    /**
     * 获取视频文件
     */
    fun getDownloadVideoFile(
        context: Context,
        videoCode: String,
        title: String,
        quality: String,
        suffix: String = DEF_VIDEO_TYPE
    ): File {
        return File(
            getDownloadVideoFolder(context, videoCode),
            createVideoName(title, quality, suffix)
        )
    }

    /**
     * 获取视频封面文件
     */
    fun getDownloadVideoCoverFile(
        context: Context,
        videoCode: String,
        title: String,
        suffix: String = DEF_VIDEO_COVER_TYPE
    ): File {
        return File(
            getDownloadVideoFolder(context, videoCode),
            createVideoCoverName(title, suffix)
        )
    }

    /**
     * 替换非法文件名字符
     */
    private fun String.replaceAllIllegalChars(): String {
        return illegalCharsRegex.replace(this, "_")
    }

    fun createVideoName(title: String, quality: String, suffix: String): String {
        return "${title.replaceAllIllegalChars()}_${quality}.$suffix"
    }

    private fun createVideoCoverName(title: String, suffix: String): String {
        return "${title.replaceAllIllegalChars()}.$suffix"
    }

    /**
     * 创建目录并在其中写入 .nomedia 文件，防止被媒体扫描器扫描到让你尴尬
     */
    private fun File.makeFolderNoMedia() {
        if (!exists()) {
            if (!mkdirs()) {
                Log.w("HFileManager", "⚠️ 目录创建失败: $absolutePath")
                return
            }
        } else if (!isDirectory) {
            Log.w("HFileManager", "⚠️ 已存在但不是文件夹: $absolutePath")
            return
        }

        val noMedia = File(this, ".nomedia")
        if (!noMedia.exists()) {
            try {
                noMedia.createNewFile()
            } catch (e: IOException) {
                Log.w("HFileManager", "⚠️ 创建 .nomedia 失败: ${e.message}")
            }
        }
    }
}
