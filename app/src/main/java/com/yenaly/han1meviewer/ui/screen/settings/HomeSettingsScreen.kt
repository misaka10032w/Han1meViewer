package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SearchGridColumnsConfig
import com.yenaly.han1meviewer.ui.component.ChoiceDialog
import com.yenaly.han1meviewer.ui.component.SettingInfoItem
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSliderItem
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.theme.ThemeColorPreset
import kotlin.math.max
import kotlin.math.min

data class HomeSettingsUiState(
    val videoLanguage: String,
    val videoLanguageLabel: String,
    val defaultVideoQuality: String,
    val darkMode: String,
    val darkModeLabel: String,
    val appLanguage: String,
    val appLanguageLabel: String,
    val allowPipMode: Boolean,
    val allowResumePlayback: Boolean,
    val showPlayedIndicator: Boolean,
    val searchArtistIgnoreVideoType: Boolean,
    val disableMobileDataWarning: Boolean,
    val disablePredictiveBack: Boolean,
    val tabletMode: Boolean,
    val disableComments: Boolean,
    val collapseDownloadedGroup: Boolean,
    val useDynamicColor: Boolean,
    val useCIUpdateChannel: Boolean,
    val useAnalytics: Boolean,
    val useLockScreen: Boolean,
    val fakeLauncherIconName: String,
    val updateSummary: String,
    val cacheSummary: String,
    val versionSummary: String,
    val updatePopupIntervalSummary: String,
    val updatePopupIntervalDays: Int,
    val dynamicColorEnabled: Boolean,
    val themeColorKey: String,
    val themeColorName: String,
    val searchGridColumnsSummary: String,
    val searchGridColumnsConfig: SearchGridColumnsConfig,
)

private enum class HomeSettingsChoiceDialog {
    VideoLanguage,
    VideoQuality,
    DarkMode,
    AppLanguage,
    ThemeColor,
}

