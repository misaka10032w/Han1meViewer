package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.settings.DownloadSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.DownloadSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframeSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsScaffold
import com.yenaly.han1meviewer.ui.navigation.settings.SharedHKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SharedHKeyframesRouteScreen
import kotlinx.serialization.json.Json

@Composable
fun MainNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destinationSpec = MainDestinationSpec.fromDestination(backStackEntry?.destination)

    LaunchedEffect(destinationSpec) {
        destinationSpec?.let(onDestinationChanged)
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        // 新页面进入：从右侧滑入，同时伴随淡入，且带有回弹感
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(450))
        },
        // 旧页面退出：向左轻微偏移，同时缩小并淡出，营造被“压在下面”的感觉
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                targetOffset = { it / 3 }, // 只偏移 1/3 的宽度
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + scaleOut(targetScale = 0.9f) + fadeOut(animationSpec = tween(300))
        },
        // 弹出（返回）新页面进入：从左侧滑入，由 0.9 放大恢复，营造“浮上来”的感觉
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                initialOffset = { it / 3 },
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + scaleIn(initialScale = 0.9f) + fadeIn(animationSpec = tween(450))
        },
        // 弹出（返回）旧页面退出：向右侧滑出，同时淡出
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable<HomeRoute> {
            HomeRouteScreen(
                activity = activity,
                onOpenDrawer = onOpenDrawer,
                onNavigateToPreview = { navController.navigate(PreviewRoute) },
                onNavigateToSearch = { query -> navController.navigate(SearchRoute(query = query)) },
                onNavigateToSearchAdvanced = { params ->
                    navController.navigate(
                        SearchRoute(advancedSearchJson = Json.encodeToString(params))
                    )
                },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<WatchHistoryRoute> {
            WatchHistoryRouteScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<MyFavVideoRoute> {
            MyFavVideoRouteScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<MyWatchLaterRoute> {
            MyWatchLaterRouteScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<MyPlaylistRoute> {
            MyPlaylistRouteScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<SubscriptionRoute> {
            SubscriptionRouteScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSearch = { query -> navController.navigate(SearchRoute(query = query)) },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<DailyCheckInRoute> {
            DailyCheckInRouteScreen(
                activity = activity,
                onBack = { navController.popBackStack() },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<DownloadRoute> {
            DownloadRouteScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
                onNavigateToLocalVideo = { code, uri -> navController.navigate(VideoRoute(code, uri)) },
            )
        }
        composable<HomeSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HomeRoute,
            ) {
                HomeSettingsRouteScreen(
                    activity = activity,
                    onNavigateToPlayerSettings = { navController.navigate(PlayerSettingsRoute) },
                    onNavigateToHKeyframeSettings = { navController.navigate(HKeyframeSettingsRoute) },
                    onNavigateToDownloadSettings = { navController.navigate(DownloadSettingsRoute) },
                    onNavigateToNetworkSettings = { navController.navigate(NetworkSettingsRoute) },
                )
            }
        }
        composable<PlayerSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HomeSettingsRoute,
            ) {
                PlayerSettingsRouteScreen(
                    onNavigateToMpvSettings = { navController.navigate(MpvPlayerSettingsRoute) },
                )
            }
        }
        composable<NetworkSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HomeSettingsRoute,
            ) {
                NetworkSettingsRouteScreen(activity = activity)
            }
        }
        composable<DownloadSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HomeSettingsRoute,
            ) {
                DownloadSettingsRouteScreen(activity = activity)
            }
        }
        composable<MpvPlayerSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = PlayerSettingsRoute,
            ) {
                MpvPlayerSettingsRouteScreen()
            }
        }
        composable<HKeyframesRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HKeyframeSettingsRoute,
            ) {
                HKeyframesRouteScreen(
                    onOpenVideo = { code -> navController.navigate(VideoRoute(code)) },
                )
            }
        }
        composable<SharedHKeyframesRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HKeyframeSettingsRoute,
            ) {
                SharedHKeyframesRouteScreen(
                    onOpenVideo = { code -> navController.navigate(VideoRoute(code)) },
                )
            }
        }
        composable<HKeyframeSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HomeSettingsRoute,
            ) {
                HKeyframeSettingsRouteScreen(
                    onNavigateToHKeyframes = { navController.navigate(HKeyframesRoute) },
                    onNavigateToSharedHKeyframes = { navController.navigate(SharedHKeyframesRoute) },
                )
            }
        }
        composable<SearchRoute> {
            SearchRouteScreen(
                route = it.toRoute(),
                onBack = { navController.popBackStack() },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<PreviewRoute> {
            PreviewRouteScreen(
                activity = activity,
                onBack = { navController.popBackStack() },
                onNavigateToPreviewComment = { date, dateCode ->
                    navController.navigate(PreviewCommentRoute(date, dateCode))
                },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<PreviewCommentRoute> {
            PreviewCommentRouteScreen(
                activity = activity,
                route = it.toRoute(),
                onBack = { navController.popBackStack() },
            )
        }
        composable<VideoRoute> {
            VideoRouteScreen(
                activity = activity,
                route = it.toRoute(),
            )
        }
    }
}
