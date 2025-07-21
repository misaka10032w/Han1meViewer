package com.yenaly.yenaly_libs.utils

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.OrientationEventListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/04/10 010 20:49
 */
class OrientationManager(
    private val context: Context,
    private var orientationChangeListener: OrientationChangeListener? = null
) : OrientationEventListener(context), LifecycleEventObserver {
    private var screenOrientation: ScreenOrientation = ScreenOrientation.PORTRAIT

    enum class ScreenOrientation {
        LANDSCAPE, REVERSED_LANDSCAPE,
        PORTRAIT, REVERSED_PORTRAIT;
        val isPortrait get() = this == PORTRAIT || this == REVERSED_PORTRAIT
        val isLandscape get() = this == LANDSCAPE || this == REVERSED_LANDSCAPE
    }

    private var lastLockedOrientation: Int? = null

    /**
     * 自动判断是否需要切换方向，只有在不同方向时才设置
     */
    fun lockOrientation(activity: Activity, orientation: ScreenOrientation, delayMillis: Long = 0L) {
        val requestedOrientation = when (orientation) {
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ScreenOrientation.REVERSED_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.REVERSED_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }

        // 防止重复设置导致 recreate
        if (activity.requestedOrientation == requestedOrientation) return

        val setOrientation = {
            lastLockedOrientation = requestedOrientation
            activity.requestedOrientation = requestedOrientation
        }

        if (delayMillis > 0) {
            Handler(Looper.getMainLooper()).postDelayed(setOrientation, delayMillis)
        } else {
            setOrientation()
        }
    }

    fun unlockOrientation(activity: Activity) {
        lastLockedOrientation = null
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    fun isOrientationLocked(): Boolean {
        return lastLockedOrientation != null
    }

    fun getCurrentScreenOrientation(): ScreenOrientation {
        return screenOrientation
    }

    override fun onOrientationChanged(orientation: Int) {
        if (orientation == -1) return
        val newOrientation = when (orientation) {
            in 60..140 -> ScreenOrientation.REVERSED_LANDSCAPE
            in 140..220 -> ScreenOrientation.REVERSED_PORTRAIT
            in 220..300 -> ScreenOrientation.LANDSCAPE
            else -> ScreenOrientation.PORTRAIT
        }
        if (newOrientation != screenOrientation) {
            screenOrientation = newOrientation
            Log.d("OrientationManager", "screenOrientation updated to $screenOrientation")
        }
        try {
            val isRotateEnabled = Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION
            )
            if (isRotateEnabled == 0) return
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        orientationChangeListener?.onOrientationChanged(screenOrientation)
    }

    fun interface OrientationChangeListener {
        fun onOrientationChanged(orientation: ScreenOrientation)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> enable()
            Lifecycle.Event.ON_STOP -> disable()
            else -> Unit
        }
    }
}
