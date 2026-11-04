package com.yenaly.han1meviewer.util

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.core.util.TypedValueCompat

/**
 * 全局 Application Context，由 [com.yenaly.han1meviewer.base.BaseInitializer] 初始化。
 */
lateinit var applicationContext: Context
    internal set

val application: Application
    get() = applicationContext as Application

/**
 * 通过 dp 获取相应 px 值。
 */
val Number.dp: Int
    get() {
        val px = TypedValueCompat.dpToPx(toFloat(), applicationContext.resources.displayMetrics)
        return (if (px >= 0) px + 0.5f else px - 0.5f).toInt()
    }

val appScreenWidth: Int
    get() = applicationContext.resources.displayMetrics.widthPixels

val statusBarHeight: Int
    get() {
        val resources = applicationContext.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

val navBarHeight: Int
    get() {
        val resources = applicationContext.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

val isOrientationLandscape: Boolean
    get() = applicationContext.resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
