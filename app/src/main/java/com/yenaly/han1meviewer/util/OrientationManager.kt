package com.yenaly.han1meviewer.util

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
 * 监听设备方向变化，并提供屏幕方向锁定/解锁能力。
 *
 * 实现了 [LifecycleEventObserver]，可在页面 ON_START / ON_STOP 时自动开启或关闭监听。
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
     * 锁定 Activity 的屏幕方向。
     *
     * @param activity 目标 Activity
     * @param orientation 目标方向
     * @param delayMillis 延迟执行时间，0 表示立即执行
     */
    fun lockOrientation(activity: Activity, orientation: ScreenOrientation, delayMillis: Long = 0L) {
        val requestedOrientation = when (orientation) {
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ScreenOrientation.REVERSED_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.REVERSED_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }

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

    /**
     * 解除屏幕方向锁定，恢复系统默认行为。
     */
    fun unlockOrientation(activity: Activity) {
        lastLockedOrientation = null
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    /**
     * 当前是否处于方向锁定状态。
     */
    fun isOrientationLocked(): Boolean = lastLockedOrientation != null

    /**
     * 获取最近一次识别到的屏幕方向。
     */
    fun getCurrentScreenOrientation(): ScreenOrientation = screenOrientation

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

    /**
     * 屏幕方向变化回调。
     */
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
