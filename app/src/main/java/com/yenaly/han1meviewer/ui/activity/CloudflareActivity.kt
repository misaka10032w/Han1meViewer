package com.yenaly.han1meviewer.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.yenaly.han1meviewer.Preferences.cloudFlareCookie
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.USER_AGENT
import com.yenaly.han1meviewer.databinding.ActivityCloudflareBinding
import com.yenaly.han1meviewer.util.CookieString
import java.util.Locale

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
        try {
            binding = ActivityCloudflareBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.webview_exception), Toast.LENGTH_LONG).show()
            finish()
            return
        }
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

        webView.webViewClient = object : WebViewClient(){
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }

        webView.evaluateJavascript("navigator.userAgent") { output ->
            val userAgent = output
                .removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
            val chromePattern = "Chrome/(\\d+\\.\\d+\\.\\d+\\.\\d+)".toRegex()
            val versionCode = chromePattern.find(userAgent)?.groupValues?.getOrNull(1) ?: userAgent
            runOnUiThread {
                binding.tipText.append(getString(R.string.current_webview_version,versionCode))
                try {
                    val parts = versionCode.split(".").map { it.toIntOrNull() ?: 0 }
                    if (parts.size >= 4) {
                        val (major, minor, patch, build) = parts
                        if (major < 120 ) {
                            binding.tipText.append(getString(R.string.webview_version_too_low))
                        }
                    } else {
                        binding.tipText.append(getString(R.string.webview_version_unknown))
                    }
                } catch (e: Exception) {
                    binding.tipText.append(getString(R.string.version_check_failed))
                }
            }
            Log.i("webViewVersion", "Version: $versionCode")
            Log.i("webViewVersion", "UserAgent: $userAgent")
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                Log.d("CFWebViewDebug", "加载进度: $newProgress")
                binding.progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                binding.progressBar.progress = newProgress
                if (newProgress >=90){
                    view?.postDelayed({
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
                    },1000)
                }
            }
        }
        webView.loadUrl(url)
    }
    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
    private fun applyAppLocale(context: Context): Context {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lang = prefs.getString("app_language", "system") ?: "system"

        val newLocale = when (lang) {
            "zh-rCN" -> Locale.SIMPLIFIED_CHINESE
            "zh" -> Locale.TRADITIONAL_CHINESE
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            else -> Resources.getSystem().configuration.locales.get(0)
        }

        Locale.setDefault(newLocale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(newLocale)
        return context.createConfigurationContext(config)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyAppLocale(newBase))
    }
}

