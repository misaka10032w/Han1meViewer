package com.yenaly.han1meviewer.ui.navigation.main

import android.content.Intent
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsDestinationSpec
import kotlinx.serialization.json.Json

private val loginRequiredDrawerItems = setOf(
    R.id.nv_fav_video,
    R.id.nv_watch_later,
    R.id.nv_playlist,
    R.id.nv_subscription,
)

fun NavHostController.navigateDrawerDestination(
    itemId: Int,
    isLoggedIn: Boolean,
    onRequireLogin: () -> Unit,
    onOpenSettings: (SettingsDestinationSpec) -> Unit,
): Boolean {
    if (itemId in loginRequiredDrawerItems && !isLoggedIn) {
        onRequireLogin()
        return false
    }

    when (itemId) {
        R.id.nv_home_page -> navigate(HomeRoute)
        R.id.nv_watch_history -> navigate(WatchHistoryRoute)
        R.id.nv_fav_video -> navigate(MyFavVideoRoute)
        R.id.nv_playlist -> navigate(MyPlaylistRoute)
        R.id.nv_watch_later -> navigate(MyWatchLaterRoute)
        R.id.nv_subscription -> navigate(SubscriptionRoute)
        R.id.nv_daily_check_in -> navigate(DailyCheckInRoute)
        R.id.nv_download -> navigate(DownloadRoute)
        R.id.nv_settings -> onOpenSettings(SettingsDestinationSpec.Home)
    }
    return true
}

fun NavHostController.handleMainIntent(intent: Intent) {
    if (intent.action == Intent.ACTION_VIEW) {
        val uri = intent.data ?: return
        when (uri.scheme) {
            "http", "https" -> {
                val videoCode = uri.getQueryParameter("v")
                if (videoCode != null) {
                    navigate(VideoRoute(videoCode))
                }
            }

            "file", "content" -> {
                navigate(VideoRoute("-1", uri.toString()))
            }
        }
        return
    }

    intent.getStringExtra("startSearchFromTag")?.let { tag ->
        intent.removeExtra("startSearchFromTag")
        navigate(SearchRoute(query = tag))
        return
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    val map = intent.getSerializableExtra("startSearchFromMap") as? HashMap<String, String>
    if (map != null) {
        intent.removeExtra("startSearchFromMap")
        navigate(SearchRoute(advancedSearchJson = Json.encodeToString(map)))
        return
    }

    val videoCode = intent.getStringExtra("startVideoCode")
    if (!videoCode.isNullOrEmpty()) {
        intent.removeExtra("startVideoCode")
        navigate(VideoRoute(videoCode))
    }
}
