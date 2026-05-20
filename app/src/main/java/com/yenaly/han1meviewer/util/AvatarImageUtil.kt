package com.yenaly.han1meviewer.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

suspend fun Context.createSquareAvatarFile(sourceUri: Uri): File? {
    return kotlin.runCatching {
        val bitmap = contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null
        val size = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        val squareBitmap = Bitmap.createBitmap(bitmap, left, top, size, size)
        val avatarFile = File(cacheDir, "avatar_upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(avatarFile).use { output ->
            squareBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        if (squareBitmap != bitmap) {
            squareBitmap.recycle()
        }
        bitmap.recycle()
        avatarFile
    }.getOrNull()
}
