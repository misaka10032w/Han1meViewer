package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import android.os.Build
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.core.text.parseAsHtml
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.HA1_GITHUB_FORUM_URL
import com.yenaly.han1meviewer.HA1_GITHUB_ISSUE_URL
import com.yenaly.han1meviewer.HanimeApplication
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.BackupManager
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.screen.settings.HomeSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.dialog.LicenseDialog
import com.yenaly.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import com.yenaly.han1meviewer.ui.theme.ThemeColorPreset
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.util.ThemeUtils
import com.yenaly.han1meviewer.util.showToast
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.utils.applicationContext
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.folderSize
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HOME_VIDEO_LANGUAGE = "video_language"
private const val HOME_DEFAULT_VIDEO_QUALITY = "default_video_quality"
private const val HOME_SHOW_PLAYED_INDICATOR = "show_played_indicator"
private const val HOME_ALLOW_PIP_MODE = "allow_pip_mode"
private const val HOME_UPDATE_POPUP_INTERVAL_DAYS = "update_popup_interval_days"
private const val HOME_USE_CI_UPDATE_CHANNEL = "use_ci_update_channel"
private const val HOME_USE_ANALYTICS = "use_analytics"
private const val HOME_FAKE_LAUNCHER_ICON = "pref_fake_launcher_icon"
private const val HOME_USE_DARK_MODE = "use_dark_mode"
private const val HOME_ALLOW_RESUME_PLAYBACK = "allow_resume_playback"
private const val HOME_SEARCH_ARTIST_IGNORE_VIDEO_TYPE = "search_artist_ignore_video_type"
private const val HOME_DISABLE_MOBILE_DATA_WARNING = "disable_mobile_data_warning"
private const val HOME_COLLAPSE_DOWNLOADED_GROUP = "collapse_downloaded_group"
private const val HOME_DISABLE_PREDICTIVE_BACK = "disable_predictive_back"
private const val HOME_TABLET_MODE = "tablet_mode"
private const val HOME_DISABLE_COMMENTS = "disable_comments"
private const val HOME_USE_LOCK_SCREEN = "use_lock_screen"
private const val HOME_APP_LANGUAGE = "app_language"
private const val HOME_THEME_COLOR = "theme_color"
private const val HOME_SEARCH_GRID_COLUMNS_COMPACT = "search_grid_columns_compact"
private const val HOME_SEARCH_GRID_COLUMNS_MEDIUM = "search_grid_columns_medium"
private const val HOME_SEARCH_GRID_COLUMNS_EXPANDED = "search_grid_columns_expanded"
private const val HOME_SEARCH_GRID_COLUMNS_LARGE = "search_grid_columns_large"
private const val HOME_HORIZONTAL_CARD_COUNT_NARROW = "horizontal_card_count_narrow"
private const val HOME_HORIZONTAL_CARD_COUNT_COMPACT = "horizontal_card_count_compact"
private const val HOME_HORIZONTAL_CARD_COUNT_MEDIUM = "horizontal_card_count_medium"
private const val HOME_HORIZONTAL_CARD_COUNT_EXPANDED = "horizontal_card_count_expanded"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSettingsRouteScreen(
    activity: MainActivity,
    onNavigateToPlayerSettings: () -> Unit,
    onNavigateToHKeyframeSettings: () -> Unit,
    onNavigateToDownloadSettings: () -> Unit,
    onNavigateToNetworkSettings: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val versionState by AppViewModel.versionFlow.collectAsStateWithLifecycle()
    var refreshKey by remember { mutableIntStateOf(0) }
    var cacheKey by remember { mutableIntStateOf(0) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showLicenseScreen by remember { mutableStateOf(false) }
    var showRestartConfirmDialog by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var showLauncherPicker by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            runCatching { BackupManager.exportTo(context, uri) }
                .onSuccess { withContext(Dispatchers.Main) { showShortToast(R.string.backup_export_success) } }
                .onFailure { withContext(Dispatchers.Main) { showShortToast(R.string.backup_export_failed) } }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImportUri = uri
    }

    val hanimeAppName = stringResource(R.string.hanime_app_name)
    val fakeNameCalc = stringResource(R.string.app_name_fake_calc)
    val fakeNameCornhub = stringResource(R.string.app_name_fake_cornhub)
    val fakeNameXXT = stringResource(R.string.app_name_fake_xxt)

    val launcherItems = remember(context) {
        listOf(
            LauncherItem(
                name = hanimeAppName,
                iconRes = R.drawable.ic_launcher_new,
                alias = "com.yenaly.han1meviewer.LauncherAliasDefault",
            ),
            LauncherItem(
                name = fakeNameCalc,
                iconRes = R.drawable.ic_launcher_calc,
                alias = "com.yenaly.han1meviewer.LauncherFakeCalc",
            ),
            LauncherItem(
                name = fakeNameCornhub,
                iconRes = R.drawable.ic_launcher_cornhub,
                alias = "com.yenaly.han1meviewer.LauncherFakeCornhub",
            ),
            LauncherItem(
                name = fakeNameXXT,
                iconRes = R.drawable.ic_launcher_xxt,
                alias = "com.yenaly.han1meviewer.LauncherFakeXxt",
            ),
        )
    }

    var cacheSummary by remember { mutableStateOf("") }

    LaunchedEffect(cacheKey) {
        cacheSummary = withContext(Dispatchers.IO) {
            generateClearCacheSummary(context, context.cacheDir?.folderSize ?: 0L).toString()
        }
    }
    val checkUpdateFailed = stringResource(R.string.check_update_failed)
    val checkingUpdate = stringResource(R.string.checking_update)
    val alreadyLatestUpdate = stringResource(R.string.already_latest_update)

    val updateSummary = remember(versionState, context) {
        when (versionState) {
            is WebsiteState.Error -> checkUpdateFailed
            is WebsiteState.Loading -> checkingUpdate
            is WebsiteState.Success -> {
                val info = (versionState as WebsiteState.Success).info
                if (info == null) {
                    alreadyLatestUpdate
                } else {
                    applicationContext.getString(R.string.check_update_success, info.version)
                }
            }
        }
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
                showRestartConfirmDialog = true
            }
        },
        onVideoQualityChange = { value ->
            saveString(HOME_DEFAULT_VIDEO_QUALITY, value)
            refreshKey++
            context.showToast(R.string.success_value, value)
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
                context.showToast(R.string.request_pip_alert)
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
        onSearchGridColumnsConfigChange = { config ->
            saveInt(HOME_SEARCH_GRID_COLUMNS_COMPACT, config.compactColumns)
            saveInt(HOME_SEARCH_GRID_COLUMNS_MEDIUM, config.mediumColumns)
            saveInt(HOME_SEARCH_GRID_COLUMNS_EXPANDED, config.expandedColumns)
            saveInt(HOME_SEARCH_GRID_COLUMNS_LARGE, config.largeColumns)
            refreshKey++
        },
        onHorizontalCardCountConfigChange = { config ->
            saveString(HOME_HORIZONTAL_CARD_COUNT_NARROW, config.narrowCount.toString())
            saveString(HOME_HORIZONTAL_CARD_COUNT_COMPACT, config.compactCount.toString())
            saveString(HOME_HORIZONTAL_CARD_COUNT_MEDIUM, config.mediumCount.toString())
            saveString(HOME_HORIZONTAL_CARD_COUNT_EXPANDED, config.expandedCount.toString())
            refreshKey++
        },
        onThemeColorChange = { key ->
            saveString(HOME_THEME_COLOR, key)
            refreshKey++
            activity.recreate()
        },
        onUseCIUpdateChannelChange = { value ->
            saveBoolean(HOME_USE_CI_UPDATE_CHANNEL, value)
            refreshKey++
            AppViewModel.getLatestVersion()
        },
        onUseAnalyticsChange = { value ->
            if (!value) {
                showAnalyticsDialog = true
                return@HomeSettingsScreen
            }
            saveBoolean(HOME_USE_ANALYTICS, true)
            refreshKey++
            Firebase.analytics.setAnalyticsCollectionEnabled(true)
        },
        onUseLockScreenChange = { value ->
            if (value) {
                if (!isDeviceSecureCompat(context)) {
                    context.showToast(R.string.not_set_sys_lock)
                    refreshKey++
                    return@HomeSettingsScreen
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    context.showToast(R.string.not_compact_lock_screen)
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
        onCheckUpdate = {
            val currentVersion = versionState
            if (currentVersion is WebsiteState.Success && currentVersion.info != null) {
                AppViewModel.showUpdateDialogIfAvailable()
            } else {
                AppViewModel.getLatestVersion(forceShow = true)
            }
        },
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
        onOpenFakeLauncherIcon = { showLauncherPicker = true },
        onOpenOpenSourceLicense = { showLicenseScreen = true },
        onOpenAbout = {},
        onClearCache = {
            val cacheDir = context.cacheDir
            val folderSize = cacheDir?.folderSize ?: 0L
            if (folderSize == 0L) {
                showShortToast(R.string.cache_empty)
                return@HomeSettingsScreen
            }
            showClearCacheConfirm = true
        },
        onExportBackup = {
            exportLauncher.launch("Han1meViewer-backup-${System.currentTimeMillis()}.json")
        },
        onImportBackup = {
            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        },
        onSubmitBug = { context.browse(HA1_GITHUB_ISSUE_URL) },
        onOpenForum = { context.browse(HA1_GITHUB_FORUM_URL) },
    )

    ConfirmDialog(
        visible = pendingImportUri != null,
        title = stringResource(R.string.backup_import_title),
        message = stringResource(R.string.backup_import_confirm_message),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            val uri = pendingImportUri ?: return@ConfirmDialog
            pendingImportUri = null
            coroutineScope.launch(Dispatchers.IO) {
                runCatching { BackupManager.importFrom(context, uri) }
                    .onSuccess {
                        withContext(Dispatchers.Main) {
                            showShortToast(R.string.backup_import_success)
                            refreshKey++
                            activity.recreate()
                        }
                    }
                    .onFailure {
                        withContext(Dispatchers.Main) {
                            showShortToast(R.string.backup_import_failed)
                        }
                    }
            }
        },
        onDismiss = { pendingImportUri = null },
    )

    ConfirmDialog(
        visible = showClearCacheConfirm,
        title = stringResource(R.string.sure_to_clear),
        message = stringResource(R.string.sure_to_clear_cache),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            showClearCacheConfirm = false
            coroutineScope.launch(Dispatchers.IO) {
                val cacheDir = context.cacheDir
                val success = cacheDir?.deleteRecursively() == true
                withContext(Dispatchers.Main) {
                    cacheKey++
                    refreshKey++
                    if (success) showShortToast(R.string.clear_success) else showShortToast(R.string.clear_failed)
                }
            }
        },
        onDismiss = { showClearCacheConfirm = false },
    )

    if (showLicenseScreen) {
        LicenseDialog(
            onDismiss = { showLicenseScreen = false }
        )
    }

    if (showAnalyticsDialog) {
        val message = stringResource(R.string.about_analytics_summary).parseAsHtml()
        val analyticsMessage = remember { message }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.about_analytics)) },
            text = {
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            text = analyticsMessage
                            movementMethod = LinkMovementMethod.getInstance()
                            setPadding(0, 0, 0, 0)
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { showAnalyticsDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    saveBoolean(HOME_USE_ANALYTICS, false)
                    refreshKey++
                    Firebase.analytics.setAnalyticsCollectionEnabled(false)
                    showAnalyticsDialog = false
                }) {
                    Text(stringResource(R.string.deny))
                }
            },
        )
    }

    ConfirmDialog(
        visible = showRestartConfirmDialog,
        title = stringResource(R.string.attention),
        message = stringResource(R.string.restart_needed),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        cancelable = false,
        onConfirm = {
            ActivityManager.restart(killProcess = true)
        },
        onDismiss = { showRestartConfirmDialog = false },
    )

    if (showLauncherPicker) {
        Dialog(
            onDismissRequest = { showLauncherPicker = false },
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.fake_app_icon),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    launcherItems.forEach { item ->
                        TextButton(
                            onClick = {
                                Preferences.preferenceSp.edit {
                                    putString(HOME_FAKE_LAUNCHER_ICON, item.alias)
                                }
                                (context.applicationContext as? HanimeApplication)?.switchLauncher(
                                    item.alias
                                )
                                context.showToast(R.string.fake_icon_hint)
                                refreshKey++
                                showLauncherPicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(30.dp),
                                )
                                Text(item.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LauncherItem(
    val name: String,
    @param:DrawableRes val iconRes: Int,
    val alias: String,
)

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
    val appLanguageValue =
        Preferences.preferenceSp.getString(HOME_APP_LANGUAGE, "system") ?: "system"
    val appLanguageLabel = when (appLanguageValue) {
        "system" -> context.getString(R.string.follow_system)
        "zh-rCN" -> context.getString(R.string.simplified_chinese)
        "zh" -> context.getString(R.string.traditional_chinese)
        "ja" -> context.getString(R.string.japanese_lang)
        "en" -> context.getString(R.string.english_lang)
        else -> appLanguageValue
    }
    val searchGridColumnsConfig = Preferences.searchGridColumnsConfig
    val horizontalCardCountConfig = Preferences.horizontalCardCountConfig
    return HomeSettingsUiState(
        videoLanguage = Preferences.videoLanguage,
        videoLanguageLabel = videoLanguageLabel,
        defaultVideoQuality = Preferences.videoQuality,
        darkMode = Preferences.useDarkMode,
        darkModeLabel = darkModeLabel,
        appLanguage = appLanguageValue,
        appLanguageLabel = appLanguageLabel,
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
        versionSummary = context.getString(
            R.string.current_version,
            "v${BuildConfig.VERSION_NAME}"
        ),
        updatePopupIntervalSummary = toIntervalDaysPrettyString(
            context,
            Preferences.updatePopupIntervalDays
        ),
        updatePopupIntervalDays = Preferences.updatePopupIntervalDays,
        dynamicColorEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        themeColorKey = Preferences.themeColor ?: ThemeColorPreset.DEFAULT.key,
        themeColorName = context.getString(ThemeColorPreset.fromKey(Preferences.themeColor).displayNameRes),
        searchGridColumnsSummary = listOf(
            searchGridColumnsConfig.compactColumns,
            searchGridColumnsConfig.mediumColumns,
            searchGridColumnsConfig.expandedColumns,
            searchGridColumnsConfig.largeColumns,
        ).joinToString(" / "),
        searchGridColumnsConfig = searchGridColumnsConfig,
        horizontalCardCountSummary = listOf(
            horizontalCardCountConfig.narrowCount,
            horizontalCardCountConfig.compactCount,
            horizontalCardCountConfig.mediumCount,
            horizontalCardCountConfig.expandedCount,
        ).joinToString(" / "),
        horizontalCardCountConfig = horizontalCardCountConfig,
    )
}
