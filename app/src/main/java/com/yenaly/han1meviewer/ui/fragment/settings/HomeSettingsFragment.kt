@file:Suppress("DEPRECATION")
package com.yenaly.han1meviewer.ui.fragment.settings

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.mikepenz.aboutlibraries.LibsBuilder
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.HA1_GITHUB_FORUM_URL
import com.yenaly.han1meviewer.HA1_GITHUB_ISSUE_URL
import com.yenaly.han1meviewer.HA1_GITHUB_RELEASES_URL
import com.yenaly.han1meviewer.HanimeApplication
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.SettingsRouter
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.settings.HomeSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.HomeSettingsUiState
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.util.ThemeUtils
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.util.showUpdateDialog
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.folderSize
import com.yenaly.yenaly_libs.utils.formatFileSizeV2
import com.yenaly.yenaly_libs.utils.showLongToast
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

class HomeSettingsFragment : androidx.fragment.app.Fragment() {

    companion object {
        const val VIDEO_LANGUAGE = "video_language"
        const val DEFAULT_VIDEO_QUALITY = "default_video_quality"
        const val SHOW_PLAYED_INDICATOR = "show_played_indicator"
        const val ALLOW_PIP_MODE = "allow_pip_mode"
        const val PLAYER_SETTINGS = "player_settings"
        const val H_KEYFRAME_SETTINGS = "h_keyframe_settings"
        const val UPDATE = "update"
        const val ABOUT = "about"
        const val CLEAR_CACHE = "clear_cache"
        const val SUBMIT_BUG = "submit_bug"
        const val FORUM = "forum"
        const val NETWORK_SETTINGS = "network_settings"
        const val APPLY_DEEP_LINKS = "apply_deep_links"
        const val DOWNLOAD_SETTINGS = "download_settings"

        const val LAST_UPDATE_POPUP_TIME = "last_update_popup_time"
        const val UPDATE_POPUP_INTERVAL_DAYS = "update_popup_interval_days"
        const val USE_CI_UPDATE_CHANNEL = "use_ci_update_channel"
        const val USE_ANALYTICS = "use_analytics"
        const val FAKE_LAUNCHER_ICON = "pref_fake_launcher_icon"
        const val USE_DARK_MODE = "use_dark_mode"
        const val USE_DYNAMIC_COLOR = "use_dynamic_color"
        const val ALLOW_RESUME_PLAYBACK = "allow_resume_playback"
        const val SEARCH_ARTIST_IGNORE_VIDEO_TYPE = "search_artist_ignore_video_type"
        const val DISABLE_MOBILE_DATA_WARNING = "disable_mobile_data_warning"
        const val COLLAPSE_DOWNLOADED_GROUP = "collapse_downloaded_group"
        const val DISABLE_PREDICTIVE_BACK = "disable_predictive_back"
        const val TABLET_MODE = "tablet_mode"
    }

    data class LauncherItem(
        val name: String,
        @param:DrawableRes val iconRes: Int,
        val alias: String,
    )

    private var checkUpdateTimes = 0
    private var updateSummary: String = ""
    private var cacheSummary: String = ""
    private var uiState by mutableStateOf<HomeSettingsUiState?>(null)

