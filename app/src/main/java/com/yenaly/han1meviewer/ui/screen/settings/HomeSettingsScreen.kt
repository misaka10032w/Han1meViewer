package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.HorizontalCardCountConfig
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SearchGridColumnsConfig
import com.yenaly.han1meviewer.ui.component.ChoiceDialog
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSliderItem
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategory
import com.yenaly.han1meviewer.ui.screen.settings.dialog.HomeCategoryLayoutDialog
import com.yenaly.han1meviewer.ui.screen.settings.dialog.HorizontalCardCountDialog
import com.yenaly.han1meviewer.ui.screen.settings.dialog.SearchGridColumnsDialog
import com.yenaly.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import com.yenaly.han1meviewer.ui.theme.ThemeColorPreset

private enum class HomeSettingsChoiceDialog {
    VideoLanguage,
    VideoQuality,
    DarkMode,
    AppLanguage,
    ThemeColor,
}

/**
 * 单个设置分类的详情内容。
 *
 * 手机和双栏模式的右侧面板共用该页面，仅根据 [category] 展示对应设置项。
 */
@Composable
fun HomeSettingsScreen(
    category: SettingsCategory,
    state: HomeSettingsUiState,
    onVideoLanguageChange: (String) -> Unit,
    onVideoQualityChange: (String) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onAllowPipModeChange: (Boolean) -> Unit,
    onAllowResumePlaybackChange: (Boolean) -> Unit,
    onShowPlayedIndicatorChange: (Boolean) -> Unit,
    onWatchedProgressThresholdChange: (Int) -> Unit,
    onSearchPaginationChange: (Boolean) -> Unit,
    onSearchArtistIgnoreVideoTypeChange: (Boolean) -> Unit,
    onDisableMobileDataWarningChange: (Boolean) -> Unit,
    onDisablePredictiveBackChange: (Boolean) -> Unit,
    onShowExitConfirmChange: (Boolean) -> Unit,
    onDisableCommentsChange: (Boolean) -> Unit,
    onSearchGridColumnsConfigChange: (SearchGridColumnsConfig) -> Unit,
    onHorizontalCardCountConfigChange: (HorizontalCardCountConfig) -> Unit,
    onUseCIUpdateChannelChange: (Boolean) -> Unit,
    onUseAnalyticsChange: (Boolean) -> Unit,
    onUseLockScreenChange: (Boolean) -> Unit,
    onThemeColorChange: (String) -> Unit,
    onPureBlackDarkModeChange: (Boolean) -> Unit,
    onHomeCategoryPreferencesChange: (List<String>, Set<String>) -> Unit,
    onOpenPlayerSettings: () -> Unit,
    onOpenAppLanguageSettings: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onUpdatePopupIntervalDaysChange: (Int) -> Unit,
    onOpenFakeLauncherIcon: () -> Unit,
    onOpenOpenSourceLicense: () -> Unit,
    onOpenAboutPage: () -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onSubmitBug: () -> Unit,
    onOpenForum: () -> Unit,
) {
    var activeDialog by rememberSaveable { mutableStateOf<HomeSettingsChoiceDialog?>(null) }
    var showSearchGridColumnsDialog by rememberSaveable { mutableStateOf(false) }
    var showHorizontalCardCountDialog by rememberSaveable { mutableStateOf(false) }
    var showHomeCategoryDialog by rememberSaveable { mutableStateOf(false) }

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
        options = ThemeColorPreset.entries
            .filter { state.dynamicColorEnabled || it != ThemeColorPreset.SYSTEM }
            .map { stringResource(it.displayNameRes) to it.key },
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

    if (showHorizontalCardCountDialog) {
        HorizontalCardCountDialog(
            initialConfig = state.horizontalCardCountConfig,
            onDismiss = { showHorizontalCardCountDialog = false },
            onConfirm = {
                showHorizontalCardCountDialog = false
                onHorizontalCardCountConfigChange(it)
            },
        )
    }

    if (showHomeCategoryDialog) {
        HomeCategoryLayoutDialog(
            state = state,
            onDismiss = { showHomeCategoryDialog = false },
            onConfirm = { order, hiddenKeys ->
                showHomeCategoryDialog = false
                onHomeCategoryPreferencesChange(order, hiddenKeys)
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        when (category) {
            SettingsCategory.Appearance -> {
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
                    SettingSwitchItem(
                        title = stringResource(R.string.pure_black_dark_mode),
                        summary = stringResource(R.string.pure_black_dark_mode_summary),
                        checked = state.pureBlackDarkMode,
                        iconRes = R.drawable.ic_baseline_contrast_24,
                        onCheckedChange = onPureBlackDarkModeChange,
                    )
                }

                item { SettingsGroupTitle(stringResource(R.string.settings_group_interface)) }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.home_category_layout),
                        summary = stringResource(
                            R.string.home_category_layout_summary,
                            state.homeCategoryItems.size - state.hiddenHomeCategoryKeys.size,
                            state.homeCategoryItems.size,
                        ),
                        iconRes = R.drawable.baseline_sort_24,
                        onClick = { showHomeCategoryDialog = true },
                    )
                }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.search_grid_columns_title),
                        summary = stringResource(R.string.search_grid_columns_summary),
                        valueText = state.searchGridColumnsSummary,
                        iconRes = R.drawable.baseline_grid_24,
                        onClick = { showSearchGridColumnsDialog = true },
                    )
                }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.horizontal_card_count_title),
                        summary = stringResource(R.string.horizontal_card_count_summary),
                        valueText = state.horizontalCardCountSummary,
                        iconRes = R.drawable.baseline_row_24,
                        onClick = { showHorizontalCardCountDialog = true },
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.search_pagination),
                        summary = stringResource(R.string.search_pagination_summary),
                        checked = state.searchPagination,
                        iconRes = R.drawable.baseline_paging_24,
                        onCheckedChange = onSearchPaginationChange,
                    )
                }

                item { SettingsGroupTitle(stringResource(R.string.settings_group_app)) }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.app_lang),
                        summary = stringResource(R.string.app_lang_sum),
                        valueText = state.appLanguageLabel,
                        iconRes = R.drawable.ic_setting_lang,
                        onClick = { activeDialog = HomeSettingsChoiceDialog.AppLanguage },
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.show_exit_confirm),
                        summary = stringResource(R.string.show_exit_confirm_summary),
                        checked = state.showExitConfirm,
                        iconRes = R.drawable.ic_baseline_close_24,
                        onCheckedChange = onShowExitConfirmChange,
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.disable_predictive_back_title),
                        summary = "暂不可用 Temporarily unavailable",
                        checked = state.disablePredictiveBack,
                        iconRes = R.drawable.ic_baseline_arrow_back_24,
                        onCheckedChange = onDisablePredictiveBackChange,
                        enabled = false,
                    )
                }
            }

            SettingsCategory.Playback -> {
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
                    SettingSliderItem(
                        title = stringResource(R.string.watched_progress_threshold),
                        summary = stringResource(
                            R.string.watched_progress_threshold_summary,
                            state.watchedProgressThreshold,
                        ),
                        value = state.watchedProgressThreshold,
                        valueRange = 0..100,
                        step = 10,
                        iconRes = R.drawable.baseline_flag_24,
                        onValueChange = onWatchedProgressThresholdChange,
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
                        title = stringResource(R.string.disable_comments_title),
                        summary = stringResource(R.string.disable_comments_sum),
                        checked = state.disableComments,
                        iconRes = R.drawable.ic_comments,
                        onCheckedChange = onDisableCommentsChange,
                    )
                }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.player_settings),
                        iconRes = R.drawable.ic_baseline_play_circle_outline_24,
                        onClick = onOpenPlayerSettings,
                    )
                }
            }

            // 网络、下载和关键帧直接展示对应设置页，不再经过重复的聚合页。
            SettingsCategory.Network,
            SettingsCategory.Download,
            SettingsCategory.Keyframes -> Unit

            SettingsCategory.Privacy -> {
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
            }

            SettingsCategory.Data -> {
                item { SettingsGroupTitle(stringResource(R.string.settings_group_backup)) }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.backup_export_title),
                        summary = stringResource(R.string.backup_export_summary),
                        iconRes = R.drawable.baseline_backup_24,
                        onClick = onExportBackup,
                    )
                }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.backup_import_title),
                        summary = stringResource(R.string.backup_import_summary),
                        iconRes = R.drawable.baseline_restore_24,
                        onClick = onImportBackup,
                    )
                }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.clear_cache),
                        summary = state.cacheSummary,
                        iconRes = R.drawable.ic_baseline_clear_all_24,
                        onClick = onClearCache,
                    )
                }
            }

            SettingsCategory.About -> {
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

                item { SettingsGroupTitle(stringResource(R.string.settings_group_feedback)) }
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

                item { SettingsGroupTitle(stringResource(R.string.settings_group_version)) }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.about),
                        summary = state.versionSummary,
                        iconRes = R.drawable.ic_baseline_info_24,
                        onClick = onOpenAboutPage,
                    )
                }
            }
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
