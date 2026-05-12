@file:Suppress("DEPRECATION")

package com.yenaly.han1meviewer.ui.screen.settings

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.mikepenz.aboutlibraries.LibsBuilder
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.HA1_GITHUB_FORUM_URL
import com.yenaly.han1meviewer.HA1_GITHUB_ISSUE_URL
import com.yenaly.han1meviewer.HanimeApplication
import com.yenaly.han1meviewer.HanimeConstants.HANIME_HOSTNAME
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.dao.download.HanimeDownloadDao
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.HanimeNetwork
import com.yenaly.han1meviewer.logic.network.interceptor.SpeedLimitInterceptor
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.logout
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.activity.SettingsActivity
import com.yenaly.han1meviewer.ui.view.video.HJzvdStd
import com.yenaly.han1meviewer.ui.view.video.HMediaKernel
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.ui.viewmodel.SettingsViewModel
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.SafFileManager.KEY_TREE_URI
import com.yenaly.han1meviewer.util.SafFileManager.checkSafPermissions
import com.yenaly.han1meviewer.util.SafFileManager.migratePrivateToSaf
import com.yenaly.han1meviewer.util.ThemeUtils
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.copyToClipboard
import com.yenaly.yenaly_libs.utils.decodeFromStringByBase64
import com.yenaly.yenaly_libs.utils.findActivity
import com.yenaly.yenaly_libs.utils.folderSize
import com.yenaly.yenaly_libs.utils.formatBytesPerSecond
import com.yenaly.yenaly_libs.utils.formatFileSizeV2
import com.yenaly.yenaly_libs.utils.showLongToast
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.textFromClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime

private const val HOME_VIDEO_LANGUAGE = "video_language"
private const val HOME_DEFAULT_VIDEO_QUALITY = "default_video_quality"
private const val HOME_SHOW_PLAYED_INDICATOR = "show_played_indicator"
private const val HOME_ALLOW_PIP_MODE = "allow_pip_mode"
private const val HOME_UPDATE_POPUP_INTERVAL_DAYS = "update_popup_interval_days"
private const val HOME_USE_CI_UPDATE_CHANNEL = "use_ci_update_channel"
private const val HOME_USE_ANALYTICS = "use_analytics"
private const val HOME_FAKE_LAUNCHER_ICON = "pref_fake_launcher_icon"
private const val HOME_USE_DARK_MODE = "use_dark_mode"
private const val HOME_USE_DYNAMIC_COLOR = "use_dynamic_color"
private const val HOME_ALLOW_RESUME_PLAYBACK = "allow_resume_playback"
private const val HOME_SEARCH_ARTIST_IGNORE_VIDEO_TYPE = "search_artist_ignore_video_type"
private const val HOME_DISABLE_MOBILE_DATA_WARNING = "disable_mobile_data_warning"
private const val HOME_COLLAPSE_DOWNLOADED_GROUP = "collapse_downloaded_group"
private const val HOME_DISABLE_PREDICTIVE_BACK = "disable_predictive_back"
private const val HOME_TABLET_MODE = "tablet_mode"
private const val HOME_DISABLE_COMMENTS = "disable_comments"
private const val HOME_USE_LOCK_SCREEN = "use_lock_screen"
private const val HOME_APP_LANGUAGE = "app_language"

private const val PLAYER_SWITCH_PLAYER_KERNEL = "switch_player_kernel"
private const val PLAYER_SHOW_BOTTOM_PROGRESS = "show_bottom_progress"
private const val PLAYER_SPEED = "player_speed"
private const val PLAYER_SLIDE_SENSITIVITY = "slide_sensitivity"
private const val PLAYER_LONG_PRESS_SPEED_TIMES = "long_press_speed_times"

private const val NETWORK_PROXY_TYPE = "proxy_type"
private const val NETWORK_PROXY_IP = "proxy_ip"
private const val NETWORK_PROXY_PORT = "proxy_port"
private const val NETWORK_DOMAIN_NAME = "domain_name"
private const val NETWORK_SELECTED_BASE_URL = "selectedBaseUrl"
private const val NETWORK_USE_BUILT_IN_HOSTS = "use_built_in_hosts"

private const val DOWNLOAD_COUNT_LIMIT = "download_count_limit"
private const val DOWNLOAD_SPEED_LIMIT = "download_speed_limit"
private const val DOWNLOAD_USE_PRIVATE_STORAGE = "use_private_storage"

private const val H_KEYFRAMES_ENABLE = "h_keyframes_enable"
private const val SHOW_COMMENT_WHEN_COUNTDOWN = "show_comment_when_countdown"
private const val SHARED_H_KEYFRAMES_ENABLE = "shared_h_keyframes_enable"
private const val SHARED_H_KEYFRAMES_USE_FIRST = "shared_h_keyframes_use_first"
private const val WHEN_COUNTDOWN_REMIND = "when_countdown_remind"

private const val MPV_PROFILE = "mpv_profile"
private const val MPV_INTERPOLATION = "mpv_interpolation"
private const val MPV_DEBAND = "mpv_deband"
private const val MPV_FRAMEDROP = "mpv_framedrop"
private const val MPV_HWDEC = "mpv_hwdecx"
private const val MPV_CACHE_SECS = "mpv_cache_secs"
private const val MPV_TLS_VERIFY = "mpv_tls_verify"
private const val MPV_NETWORK_TIMEOUT = "mpv_network_timeout"
private const val ENABLE_GPU_NEXT_RENDERER = "mpv_gpu_next_render"
private const val CUSTOM_PARAMS = "mpv_custom_parameters"

