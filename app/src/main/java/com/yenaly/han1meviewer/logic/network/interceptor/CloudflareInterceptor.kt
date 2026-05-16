package com.yenaly.han1meviewer.logic.network.interceptor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.yenaly.han1meviewer.ui.activity.CloudflareActivity
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.CountDownLatch

class CloudflareInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 403 && response.header("cf-mitigated") == "challenge") {
            response.close()
            val url = request.url.toString()

            // 用 CountDownLatch 代替同步锁
            val latch = CountDownLatch(1)

            // 设置回调，用户验证完成后调用
            CloudflareActivity.onFinished = {
                latch.countDown()
            }

            val intent = Intent(context, CloudflareActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(CloudflareActivity.EXTRA_URL, url)
            try {
                context.startActivity(intent)
            } catch (e: Exception){
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context.applicationContext,
                        "cf启动失败错误: ${e.javaClass.simpleName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                CloudflareActivity.onFinished?.invoke()
            }

            // 等待 WebView 验证完成
            latch.await()
            return chain.proceed(request)
        }
        return response
    }
}


