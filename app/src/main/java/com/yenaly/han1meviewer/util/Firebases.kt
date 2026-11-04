package com.yenaly.han1meviewer.util

import android.app.Activity
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

fun Activity.logScreenViewEvent(screenClassName: String) {
    Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
        // example: MainActivity-HomeRouteScreen
        val screenName = this@logScreenViewEvent.javaClass.simpleName +
            "-" + screenClassName
        Log.d("logScreenViewEvent", "screenName: $screenName")
        param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClassName)
    }
}
