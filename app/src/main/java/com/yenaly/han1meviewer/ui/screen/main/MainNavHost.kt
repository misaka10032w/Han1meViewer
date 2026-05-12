package com.yenaly.han1meviewer.ui.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yenaly.han1meviewer.ui.activity.MainActivity

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
    ) {
        composable<HomeRoute> {
            HomeRouteScreen(
                activity = activity,
                onNavigateToPreview = { navController.navigate(PreviewRoute) },
                onNavigateToSearch = { query -> navController.navigate(SearchRoute(query = query)) },
                onNavigateToSearchAdvanced = { _ -> },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<WatchHistoryRoute> {
            WatchHistoryRouteScreen(
                activity = activity,
                onBack = { navController.popBackStack() },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<MyFavVideoRoute> {
            MyFavVideoRouteScreen(
                activity = activity,
                onBack = { navController.popBackStack() },
                onNavigateToVideo = { code -> navController.navigate(VideoRoute(code)) },
            )
        }
        composable<MyWatchLaterRoute> {
            MyWatchLaterRouteScreen(
                activity = activity,
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
        }
        composable<PreviewRoute> {
        }
        composable<PreviewCommentRoute> {
        }
        composable<VideoRoute> {
        }
    }
}
