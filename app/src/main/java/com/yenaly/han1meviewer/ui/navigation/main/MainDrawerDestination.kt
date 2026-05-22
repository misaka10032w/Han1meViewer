package com.yenaly.han1meviewer.ui.navigation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.yenaly.han1meviewer.R

enum class MainDrawerDestination(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    val requiresLogin: Boolean = false,
) {
    Home(
        iconRes = R.drawable.ic_baseline_home_24,
        titleRes = R.string.home_page,
    ),
    Settings(
        iconRes = R.drawable.ic_baseline_settings_24,
        titleRes = R.string.settings,
    ),
    DailyCheckIn(
        iconRes = R.drawable.ic_baseline_thumb_up_alt_24,
        titleRes = R.string.has_mastur,
    ),
    WatchLater(
        iconRes = R.drawable.ic_baseline_watch_later_24,
        titleRes = R.string.watch_later,
        requiresLogin = true,
    ),
    FavVideo(
        iconRes = R.drawable.ic_baseline_favorite_24,
        titleRes = R.string.fav_video,
        requiresLogin = true,
    ),
    Playlist(
        iconRes = R.drawable.ic_baseline_list_24,
        titleRes = R.string.play_list,
        requiresLogin = true,
    ),
    Subscription(
        iconRes = R.drawable.ic_subscribtion,
        titleRes = R.string.my_subscribe,
        requiresLogin = true,
    ),
    CreatorCenter(
        iconRes = R.drawable.baseline_creator_center_24,
        titleRes = R.string.creator_center,
        requiresLogin = true,
    ),
    WatchHistory(
        iconRes = R.drawable.ic_baseline_history_24,
        titleRes = R.string.watch_history,
    ),
    Download(
        iconRes = R.drawable.ic_baseline_download_24,
        titleRes = R.string.download,
    ),
}
