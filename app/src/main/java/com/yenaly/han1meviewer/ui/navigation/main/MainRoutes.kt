package com.yenaly.han1meviewer.ui.navigation.main

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.yenaly.han1meviewer.ui.navigation.settings.AboutAppRoute
import com.yenaly.han1meviewer.ui.navigation.settings.DownloadSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategoryRoute
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
object CreatorCenterRoute

@Serializable
object AccountRoute

@Serializable
data class AvatarCropRoute(
    val sourceUri: String,
)

@Serializable
data class SearchRoute(
    val query: String? = null,
    val advancedSearchJson: String? = null,
)

@Serializable
object PreviewRoute

@Serializable
object ContentLibraryRoute

@Serializable
object ProfileRoute

@Serializable
object GetchuPreviewRoute

@Serializable
data class GetchuPreviewDetailRoute(
    val id: String,
)

@Serializable
data class PreviewCommentRoute(
    val date: String,
    val dateCode: String,
)

@Serializable
data class VideoRoute(
    val videoCode: String,
    val localUri: String? = null,
    val autoPlay: Boolean = false,
)

enum class MainDestinationSpec(
    val topLevelDestination: MainTopLevelDestination?,
    val routeClass: KClass<*>,
    val showNavigationSuite: Boolean,
) {
    Home(
        topLevelDestination = MainTopLevelDestination.Home,
        routeClass = HomeRoute::class,
        showNavigationSuite = true,
    ),
    Subscription(
        topLevelDestination = MainTopLevelDestination.Subscription,
        routeClass = SubscriptionRoute::class,
        showNavigationSuite = true,
    ),
    ContentLibrary(
        topLevelDestination = MainTopLevelDestination.ContentLibrary,
        routeClass = ContentLibraryRoute::class,
        showNavigationSuite = true,
    ),
    Profile(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = ProfileRoute::class,
        showNavigationSuite = true,
    ),
    WatchHistory(
        topLevelDestination = MainTopLevelDestination.ContentLibrary,
        routeClass = WatchHistoryRoute::class,
        showNavigationSuite = false,
    ),
    MyFavVideo(
        topLevelDestination = MainTopLevelDestination.ContentLibrary,
        routeClass = MyFavVideoRoute::class,
        showNavigationSuite = false,
    ),
    MyWatchLater(
        topLevelDestination = MainTopLevelDestination.ContentLibrary,
        routeClass = MyWatchLaterRoute::class,
        showNavigationSuite = false,
    ),
    MyPlaylist(
        topLevelDestination = MainTopLevelDestination.ContentLibrary,
        routeClass = MyPlaylistRoute::class,
        showNavigationSuite = false,
    ),
    DailyCheckIn(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = DailyCheckInRoute::class,
        showNavigationSuite = false,
    ),
    Download(
        topLevelDestination = MainTopLevelDestination.ContentLibrary,
        routeClass = DownloadRoute::class,
        showNavigationSuite = false,
    ),
    CreatorCenter(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = CreatorCenterRoute::class,
        showNavigationSuite = false,
    ),
    Account(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = AccountRoute::class,
        showNavigationSuite = false,
    ),
    AvatarCrop(
        topLevelDestination = null,
        routeClass = AvatarCropRoute::class,
        showNavigationSuite = false,
    ),
    SettingsHome(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = HomeSettingsRoute::class,
        showNavigationSuite = false,
    ),
    SettingsCategory(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = SettingsCategoryRoute::class,
        showNavigationSuite = false,
    ),
    SettingsPlayer(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = PlayerSettingsRoute::class,
        showNavigationSuite = false,
    ),
    SettingsNetwork(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = NetworkSettingsRoute::class,
        showNavigationSuite = false,
    ),
    SettingsDownload(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = DownloadSettingsRoute::class,
        showNavigationSuite = false,
    ),
    SettingsMpv(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = MpvPlayerSettingsRoute::class,
        showNavigationSuite = false,
    ),
    SettingsHKeyframes(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = HKeyframesRoute::class,
        showNavigationSuite = false,
    ),
    SettingsSharedHKeyframes(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = SharedHKeyframesRoute::class,
        showNavigationSuite = false,
    ),
    SettingsHKeyframeSettings(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = HKeyframeSettingsRoute::class,
        showNavigationSuite = false,
    ),
    SettingsAboutApp(
        topLevelDestination = MainTopLevelDestination.Profile,
        routeClass = AboutAppRoute::class,
        showNavigationSuite = false,
    ),
    Search(
        topLevelDestination = null,
        routeClass = SearchRoute::class,
        showNavigationSuite = false,
    ),
    Preview(
        topLevelDestination = null,
        routeClass = PreviewRoute::class,
        showNavigationSuite = false,
    ),
    GetchuPreview(
        topLevelDestination = null,
        routeClass = GetchuPreviewRoute::class,
        showNavigationSuite = false,
    ),
    GetchuPreviewDetail(
        topLevelDestination = null,
        routeClass = GetchuPreviewDetailRoute::class,
        showNavigationSuite = false,
    ),
    PreviewComment(
        topLevelDestination = null,
        routeClass = PreviewCommentRoute::class,
        showNavigationSuite = false,
    ),
    Video(
        topLevelDestination = null,
        routeClass = VideoRoute::class,
        showNavigationSuite = false,
    );

    companion object {
        fun fromDestination(destination: NavDestination?): MainDestinationSpec? {
            if (destination == null) return null
            return entries.firstOrNull { destination.hasRoute(it.routeClass) }
        }
    }
}
