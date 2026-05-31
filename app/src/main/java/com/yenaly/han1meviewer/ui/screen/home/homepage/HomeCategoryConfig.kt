package com.yenaly.han1meviewer.ui.screen.home.homepage

import androidx.annotation.StringRes
import androidx.core.content.edit
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys

const val HOME_CATEGORY_LATEST_HANIME = "latest_hanime"
const val HOME_CATEGORY_LATEST_RELEASE = "latest_release"
const val HOME_CATEGORY_LATEST_UPLOAD = "latest_upload"
const val HOME_CATEGORY_WATCHING_NOW = "watching_now"
const val HOME_CATEGORY_SHORT_EPISODE = "short_episode"
const val HOME_CATEGORY_MOTION_ANIME = "motion_anime"
const val HOME_CATEGORY_3D_CG = "3d_cg"
const val HOME_CATEGORY_2_5D = "2_5d"
const val HOME_CATEGORY_2D_ANIME = "2d_anime"
const val HOME_CATEGORY_AI_GENERATED = "ai_generated"
const val HOME_CATEGORY_MMD = "mmd"
const val HOME_CATEGORY_COSPLAY = "cosplay"

data class HomeCategoryPreferenceItem(
    val key: String,
    @param:StringRes val normalTitleRes: Int,
    @param:StringRes val avTitleRes: Int? = null,
)

val defaultHomeCategoryPreferenceItems = listOf(
    HomeCategoryPreferenceItem(HOME_CATEGORY_LATEST_HANIME, R.string.latest_hanime, R.string.latest_av),
    HomeCategoryPreferenceItem(HOME_CATEGORY_LATEST_RELEASE, R.string.latest_release),
    HomeCategoryPreferenceItem(HOME_CATEGORY_LATEST_UPLOAD, R.string.latest_upload),
    HomeCategoryPreferenceItem(HOME_CATEGORY_WATCHING_NOW, R.string.they_watched),
    HomeCategoryPreferenceItem(HOME_CATEGORY_SHORT_EPISODE, R.string.category_instant_noodle, R.string.amateur_nomask),
    HomeCategoryPreferenceItem(HOME_CATEGORY_MOTION_ANIME, R.string.category_motion_anime, R.string.hd_uncensored),
    HomeCategoryPreferenceItem(HOME_CATEGORY_3D_CG, R.string.category_3d_animation, R.string.ai_decensored),
    HomeCategoryPreferenceItem(HOME_CATEGORY_2_5D, R.string.animation_2_5d, R.string.china_av),
    HomeCategoryPreferenceItem(HOME_CATEGORY_2D_ANIME, R.string.animation_2d, R.string.chinese_amateur),
    HomeCategoryPreferenceItem(HOME_CATEGORY_AI_GENERATED, R.string.ai_generated, R.string.chinese_subtitle),
    HomeCategoryPreferenceItem(HOME_CATEGORY_MMD, R.string.mmd, R.string.ranking_today),
    HomeCategoryPreferenceItem(HOME_CATEGORY_COSPLAY, R.string.category_cosplay, R.string.ranking_this_month),
)

val defaultHomeCategoryOrder: List<String>
    get() = defaultHomeCategoryPreferenceItems.map { it.key }

val homeCategoryOrder: List<String>
    get() = normalizeHomeCategoryKeys(
        Preferences.preferenceSp.getString(SettingsPreferenceKeys.HOME_CATEGORY_ORDER, null)
            ?.split(',')
            .orEmpty()
            .filter { it.isNotBlank() }
    )

val hiddenHomeCategoryKeys: Set<String>
    get() = Preferences.preferenceSp.getString(SettingsPreferenceKeys.HOME_CATEGORY_HIDDEN, null)
        ?.split(',')
        .orEmpty()
        .filter { it.isNotBlank() }
        .toSet()

fun saveHomeCategoryPreferences(order: List<String>, hiddenKeys: Set<String>) {
    Preferences.preferenceSp.edit {
        putString(SettingsPreferenceKeys.HOME_CATEGORY_ORDER, normalizeHomeCategoryKeys(order).joinToString(","))
        putString(
            SettingsPreferenceKeys.HOME_CATEGORY_HIDDEN,
            hiddenKeys.filter { it in defaultHomeCategoryOrder }.joinToString(",")
        )
    }
}

private fun normalizeHomeCategoryKeys(keys: List<String>): List<String> {
    val defaults = defaultHomeCategoryOrder
    return keys.distinct().filter { it in defaults } + defaults.filterNot { it in keys }
}
