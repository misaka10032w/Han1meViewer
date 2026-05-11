package com.yenaly.han1meviewer.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.preference.PreferenceManager
import com.yenaly.han1meviewer.ui.screen.login.ManualInputCookiesScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import java.util.Locale

class ManualInputCookiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HanimeTheme {
                ManualInputCookiesScreen(
                    onBack = { finish() },
                    onCookieScanned = { scannedCookie ->
                        val resultIntent = Intent().apply {
                            putExtra("cookie", scannedCookie)
                            Log.i("LoginActivity", scannedCookie)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                )
            }
        }
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
