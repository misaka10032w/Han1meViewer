package com.yenaly.han1meviewer

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.interceptor.SpeedLimitInterceptor
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys
import com.yenaly.han1meviewer.ui.view.video.HJzvdStd
import com.yenaly.han1meviewer.ui.view.video.HMediaKernel
import com.yenaly.han1meviewer.util.CookieString
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.yenaly_libs.utils.applicationContext
import com.yenaly.yenaly_libs.utils.getSpValue
import com.yenaly.yenaly_libs.utils.putSpValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

object Preferences {
    /**
     * [Preference][androidx.preference.PreferenceFragmentCompat]自帶的SP
     */
    val preferenceSp: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )

    // app 相關

    /**
     * 是否登入，一般跟[loginCookie]一起賦值
     */
    var isAlreadyLogin: Boolean
        get() = getSpValue(ALREADY_LOGIN, false)
        set(value) {
            loginStateFlow.value = value
            putSpValue(ALREADY_LOGIN, value)
        }

    val loginStateFlow = MutableStateFlow(isAlreadyLogin)

    val savedUserId: String
        get() = preferenceSp.getString(SAVED_USER_ID,"") ?: ""

    /**
     * 保存的string格式的登入cookie
     */
    var loginCookie
        get() = CookieString(getSpValue(LOGIN_COOKIE, EMPTY_STRING))
        set(value) {
            loginCookieStateFlow.value = value
            putSpValue(LOGIN_COOKIE, value.cookie)
        }

    val loginCookieStateFlow = MutableStateFlow(loginCookie)

    var cloudFlareCookie
        get() = CookieString(getSpValue(CLOUDFLARE_COOKIE   , EMPTY_STRING))
        set(value) {
            cloudFlareCookieStateFlow.value = value
            putSpValue(CLOUDFLARE_COOKIE, value.cookie)
        }
    val cloudFlareCookieStateFlow = MutableStateFlow(cloudFlareCookie)

    // 更新 相關

    private const val UPDATE_NODE_ID = "update_node_id"

    var updateNodeId: String
        get() = getSpValue(UPDATE_NODE_ID, EMPTY_STRING)
        set(value) = putSpValue(UPDATE_NODE_ID, value)

    var lastUpdatePopupTime
        get() = getSpValue(SettingsPreferenceKeys.LAST_UPDATE_POPUP_TIME, 0L)
        set(value) = putSpValue(SettingsPreferenceKeys.LAST_UPDATE_POPUP_TIME, value)

    val updatePopupIntervalDays
        get() = preferenceSp.getInt(SettingsPreferenceKeys.UPDATE_POPUP_INTERVAL_DAYS, 0)

    val useCIUpdateChannel
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.USE_CI_UPDATE_CHANNEL, false)

    // Check if show update dialog.
    @OptIn(ExperimentalTime::class)
    val isUpdateDialogVisible: Boolean
        get() {
            val now = kotlin.time.Clock.System.now()
            val lastCheckTime = kotlin.time.Instant.fromEpochSeconds(lastUpdatePopupTime)
            val interval = updatePopupIntervalDays
            return now > lastCheckTime + interval.days
        }

    // 設定 相關

    val switchPlayerKernel: String
        get() = preferenceSp.getString(
            SettingsPreferenceKeys.SWITCH_PLAYER_KERNEL,
            HMediaKernel.Type.ExoPlayer.name
        ) ?: HMediaKernel.Type.ExoPlayer.name

    val showBottomProgress: Boolean
        get() = preferenceSp.getBoolean(
            SettingsPreferenceKeys.SHOW_BOTTOM_PROGRESS,
            true
        )

    val playerSpeed: Float
        get() = preferenceSp.getString(
            SettingsPreferenceKeys.PLAYER_SPEED,
            HJzvdStd.DEF_SPEED.toString()
        )?.toFloat() ?: HJzvdStd.DEF_SPEED

    val slideSensitivity: Int
        get() = preferenceSp.getInt(
            SettingsPreferenceKeys.SLIDE_SENSITIVITY,
            HJzvdStd.DEF_PROGRESS_SLIDE_SENSITIVITY
        )

    val longPressSpeedTime: Float
        get() = preferenceSp.getString(
            SettingsPreferenceKeys.LONG_PRESS_SPEED_TIMES,
            HJzvdStd.DEF_LONG_PRESS_SPEED_TIMES.toString()
        )?.toFloat() ?: HJzvdStd.DEF_LONG_PRESS_SPEED_TIMES

    val videoLanguage: String
        get() = preferenceSp.getString(SettingsPreferenceKeys.VIDEO_LANGUAGE, "zhs") ?: "zht"

    val videoQuality: String
        get() = preferenceSp.getString(SettingsPreferenceKeys.DEFAULT_VIDEO_QUALITY, "1080P") ?: "1080P"

    val showPlayedIndicator: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.SHOW_PLAYED_INDICATOR,true)

    val fakeLauncherIcon: String
        get() = preferenceSp.getString(
            SettingsPreferenceKeys.FAKE_LAUNCHER_ICON,
            "com.yenaly.han1meviewer.LauncherAliasDefault") ?: "com.yenaly.han1meviewer.LauncherAliasDefault"

    val baseUrl: String
        get() = preferenceSp.getString(SettingsPreferenceKeys.DOMAIN_NAME, HANIME_URL[0])
            ?: HANIME_URL[0]

    val selectedBaseUrl: String
        get() = preferenceSp.getString(SettingsPreferenceKeys.SELECTED_BASE_URL, HANIME_URL[0])
            ?: HANIME_URL[0]

    val useBuiltInHosts: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.USE_BUILT_IN_HOSTS, false)

    // 關鍵H幀 相關

    val whenCountdownRemind: Int
        get() = preferenceSp.getInt(
            SettingsPreferenceKeys.WHEN_COUNTDOWN_REMIND,
            HJzvdStd.DEF_COUNTDOWN_SEC
        ) * 1_000 // 越不了界，最大就30_000ms而已

    val showCommentWhenCountdown: Boolean
        get() = preferenceSp.getBoolean(
            SettingsPreferenceKeys.SHOW_COMMENT_WHEN_COUNTDOWN,
            false
        )

    val hKeyframesEnable: Boolean
        get() = preferenceSp.getBoolean(
            SettingsPreferenceKeys.H_KEYFRAMES_ENABLE,
            true
        )

    val sharedHKeyframesEnable: Boolean
        get() = preferenceSp.getBoolean(
            SettingsPreferenceKeys.SHARED_H_KEYFRAMES_ENABLE,
            true
        )

    val sharedHKeyframesUseFirst: Boolean
        get() = preferenceSp.getBoolean(
            SettingsPreferenceKeys.SHARED_H_KEYFRAMES_USE_FIRST,
            false
        )

    // 代理 相關

    val proxyType: Int
        get() = preferenceSp.getInt(
            SettingsPreferenceKeys.PROXY_TYPE,
            HProxySelector.TYPE_SYSTEM
        )

    val proxyIp: String
        get() = preferenceSp.getString(SettingsPreferenceKeys.PROXY_IP, EMPTY_STRING).orEmpty()

    val proxyPort: Int
        get() = preferenceSp.getInt(SettingsPreferenceKeys.PROXY_PORT, -1)

    // 隐私 相關

    val isAnalyticsEnabled: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.USE_ANALYTICS, true)

    // 下载 相關

    val downloadCountLimit: Int
        get() = preferenceSp.getInt(
            SettingsPreferenceKeys.DOWNLOAD_COUNT_LIMIT,
            // HanimeDownloadManager.MAX_CONCURRENT_DOWNLOAD_DEF
            HanimeDownloadManagerV2.MAX_CONCURRENT_DOWNLOAD_DEF
        )

    val collapseDownloadedGroup: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.COLLAPSE_DOWNLOADED_GROUP,false)
    val isUsePrivateStorage: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.USE_PRIVATE_STORAGE,true)
    val safDownloadPath: String?
        get() = preferenceSp.getString(SafFileManager.KEY_TREE_URI,null)

    val useDarkMode: String
        get() = preferenceSp.getString(SettingsPreferenceKeys.USE_DARK_MODE,"always_off") ?: "always_off"
    val useDynamicColor: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.USE_DYNAMIC_COLOR,false)
    val themeColor: String?
        get() = preferenceSp.getString(SettingsPreferenceKeys.THEME_COLOR, null)
    val allowResumePlayback: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.ALLOW_RESUME_PLAYBACK,true)

    val searchArtistIgnoreVideoType: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.SEARCH_ARTIST_IGNORE_VIDEO_TYPE,false)

    val disableMobileDataWarning: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.DISABLE_MOBILE_DATA_WARNING,false)

    val disablePredictiveBack: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.DISABLE_PREDICTIVE_BACK, false)

    val tabletMode: Boolean
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.TABLET_MODE, false)

    /**
     * MPV播放器设置
     */
    val mpvProfile: String // 预设模式
        get() = preferenceSp.getString(SettingsPreferenceKeys.MPV_PROFILE, "fast") ?: "fast"

    val enableGPUNextRenderer: Boolean // gpu-next 渲染器
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.ENABLE_GPU_NEXT_RENDERER, false)

    val mpvInterpolation: Boolean  // 插帧相关
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.MPV_INTERPOLATION, false)

    val mpvDeband: Boolean  // 去色带
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.MPV_DEBAND, true)

    val mpvFramedrop: Boolean  // GPU 繁忙时允许丢帧
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.MPV_FRAMEDROP, true)

    val mpvHwdec: String  // 硬件解码
        get() = preferenceSp.getString(SettingsPreferenceKeys.MPV_HWDEC, "Auto")?: "Auto"

    val mpvCacheSecs: Int  // 预缓存秒数
        get() = preferenceSp.getInt(SettingsPreferenceKeys.MPV_CACHE_SECS, 60)

    val mpvTlsVerify: Boolean  // 忽略证书验证
        get() = preferenceSp.getBoolean(SettingsPreferenceKeys.MPV_TLS_VERIFY, true)

    val mpvNetworkTimeout: Int  // 请求超时
        get() = preferenceSp.getInt(SettingsPreferenceKeys.MPV_NETWORK_TIMEOUT, 10)

    val customMpvParams: String
        get() = preferenceSp.getString(SettingsPreferenceKeys.CUSTOM_PARAMS,"")?: ""

    /**
     * 对应关系详见 [SpeedLimitInterceptor.SPEED_BYTES]
     */
    val downloadSpeedLimit: Long
        get() {
            val index = preferenceSp.getInt(
                SettingsPreferenceKeys.DOWNLOAD_SPEED_LIMIT,
                SpeedLimitInterceptor.NO_LIMIT_INDEX
            )
            return SpeedLimitInterceptor.SPEED_BYTES[index]
        }
}