@Composable
fun HomeSettingsScreen(
    state: HomeSettingsUiState,
    onVideoLanguageChange: (String) -> Unit,
    onVideoQualityChange: (String) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onAllowPipModeChange: (Boolean) -> Unit,
    onAllowResumePlaybackChange: (Boolean) -> Unit,
    onShowPlayedIndicatorChange: (Boolean) -> Unit,
    onSearchArtistIgnoreVideoTypeChange: (Boolean) -> Unit,
    onDisableMobileDataWarningChange: (Boolean) -> Unit,
    onDisablePredictiveBackChange: (Boolean) -> Unit,
    onTabletModeChange: (Boolean) -> Unit,
    onDisableCommentsChange: (Boolean) -> Unit,
    onCollapseDownloadedGroupChange: (Boolean) -> Unit,
    onSearchGridColumnsConfigChange: (SearchGridColumnsConfig) -> Unit,
    onUseCIUpdateChannelChange: (Boolean) -> Unit,
    onUseAnalyticsChange: (Boolean) -> Unit,
    onUseLockScreenChange: (Boolean) -> Unit,
    onThemeColorChange: (String) -> Unit,
    onOpenPlayerSettings: () -> Unit,
    onOpenHKeyframeSettings: () -> Unit,
    onOpenDownloadSettings: () -> Unit,
    onOpenNetworkSettings: () -> Unit,
    onOpenAppLanguageSettings: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onUpdatePopupIntervalDaysChange: (Int) -> Unit,
    onOpenApplyDeepLinks: () -> Unit,
    onOpenFakeLauncherIcon: () -> Unit,
    onOpenOpenSourceLicense: () -> Unit,
    onOpenAbout: () -> Unit,
    onClearCache: () -> Unit,
    onSubmitBug: () -> Unit,
    onOpenForum: () -> Unit,
) {
    var activeDialog by rememberSaveable { mutableStateOf<HomeSettingsChoiceDialog?>(null) }
    var showSearchGridColumnsDialog by rememberSaveable { mutableStateOf(false) }

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.VideoLanguage,
        title = stringResource(R.string.video_language),
        options = listOf(
            stringResource(R.string.traditional_chinese) to "zht",
            stringResource(R.string.simplified_chinese) to "zhs",
        ),
        selectedValue = state.videoLanguage,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onVideoLanguageChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.VideoQuality,
        title = stringResource(R.string.default_video_quilty),
        options = listOf("480P" to "480P", "720P" to "720P", "1080P" to "1080P"),
        selectedValue = state.defaultVideoQuality,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onVideoQualityChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.DarkMode,
        title = stringResource(R.string.dark_theme),
        options = listOf(
            stringResource(R.string.follow_system) to "follow_system",
            stringResource(R.string.always_off) to "always_off",
            stringResource(R.string.always_on) to "always_on",
        ),
        selectedValue = state.darkMode,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onDarkModeChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.AppLanguage,
        title = stringResource(R.string.app_lang),
        options = listOf(
            stringResource(R.string.follow_system) to "system",
            stringResource(R.string.simplified_chinese) to "zh-rCN",
            stringResource(R.string.traditional_chinese) to "zh",
            stringResource(R.string.japanese_lang) to "ja",
            stringResource(R.string.english_lang) to "en",
        ),
        selectedValue = state.appLanguage,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onOpenAppLanguageSettings(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.ThemeColor,
        title = stringResource(R.string.theme_color),
        options = ThemeColorPreset.entries.map { stringResource(it.displayNameRes) to it.key },
        selectedValue = state.themeColorKey,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onThemeColorChange(it)
        },
    )

    if (showSearchGridColumnsDialog) {
        SearchGridColumnsDialog(
            initialConfig = state.searchGridColumnsConfig,
            onDismiss = { showSearchGridColumnsDialog = false },
            onConfirm = {
                showSearchGridColumnsDialog = false
                onSearchGridColumnsConfigChange(it)
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item { SettingsGroupTitle(stringResource(R.string.video)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.video_language),
                valueText = state.videoLanguageLabel,
                iconRes = R.drawable.baseline_simp_to_trad_24,
                onClick = { activeDialog = HomeSettingsChoiceDialog.VideoLanguage },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.default_video_quilty),
                valueText = state.defaultVideoQuality,
                iconRes = R.drawable.ic_video_quilty,
                onClick = { activeDialog = HomeSettingsChoiceDialog.VideoQuality },
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.allow_pip_title),
                summary = stringResource(R.string.allow_pip_disc),
                checked = state.allowPipMode,
                iconRes = R.drawable.ic_pip_mode,
                onCheckedChange = onAllowPipModeChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.resume_playback_title),
                summary = stringResource(R.string.resume_playback_summary),
                checked = state.allowResumePlayback,
                iconRes = R.drawable.ic_baseline_skip_24,
                onCheckedChange = onAllowResumePlaybackChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.show_played_indicator),
                summary = stringResource(R.string.show_played_indicator_summary),
                checked = state.showPlayedIndicator,
                iconRes = R.drawable.ic_baseline_history_24,
                onCheckedChange = onShowPlayedIndicatorChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.search_artist_ignore_video_type),
                summary = stringResource(R.string.search_artist_ignore_video_type_summary),
                checked = state.searchArtistIgnoreVideoType,
                iconRes = R.drawable.baseline_prohibit_24,
                onCheckedChange = onSearchArtistIgnoreVideoTypeChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.disable_mobile_data_warning),
                summary = stringResource(R.string.disable_mobile_data_warning_summary),
                checked = state.disableMobileDataWarning,
                iconRes = R.drawable.baseline_mobile_data_24,
                onCheckedChange = onDisableMobileDataWarningChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.disable_predictive_back_title),
                summary = "暂不可用 Temporarily unavailable",
                checked = state.disablePredictiveBack,
                iconRes = R.drawable.ic_baseline_arrow_back_24,
                onCheckedChange = onDisablePredictiveBackChange,
                enabled = false
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.tablet_mode),
                summary = stringResource(R.string.tablet_mode_summary),
                checked = state.tabletMode,
                iconRes = R.drawable.ic_baseline_tablet_24,
                onCheckedChange = onTabletModeChange,
            )
        }
        if (state.tabletMode) {
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.search_grid_columns_title),
                    summary = stringResource(R.string.search_grid_columns_summary),
                    valueText = state.searchGridColumnsSummary,
                    iconRes = R.drawable.ic_baseline_tablet_24,
                    onClick = { showSearchGridColumnsDialog = true },
                )
            }
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.player_settings),
                iconRes = R.drawable.ic_baseline_play_circle_outline_24,
                onClick = onOpenPlayerSettings,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.h_keyframe_settings),
                iconRes = R.drawable.baseline_h_24,
                onClick = onOpenHKeyframeSettings,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.disable_comments_title),
                summary = stringResource(R.string.disable_comments_sum),
                checked = state.disableComments,
                iconRes = R.drawable.ic_comments,
                onCheckedChange = onDisableCommentsChange,
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.download)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.download_settings),
                iconRes = R.drawable.ic_baseline_download_24,
                onClick = onOpenDownloadSettings,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.collapse_downloaded_groups),
                summary = stringResource(R.string.collapse_downloaded_groups_summary),
                checked = state.collapseDownloadedGroup,
                iconRes = R.drawable.ic_baseline_fold_24,
                onCheckedChange = onCollapseDownloadedGroupChange,
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.network)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.network_settings),
                iconRes = R.drawable.ic_baseline_language_24,
                onClick = onOpenNetworkSettings,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.apply_deep_links),
                summary = stringResource(R.string.apply_deep_links_summary),
                iconRes = R.drawable.baseline_add_link_24,
                onClick = onOpenApplyDeepLinks,
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.theme)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.dark_theme),
                valueText = state.darkModeLabel,
                iconRes = R.drawable.ic_baseline_moon_24,
                onClick = { activeDialog = HomeSettingsChoiceDialog.DarkMode },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.theme_color),
                valueText = state.themeColorName,
                iconRes = R.drawable.ic_baseline_theme_24,
                onClick = { activeDialog = HomeSettingsChoiceDialog.ThemeColor },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.app_lang),
                summary = stringResource(R.string.app_lang_sum),
                valueText = state.appLanguageLabel,
                iconRes = R.drawable.ic_setting_lang,
                onClick = { activeDialog = HomeSettingsChoiceDialog.AppLanguage },
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.update)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.check_update),
                summary = state.updateSummary,
                iconRes = R.drawable.ic_baseline_update_24,
                onClick = onCheckUpdate,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.use_ci_update_channel),
                checked = state.useCIUpdateChannel,
                iconRes = R.drawable.ic_setting_ci,
                onCheckedChange = onUseCIUpdateChannelChange,
            )
        }
        item {
            SettingSliderItem(
                title = stringResource(R.string.update_popup_interval_days),
                summary = state.updatePopupIntervalSummary,
                value = state.updatePopupIntervalDays,
                valueRange = 0..30,
                iconRes = R.drawable.ic_clock,
                onValueChange = onUpdatePopupIntervalDaysChange,
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.privacy)) }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.analytics_title),
                summary = stringResource(R.string.analytics_summary),
                checked = state.useAnalytics,
                iconRes = R.drawable.baseline_data_usage_24,
                onCheckedChange = onUseAnalyticsChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.use_lock_screen),
                summary = stringResource(R.string.use_lock_screen_sum),
                checked = state.useLockScreen,
                iconRes = R.drawable.ic_setting_applock,
                onCheckedChange = onUseLockScreenChange,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.fake_app_icon),
                summary = stringResource(R.string.select_fake_icon),
                valueText = state.fakeLauncherIconName,
                iconRes = R.drawable.ic_baseline_mask,
                onClick = onOpenFakeLauncherIcon,
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.other)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.clear_cache),
                summary = state.cacheSummary,
                iconRes = R.drawable.ic_baseline_clear_all_24,
                onClick = onClearCache,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.submit_bug),
                summary = stringResource(R.string.submit_bug_summary),
                iconRes = R.drawable.baseline_bug_report_24,
                onClick = onSubmitBug,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.forum),
                summary = stringResource(R.string.forum_summary),
                iconRes = R.drawable.baseline_forum_24,
                onClick = onOpenForum,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.open_source_license),
                iconRes = R.drawable.ic_oss,
                onClick = onOpenOpenSourceLicense,
            )
        }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about),
                summary = state.versionSummary,
                iconRes = R.drawable.ic_baseline_info_24,
            )
        }
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        HorizontalDivider()
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 1000)
@Composable
private fun HomeSettingsScreenPreview() {
    ComponentPreview {
        HomeSettingsScreen(
            state = HomeSettingsUiState(
                videoLanguage = "zhs",
                videoLanguageLabel = "简体中文",
                defaultVideoQuality = "1080P",
                darkMode = "follow_system",
                darkModeLabel = "跟随系统",
                appLanguage = "system",
                appLanguageLabel = "跟随系统",
                allowPipMode = true,
                allowResumePlayback = true,
                showPlayedIndicator = true,
                searchArtistIgnoreVideoType = false,
                disableMobileDataWarning = false,
                disablePredictiveBack = false,
                tabletMode = false,
                disableComments = false,
                collapseDownloadedGroup = false,
                useDynamicColor = true,
                useCIUpdateChannel = false,
                useAnalytics = true,
                useLockScreen = false,
                fakeLauncherIconName = "Han1meViewer",
                updateSummary = "已經是最新版本！",
                cacheSummary = "目前佔用了 12MB 的儲存空間",
                versionSummary = "當前版本：v1.0.0",
                updatePopupIntervalSummary = "7天\n最近還沒跳出過更新視窗哦",
                updatePopupIntervalDays = 7,
                dynamicColorEnabled = true,
                themeColorKey = "default",
                themeColorName = "預設（暖紅）",
                searchGridColumnsSummary = "2 / 3 / 4 / 5",
                searchGridColumnsConfig = SearchGridColumnsConfig(),
            ),
            onVideoLanguageChange = {},
            onVideoQualityChange = {},
            onDarkModeChange = {},
            onAllowPipModeChange = {},
            onAllowResumePlaybackChange = {},
            onShowPlayedIndicatorChange = {},
            onSearchArtistIgnoreVideoTypeChange = {},
            onDisableMobileDataWarningChange = {},
            onDisablePredictiveBackChange = {},
            onTabletModeChange = {},
            onDisableCommentsChange = {},
            onCollapseDownloadedGroupChange = {},
            onSearchGridColumnsConfigChange = {},
            onUseCIUpdateChannelChange = {},
            onUseAnalyticsChange = {},
            onUseLockScreenChange = {},
            onThemeColorChange = {},
            onOpenPlayerSettings = {},
            onOpenHKeyframeSettings = {},
            onOpenDownloadSettings = {},
            onOpenNetworkSettings = {},
            onOpenAppLanguageSettings = {},
            onCheckUpdate = {},
            onUpdatePopupIntervalDaysChange = {},
            onOpenApplyDeepLinks = {},
            onOpenFakeLauncherIcon = {},
            onOpenOpenSourceLicense = {},
            onOpenAbout = {},
            onClearCache = {},
            onSubmitBug = {},
            onOpenForum = {},
        )
    }
}

