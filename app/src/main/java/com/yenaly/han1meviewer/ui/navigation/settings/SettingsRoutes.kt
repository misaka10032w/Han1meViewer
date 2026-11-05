package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
    val screenClassName: String,
    val routeClass: KClass<*>,
    val showToolbar: Boolean = true,
) {
    Home(
        routeKey = "home",
        titleRes = R.string.settings,
        iconRes = R.drawable.ic_baseline_home_24,
        screenClassName = "HomeSettingsScreen",
        routeClass = HomeSettingsRoute::class,
    ),
    Player(
        routeKey = "player",
        titleRes = R.string.player_settings,
        iconRes = R.drawable.baseline_player_24,
        screenClassName = "PlayerSettingsScreen",
        routeClass = PlayerSettingsRoute::class,
    ),
    Network(
        routeKey = "network",
        titleRes = R.string.network_settings,
        iconRes = R.drawable.baseline_online_manage_24,
        screenClassName = "NetworkSettingsScreen",
        routeClass = NetworkSettingsRoute::class,
    ),
    Download(
        routeKey = "download",
        titleRes = R.string.download_settings,
        iconRes = R.drawable.ic_baseline_download_24,
        screenClassName = "DownloadSettingsScreen",
        routeClass = DownloadSettingsRoute::class,
    ),
    Mpv(
        routeKey = "mpv",
        titleRes = R.string.mpv_advanced_settings,
        iconRes = R.drawable.baseline_render_24,
        screenClassName = "MpvPlayerSettingsScreen",
        routeClass = MpvPlayerSettingsRoute::class,
    ),
    HKeyframes(
        routeKey = "h_keyframes",
        titleRes = R.string.h_keyframe_manage,
        iconRes = R.drawable.baseline_frame_inter_24,
        screenClassName = "HKeyframesScreen",
        routeClass = HKeyframesRoute::class,
    ),
    SharedHKeyframes(
        routeKey = "shared_h_keyframes",
        titleRes = R.string.shared_h_keyframe_manage,
        iconRes = R.drawable.baseline_share_24,
        screenClassName = "SharedHKeyframesScreen",
        routeClass = SharedHKeyframesRoute::class,
    ),
    HKeyframeSettings(
        routeKey = "h_keyframe_settings",
        titleRes = R.string.h_keyframe_settings,
        iconRes = R.drawable.baseline_custom_24,
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
