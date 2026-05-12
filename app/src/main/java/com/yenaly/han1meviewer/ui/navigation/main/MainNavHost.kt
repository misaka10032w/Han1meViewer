package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.ui.activity.MainActivity
import kotlinx.serialization.json.Json

@Composable
fun MainNavHost(
    activity: MainActivity,
    navController: NavHostController,
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
        popExitTransition = {
            scaleOut(
                targetScale = 0.9f,
                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
            )
        },
        popEnterTransition = {
            EnterTransition.None
        },
    ) {
        composable<HomeRoute> {
            HomeRouteScreen(
                activity = activity,
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
        composable<SearchRoute> {
            SearchRouteScreen(
                activity = activity,
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
