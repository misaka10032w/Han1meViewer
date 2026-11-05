package com.yenaly.han1meviewer.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.net.toUri
import androidx.core.util.TypedValueCompat

/**
 * 全局 Application Context，由 [com.yenaly.han1meviewer.base.BaseInitializer] 初始化。
 */
lateinit var applicationContext: Context
    internal set

/**
 * 全局 Application 实例。
 */
val application: Application
    get() = applicationContext as Application

/**
 * 将 dp 数值转换为对应的像素值。
 */
val Number.dp: Int
    get() {
        val px = TypedValueCompat.dpToPx(toFloat(), applicationContext.resources.displayMetrics)
        return (if (px >= 0) px + 0.5f else px - 0.5f).toInt()
    }

/**
 * 应用可用屏幕宽度，单位像素。
 */
val appScreenWidth: Int
    get() = applicationContext.resources.displayMetrics.widthPixels

/**
 * 系统状态栏高度，单位像素。
 */
@get:SuppressLint("DiscouragedApi", "InternalInsetResource")
val statusBarHeight: Int
    get() {
        val resources = applicationContext.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

/**
 * 系统导航栏高度，单位像素。
 */
@get:SuppressLint("DiscouragedApi", "InternalInsetResource")
val navBarHeight: Int
    get() {
        val resources = applicationContext.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

/**
 * 从 Context 链中查找指定类型的 Activity，找不到时返回 null。
 */
inline fun <reified T : Activity> Context.findActivityOrNull(): T? {
    var context = this
    while (context is ContextWrapper) {
        if (context is T) {
            return context
        }
        context = context.baseContext
    }
    return null
}

/**
 * 使用外部浏览器打开指定链接。
 */
infix fun Context.browse(uri: String) {
    val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}
