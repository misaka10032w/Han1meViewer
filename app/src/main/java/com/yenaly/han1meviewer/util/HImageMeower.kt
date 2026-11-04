package com.yenaly.han1meviewer.util

import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.ImageResult
import com.yenaly.han1meviewer.logic.network.HDns
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Suppress("NOTHING_TO_INLINE")
object HImageMeower {

    private const val TAG = "CoilImageNyanner"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .dns(HDns())
        .build()

    private val imageLoader = ImageLoader.Builder(applicationContext)
        .okHttpClient(okHttpClient)
        .build()

    suspend fun execute(data: Any): ImageResult {
        Log.d(TAG, "execute: $data")
        return imageLoader.execute(
            ImageRequest.Builder(applicationContext).data(data).build()
        )
    }
}
