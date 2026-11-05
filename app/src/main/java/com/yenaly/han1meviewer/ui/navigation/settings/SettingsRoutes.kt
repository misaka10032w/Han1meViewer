package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.R
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
object HomeSettingsRoute

@Serializable
data class SettingsCategoryRoute(val categoryKey: String)

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
    val category: SettingsCategory? = null,
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
        category = SettingsCategory.Playback,
    ),
    Network(
        routeKey = "network",
        titleRes = R.string.network_settings,
        iconRes = R.drawable.baseline_online_manage_24,
        screenClassName = "NetworkSettingsScreen",
        routeClass = NetworkSettingsRoute::class,
        category = SettingsCategory.Network,
    ),
    Download(
        routeKey = "download",
        titleRes = R.string.download_settings,
        iconRes = R.drawable.ic_baseline_download_24,
        screenClassName = "DownloadSettingsScreen",
        routeClass = DownloadSettingsRoute::class,
        category = SettingsCategory.Download,
    ),
    Mpv(
        routeKey = "mpv",
        titleRes = R.string.mpv_advanced_settings,
        iconRes = R.drawable.baseline_render_24,
        screenClassName = "MpvPlayerSettingsScreen",
        routeClass = MpvPlayerSettingsRoute::class,
        category = SettingsCategory.Playback,
    ),
    HKeyframes(
        routeKey = "h_keyframes",
        titleRes = R.string.h_keyframe_manage,
        iconRes = R.drawable.baseline_frame_inter_24,
        screenClassName = "HKeyframesScreen",
        routeClass = HKeyframesRoute::class,
        category = SettingsCategory.Keyframes,
    ),
    SharedHKeyframes(
        routeKey = "shared_h_keyframes",
        titleRes = R.string.shared_h_keyframe_manage,
        iconRes = R.drawable.baseline_share_24,
        screenClassName = "SharedHKeyframesScreen",
        routeClass = SharedHKeyframesRoute::class,
        category = SettingsCategory.Keyframes,
    ),
    HKeyframeSettings(
        routeKey = "h_keyframe_settings",
        titleRes = R.string.h_keyframe_settings,
        iconRes = R.drawable.baseline_custom_24,
        screenClassName = "HKeyframeSettingsScreen",
        routeClass = HKeyframeSettingsRoute::class,
        category = SettingsCategory.Keyframes,
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

/**
 * 设置页当前展示的页面类型。
 *
 * 用于统一标题、返回行为、双栏左侧选中态和统计埋点。
 */
sealed interface SettingsScreenDestination {
    val titleRes: Int
    val screenClassName: String
    val showToolbar: Boolean
    val category: SettingsCategory?

    /**
     * 用于 [androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator]
     * 的可保存内容键，不能直接使用 sealed 对象。
     */
    val contentKey: String

    data object Home : SettingsScreenDestination {
        override val titleRes: Int = R.string.settings
        override val screenClassName: String = "HomeSettingsScreen"
        override val showToolbar: Boolean = true
        override val category: SettingsCategory? = null
        override val contentKey: String = "home"
    }

    data class Category(
        val value: SettingsCategory,
    ) : SettingsScreenDestination {
        override val titleRes: Int get() = value.titleRes
        override val screenClassName: String get() = value.screenClassName
        override val showToolbar: Boolean get() = true
        override val category: SettingsCategory get() = value
        override val contentKey: String get() = "category:${value.key}"
    }

    data class Sub(
        val value: SettingsDestinationSpec,
    ) : SettingsScreenDestination {
        override val titleRes: Int get() = value.titleRes
        override val screenClassName: String get() = value.screenClassName
        override val showToolbar: Boolean get() = value.showToolbar
        override val category: SettingsCategory? get() = value.category
        override val contentKey: String get() = "sub:${value.routeKey}"
    }
}

fun NavBackStackEntry.resolveSettingsScreenDestination(): SettingsScreenDestination {
    if (destination.hasRoute<SettingsCategoryRoute>()) {
        val category = runCatching { toRoute<SettingsCategoryRoute>() }
            .getOrNull()
            ?.let { SettingsCategory.fromKey(it.categoryKey) }
        if (category != null) return SettingsScreenDestination.Category(category)
    }
    val spec = SettingsDestinationSpec.fromDestination(destination)
    return if (spec != null && spec != SettingsDestinationSpec.Home) {
        SettingsScreenDestination.Sub(spec)
    } else {
        SettingsScreenDestination.Home
    }
}
