package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.component.SettingChoiceItem
import com.yenaly.han1meviewer.ui.component.SettingInfoItem
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSliderItem
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn

data class HomeSettingsUiState(
    val videoLanguage: String,
    val defaultVideoQuality: String,
    val darkMode: String,
    val appLanguage: String,
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
)

private enum class HomeSettingsChoiceDialog {
    VideoLanguage,
    VideoQuality,
    DarkMode,
    AppLanguage,
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
    onUseDynamicColorChange: (Boolean) -> Unit,
    onUseCIUpdateChannelChange: (Boolean) -> Unit,
    onUseAnalyticsChange: (Boolean) -> Unit,
    onUseLockScreenChange: (Boolean) -> Unit,
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item { SettingsGroupTitle(stringResource(R.string.video)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.video_language),
                valueText = state.videoLanguage,
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
                summary = stringResource(R.string.disable_predictive_back_summary),
                checked = state.disablePredictiveBack,
                iconRes = R.drawable.ic_baseline_arrow_back_24,
                onCheckedChange = onDisablePredictiveBackChange,
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
                valueText = state.darkMode,
                iconRes = R.drawable.ic_baseline_moon_24,
                onClick = { activeDialog = HomeSettingsChoiceDialog.DarkMode },
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.dynamic_color_title),
                summary = stringResource(R.string.dynamic_color_summary),
                checked = state.useDynamicColor,
                iconRes = R.drawable.ic_baseline_theme_24,
                onCheckedChange = onUseDynamicColorChange,
                modifier = Modifier,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.app_lang),
                summary = stringResource(R.string.app_lang_sum),
                valueText = state.appLanguage,
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
private fun ChoiceDialog(
    visible: Boolean,
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (label, value) ->
                    SettingChoiceItem(
                        title = label,
                        selected = selectedValue == value,
                        iconRes = null,
                        onClick = { onSelect(value) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
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
                defaultVideoQuality = "1080P",
                darkMode = "follow_system",
                appLanguage = "system",
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
            onUseDynamicColorChange = {},
            onUseCIUpdateChannelChange = {},
            onUseAnalyticsChange = {},
            onUseLockScreenChange = {},
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
