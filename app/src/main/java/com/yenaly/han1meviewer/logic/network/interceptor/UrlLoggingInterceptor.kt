package com.yenaly.han1meviewer.logic.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class UrlLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name())
        Log.i("NetworkRequest",decodedUrl)
        return chain.proceed(request)
    }
}