@Composable
fun HomeSettingsRouteScreen(
    onNavigateToPlayerSettings: () -> Unit,
    onNavigateToHKeyframeSettings: () -> Unit,
    onNavigateToDownloadSettings: () -> Unit,
    onNavigateToNetworkSettings: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity<Activity>()
    val coroutineScope = rememberCoroutineScope()
    val versionState by AppViewModel.versionFlow.collectAsStateWithLifecycle()
    var refreshKey by remember { mutableIntStateOf(0) }
    var cacheKey by remember { mutableIntStateOf(0) }

    val launcherItems = remember(context) {
        listOf(
            LauncherItem(
                name = context.getString(R.string.hanime_app_name),
                iconRes = R.drawable.ic_launcher_new,
                alias = "com.yenaly.han1meviewer.LauncherAliasDefault",
            ),
            LauncherItem(
                name = context.getString(R.string.app_name_fake_calc),
                iconRes = R.drawable.ic_launcher_calc,
                alias = "com.yenaly.han1meviewer.LauncherFakeCalc",
            ),
            LauncherItem(
                name = context.getString(R.string.app_name_fake_cornhub),
                iconRes = R.drawable.ic_launcher_cornhub,
                alias = "com.yenaly.han1meviewer.LauncherFakeCornhub",
            ),
            LauncherItem(
                name = context.getString(R.string.app_name_fake_xxt),
                iconRes = R.drawable.ic_launcher_xxt,
                alias = "com.yenaly.han1meviewer.LauncherFakeXxt",
            ),
        )
    }

    val updateSummary = remember(versionState, context) {
        when (versionState) {
            is WebsiteState.Error -> context.getString(R.string.check_update_failed)
            is WebsiteState.Loading -> context.getString(R.string.checking_update)
            is WebsiteState.Success -> {
                val info = (versionState as WebsiteState.Success).info
                if (info == null) {
                    context.getString(R.string.already_latest_update)
                } else {
                    context.getString(R.string.check_update_success, info.version)
                }
            }
        }
    }
    val cacheSummary = remember(cacheKey, context) {
        generateClearCacheSummary(context, context.cacheDir?.folderSize ?: 0L).toString()
    }
    val uiState = remember(refreshKey, updateSummary, cacheSummary, launcherItems, context) {
        buildHomeSettingsUiState(
            context = context,
            launcherItems = launcherItems,
            updateSummary = updateSummary,
            cacheSummary = cacheSummary,
        )
    }

    HomeSettingsScreen(
        state = uiState,
        onVideoLanguageChange = { value ->
            if (value != Preferences.videoLanguage) {
                saveString(HOME_VIDEO_LANGUAGE, value)
                refreshKey++
                context.showAlertDialog {
                    setCancelable(false)
                    setTitle(R.string.attention)
                    setMessage(context.getString(R.string.restart_or_not_working, context.getString(R.string.video_language)))
                    setPositiveButton(R.string.confirm) { _, _ -> ActivityManager.restart(killProcess = true) }
                    setNegativeButton(R.string.cancel, null)
                }
            }
        },
        onVideoQualityChange = { value ->
            saveString(HOME_DEFAULT_VIDEO_QUALITY, value)
            refreshKey++
            Toast.makeText(context, "Success：$value", Toast.LENGTH_SHORT).show()
        },
        onDarkModeChange = { value ->
            if (value != Preferences.useDarkMode) {
                saveString(HOME_USE_DARK_MODE, value)
                ThemeUtils.applyDarkModeFromPreferences(context)
                activity.recreate()
            }
        },
        onAllowPipModeChange = { enabled ->
            if (enabled && !isPipPermissionGranted(context)) {
                Toast.makeText(context, context.getString(R.string.request_pip_alert), Toast.LENGTH_SHORT).show()
                openPipPermissionSettings(context)
                saveBoolean(HOME_ALLOW_PIP_MODE, false)
                refreshKey++
                return@HomeSettingsScreen
            }
            saveBoolean(HOME_ALLOW_PIP_MODE, enabled)
            refreshKey++
        },
        onAllowResumePlaybackChange = {
            saveBoolean(HOME_ALLOW_RESUME_PLAYBACK, it)
            refreshKey++
        },
        onShowPlayedIndicatorChange = {
            saveBoolean(HOME_SHOW_PLAYED_INDICATOR, it)
            refreshKey++
        },
        onSearchArtistIgnoreVideoTypeChange = {
            saveBoolean(HOME_SEARCH_ARTIST_IGNORE_VIDEO_TYPE, it)
            refreshKey++
        },
        onDisableMobileDataWarningChange = {
            saveBoolean(HOME_DISABLE_MOBILE_DATA_WARNING, it)
            refreshKey++
        },
        onDisablePredictiveBackChange = {
            saveBoolean(HOME_DISABLE_PREDICTIVE_BACK, it)
            refreshKey++
            activity.recreate()
        },
        onTabletModeChange = {
            saveBoolean(HOME_TABLET_MODE, it)
            refreshKey++
        },
        onDisableCommentsChange = {
            saveBoolean(HOME_DISABLE_COMMENTS, it)
            refreshKey++
        },
        onCollapseDownloadedGroupChange = {
            saveBoolean(HOME_COLLAPSE_DOWNLOADED_GROUP, it)
            refreshKey++
        },
        onUseDynamicColorChange = { value ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@HomeSettingsScreen
            if (value != Preferences.useDynamicColor) {
                saveBoolean(HOME_USE_DYNAMIC_COLOR, value)
                refreshKey++
                context.showAlertDialog {
                    setCancelable(false)
                    setTitle(R.string.attention)
                    setMessage(context.getString(R.string.restart_or_not_working, context.getString(R.string.dynamic_color_title)))
                    setPositiveButton(R.string.confirm) { _, _ -> ActivityManager.restart(killProcess = true) }
                    setNegativeButton(R.string.cancel, null)
                }
            }
        },
        onUseCIUpdateChannelChange = { value ->
            saveBoolean(HOME_USE_CI_UPDATE_CHANNEL, value)
            refreshKey++
            AppViewModel.getLatestVersion()
        },
        onUseAnalyticsChange = { value ->
            if (!value) {
                context.showAlertDialog {
                    setTitle(R.string.about_analytics)
                    setMessage(context.getString(R.string.about_analytics_summary).parseAsHtml())
                    setCancelable(false)
                    setPositiveButton(R.string.ok, null)
                    setNeutralButton(R.string.deny) { _, _ ->
                        saveBoolean(HOME_USE_ANALYTICS, false)
                        refreshKey++
                        Firebase.analytics.setAnalyticsCollectionEnabled(false)
                    }
                }
                return@HomeSettingsScreen
            }
            saveBoolean(HOME_USE_ANALYTICS, true)
            refreshKey++
            Firebase.analytics.setAnalyticsCollectionEnabled(true)
        },
        onUseLockScreenChange = { value ->
            if (value) {
                if (!isDeviceSecureCompat(context)) {
                    Toast.makeText(context, context.getString(R.string.not_set_sys_lock), Toast.LENGTH_LONG).show()
                    refreshKey++
                    return@HomeSettingsScreen
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    Toast.makeText(context, context.getString(R.string.not_compact_lock_screen), Toast.LENGTH_LONG).show()
                    refreshKey++
                    return@HomeSettingsScreen
                }
            }
            saveBoolean(HOME_USE_LOCK_SCREEN, value)
            refreshKey++
        },
        onOpenPlayerSettings = onNavigateToPlayerSettings,
        onOpenHKeyframeSettings = onNavigateToHKeyframeSettings,
        onOpenDownloadSettings = onNavigateToDownloadSettings,
        onOpenNetworkSettings = onNavigateToNetworkSettings,
        onOpenAppLanguageSettings = { value ->
            val old = Preferences.preferenceSp.getString(HOME_APP_LANGUAGE, "system") ?: "system"
            if (old != value) {
                Preferences.preferenceSp.edit { putString(HOME_APP_LANGUAGE, value) }
                refreshKey++
                activity.recreate()
            }
        },
        onCheckUpdate = { AppViewModel.getLatestVersion() },
        onUpdatePopupIntervalDaysChange = {
            Preferences.preferenceSp.edit { putInt(HOME_UPDATE_POPUP_INTERVAL_DAYS, it) }
            refreshKey++
        },
        onOpenApplyDeepLinks = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                showShortToast(R.string.action_app_open_by_default_settings_not_support)
            } else {
                showApplyDeepLinksDialog(context, activity)
            }
        },
        onOpenFakeLauncherIcon = {
            val adapter = object : ArrayAdapter<LauncherItem>(context, 0, launcherItems) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: LayoutInflater.from(context)
                        .inflate(R.layout.item_simple_icon_with_text, parent, false)
                    val item = getItem(position)!!
                    view.findViewById<ImageView>(R.id.icon).setImageResource(item.iconRes)
                    view.findViewById<TextView>(R.id.name).text = item.name
                    return view
                }
            }
            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.fake_app_icon))
                .setAdapter(adapter) { _, which ->
                    val selected = launcherItems[which]
                    Preferences.preferenceSp.edit { putString(HOME_FAKE_LAUNCHER_ICON, selected.alias) }
                    (context.applicationContext as? HanimeApplication)?.switchLauncher(selected.alias)
                    showLongToast(context.getString(R.string.fake_icon_hint))
                    refreshKey++
                }
                .show()
        },
        onOpenOpenSourceLicense = {
            LibsBuilder()
                .withShowLoadingProgress(true)
                .withSearchEnabled(false)
                .withActivityTitle(context.getString(R.string.open_source_license))
                .withAboutIconShown(true)
                .withAboutVersionShown(true)
                .start(context)
        },
        onOpenAbout = {},
        onClearCache = {
            val cacheDir = context.cacheDir
            val folderSize = cacheDir?.folderSize ?: 0L
            if (folderSize == 0L) {
                showShortToast(R.string.cache_empty)
                return@HomeSettingsScreen
            }
            context.showAlertDialog {
                setTitle(R.string.sure_to_clear)
                setMessage(R.string.sure_to_clear_cache)
                setPositiveButton(R.string.confirm) { _, _ ->
                    coroutineScope.launch(Dispatchers.IO) {
                        val success = cacheDir?.deleteRecursively() == true
                        withContext(Dispatchers.Main) {
                            cacheKey++
                            refreshKey++
                            if (success) showShortToast(R.string.clear_success) else showShortToast(R.string.clear_failed)
                        }
                    }
                }
                setNegativeButton(R.string.cancel, null)
            }
        },
        onSubmitBug = { context.browse(HA1_GITHUB_ISSUE_URL) },
        onOpenForum = { context.browse(HA1_GITHUB_FORUM_URL) },
    )
}

