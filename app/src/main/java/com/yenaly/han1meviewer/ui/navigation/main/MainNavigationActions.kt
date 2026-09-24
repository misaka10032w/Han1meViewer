package com.yenaly.han1meviewer.ui.navigation.main

import android.content.Intent
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.ui.navigation.canNavigateSafely
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
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
    asTopLevel: Boolean = false,
): Boolean {
    if (destination in loginRequiredDrawerItems && !isLoggedIn) {
        onRequireLogin()
        return false
    }

    val options: androidx.navigation.NavOptionsBuilder.() -> Unit = if (asTopLevel) {
        {
            popUpTo(HomeRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        {}
    }
    when (destination) {
        MainDrawerDestination.Home -> {
            if (asTopLevel) {
                if (!canNavigateSafely()) return false
                if (!popBackStack(HomeRoute, inclusive = false)) {
                    navigateSafely(HomeRoute)
                }
            } else {
                navigateSafely(HomeRoute)
            }
        }
        MainDrawerDestination.Settings -> navigateSafely(HomeSettingsRoute, options)
        MainDrawerDestination.DailyCheckIn -> navigateSafely(DailyCheckInRoute, options)
        MainDrawerDestination.WatchLater -> navigateSafely(MyWatchLaterRoute, options)
        MainDrawerDestination.FavVideo -> navigateSafely(MyFavVideoRoute, options)
        MainDrawerDestination.Playlist -> navigateSafely(MyPlaylistRoute, options)
        MainDrawerDestination.Subscription -> navigateSafely(SubscriptionRoute, options)
        MainDrawerDestination.CreatorCenter -> navigateSafely(CreatorCenterRoute, options)
        MainDrawerDestination.WatchHistory -> navigateSafely(WatchHistoryRoute, options)
        MainDrawerDestination.Download -> navigateSafely(DownloadRoute, options)
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
                    navigateSafely(VideoRoute(videoCode))
                }
            }

            "file", "content" -> {
                navigateSafely(VideoRoute("-1", uri.toString()))
            }
        }
        return
    }

    intent.getStringExtra("startSearchFromTag")?.let { tag ->
        intent.removeExtra("startSearchFromTag")
        navigateSafely(SearchRoute(query = tag))
        return
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    val map = intent.getSerializableExtra("startSearchFromMap") as? HashMap<String, String>
    if (map != null) {
        intent.removeExtra("startSearchFromMap")
        navigateSafely(SearchRoute(advancedSearchJson = Json.encodeToString(map)))
        return
    }

    val videoCode = intent.getStringExtra("startVideoCode")
    if (!videoCode.isNullOrEmpty()) {
        intent.removeExtra("startVideoCode")
        navigateSafely(VideoRoute(videoCode))
    }
}
