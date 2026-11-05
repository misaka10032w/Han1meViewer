package com.yenaly.han1meviewer.base

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

/**
 * Compose 页面使用的 Activity 基类，统一处理语言切换与 edge-to-edge。
 */
open class BaseActivity : ComponentActivity() {

    /**
     * 设置界面样式。默认启用 edge-to-edge，子类可按需覆盖。
     */
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

/**
 * 需要 Fragment 能力的 Activity 基类，例如使用 BiometricPrompt 的页面。
 */
open class BaseFragmentActivity : AppCompatActivity() {

    /**
     * 设置界面样式。默认启用 edge-to-edge，子类可按需覆盖。
     */
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
