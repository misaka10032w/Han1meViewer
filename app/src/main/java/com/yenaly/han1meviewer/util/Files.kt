package com.yenaly.han1meviewer.util

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.IntRange
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.net.toUri
import com.yenaly.han1meviewer.FILE_PROVIDER_AUTHORITY
import com.yenaly.han1meviewer.HFileManager
import com.yenaly.han1meviewer.HJson
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale

@Deprecated(
    "Use alternative",
    ReplaceWith(
        "HFileManager.createVideoName(title, quality, suffix)",
        imports = ["com.yenaly.han1meviewer.HFileManager"]
    )
)
fun createDownloadName(title: String, quality: String, suffix: String = HFileManager.DEF_VIDEO_TYPE) =
    "${title}_${quality}.${suffix}"

@Deprecated(
    "Use alternative",
    ReplaceWith(
        "HFileManager.getDownloadVideoFile(videoCode, title, quality, suffix)",
        imports = ["com.yenaly.han1meviewer.HFileManager"]
    )
)
fun getDownloadedHanimeFile(title: String, quality: String, suffix: String = HFileManager.DEF_VIDEO_TYPE): File {
    return File(HFileManager.getAppDownloadFolder(application),
        HFileManager.createVideoName(title, quality, suffix)
    )
}

@Deprecated("不用了")
fun checkDownloadedHanimeFile(startsWith: String): Boolean {
    return HFileManager.getAppDownloadFolder(application).let { folder ->
        folder.listFiles()?.any { it.name.startsWith(startsWith) }
    } == true
}

/**
 * Must be Activity Context!
 */
fun Context.openDownloadedHanimeVideoLocally(
    uri: String, onFileNotFound: (() -> Unit)? = null,
) {
    val videoUri = uri.toUri()
    if (videoUri.scheme == ContentResolver.SCHEME_CONTENT) {
        val resolver = contentResolver
        try {
            resolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
                if (pfd.statSize <= 0) {
                    onFileNotFound?.invoke()
                    return
                }
            }
        } catch (_: Exception) {
            onFileNotFound?.invoke()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(videoUri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            GlobalToasts.show(getString(R.string.action_not_support), level = GlobalToasts.ToastLevel.WARNING)
            e.printStackTrace()
        }
    } else {
        val videoFile = File(videoUri.path ?: "")
        if (!videoFile.exists()) {
            onFileNotFound?.invoke()
            return
        }
        val fileUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, videoFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            GlobalToasts.show(getString(R.string.action_not_support), level = GlobalToasts.ToastLevel.WARNING)
            e.printStackTrace()
        }
    }
}

/**
 * copyTo with progress
 */
suspend fun InputStream.copyTo(
    out: OutputStream,
    contentLength: Long,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    progress: (suspend (Int, Long, Long) -> Unit)? = null,
): Long {
    return withContext(Dispatchers.IO) {
        this@copyTo.use {
            var bytesCopied: Long = 0
            val buffer = ByteArray(bufferSize)
            var bytes = read(buffer)
            var percent = 0
            while (bytes >= 0) {
                ensureActive()
                out.write(buffer, 0, bytes)
                bytesCopied += bytes
                if (contentLength > 0) {
                    val newPercent = (bytesCopied * 100 / contentLength).toInt()
                    if (newPercent != percent) {
                        percent = newPercent
                        progress?.invoke(percent.coerceAtMost(100), contentLength, bytesCopied)
                    }
                }
                bytes = read(buffer)
            }
            Log.i("progress",bytesCopied.toString())
            bytesCopied
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> loadAssetAs(filePath: String): T? = runCatching {
    applicationContext.assets.open(filePath).use { inputStream ->
        HJson.decodeFromStream<T>(inputStream)
    }
}.getOrNull()

/**
 * 递归计算文件夹占用的总字节数。
 */
val File?.folderSize: Long
    get() {
        var size = 0L
        this?.listFiles()?.forEach { file ->
            size += if (file.isDirectory) file.folderSize else file.length()
        }
        return size
    }

/**
 * 文件不存在时创建文件及其父目录。
 *
 * @return 文件最终是否存在且为普通文件
 */
fun File.createFileIfNotExists(): Boolean {
    return if (!exists()) {
        parentFile?.mkdirs()
        createNewFile()
    } else {
        isFile
    }
}

/**
 * 将 Drawable 压缩后写入输出流。
 *
 * @return 是否写入成功
 */
fun Drawable.saveTo(
    outputStream: OutputStream,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 100
): Boolean {
    return toBitmapOrNull()?.saveTo(outputStream, format, quality) == true
}

/**
 * 将 Bitmap 压缩后写入输出流。
 *
 * @return 是否写入成功
 */
fun Bitmap.saveTo(
    outputStream: OutputStream,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 100
): Boolean {
    return try {
        outputStream.buffered().use { stream ->
            compress(format, quality, stream)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private val SI_UNITS = arrayOf("B", "kB", "MB", "GB", "TB")
private val IEC_UNITS = arrayOf("B", "KiB", "MiB", "GiB", "TiB")

/**
 * 将字节数格式化为带单位的文件大小文本。
 *
 * @param useSi 使用十进制单位（1000），默认使用二进制单位（1024）
 * @param decimalPlaces 保留的小数位数
 * @param stripTrailingZeros 能整除时是否省略小数部分
 */
fun Long.formatFileSizeV2(
    useSi: Boolean = false,
    @IntRange(from = 0) decimalPlaces: Int = 1,
    stripTrailingZeros: Boolean = true,
): String {
    val unit = if (useSi) 1000 else 1024
    if (this < unit) return "$this B"

    val units = if (useSi) SI_UNITS else IEC_UNITS

    var value = this.toDouble()
    var unitIndex = 0

    while (value >= unit && unitIndex < units.size - 1) {
        value /= unit
        unitIndex++
    }

    return if (decimalPlaces == 0 || (stripTrailingZeros && value % 1 == 0.0)) {
        "%.0f %s".format(Locale.getDefault(), value, units[unitIndex])
    } else {
        "%.${decimalPlaces}f %s".format(Locale.getDefault(), value, units[unitIndex])
    }
}

/**
 * 将字节数格式化为带单位的速度文本，例如 `1.2 MB/s`。
 *
 * @see formatFileSizeV2
 */
fun Long.formatBytesPerSecond(
    useSi: Boolean = false,
    @IntRange(from = 0) decimalPlaces: Int = 1,
    stripTrailingZeros: Boolean = true,
): String = formatFileSizeV2(useSi, decimalPlaces, stripTrailingZeros) + "/s"