@Composable
private fun SearchGridColumnsDialog(
    initialConfig: SearchGridColumnsConfig,
    onDismiss: () -> Unit,
    onConfirm: (SearchGridColumnsConfig) -> Unit,
) {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val currentWidthDp = with(density) { containerSize.width.toDp().value.toInt() }
    val currentHeightDp = with(density) { containerSize.height.toDp().value.toInt() }
    val portraitWidthDp = min(currentWidthDp, currentHeightDp)
    val landscapeWidthDp = max(currentWidthDp, currentHeightDp)
    var compactColumnsText by remember(initialConfig) {
        mutableStateOf(initialConfig.compactColumns.toString())
    }
    var mediumColumnsText by remember(initialConfig) {
        mutableStateOf(initialConfig.mediumColumns.toString())
    }
    var expandedColumnsText by remember(initialConfig) {
        mutableStateOf(initialConfig.expandedColumns.toString())
    }
    var largeColumnsText by remember(initialConfig) {
        mutableStateOf(initialConfig.largeColumns.toString())
    }
    val compactColumns = compactColumnsText.toSearchGridColumnsOrNull()
    val mediumColumns = mediumColumnsText.toSearchGridColumnsOrNull()
    val expandedColumns = expandedColumnsText.toSearchGridColumnsOrNull()
    val largeColumns = largeColumnsText.toSearchGridColumnsOrNull()
    val currentConfig = SearchGridColumnsConfig(
        compactColumns = compactColumns ?: initialConfig.compactColumns,
        mediumColumns = mediumColumns ?: initialConfig.mediumColumns,
        expandedColumns = expandedColumns ?: initialConfig.expandedColumns,
        largeColumns = largeColumns ?: initialConfig.largeColumns,
    )
    val canConfirm = compactColumns != null &&
        mediumColumns != null &&
        expandedColumns != null &&
        largeColumns != null
    val portraitBucketLabel = searchGridColumnsBucketLabel(portraitWidthDp)
    val landscapeBucketLabel = searchGridColumnsBucketLabel(landscapeWidthDp)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.search_grid_columns_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.search_grid_columns_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.search_grid_columns_current_width_hint,
                        portraitWidthDp,
                        landscapeWidthDp,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.search_grid_columns_current_bucket_hint,
                        portraitBucketLabel,
                        currentConfig.columnsForWidthDp(portraitWidthDp),
                        landscapeBucketLabel,
                        currentConfig.columnsForWidthDp(landscapeWidthDp),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SearchGridColumnsInputRow(
                    label = stringResource(R.string.search_grid_columns_range_compact),
                    value = compactColumnsText,
                    onValueChange = { compactColumnsText = it },
                    isError = compactColumns == null,
                    isHighlighted = portraitWidthDp <= 600 || landscapeWidthDp <= 600,
                    highlightLabels = buildList {
                        if (portraitWidthDp <= 600) add(stringResource(R.string.search_grid_columns_current_portrait))
                        if (landscapeWidthDp <= 600) add(stringResource(R.string.search_grid_columns_current_landscape))
                    },
                )
                SearchGridColumnsInputRow(
                    label = stringResource(R.string.search_grid_columns_range_medium),
                    value = mediumColumnsText,
                    onValueChange = { mediumColumnsText = it },
                    isError = mediumColumns == null,
                    isHighlighted = (portraitWidthDp in 601..900) || (landscapeWidthDp in 601..900),
                    highlightLabels = buildList {
                        if (portraitWidthDp in 601..900) add(stringResource(R.string.search_grid_columns_current_portrait))
                        if (landscapeWidthDp in 601..900) add(stringResource(R.string.search_grid_columns_current_landscape))
                    },
                )
                SearchGridColumnsInputRow(
                    label = stringResource(R.string.search_grid_columns_range_expanded),
                    value = expandedColumnsText,
                    onValueChange = { expandedColumnsText = it },
                    isError = expandedColumns == null,
                    isHighlighted = (portraitWidthDp in 901..1200) || (landscapeWidthDp in 901..1200),
                    highlightLabels = buildList {
                        if (portraitWidthDp in 901..1200) add(stringResource(R.string.search_grid_columns_current_portrait))
                        if (landscapeWidthDp in 901..1200) add(stringResource(R.string.search_grid_columns_current_landscape))
                    },
                )
                SearchGridColumnsInputRow(
                    label = stringResource(R.string.search_grid_columns_range_large),
                    value = largeColumnsText,
                    onValueChange = { largeColumnsText = it },
                    isError = largeColumns == null,
                    isHighlighted = portraitWidthDp >= 1201 || landscapeWidthDp >= 1201,
                    highlightLabels = buildList {
                        if (portraitWidthDp >= 1201) add(stringResource(R.string.search_grid_columns_current_portrait))
                        if (landscapeWidthDp >= 1201) add(stringResource(R.string.search_grid_columns_current_landscape))
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        currentConfig,
                    )
                },
                enabled = canConfirm,
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        compactColumnsText = SearchGridColumnsConfig.DEFAULT_COMPACT_COLUMNS.toString()
                        mediumColumnsText = SearchGridColumnsConfig.DEFAULT_MEDIUM_COLUMNS.toString()
                        expandedColumnsText = SearchGridColumnsConfig.DEFAULT_EXPANDED_COLUMNS.toString()
                        largeColumnsText = SearchGridColumnsConfig.DEFAULT_LARGE_COLUMNS.toString()
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}

@Composable
private fun SearchGridColumnsInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    isHighlighted: Boolean,
    highlightLabels: List<String>,
) {
    val containerColor = if (isHighlighted) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = value,
                onValueChange = {
                    if (it.isEmpty() || it.all(Char::isDigit)) {
                        onValueChange(it)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.32f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = isError,
            )
        }
        if (highlightLabels.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                highlightLabels.forEach { highlightLabel ->
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(highlightLabel) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            disabledLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }
    }
}

private fun String.toSearchGridColumnsOrNull(): Int? {
    val parsed = toIntOrNull() ?: return null
    return parsed.takeIf { it in 1..12 }
}

@Composable
private fun searchGridColumnsBucketLabel(widthDp: Int): String {
    return when {
        widthDp <= 600 -> stringResource(R.string.search_grid_columns_range_compact)
        widthDp <= 900 -> stringResource(R.string.search_grid_columns_range_medium)
        widthDp <= 1200 -> stringResource(R.string.search_grid_columns_range_expanded)
        else -> stringResource(R.string.search_grid_columns_range_large)
    }
}
