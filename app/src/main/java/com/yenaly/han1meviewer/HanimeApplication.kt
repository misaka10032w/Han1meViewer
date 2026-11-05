package com.yenaly.han1meviewer

import android.content.ComponentCallbacks2
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.color.DynamicColors
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.database.database
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.theme.ThemeColorPreset
import com.yenaly.han1meviewer.util.AnimeShaders
import com.yenaly.han1meviewer.util.ThemeUtils
import com.developer.crashx.config.CrashConfig
import com.yenaly.han1meviewer.base.BaseApplication
import com.yenaly.han1meviewer.base.AppLocale
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import `is`.xyz.mpv.MPVLib
import java.net.ProxySelector

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 17:32
 */
class HanimeApplication : BaseApplication(), SingletonImageLoader.Factory {

    companion object {
        const val TAG = "HanimeApplication"

        /**
         * 全局图片内存缓存上限。
         *
         * Coil3 默认会使用堆的 20%，在 256 MB 堆的设备上约 51 MB；再叠加播放器缓冲后
         * 很容易触发 OOM。统一限制到 32 MB，并让所有页面共用同一个 ImageLoader。
         */
        private const val IMAGE_MEMORY_CACHE_BYTES = 32L * 1024 * 1024
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(IMAGE_MEMORY_CACHE_BYTES)
                    .build()
            }
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // #676 部分情况下会OOM，设置内存上限
        // UI_HIDDEN 是官方推荐释放 Bitmap 等界面资源的等级，同时也能覆盖后台内存压力。
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            SingletonImageLoader.get(this).memoryCache?.clear()
        }
    }

    private fun initCrashX() {
        CrashConfig.Builder.create()
            .backgroundMode(CrashConfig.BACKGROUND_MODE_SHOW_CUSTOM)
            .enabled(true)
            .includeDeviceInfo(true)
            .showErrorDetails(true)
            .showRestartButton(true)
            .showCloseButton(true)
            .showReportButton(true)
            .showCopyButtonInDetails(true)
            .logErrorOnRestart(true)
            .trackActivities(true)
            .minTimeBetweenCrashesMs(3000)
            .errorTitle(getString(R.string.crash_title))
            .errorDrawable(R.drawable.h_chan_cry)
            .errorMessage(getString(R.string.crash_message))
            .restartButtonText(getString(R.string.crash_restart))
            .closeButtonText(getString(R.string.crash_close))
            .detailsButtonText(getString(R.string.crash_details))
            .reportButtonText(getString(R.string.crash_report))
            .copyButtonText(getString(R.string.crash_copy))
            .restartActivity(MainActivity::class.java)
            .apply()
    }

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val am = getSystemService(android.app.ActivityManager::class.java)
        return am?.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName == packageName
    }

    override fun onCreate() {
        super.onCreate()
        if (!isMainProcess()) return
        initCrashX()
        ThemeUtils.applyDarkModeFromPreferences(this)
        if (ThemeColorPreset.fromKey(Preferences.themeColor) == ThemeColorPreset.SYSTEM) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        ProxySelector.setDefault(HProxySelector())
        HProxySelector.rebuildNetwork()
        initFirebase()
        initNotificationChannel()
        MPVLib.create(applicationContext)
        MPVLib.init()

        if (AnimeShaders.copyShaderAssets(applicationContext) <= 0) {
            Log.w(TAG, "Shader 复制失败")
        }
        if (AnimeShaders.copyCertAssets(applicationContext) <= 0) {
            Log.w(TAG, "cert 复制失败")
        }
        val selected = Preferences.fakeLauncherIcon
        switchLauncher(selected)
    }

    private fun initFirebase() {
        // 用于处理 Firebase Analytics 初始化
        Firebase.analytics.setAnalyticsCollectionEnabled(Preferences.isAnalyticsEnabled)
        // 用于处理 Firebase Crashlytics 初始化
        Firebase.crashlytics.apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            setCustomKeys {
                key(
                    FirebaseConstants.APP_LANGUAGE,
                    AppLocale.preferredLanguage.toLanguageTag()
                )
                key(
                    FirebaseConstants.VERSION_SOURCE,
                    BuildConfig.VERSION_SOURCE
                )
            }
        }
        // 用于处理 Firebase Remote Config 初始化
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3 * 60 * 60
                fetchTimeoutInSeconds = 10
            })
            setDefaultsAsync(FirebaseConstants.remoteConfigDefaults)
            fetchAndActivate().addOnCompleteListener {
                AppViewModel.getLatestVersion(delayMillis = 200)
            }
        }
        Firebase.database.setPersistenceEnabled(true)
    }

    private fun initNotificationChannel() {
        val nm = NotificationManagerCompat.from(this)

        val hanimeDownloadChannel = NotificationChannelCompat.Builder(
            DOWNLOAD_NOTIFICATION_CHANNEL,
            NotificationManagerCompat.IMPORTANCE_HIGH
        ).setName("Hanime Download").build()
        nm.createNotificationChannel(hanimeDownloadChannel)

        val appUpdateChannel = NotificationChannelCompat.Builder(
            UPDATE_NOTIFICATION_CHANNEL,
            NotificationManagerCompat.IMPORTANCE_HIGH
        ).setName("App Update").build()
        nm.createNotificationChannel(appUpdateChannel)
    }
    fun switchLauncher(alias: String) {
        val pm = packageManager

        val allAliases = listOf(
            "com.yenaly.han1meviewer.LauncherAliasDefault",
            "com.yenaly.han1meviewer.LauncherFakeCalc",
            "com.yenaly.han1meviewer.LauncherFakeCornhub",
            "com.yenaly.han1meviewer.LauncherFakeXxt"
        )

        allAliases.forEach { a ->
            val state = if (a == alias)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            pm.setComponentEnabledSetting(
                ComponentName(this, a),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}