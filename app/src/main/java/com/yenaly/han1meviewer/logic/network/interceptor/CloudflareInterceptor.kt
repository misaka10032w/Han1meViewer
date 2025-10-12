package com.yenaly.han1meviewer.logic.network.interceptor

import android.content.Context
import android.content.Intent
import com.yenaly.han1meviewer.ui.activity.CloudflareActivity
import okhttp3.Interceptor
import okhttp3.Response

class CloudflareInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 403 && response.header("cf-mitigated") == "challenge") {
            response.close()
            val url = request.url.toString()

            // 用同步锁等待验证完成
            val lock = Object()
            var finished = false

            CloudflareActivity.onFinished = {
                synchronized(lock) {
                    finished = true
                    lock.notifyAll()
                }
            }

            val intent = Intent(context, CloudflareActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(CloudflareActivity.EXTRA_URL, url)
            context.startActivity(intent)

            // 等待 WebView 验证完成
            synchronized(lock) {
                while (!finished) {
                    lock.wait()
                }
            }
            return chain.proceed(request)
        }
        return response
    }
}