@Composable
fun PlayerSettingsRouteScreen(
    onNavigateToMpvSettings: () -> Unit,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val uiState = remember(refreshKey, context) { buildPlayerSettingsUiState(context) }

    PlayerSettingsScreen(
        state = uiState,
        kernelOptions = HMediaKernel.Type.entries.map { it.name to it.name },
        speedOptions = HJzvdStd.speedStringArray.zip(HJzvdStd.speedArray.map { it.toString() }),
        longPressSpeedOptions = listOf(
            context.getString(R.string.d_speed_times, 1f) to "1",
            context.getString(R.string.d_speed_times, 1.5f) to "1.5",
            context.getString(R.string.d_speed_times, 2f) to "2",
            "${context.getString(R.string.d_speed_times, 2.5f)} (${context.getString(R.string.default_)})" to "2.5",
            context.getString(R.string.d_speed_times, 2.8f) to "2.8",
            context.getString(R.string.d_speed_times, 3f) to "3",
            context.getString(R.string.d_speed_times, 3.2f) to "3.2",
            context.getString(R.string.d_speed_times, 3.5f) to "3.5",
            context.getString(R.string.d_speed_times, 3.8f) to "3.8",
            context.getString(R.string.d_speed_times, 4f) to "4",
        ),
        onKernelChange = {
            saveString(PLAYER_SWITCH_PLAYER_KERNEL, it)
            refreshKey++
        },
        onShowBottomProgressChange = {
            saveBoolean(PLAYER_SHOW_BOTTOM_PROGRESS, it)
            refreshKey++
        },
        onPlayerSpeedChange = {
            saveString(PLAYER_SPEED, it)
            refreshKey++
        },
        onLongPressSpeedChange = {
            saveString(PLAYER_LONG_PRESS_SPEED_TIMES, it)
            refreshKey++
        },
        onSlideSensitivityChange = {
            Preferences.preferenceSp.edit { putInt(PLAYER_SLIDE_SENSITIVITY, it) }
            refreshKey++
        },
        onOpenMpvSettings = onNavigateToMpvSettings,
    )
}

