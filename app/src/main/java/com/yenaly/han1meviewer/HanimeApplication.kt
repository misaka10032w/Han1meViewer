package com.yenaly.han1meviewer

import android.content.ComponentName
import android.content.pm.PackageManager
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
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.util.AnimeShaders
import com.yenaly.han1meviewer.util.ThemeUtils
import com.yenaly.yenaly_libs.base.YenalyApplication
import com.yenaly.yenaly_libs.utils.LanguageHelper
import `is`.xyz.mpv.MPVLib
import java.net.ProxySelector

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 17:32
 */
class HanimeApplication : YenalyApplication() {

    companion object {
        const val TAG = "HanimeApplication"

        init {
            SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, _ ->
                return@setDefaultRefreshHeaderCreator MaterialHeader(context)
            }
            SmartRefreshLayout.setDefaultRefreshFooterCreator { context, _ ->
                return@setDefaultRefreshFooterCreator ClassicsFooter(context)
            }
        }
    }

    /**
     * 已经在 [HInitializer] 中处理了
     */
    override val isDefaultCrashHandlerEnabled: Boolean = false

    override fun onCreate() {
        super.onCreate()

        ThemeUtils.applyDarkModeFromPreferences(this)
        if (Preferences.useDynamicColor) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }

        TranslationManager.getInstance(this).initialize()
        Parser.initialize(this)

        appScope.launch {
            TranslationMigrationHelper.migrateIfNeeded(this@HanimeApplication)
        }

        ProxySelector.setDefault(HProxySelector())
        HProxySelector.rebuildNetwork()

        initFirebase()
        initNotificationChannel()

        MPVLib.create(applicationContext)

        if (AnimeShaders.copyShaderAssets(applicationContext) <= 0) {
            Log.w(TAG, "Shader copy failed")
        }

        switchLauncher(Preferences.fakeLauncherIcon)
    }

    private fun initFirebase() {
        // Firebase Analytics
        Firebase.analytics.setAnalyticsCollectionEnabled(
            Preferences.isAnalyticsEnabled
        )

        // Firebase Crashlytics
        Firebase.crashlytics.apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            setCustomKeys {
                key(
                    FirebaseConstants.APP_LANGUAGE,
                    LanguageHelper.preferredLanguage.toLanguageTag()
                )
                key(
                    FirebaseConstants.VERSION_SOURCE,
                    BuildConfig.VERSION_SOURCE
                )
            }
        }

        // Firebase Remote Config
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds =
                        if (BuildConfig.DEBUG) 0 else 3 * 60 * 60
                    fetchTimeoutInSeconds = 10
                }
            )
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