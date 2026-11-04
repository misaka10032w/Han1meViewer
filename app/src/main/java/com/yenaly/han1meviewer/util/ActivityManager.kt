package com.yenaly.han1meviewer.util

import android.app.Activity
import android.content.Intent
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

object ActivityManager {

    var currentActivity: WeakReference<Activity?> = WeakReference(null)
        internal set

    fun restart(killProcess: Boolean = true) {
        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            applicationContext.startActivity(intent)
        }
        if (killProcess) exitProcess(0)
    }
}