@Composable
fun NetworkSettingsRouteScreen() {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var currentHost by remember { mutableStateOf(Preferences.baseUrl) }
    var isDelayTesting by remember { mutableStateOf(false) }
    val delayResults = remember { mutableStateListOf<DelayResultUi>() }
    val delayHandler = remember { Handler(Looper.getMainLooper()) }
    val executor = remember { Executors.newCachedThreadPool() }
    val uiState = remember(refreshKey, context) { buildNetworkSettingsUiState(context) }

    fun stopDelayTest() {
        isDelayTesting = false
        delayHandler.removeCallbacksAndMessages(null)
    }

    fun measureDelay(ip: String): Int {
        return try {
            val start = System.currentTimeMillis()
            val address = InetAddress.getByName(ip)
            val reachable = address.isReachable(2000)
            if (reachable) (System.currentTimeMillis() - start).toInt() else -1
        } catch (_: Exception) {
            -1
        }
    }

    fun testIp(ip: String) {
        if (!isDelayTesting) return
        executor.execute {
            val delay = measureDelay(ip)
            delayHandler.post {
                val index = delayResults.indexOfFirst { it.ip == ip }
                if (index >= 0) {
                    delayResults[index] = DelayResultUi(ip, delay)
                }
            }
        }
    }

    fun scheduleNextTest(ipList: List<String>) {
        if (!isDelayTesting) return
        ipList.forEach(::testIp)
        delayHandler.postDelayed({ scheduleNextTest(ipList) }, 2000)
    }

    DisposableEffect(Unit) {
        onDispose {
            stopDelayTest()
            executor.shutdownNow()
        }
    }

    NetworkSettingsScreen(
        state = uiState,
        domainOptions = buildDomainOptions(context),
        currentHost = currentHost,
        delayResults = delayResults,
        isDelayTesting = isDelayTesting,
        proxyType = Preferences.proxyType,
        proxyIp = Preferences.proxyIp,
        proxyPort = Preferences.proxyPort,
        onDomainChange = { newValue ->
            val origin = Preferences.baseUrl
            if (newValue != origin) {
                context.showAlertDialog {
                    setCancelable(false)
                    setTitle(R.string.attention)
                    setMessage(context.getString(R.string.domain_change_tips).trimIndent())
                    setPositiveButton(R.string.confirm) { _, _ ->
                        Preferences.preferenceSp.edit(commit = true) {
                            putString(NETWORK_DOMAIN_NAME, newValue)
                            putString(NETWORK_SELECTED_BASE_URL, newValue)
                        }
                        logout()
                        ActivityManager.restart(killProcess = true)
                    }
                    setNegativeButton(R.string.cancel, null)
                }
            }
        },
        onUseBuiltInHostsChange = { value ->
            Preferences.preferenceSp.edit { putBoolean(NETWORK_USE_BUILT_IN_HOSTS, value) }
            refreshKey++
            context.showAlertDialog {
                setCancelable(false)
                setTitle(R.string.attention)
                setMessage(context.getString(R.string.restart_or_not_working, EMPTY_STRING))
                setPositiveButton(R.string.confirm) { _, _ -> ActivityManager.restart(killProcess = true) }
                setNegativeButton(R.string.cancel, null)
            }
        },
        onOpenDelayTest = {
            val host = Preferences.baseUrl.toUri().host ?: context.getString(R.string.unknow)
            currentHost = Preferences.baseUrl
            delayResults.clear()
            isDelayTesting = true
            executor.execute {
                val ipList = HDns().getCDNList(host)
                Handler(Looper.getMainLooper()).post {
                    Log.i("delayTest", ipList.toString())
                    delayResults.clear()
                    delayResults.addAll(ipList.map { DelayResultUi(it, -1) })
                    scheduleNextTest(ipList)
                }
            }
        },
        onDismissDelayTest = { stopDelayTest() },
        onApplyProxy = { type, ip, port ->
            val valid = when (type) {
                HProxySelector.TYPE_DIRECT, HProxySelector.TYPE_SYSTEM -> true
                HProxySelector.TYPE_HTTP, HProxySelector.TYPE_SOCKS -> HProxySelector.validateIp(ip) && HProxySelector.validatePort(port)
                else -> false
            }
            if (!valid) {
                showShortToast("Invalid IP(v4) or Port(0..65535)")
                return@NetworkSettingsScreen
            }
            if (type == HProxySelector.TYPE_SOCKS) {
                context.showAlertDialog {
                    setTitle(R.string.warning)
                    setMessage(R.string.mpv_socks5_warning)
                    setPositiveButton(R.string.confirm) { _, _ -> }
                }
            }
            Preferences.preferenceSp.edit(commit = true) {
                putInt(NETWORK_PROXY_TYPE, type)
                putString(NETWORK_PROXY_IP, ip)
                putInt(NETWORK_PROXY_PORT, port)
            }
            HProxySelector.rebuildNetwork()
            HanimeNetwork.rebuildNetwork()
            refreshKey++
        },
    )
}

