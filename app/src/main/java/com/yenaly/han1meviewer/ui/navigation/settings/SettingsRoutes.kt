package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.yenaly.han1meviewer.R
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
object HomeSettingsRoute

@Serializable
object PlayerSettingsRoute

@Serializable
object NetworkSettingsRoute

@Serializable
object DownloadSettingsRoute

@Serializable
object MpvPlayerSettingsRoute

@Serializable
object HKeyframesRoute

@Serializable
object SharedHKeyframesRoute

@Serializable
object HKeyframeSettingsRoute

enum class SettingsDestinationSpec(
    val routeKey: String,
    val titleRes: Int,
    val screenClassName: String,
    val routeClass: KClass<*>,
    val showToolbar: Boolean = true,
) {
    Home(
        routeKey = "home",
        titleRes = R.string.settings,
        screenClassName = "HomeSettingsScreen",
        routeClass = HomeSettingsRoute::class,
    ),
    Player(
        routeKey = "player",
        titleRes = R.string.player_settings,
        screenClassName = "PlayerSettingsScreen",
        routeClass = PlayerSettingsRoute::class,
    ),
    Network(
        routeKey = "network",
        titleRes = R.string.network_settings,
        screenClassName = "NetworkSettingsScreen",
        routeClass = NetworkSettingsRoute::class,
    ),
    Download(
        routeKey = "download",
        titleRes = R.string.download_settings,
        screenClassName = "DownloadSettingsScreen",
        routeClass = DownloadSettingsRoute::class,
    ),
    Mpv(
        routeKey = "mpv",
        titleRes = R.string.mpv_advanced_settings,
        screenClassName = "MpvPlayerSettingsScreen",
        routeClass = MpvPlayerSettingsRoute::class,
    ),
    HKeyframes(
        routeKey = "h_keyframes",
        titleRes = R.string.h_keyframe_manage,
        screenClassName = "HKeyframesScreen",
        routeClass = HKeyframesRoute::class,
    ),
    SharedHKeyframes(
        routeKey = "shared_h_keyframes",
        titleRes = R.string.shared_h_keyframe_manage,
        screenClassName = "SharedHKeyframesScreen",
        routeClass = SharedHKeyframesRoute::class,
    ),
    HKeyframeSettings(
        routeKey = "h_keyframe_settings",
        titleRes = R.string.h_keyframe_settings,
        screenClassName = "HKeyframeSettingsScreen",
        routeClass = HKeyframeSettingsRoute::class,
    );

    val route: Any
        get() = when (this) {
            Home -> HomeSettingsRoute
            Player -> PlayerSettingsRoute
            Network -> NetworkSettingsRoute
            Download -> DownloadSettingsRoute
            Mpv -> MpvPlayerSettingsRoute
            HKeyframes -> HKeyframesRoute
            SharedHKeyframes -> SharedHKeyframesRoute
            HKeyframeSettings -> HKeyframeSettingsRoute
        }

    companion object {
        fun fromRouteKey(routeKey: String?): SettingsDestinationSpec? =
            entries.firstOrNull { it.routeKey == routeKey }

        fun fromDestination(destination: NavDestination?): SettingsDestinationSpec? {
            if (destination == null) return null
            return entries.firstOrNull { destination.hasRoute(it.routeClass) }
        }
    }
}
