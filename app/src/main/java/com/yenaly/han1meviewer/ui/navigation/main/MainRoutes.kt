package com.yenaly.han1meviewer.ui.navigation.main

import androidx.annotation.IdRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.yenaly.han1meviewer.R
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
object HomeRoute

@Serializable
object WatchHistoryRoute

@Serializable
object MyFavVideoRoute

@Serializable
object MyWatchLaterRoute

@Serializable
object MyPlaylistRoute

@Serializable
object SubscriptionRoute

@Serializable
object DailyCheckInRoute

@Serializable
object DownloadRoute

@Serializable
data class SearchRoute(
    val query: String? = null,
    val advancedSearchJson: String? = null,
)

@Serializable
object PreviewRoute

@Serializable
data class PreviewCommentRoute(
    val date: String,
    val dateCode: String,
)

@Serializable
data class VideoRoute(
    val videoCode: String,
    val localUri: String? = null,
)

enum class MainDestinationSpec(
    @param:IdRes val legacyId: Int?,
    @param:IdRes val menuItemId: Int?,
    val routeClass: KClass<*>,
    val drawerEnabled: Boolean,
    val showToolbar: Boolean,
) {
    Home(
        legacyId = R.id.nv_home_page,
        menuItemId = R.id.nv_home_page,
        routeClass = HomeRoute::class,
        drawerEnabled = true,
        showToolbar = false,
    ),
    WatchHistory(
        legacyId = R.id.nv_watch_history,
        menuItemId = R.id.nv_watch_history,
        routeClass = WatchHistoryRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    ),
    MyFavVideo(
        legacyId = R.id.nv_fav_video,
        menuItemId = R.id.nv_fav_video,
        routeClass = MyFavVideoRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    ),
    MyWatchLater(
        legacyId = R.id.nv_watch_later,
        menuItemId = R.id.nv_watch_later,
        routeClass = MyWatchLaterRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    ),
    MyPlaylist(
        legacyId = R.id.myPlayListFragmentV2,
        menuItemId = R.id.nv_playlist,
        routeClass = MyPlaylistRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    ),
    Subscription(
        legacyId = R.id.nv_subscription,
        menuItemId = R.id.nv_subscription,
        routeClass = SubscriptionRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    ),
    DailyCheckIn(
        legacyId = R.id.nv_daily_check_in,
        menuItemId = R.id.nv_daily_check_in,
        routeClass = DailyCheckInRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    ),
    Download(
        legacyId = R.id.nv_download,
        menuItemId = R.id.nv_download,
        routeClass = DownloadRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    ),
    Search(
        legacyId = R.id.searchFragment,
        menuItemId = null,
        routeClass = SearchRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    ),
    Preview(
        legacyId = R.id.nv_preview,
        menuItemId = null,
        routeClass = PreviewRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    ),
    PreviewComment(
        legacyId = R.id.nv_preview_comment,
        menuItemId = null,
        routeClass = PreviewCommentRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    ),
    Video(
        legacyId = R.id.videoFragment,
        menuItemId = null,
        routeClass = VideoRoute::class,
        drawerEnabled = false,
        showToolbar = false,
    );

    companion object {
        fun fromDestination(destination: NavDestination?): MainDestinationSpec? {
            if (destination == null) return null
            return entries.firstOrNull { destination.hasRoute(it.routeClass) }
        }

        fun fromLegacyId(@IdRes id: Int): MainDestinationSpec? =
            entries.firstOrNull { it.legacyId == id }
    }
}
