package com.yenaly.han1meviewer.ui.navigation.main

import android.content.Intent
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import kotlinx.serialization.json.Json

private val loginRequiredDrawerItems = setOf(
    MainDrawerDestination.FavVideo,
    MainDrawerDestination.WatchLater,
    MainDrawerDestination.Playlist,
    MainDrawerDestination.Subscription,
)

fun NavHostController.navigateDrawerDestination(
    destination: MainDrawerDestination,
    isLoggedIn: Boolean,
    onRequireLogin: () -> Unit,
): Boolean {
    if (destination in loginRequiredDrawerItems && !isLoggedIn) {
        onRequireLogin()
        return false
    }

    when (destination) {
        MainDrawerDestination.Home -> navigate(HomeRoute)
        MainDrawerDestination.Settings -> navigate(HomeSettingsRoute)
        MainDrawerDestination.DailyCheckIn -> navigate(DailyCheckInRoute)
        MainDrawerDestination.WatchLater -> navigate(MyWatchLaterRoute)
        MainDrawerDestination.FavVideo -> navigate(MyFavVideoRoute)
        MainDrawerDestination.Playlist -> navigate(MyPlaylistRoute)
        MainDrawerDestination.Subscription -> navigate(SubscriptionRoute)
        MainDrawerDestination.WatchHistory -> navigate(WatchHistoryRoute)
        MainDrawerDestination.Download -> navigate(DownloadRoute)
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
