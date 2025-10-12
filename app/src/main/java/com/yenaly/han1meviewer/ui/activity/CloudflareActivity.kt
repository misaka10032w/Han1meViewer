package com.yenaly.han1meviewer.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.yenaly.han1meviewer.Preferences.cloudFlareCookie
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.USER_AGENT
import com.yenaly.han1meviewer.databinding.ActivityCloudflareBinding
import com.yenaly.han1meviewer.util.CookieString

class CloudflareActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "request_url"
        var onFinished: (() -> Unit)? = null
    }

    private lateinit var binding: ActivityCloudflareBinding
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityCloudflareBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeActionContentDescription(R.string.back)
        }

        webView = binding.wvCloudflare
        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }

       binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        initWebview(url, webView)
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebview(url: String, webView: WebView){
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            javaScriptCanOpenWindowsAutomatically = true
            userAgentString = USER_AGENT
        }

        val cookieManager = CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript("document.head.innerHTML") { html ->
                    if (!html.contains("#challenge-form") &&
                        !html.contains("#challenge-success-text") &&
                        !html.contains("#challenge-error-text")
                    ) {
                        val cookies = cookieManager.getCookie(url) ?: ""
                        if (cookies.contains("cf_clearance")) {
                            cloudFlareCookie = CookieString(cookies)
                            Log.i("CloudflareActivity",cookies)
                            cookieManager.flush()
                            onFinished?.invoke()
                            onFinished = null
                            finish()
                        }
                    }
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                binding.progressBar.progress = newProgress
            }
        }
        webView.loadUrl(url)
    }
    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}

