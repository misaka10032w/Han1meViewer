package com.yenaly.han1meviewer.util

import android.app.Activity
import android.util.Log

/**
 * 记录设置页等以 Activity 为宿主的 screen_view 事件。
 *
 * example: MainActivity-HomeRouteScreen
 */
fun Activity.logScreenViewEvent(screenClassName: String) {
    val screenName = this.javaClass.simpleName + "-" + screenClassName
    Log.d("logScreenViewEvent", "screenName: $screenName")
    HAnalytics.logScreenView(screenName, screenClassName)
}