@Composable
fun DownloadSettingsRouteScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity<SettingsActivity>()
    var refreshKey by remember { mutableIntStateOf(0) }
    val dao = remember { DownloadDatabase.instance.hanimeDownloadDao }
    val uiState = remember(refreshKey, context) { buildDownloadSettingsUiState(context) }

    val openDirectoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            SafFileManager.persistUriPermission(context, result.data)
            Preferences.preferenceSp.edit { putBoolean(DOWNLOAD_USE_PRIVATE_STORAGE, false) }
            showLongToast(context.getString(R.string.directory_saved, result.data))
            refreshKey++
        } else {
            showLongToast(context.getString(R.string.no_directory_selected))
        }
    }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) return@rememberLauncherForActivityResult
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (activity.shouldShowRequestPermissionRationale(permission)) {
            Toast.makeText(context, "拒绝？拒绝就不好办了喵👿", Toast.LENGTH_LONG).show()
            onNavigateBack()
        } else {
            AlertDialog.Builder(context)
                .setTitle("权限被永久拒绝")
                .setMessage("请前往设置开启存储权限，以便保存下载内容。")
                .setPositiveButton("去设置") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                }
                .setNegativeButton("取消") { _, _ ->
                    onNavigateBack()
                }
                .show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermission.launch(permission)
            }
        }
    }

    DownloadSettingsScreen(
        state = uiState,
        maxDownloadCountLimit = 10,
        maxDownloadSpeedLimitIndex = SpeedLimitInterceptor.SPEED_BYTES.lastIndex,
        onOpenDownloadPath = {
            context.showAlertDialog {
                setTitle(context.getString(R.string.select_download_folder))
                setMessage(context.getString(R.string.select_folder_message))
                setPositiveButton(R.string.ok) { _, _ ->
                    openDirectoryPicker.launch(SafFileManager.buildOpenDirectoryIntent())
                }
                setNegativeButton(context.getString(R.string.cancel)) { _, _ -> }
                if (!Preferences.isUsePrivateStorage) {
                    setNeutralButton(context.getString(R.string.restore_default_path)) { _, _ ->
                        context.showAlertDialog {
                            setTitle(context.getString(R.string.restore_default_path))
                            setMessage(context.getString(R.string.restore_default_message))
                            setPositiveButton(R.string.ok) { _, _ ->
                                Preferences.preferenceSp.edit {
                                    putBoolean(DOWNLOAD_USE_PRIVATE_STORAGE, true)
                                    remove(KEY_TREE_URI)
                                }
                                refreshKey++
                                showLongToast(context.getString(R.string.default_path_restored))
                            }
                            setNegativeButton(context.getString(R.string.cancel)) { _, _ -> }
                        }
                    }
                }
            }
        },
        onRestoreDefaultPath = { },
        onImportDownloadedFiles = {
            importDownloadedFiles(context, activity, dao, onCompleted = { refreshKey++ })
        },
        onDownloadCountLimitChange = { value ->
            Preferences.preferenceSp.edit { putInt(DOWNLOAD_COUNT_LIMIT, value) }
            HanimeDownloadManagerV2.maxConcurrentDownloadCount = value
            refreshKey++
        },
        onDownloadSpeedLimitChange = { value ->
            Preferences.preferenceSp.edit { putInt(DOWNLOAD_SPEED_LIMIT, value) }
            refreshKey++
        },
    )
}

@Composable
fun MpvPlayerSettingsRouteScreen() {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var activeDialog by remember { mutableStateOf<MpvChoiceDialog?>(null) }
    val uiState = remember(refreshKey, context) { buildMpvPlayerSettingsUiState(context) }

    MpvPlayerSettingsScreen(
        state = uiState,
        profileOptions = listOf(
            context.getString(R.string.profile_fast) to "fast",
            context.getString(R.string.profile_gpu_hq) to "gpu-hq",
        ),
        hwdecOptions = listOf(
            context.getString(R.string.decoding_auto) to "Auto",
            context.getString(R.string.decoding_hw) to "HW",
            context.getString(R.string.decoding_hw_plus) to "HW+",
            context.getString(R.string.decoding_vulkan_copy) to "Vulkan",
            context.getString(R.string.decoding_vulkan) to "Vulkan+",
            context.getString(R.string.decoding_sw) to "SW",
        ),
        activeDialog = activeDialog,
        onOpenProfileDialog = { activeDialog = MpvChoiceDialog.Profile },
        onOpenHwdecDialog = { activeDialog = MpvChoiceDialog.Hwdec },
        onOpenCustomParamsDialog = { activeDialog = MpvChoiceDialog.CustomParams },
        onDismissDialog = { activeDialog = null },
        onProfileChange = {
            saveString(MPV_PROFILE, it)
            refreshKey++
        },
        onEnableGpuNextRendererChange = {
            saveBoolean(ENABLE_GPU_NEXT_RENDERER, it)
            refreshKey++
        },
        onInterpolationChange = {
            saveBoolean(MPV_INTERPOLATION, it)
            refreshKey++
        },
        onDebandChange = {
            saveBoolean(MPV_DEBAND, it)
            refreshKey++
        },
        onFramedropChange = {
            saveBoolean(MPV_FRAMEDROP, it)
            refreshKey++
        },
        onHwdecChange = {
            saveString(MPV_HWDEC, it)
            refreshKey++
        },
        onCacheSecsChange = {
            Preferences.preferenceSp.edit { putInt(MPV_CACHE_SECS, it) }
            refreshKey++
        },
        onTlsVerifyChange = {
            saveBoolean(MPV_TLS_VERIFY, it)
            refreshKey++
        },
        onNetworkTimeoutChange = {
            Preferences.preferenceSp.edit { putInt(MPV_NETWORK_TIMEOUT, it) }
            refreshKey++
        },
        onCustomParamsChange = {
            saveString(CUSTOM_PARAMS, it)
            refreshKey++
        },
    )
}

@Composable
fun HKeyframesRouteScreen() {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()
    val items by viewModel.loadAllHKeyframes().collectAsStateWithLifecycle(initialValue = emptyList())
    val shareRegex = remember { Regex(">>>(.+)<<<") }

    LaunchedEffect(Unit) {
        val text = textFromClipboard
        val entity = withContext(Dispatchers.Default) {
            val matchResult = text?.let(shareRegex::find) ?: return@withContext null
            val (toBase64) = matchResult.destructured
            val toJson = toBase64.decodeFromStringByBase64()
            Json.decodeFromString<HKeyframeEntity>(toJson)
        }
        if (entity != null) {
            context.showAlertDialog {
                setTitle(R.string.h_keyframes_shared_by_other_detected)
                setMessage(
                    context.getString(
                        R.string.shared_h_keyframe_detected_msg,
                        entity.title,
                        entity.videoCode,
                        entity.keyframes.size,
                    ).trimIndent()
                )
                setPositiveButton(R.string.confirm) { _, _ ->
                    viewModel.insertHKeyframes(entity.copy(lastModifiedTime = System.currentTimeMillis()))
                }
                setNegativeButton(R.string.cancel, null)
            }
        } else {
            showShortToast(R.string.h_keyframes_shared_by_other_not_detected)
        }
    }

    HKeyframesScreen(
        items = items,
        onOpenVideo = { openVideoFromSettings(context, it) },
        onDeleteEntity = { entity ->
            viewModel.deleteHKeyframes(entity)
        },
        onUpdateEntityTitle = { entity, newTitle ->
            viewModel.updateHKeyframes(entity.copy(title = newTitle))
            showShortToast(R.string.modify_success)
        },
        onDeleteKeyframe = { videoCode, keyframe ->
            viewModel.removeHKeyframe(videoCode, keyframe)
            showShortToast(R.string.delete_success)
        },
        onUpdateKeyframe = { videoCode, oldKeyframe, newKeyframe ->
            viewModel.modifyHKeyframe(videoCode, oldKeyframe, newKeyframe)
            showShortToast(R.string.modify_success)
        },
        onCopyShareContent = {
            it.copyToClipboard()
            showShortToast(R.string.copy_to_clipboard)
        },
    )
}

