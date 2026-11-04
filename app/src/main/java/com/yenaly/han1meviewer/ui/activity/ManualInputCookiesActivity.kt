package com.yenaly.han1meviewer.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import com.yenaly.han1meviewer.base.BaseActivity
import com.yenaly.han1meviewer.ui.screen.login.ManualInputCookiesScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme

class ManualInputCookiesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
