package com.yenaly.han1meviewer.base

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

open class BaseFragmentActivity : AppCompatActivity() {

    open fun setUiStyle() {
        enableEdgeToEdge()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setUiStyle()
        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }
}
