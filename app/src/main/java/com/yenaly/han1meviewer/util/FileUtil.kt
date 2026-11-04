package com.yenaly.han1meviewer.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmapOrNull
import java.io.File
import java.io.OutputStream

val File?.folderSize: Long
    get() {
        var size = 0L
        this?.listFiles()?.forEach { file ->
            size += if (file.isDirectory) file.folderSize else file.length()
        }
        return size
    }

fun File.createFileIfNotExists(): Boolean {
    return if (!exists()) {
        parentFile?.mkdirs()
        createNewFile()
    } else {
        isFile
    }
}

fun Drawable.saveTo(
    outputStream: OutputStream,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 100
): Boolean {
    return toBitmapOrNull()?.saveTo(outputStream, format, quality) == true
}

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
