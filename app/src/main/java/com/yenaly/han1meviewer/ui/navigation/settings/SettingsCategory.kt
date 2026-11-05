package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.yenaly.han1meviewer.R

/**
 * 设置页一级分类。
 *
 * 手机与平板双栏共用这套入口，具体的二级设置项由分类页面承载。
 */
enum class SettingsCategory(
    val key: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:DrawableRes val iconRes: Int,
    val screenClassName: String,
) {
    Appearance(
        key = "appearance",
        titleRes = R.string.settings_category_appearance,
        descriptionRes = R.string.settings_category_appearance_desc,
        iconRes = R.drawable.ic_baseline_theme_24,
        screenClassName = "AppearanceSettingsScreen",
    ),
    Playback(
        key = "playback",
        titleRes = R.string.settings_category_playback,
        descriptionRes = R.string.settings_category_playback_desc,
        iconRes = R.drawable.ic_baseline_play_circle_outline_24,
        screenClassName = "PlaybackSettingsScreen",
    ),
    Network(
        key = "network",
        titleRes = R.string.settings_category_network,
        descriptionRes = R.string.settings_category_network_desc,
        iconRes = R.drawable.ic_baseline_language_24,
        screenClassName = "NetworkSettingsScreen",
    ),
    Download(
        key = "download",
        titleRes = R.string.settings_category_download,
        descriptionRes = R.string.settings_category_download_desc,
        iconRes = R.drawable.ic_baseline_download_24,
        screenClassName = "DownloadSettingsScreen",
    ),
    Keyframes(
        key = "keyframes",
        titleRes = R.string.settings_category_keyframes,
        descriptionRes = R.string.settings_category_keyframes_desc,
        iconRes = R.drawable.baseline_frame_inter_24,
        screenClassName = "KeyframesSettingsScreen",
    ),
    Privacy(
        key = "privacy",
        titleRes = R.string.settings_category_privacy,
        descriptionRes = R.string.settings_category_privacy_desc,
        iconRes = R.drawable.ic_setting_applock,
        screenClassName = "PrivacySettingsScreen",
    ),
    Data(
        key = "data",
        titleRes = R.string.settings_category_data,
        descriptionRes = R.string.settings_category_data_desc,
        iconRes = R.drawable.baseline_backup_24,
        screenClassName = "DataSettingsScreen",
    ),
    About(
        key = "about",
        titleRes = R.string.settings_category_about,
        descriptionRes = R.string.settings_category_about_desc,
        iconRes = R.drawable.ic_baseline_info_24,
        screenClassName = "AboutSettingsScreen",
    );

    companion object {
        fun fromKey(key: String?): SettingsCategory? =
            entries.firstOrNull { it.key == key }
    }
}