@Composable
fun SharedHKeyframesRouteScreen() {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()
    val items by viewModel.loadAllSharedHKeyframes().collectAsStateWithLifecycle(initialValue = emptyList())

    SharedHKeyframesScreen(
        items = items,
        onOpenVideo = { openVideoFromSettings(context, it) },
    )
}

@Composable
fun HKeyframeSettingsRouteScreen(
    onNavigateToHKeyframes: () -> Unit,
    onNavigateToSharedHKeyframes: () -> Unit,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val uiState = remember(refreshKey, context) { buildHKeyframeSettingsUiState(context) }

    HKeyframeSettingsScreen(
        state = uiState,
        onHKeyframesEnableChange = {
            saveBoolean(H_KEYFRAMES_ENABLE, it)
            refreshKey++
        },
        onOpenHKeyframeManage = onNavigateToHKeyframes,
        onSharedHKeyframesEnableChange = {
            saveBoolean(SHARED_H_KEYFRAMES_ENABLE, it)
            refreshKey++
        },
        onSharedHKeyframesUseFirstChange = {
            saveBoolean(SHARED_H_KEYFRAMES_USE_FIRST, it)
            refreshKey++
        },
        onOpenSharedHKeyframeManage = onNavigateToSharedHKeyframes,
        onShowCommentWhenCountdownChange = {
            saveBoolean(SHOW_COMMENT_WHEN_COUNTDOWN, it)
            refreshKey++
        },
        onWhenCountdownRemindChange = {
            Preferences.preferenceSp.edit { putInt(WHEN_COUNTDOWN_REMIND, it) }
            refreshKey++
        },
    )
}

private data class LauncherItem(
    val name: String,
    @param:DrawableRes val iconRes: Int,
    val alias: String,
)

private fun saveBoolean(key: String, value: Boolean) {
    Preferences.preferenceSp.edit { putBoolean(key, value) }
}

private fun saveString(key: String, value: String) {
    Preferences.preferenceSp.edit { putString(key, value) }
}