    private val launcherItems by lazy {
        listOf(
            LauncherItem(getString(R.string.hanime_app_name), R.drawable.ic_launcher_new, "com.yenaly.han1meviewer.LauncherAliasDefault"),
            LauncherItem(getString(R.string.app_name_fake_calc), R.drawable.ic_launcher_calc, "com.yenaly.han1meviewer.LauncherFakeCalc"),
            LauncherItem(getString(R.string.app_name_fake_cornhub), R.drawable.ic_launcher_cornhub, "com.yenaly.han1meviewer.LauncherFakeCornhub"),
            LauncherItem(getString(R.string.app_name_fake_xxt), R.drawable.ic_launcher_xxt, "com.yenaly.han1meviewer.LauncherFakeXxt"),
        )
    }

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.showToolbar()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.settings),
            canNavigateBack = true,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        updateSummary = getString(R.string.checking_update)
        cacheSummary = generateClearCacheSummary(requireContext().cacheDir?.folderSize ?: 0L).toString()
        uiState = buildUiState()

        return ComposeView(requireContext()).apply {
            setContent {
                HanimeTheme {
                    uiState?.let { state ->
                        HomeSettingsScreen(
                            state = state,
                            onVideoLanguageChange = ::onVideoLanguageChanged,
                            onVideoQualityChange = ::onVideoQualityChanged,
                            onDarkModeChange = ::onDarkModeChanged,
                            onAllowPipModeChange = ::onAllowPipModeChanged,
                            onAllowResumePlaybackChange = { saveBoolean(ALLOW_RESUME_PLAYBACK, it) },
                            onShowPlayedIndicatorChange = { saveBoolean(SHOW_PLAYED_INDICATOR, it) },
                            onSearchArtistIgnoreVideoTypeChange = { saveBoolean(SEARCH_ARTIST_IGNORE_VIDEO_TYPE, it) },
                            onDisableMobileDataWarningChange = { saveBoolean(DISABLE_MOBILE_DATA_WARNING, it) },
                            onDisablePredictiveBackChange = ::onDisablePredictiveBackChanged,
                            onTabletModeChange = { saveBoolean(TABLET_MODE, it) },
                            onDisableCommentsChange = { saveBoolean("disable_comments", it) },
                            onCollapseDownloadedGroupChange = { saveBoolean(COLLAPSE_DOWNLOADED_GROUP, it) },
                            onUseDynamicColorChange = ::onDynamicColorChanged,
                            onUseCIUpdateChannelChange = ::onUseCiUpdateChannelChanged,
                            onUseAnalyticsChange = ::onUseAnalyticsChanged,
                            onUseLockScreenChange = ::onUseLockScreenChanged,
                            onOpenPlayerSettings = { SettingsRouter.with(this@HomeSettingsFragment).navigateWithinSettings(R.id.playerSettingsFragment) },
                            onOpenHKeyframeSettings = { SettingsRouter.with(this@HomeSettingsFragment).navigateWithinSettings(R.id.hKeyframeSettingsFragment) },
                            onOpenDownloadSettings = { SettingsRouter.with(this@HomeSettingsFragment).navigateWithinSettings(R.id.downloadSettingsFragment) },
                            onOpenNetworkSettings = { SettingsRouter.with(this@HomeSettingsFragment).navigateWithinSettings(R.id.networkSettingsFragment) },
                            onOpenAppLanguageSettings = ::onAppLanguageChanged,
                            onCheckUpdate = ::onCheckUpdateClick,
                            onUpdatePopupIntervalDaysChange = ::onUpdatePopupIntervalDaysChanged,
                            onOpenApplyDeepLinks = { showApplyDeepLinksDialog(requireContext()) },
                            onOpenFakeLauncherIcon = ::showFakeLauncherDialog,
                            onOpenOpenSourceLicense = ::openOpenSourceLicense,
                            onOpenAbout = {},
                            onClearCache = ::clearCache,
                            onSubmitBug = { browse(HA1_GITHUB_ISSUE_URL) },
                            onOpenForum = { browse(HA1_GITHUB_FORUM_URL) },
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFlow()
    }

    private fun buildUiState(): HomeSettingsUiState {
        val currentAlias = Preferences.fakeLauncherIcon
        val currentItem = launcherItems.find { it.alias == currentAlias } ?: launcherItems.first()
        val videoLanguageLabel = when (Preferences.videoLanguage) {
            "zht" -> getString(R.string.traditional_chinese)
            "zhs" -> getString(R.string.simplified_chinese)
            else -> Preferences.videoLanguage
        }
        val darkModeLabel = when (Preferences.useDarkMode) {
            "follow_system" -> getString(R.string.follow_system)
            "always_off" -> getString(R.string.always_off)
            "always_on" -> getString(R.string.always_on)
            else -> Preferences.useDarkMode
        }
        val appLanguageValue = Preferences.preferenceSp.getString("app_language", "system") ?: "system"
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
            allowPipMode = Preferences.preferenceSp.getBoolean(ALLOW_PIP_MODE, false),
            allowResumePlayback = Preferences.allowResumePlayback,
            showPlayedIndicator = Preferences.showPlayedIndicator,
            searchArtistIgnoreVideoType = Preferences.searchArtistIgnoreVideoType,
            disableMobileDataWarning = Preferences.disableMobileDataWarning,
            disablePredictiveBack = Preferences.disablePredictiveBack,
            tabletMode = Preferences.tabletMode,
            disableComments = Preferences.preferenceSp.getBoolean("disable_comments", false),
            collapseDownloadedGroup = Preferences.collapseDownloadedGroup,
            useDynamicColor = Preferences.useDynamicColor,
            useCIUpdateChannel = Preferences.useCIUpdateChannel,
            useAnalytics = Preferences.isAnalyticsEnabled,
            useLockScreen = Preferences.preferenceSp.getBoolean("use_lock_screen", false),
            fakeLauncherIconName = currentItem.name,
            updateSummary = updateSummary,
            cacheSummary = cacheSummary,
            versionSummary = getString(R.string.current_version, "v${BuildConfig.VERSION_NAME}"),
            updatePopupIntervalSummary = toIntervalDaysPrettyString(Preferences.updatePopupIntervalDays),
            updatePopupIntervalDays = Preferences.updatePopupIntervalDays,
            dynamicColorEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        )
    }

    private fun saveBoolean(key: String, value: Boolean) {
        Preferences.preferenceSp.edit { putBoolean(key, value) }
        uiState = buildUiState()
    }

    private fun saveString(key: String, value: String) {
        Preferences.preferenceSp.edit { putString(key, value) }
        uiState = buildUiState()
    }

    private fun onVideoLanguageChanged(value: String) {
        if (value != Preferences.videoLanguage) {
            saveString(VIDEO_LANGUAGE, value)
            requireContext().showAlertDialog {
                setCancelable(false)
                setTitle(R.string.attention)
                setMessage(getString(R.string.restart_or_not_working, getString(R.string.video_language)))
                setPositiveButton(R.string.confirm) { _, _ -> ActivityManager.restart(killProcess = true) }
                setNegativeButton(R.string.cancel, null)
            }
        }
    }

    private fun onVideoQualityChanged(value: String) {
        saveString(DEFAULT_VIDEO_QUALITY, value)
        Toast.makeText(requireContext(), "Success：$value", Toast.LENGTH_SHORT).show()
    }

    private fun onDarkModeChanged(value: String) {
        if (value != Preferences.useDarkMode) {
            saveString(USE_DARK_MODE, value)
            ThemeUtils.applyDarkModeFromPreferences(requireContext())
            requireActivity().recreate()
        }
    }

    private fun onDynamicColorChanged(value: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (value != Preferences.useDynamicColor) {
            Preferences.preferenceSp.edit { putBoolean(USE_DYNAMIC_COLOR, value) }
            requireContext().showAlertDialog {
                setCancelable(false)
                setTitle(R.string.attention)
                setMessage(getString(R.string.restart_or_not_working, getString(R.string.dynamic_color_title)))
                setPositiveButton(R.string.confirm) { _, _ -> ActivityManager.restart(killProcess = true) }
                setNegativeButton(R.string.cancel, null)
            }
        }
    }

    private fun onAllowPipModeChanged(enabled: Boolean) {
        if (enabled && !isPipPermissionGranted(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.request_pip_alert), Toast.LENGTH_SHORT).show()
            openPipPermissionSettings(requireContext())
            saveBoolean(ALLOW_PIP_MODE, false)
            return
        }
        saveBoolean(ALLOW_PIP_MODE, enabled)
    }

    private fun onDisablePredictiveBackChanged(value: Boolean) {
        saveBoolean(DISABLE_PREDICTIVE_BACK, value)
        activity?.recreate()
    }

    private fun onUseCiUpdateChannelChanged(value: Boolean) {
        saveBoolean(USE_CI_UPDATE_CHANNEL, value)
        AppViewModel.getLatestVersion()
    }

    private fun onUseAnalyticsChanged(value: Boolean) {
        if (!value) {
            requireContext().showAlertDialog {
                setTitle(R.string.about_analytics)
                setMessage(requireContext().getString(R.string.about_analytics_summary).parseAsHtml())
                setCancelable(false)
                setPositiveButton(R.string.ok, null)
                setNeutralButton(R.string.deny) { _, _ ->
                    saveBoolean(USE_ANALYTICS, false)
                    Firebase.analytics.setAnalyticsCollectionEnabled(false)
                }
            }
            return
        }
        saveBoolean(USE_ANALYTICS, true)
        Firebase.analytics.setAnalyticsCollectionEnabled(true)
    }

    private fun showAppLanguageNotice() {
        Handler(Looper.getMainLooper()).post {
            activity?.recreate()
        }
    }

    private fun onAppLanguageChanged(value: String) {
        val old = Preferences.preferenceSp.getString("app_language", "system") ?: "system"
        if (old != value) {
            Preferences.preferenceSp.edit { putString("app_language", value) }
            uiState = buildUiState()
            showAppLanguageNotice()
        }
    }

    private fun onUseLockScreenChanged(value: Boolean) {
        if (value) {
            if (!isDeviceSecureCompat(requireContext())) {
                Toast.makeText(requireContext(), getString(R.string.not_set_sys_lock), Toast.LENGTH_LONG).show()
                uiState = buildUiState()
                return
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                Toast.makeText(requireContext(), getString(R.string.not_compact_lock_screen), Toast.LENGTH_LONG).show()
                uiState = buildUiState()
                return
            }
        }
        saveBoolean("use_lock_screen", value)
    }

    private fun onCheckUpdateClick() {
        AppViewModel.getLatestVersion()
    }

    private fun onUpdatePopupIntervalDaysChanged(value: Int) {
        Preferences.preferenceSp.edit { putInt(UPDATE_POPUP_INTERVAL_DAYS, value) }
        uiState = buildUiState()
    }

    private fun clearCache() {
        val cacheDir = requireContext().cacheDir
        val folderSize = cacheDir?.folderSize ?: 0L
        if (folderSize == 0L) {
            showShortToast(R.string.cache_empty)
            return
        }
        requireContext().showAlertDialog {
            setTitle(R.string.sure_to_clear)
            setMessage(R.string.sure_to_clear_cache)
            setPositiveButton(R.string.confirm) { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val success = cacheDir?.deleteRecursively() == true
                    val newSize = cacheDir.folderSize
                    withContext(Dispatchers.Main) {
                        cacheSummary = generateClearCacheSummary(newSize).toString()
                        if (success) showShortToast(R.string.clear_success) else showShortToast(R.string.clear_failed)
                        uiState = buildUiState()
                    }
                }
            }
            setNegativeButton(R.string.cancel, null)
        }
    }

    private fun showFakeLauncherDialog() {
        val adapter = object : ArrayAdapter<LauncherItem>(requireContext(), 0, launcherItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_simple_icon_with_text, parent, false)
                val item = getItem(position)!!
                view.findViewById<ImageView>(R.id.icon).setImageResource(item.iconRes)
                view.findViewById<TextView>(R.id.name).text = item.name
                return view
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.fake_app_icon))
            .setAdapter(adapter) { _, which ->
                val selected = launcherItems[which]
                Preferences.preferenceSp.edit { putString(FAKE_LAUNCHER_ICON, selected.alias) }
                (requireContext().applicationContext as? HanimeApplication)?.switchLauncher(selected.alias)
                showLongToast(getString(R.string.fake_icon_hint))
                uiState = buildUiState()
            }
            .show()
    }

    private fun openOpenSourceLicense() {
        LibsBuilder()
            .withShowLoadingProgress(true)
            .withSearchEnabled(false)
            .withActivityTitle(getString(R.string.open_source_license))
            .withAboutIconShown(true)
            .withAboutVersionShown(true)
            .start(requireContext())
    }

    private fun initFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppViewModel.versionFlow.collect { state ->
                    updateSummary = when (state) {
                        is WebsiteState.Error -> {
                            checkUpdateTimes++
                            getString(R.string.check_update_failed)
                        }

                        is WebsiteState.Loading -> getString(R.string.checking_update)

                        is WebsiteState.Success -> {
                            if (state.info == null) {
                                getString(R.string.already_latest_update)
                            } else {
                                getString(R.string.check_update_success, state.info.version)
                            }
                        }
                    }
                    uiState = buildUiState()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showApplyDeepLinksDialog(context: Context) {
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
                    requireActivity().startActivity(intent)
                } catch (e: Exception) {
                    showShortToast(R.string.action_app_open_by_default_settings_not_support)
                    e.printStackTrace()
                }
            }
            setNegativeButton(R.string.cancel, null)
        }
    }

    private fun generateClearCacheSummary(size: Long): CharSequence {
        return getString(R.string.cache_usage_summary, size.formatFileSizeV2()).parseAsHtml()
    }

    @OptIn(ExperimentalTime::class)
    private fun toIntervalDaysPrettyString(value: Int): String {
        val lastUpdatePopupTime = Preferences.lastUpdatePopupTime
        val msg = if (lastUpdatePopupTime == 0L) {
            getString(R.string.no_update_popup_yet)
        } else {
            getString(
                R.string.last_update_popup_check_time,
                Instant.fromEpochSeconds(lastUpdatePopupTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .format(LocalDateTime.Formats.ISO)
            )
        }
        return when (value) {
            0 -> getString(R.string.at_any_time)
            else -> getString(R.string.which_days, value)
        } + "\n" + msg
    }

    private fun isDeviceSecureCompat(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isDeviceSecure
    }

    fun isPipPermissionGranted(context: Context): Boolean {
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
}
