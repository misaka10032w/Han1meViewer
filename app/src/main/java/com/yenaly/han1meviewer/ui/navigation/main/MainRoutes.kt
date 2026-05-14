package com.yenaly.han1meviewer.ui.navigation.main

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.yenaly.han1meviewer.ui.navigation.settings.DownloadSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SharedHKeyframesRoute
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
    val drawerDestination: MainDrawerDestination?,
    val routeClass: KClass<*>,
    val drawerEnabled: Boolean,
) {
    Home(
        drawerDestination = MainDrawerDestination.Home,
        routeClass = HomeRoute::class,
        drawerEnabled = true,
    ),
    WatchHistory(
        drawerDestination = MainDrawerDestination.WatchHistory,
        routeClass = WatchHistoryRoute::class,
        drawerEnabled = false,
    ),
    MyFavVideo(
        drawerDestination = MainDrawerDestination.FavVideo,
        routeClass = MyFavVideoRoute::class,
        drawerEnabled = false,
    ),
    MyWatchLater(
        drawerDestination = MainDrawerDestination.WatchLater,
        routeClass = MyWatchLaterRoute::class,
        drawerEnabled = false,
    ),
    MyPlaylist(
        drawerDestination = MainDrawerDestination.Playlist,
        routeClass = MyPlaylistRoute::class,
        drawerEnabled = false,
    ),
    Subscription(
        drawerDestination = MainDrawerDestination.Subscription,
        routeClass = SubscriptionRoute::class,
        drawerEnabled = false,
    ),
    DailyCheckIn(
        drawerDestination = MainDrawerDestination.DailyCheckIn,
        routeClass = DailyCheckInRoute::class,
        drawerEnabled = false,
    ),
    Download(
        drawerDestination = MainDrawerDestination.Download,
        routeClass = DownloadRoute::class,
        drawerEnabled = false,
    ),
    SettingsHome(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = HomeSettingsRoute::class,
        drawerEnabled = false,
    ),
    SettingsPlayer(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = PlayerSettingsRoute::class,
        drawerEnabled = false,
    ),
    SettingsNetwork(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = NetworkSettingsRoute::class,
        drawerEnabled = false,
    ),
    SettingsDownload(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = DownloadSettingsRoute::class,
        drawerEnabled = false,
    ),
    SettingsMpv(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = MpvPlayerSettingsRoute::class,
        drawerEnabled = false,
    ),
    SettingsHKeyframes(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = HKeyframesRoute::class,
        drawerEnabled = false,
    ),
    SettingsSharedHKeyframes(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = SharedHKeyframesRoute::class,
        drawerEnabled = false,
    ),
    SettingsHKeyframeSettings(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = HKeyframeSettingsRoute::class,
        drawerEnabled = false,
    ),
    Search(
        drawerDestination = null,
        routeClass = SearchRoute::class,
        drawerEnabled = false,
    ),
    Preview(
        drawerDestination = null,
        routeClass = PreviewRoute::class,
        drawerEnabled = false,
    ),
    PreviewComment(
        drawerDestination = null,
        routeClass = PreviewCommentRoute::class,
        drawerEnabled = false,
    ),
    Video(
        drawerDestination = null,
        routeClass = VideoRoute::class,
        drawerEnabled = false,
    );

    companion object {
        fun fromDestination(destination: NavDestination?): MainDestinationSpec? {
            if (destination == null) return null
            return entries.firstOrNull { destination.hasRoute(it.routeClass) }
        }
    }
}