private fun buildHomeSettingsUiState(
    context: Context,
    launcherItems: List<LauncherItem>,
    updateSummary: String,
    cacheSummary: String,
): HomeSettingsUiState {
    val currentAlias = Preferences.fakeLauncherIcon
    val currentItem = launcherItems.find { it.alias == currentAlias } ?: launcherItems.first()
    val videoLanguageLabel = when (Preferences.videoLanguage) {
        "zht" -> context.getString(R.string.traditional_chinese)
        "zhs" -> context.getString(R.string.simplified_chinese)
        else -> Preferences.videoLanguage
    }
    val darkModeLabel = when (Preferences.useDarkMode) {
        "follow_system" -> context.getString(R.string.follow_system)
        "always_off" -> context.getString(R.string.always_off)
        "always_on" -> context.getString(R.string.always_on)
        else -> Preferences.useDarkMode
    }
    val appLanguageValue = Preferences.preferenceSp.getString(HOME_APP_LANGUAGE, "system") ?: "system"
    val appLanguageLabel = when (appLanguageValue) {
        "system" -> "跟随系统"
        "zh-rCN" -> "简体中文"
        "zh" -> "繁體中文"
        "ja" -> "日本語"
        "en" -> "English"
        else -> appLanguageValue
    }
    return HomeSettingsUiState(
        videoLanguage = videoLanguageLabel,
        defaultVideoQuality = Preferences.videoQuality,
        darkMode = darkModeLabel,
        appLanguage = appLanguageLabel,
        allowPipMode = Preferences.preferenceSp.getBoolean(HOME_ALLOW_PIP_MODE, false),
        allowResumePlayback = Preferences.allowResumePlayback,
        showPlayedIndicator = Preferences.showPlayedIndicator,
        searchArtistIgnoreVideoType = Preferences.searchArtistIgnoreVideoType,
        disableMobileDataWarning = Preferences.disableMobileDataWarning,
        disablePredictiveBack = Preferences.disablePredictiveBack,
        tabletMode = Preferences.tabletMode,
        disableComments = Preferences.preferenceSp.getBoolean(HOME_DISABLE_COMMENTS, false),
        collapseDownloadedGroup = Preferences.collapseDownloadedGroup,
        useDynamicColor = Preferences.useDynamicColor,
        useCIUpdateChannel = Preferences.useCIUpdateChannel,
        useAnalytics = Preferences.isAnalyticsEnabled,
        useLockScreen = Preferences.preferenceSp.getBoolean(HOME_USE_LOCK_SCREEN, false),
        fakeLauncherIconName = currentItem.name,
        updateSummary = updateSummary,
        cacheSummary = cacheSummary,
        versionSummary = context.getString(R.string.current_version, "v${BuildConfig.VERSION_NAME}"),
        updatePopupIntervalSummary = toIntervalDaysPrettyString(context, Preferences.updatePopupIntervalDays),
        updatePopupIntervalDays = Preferences.updatePopupIntervalDays,
        dynamicColorEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
}

private fun buildPlayerSettingsUiState(context: Context): PlayerSettingsUiState {
    val kernel = Preferences.switchPlayerKernel
    val isMpvPlayer = kernel == HMediaKernel.Type.MpvPlayer.name
    val currentSpeed = Preferences.playerSpeed
    val currentLongPressSpeed = Preferences.longPressSpeedTime
    val speedDisplay = HJzvdStd.speedStringArray.getOrElse(
        HJzvdStd.speedArray.indexOfFirst { it == currentSpeed }.takeIf { it >= 0 } ?: HJzvdStd.DEF_SPEED_INDEX
    ) { HJzvdStd.speedStringArray[HJzvdStd.DEF_SPEED_INDEX] }
    return PlayerSettingsUiState(
        kernel = kernel,
        kernelDisplay = kernel,
        mpvSettingsEnabled = isMpvPlayer,
        mpvSettingsSummary = if (isMpvPlayer) {
            context.getString(R.string.mpv_advanced_settings_summary)
        } else {
            context.getString(R.string.mpv_settings_disabled_summary)
        },
        showBottomProgress = Preferences.showBottomProgress,
        playerSpeed = speedDisplay,
        longPressSpeedTimes = context.getString(R.string.d_speed_times, currentLongPressSpeed),
        slideSensitivity = Preferences.slideSensitivity,
        slideSensitivitySummary = toPrettySensitivityString(context, Preferences.slideSensitivity),
    )
}

private fun buildNetworkSettingsUiState(context: Context): NetworkSettingsUiState {
    return NetworkSettingsUiState(
        domainName = Preferences.baseUrl,
        domainDisplay = buildDomainOptions(context).firstOrNull { it.second == Preferences.baseUrl }?.first ?: Preferences.baseUrl,
        proxySummary = when (Preferences.proxyType) {
            HProxySelector.TYPE_DIRECT -> context.getString(R.string.direct)
            HProxySelector.TYPE_SYSTEM -> context.getString(R.string.system_proxy)
            HProxySelector.TYPE_HTTP -> context.getString(R.string.http_proxy, Preferences.proxyIp, Preferences.proxyPort)
            HProxySelector.TYPE_SOCKS -> context.getString(R.string.socks_proxy, Preferences.proxyIp, Preferences.proxyPort)
            else -> context.getString(R.string.direct)
        },
        useBuiltInHosts = Preferences.useBuiltInHosts,
        delaySummary = context.getString(R.string.node_latency_sum),
    )
}

private fun buildDownloadSettingsUiState(context: Context): DownloadSettingsUiState {
    val uri = SafFileManager.getSavedUri()
    val pathSummary = if (Preferences.isUsePrivateStorage) {
        context.getExternalFilesDir(null)?.absolutePath.orEmpty()
    } else {
        DocumentFile.fromTreeUri(
            context,
            uri ?: return DownloadSettingsUiState(
                downloadPathSummary = "null",
                downloadCountLimit = Preferences.downloadCountLimit,
                downloadCountLimitSummary = toDownloadCountLimitPrettyString(context, Preferences.downloadCountLimit),
                downloadSpeedLimitIndex = Preferences.preferenceSp.getInt(
                    DOWNLOAD_SPEED_LIMIT,
                    SpeedLimitInterceptor.NO_LIMIT_INDEX,
                ),
                downloadSpeedLimitSummary = SpeedLimitInterceptor.SPEED_BYTES[
                    Preferences.preferenceSp.getInt(
                        DOWNLOAD_SPEED_LIMIT,
                        SpeedLimitInterceptor.NO_LIMIT_INDEX,
                    )
                ].toDownloadSpeedPrettyString(context),
            )
        )?.name ?: uri.toString()
    }
    val speedIndex = Preferences.preferenceSp.getInt(
        DOWNLOAD_SPEED_LIMIT,
        SpeedLimitInterceptor.NO_LIMIT_INDEX,
    )
    return DownloadSettingsUiState(
        downloadPathSummary = pathSummary,
        downloadCountLimit = Preferences.downloadCountLimit,
        downloadCountLimitSummary = toDownloadCountLimitPrettyString(context, Preferences.downloadCountLimit),
        downloadSpeedLimitIndex = speedIndex,
        downloadSpeedLimitSummary = SpeedLimitInterceptor.SPEED_BYTES[speedIndex]
            .toDownloadSpeedPrettyString(context),
    )
}

private fun buildMpvPlayerSettingsUiState(context: Context): MpvPlayerSettingsUiState {
    val profile = Preferences.mpvProfile
    val hwdec = Preferences.mpvHwdec
    return MpvPlayerSettingsUiState(
        profile = profile,
        profileDisplay = when (profile) {
            "fast" -> context.getString(R.string.profile_fast)
            "gpu-hq" -> context.getString(R.string.profile_gpu_hq)
            else -> profile
        },
        enableGpuNextRenderer = Preferences.enableGPUNextRenderer,
        interpolation = Preferences.mpvInterpolation,
        deband = Preferences.mpvDeband,
        framedrop = Preferences.mpvFramedrop,
        hwdec = hwdec,
        hwdecDisplay = "${context.getString(R.string.mpv_hwdec_summary)} ($hwdec)",
        cacheSecs = Preferences.mpvCacheSecs,
        cacheSecsSummary = "${context.getString(R.string.mpv_cache_secs_summary)} (${Preferences.mpvCacheSecs} S)",
        tlsVerify = Preferences.mpvTlsVerify,
        networkTimeout = Preferences.mpvNetworkTimeout,
        networkTimeoutSummary = "${context.getString(R.string.mpv_network_timeout_summary)} (${Preferences.mpvNetworkTimeout} S)",
        customParams = Preferences.customMpvParams,
    )
}

private fun buildHKeyframeSettingsUiState(context: Context): HKeyframeSettingsUiState {
    return HKeyframeSettingsUiState(
        hKeyframesEnable = Preferences.hKeyframesEnable,
        hKeyframesSummary = if (Preferences.hKeyframesEnable) {
            context.getString(R.string.h_keyframes_enable_tip)
        } else {
            context.getString(R.string.h_keyframes_disable_tip)
        },
        sharedHKeyframesEnable = Preferences.sharedHKeyframesEnable,
        sharedHKeyframesUseFirst = Preferences.sharedHKeyframesUseFirst,
        showCommentWhenCountdown = Preferences.showCommentWhenCountdown,
        whenCountdownRemind = Preferences.whenCountdownRemind / 1000,
        whenCountdownRemindSummary = toPrettyCountdownRemindString(context, Preferences.whenCountdownRemind / 1000),
    )
}

private fun buildDomainOptions(context: Context): List<Pair<String, String>> = listOf(
    "${HANIME_HOSTNAME[0]} (${context.getString(R.string.default_)})" to HANIME_URL[0],
    "${HANIME_HOSTNAME[1]} (${context.getString(R.string.alternative)})" to HANIME_URL[1],
    "${HANIME_HOSTNAME[2]} (${context.getString(R.string.alternative)})" to HANIME_URL[2],
    "${HANIME_HOSTNAME[3]} (av)" to HANIME_URL[3],
)

private fun importDownloadedFiles(
    context: Context,
    activity: Activity,
    dao: HanimeDownloadDao,
    onCompleted: () -> Unit,
) {
    if (!Preferences.isUsePrivateStorage &&
        !Preferences.safDownloadPath.isNullOrBlank() &&
        checkSafPermissions(context)
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.confirm_import))
            .setMessage(context.getString(R.string.import_warning))
            .setPositiveButton(R.string.ok) { _, _ ->
                val dialogView = activity.layoutInflater.inflate(R.layout.layout_dialog_progress, null, false)
                val titleTv = dialogView.findViewById<MaterialTextView>(R.id.progress_title)
                val percentTv = dialogView.findViewById<MaterialTextView>(R.id.progress_value)
                val progressBar = dialogView.findViewById<LinearProgressIndicator>(R.id.progress_bar)

                val progressDialog = MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(R.string.import_progress))
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()
                progressDialog.show()
                migratePrivateToSaf(context, dao) { migrated, total ->
                    Log.i("migrate", "$migrated,$total")
                    when (total) {
                        0 -> {
                            progressDialog.dismiss()
                            showLongToast(context.getString(R.string.no_exportable_files))
                            return@migratePrivateToSaf
                        }

                        -1 -> {
                            progressDialog.dismiss()
                            showLongToast(context.getString(R.string.permission_error))
                            return@migratePrivateToSaf
                        }
                    }
                    val percent = migrated * 100 / total
                    titleTv.text = context.getString(R.string.importing)
                    progressBar.max = 100
                    progressBar.progress = percent
                    percentTv.text = context.getString(R.string.import_progress_format).format(migrated, total, percent)

                    if (migrated == total) {
                        progressDialog.dismiss()
                        showLongToast(context.getString(R.string.import_complete, total))
                        onCompleted()
                    }
                }
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    } else {
        context.showAlertDialog {
            setTitle(context.getString(R.string.specify_path_first))
            setMessage(context.getString(R.string.path_permission_message))
            setPositiveButton(R.string.understood) { _, _ -> }
        }
    }
}

