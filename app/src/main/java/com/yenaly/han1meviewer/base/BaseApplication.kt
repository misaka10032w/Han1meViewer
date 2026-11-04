package com.yenaly.han1meviewer.base

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.startup.Initializer
import com.yenaly.han1meviewer.util.ActivityManager
import com.yenaly.han1meviewer.util.applicationContext
import java.lang.ref.WeakReference

/**
 * Application 基类，负责维护当前前台 Activity。
 */
open class BaseApplication : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) {
        ActivityManager.currentActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}

/**
 * androidx.startup 初始化器，保存全局 Application Context。
 */
open class BaseInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        applicationContext = context
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