private fun openVideoFromSettings(context: Context, videoCode: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("startVideoCode", videoCode)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    context.startActivity(intent)
}

private fun generateClearCacheSummary(context: Context, size: Long): CharSequence {
    return context.getString(R.string.cache_usage_summary, size.formatFileSizeV2()).parseAsHtml()
}

@OptIn(ExperimentalTime::class)
private fun toIntervalDaysPrettyString(context: Context, value: Int): String {
    val lastUpdatePopupTime = Preferences.lastUpdatePopupTime
    val msg = if (lastUpdatePopupTime == 0L) {
        context.getString(R.string.no_update_popup_yet)
    } else {
        context.getString(
            R.string.last_update_popup_check_time,
            Instant.fromEpochSeconds(lastUpdatePopupTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .format(LocalDateTime.Formats.ISO),
        )
    }
    return when (value) {
        0 -> context.getString(R.string.at_any_time)
        else -> context.getString(R.string.which_days, value)
    } + "\n" + msg
}

private fun toPrettySensitivityString(context: Context, @IntRange(from = 1, to = 9) value: Int): String {
    val pretty = when (value) {
        1, 2 -> context.getString(R.string.high)
        3, 4 -> context.getString(R.string.moderately_high)
        5 -> context.getString(R.string.moderate)
        6 -> context.getString(R.string.slightly_low)
        7 -> context.getString(R.string.low)
        8 -> context.getString(R.string.very_low)
        9 -> context.getString(R.string.extremely_low)
        else -> error("Invalid sensitivity value: $value")
    }
    return context.getString(R.string.current_slide_sensitivity, pretty)
}

private fun toPrettyCountdownRemindString(context: Context, @IntRange(from = 5, to = 30) value: Int): String {
    return buildString {
        append(context.getString(R.string.will_remind_before_d_seconds, value))
        if (value == HJzvdStd.DEF_COUNTDOWN_SEC) append(" (${context.getString(R.string.default_)})")
    }
}

private fun Long.toDownloadSpeedPrettyString(context: Context): String {
    return if (this == 0L) {
        context.getString(R.string.no_limit)
    } else {
        formatBytesPerSecond()
    }
}

private fun toDownloadCountLimitPrettyString(context: Context, value: Int): String {
    return if (value == 0) context.getString(R.string.no_limit) else value.toString()
}

private fun isDeviceSecureCompat(context: Context): Boolean {
    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return km.isDeviceSecure
}

private fun isPipPermissionGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
            android.os.Process.myUid(),
            context.packageName,
        )
        mode == AppOpsManager.MODE_ALLOWED
    } else {
        true
    }
}

private fun openPipPermissionSettings(context: Context) {
    val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", "package:${context.packageName}".toUri())
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

private fun showApplyDeepLinksDialog(context: Context, activity: Activity) {
    context.showAlertDialog {
        setTitle(R.string.apply_deep_links)
        setView(R.layout.dialog_apply_deep_links)
        setPositiveButton(R.string.go_to_settings) { _, _ ->
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                    addCategory(Intent.CATEGORY_DEFAULT)
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                showShortToast(R.string.action_app_open_by_default_settings_not_support)
                e.printStackTrace()
            }
        }
        setNegativeButton(R.string.cancel, null)
    }
}
