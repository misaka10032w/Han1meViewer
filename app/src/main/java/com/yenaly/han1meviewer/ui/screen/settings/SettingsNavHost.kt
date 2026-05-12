package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yenaly.han1meviewer.ui.activity.SettingsActivity
import com.yenaly.han1meviewer.util.logScreenViewEvent

@Composable
fun SettingsActivityContent(
    activity: SettingsActivity,
    startDestination: SettingsDestinationSpec,
    onExitSettings: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = SettingsDestinationSpec.fromDestination(backStackEntry?.destination)
        ?: startDestination

    activity.navigateBackAction = {
        if (!navController.popBackStack()) {
            onExitSettings()
        }
    }

    LaunchedEffect(currentDestination) {
        activity.logScreenViewEvent(currentDestination.screenClassName)
        if (currentDestination.showToolbar) {
            activity.showToolbar()
            activity.setupToolbar(
                activity.getString(currentDestination.titleRes),
                canNavigateBack = true,
            )
        } else {
            activity.hideToolbar()
        }
    }

    SettingsNavHost(
        navController = navController,
        startDestination = startDestination,
        onExitSettings = onExitSettings,
    )
}

@Composable
private fun SettingsNavHost(
    navController: NavHostController,
    startDestination: SettingsDestinationSpec,
    onExitSettings: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
    ) {
        composable<HomeSettingsRoute> {
            HomeSettingsRouteScreen(
                onNavigateToPlayerSettings = { navController.navigate(PlayerSettingsRoute) },
                onNavigateToHKeyframeSettings = { navController.navigate(HKeyframeSettingsRoute) },
                onNavigateToDownloadSettings = { navController.navigate(DownloadSettingsRoute) },
                onNavigateToNetworkSettings = { navController.navigate(NetworkSettingsRoute) },
            )
        }
        composable<PlayerSettingsRoute> {
            PlayerSettingsRouteScreen(
                onNavigateToMpvSettings = { navController.navigate(MpvPlayerSettingsRoute) },
            )
        }
        composable<NetworkSettingsRoute> {
            NetworkSettingsRouteScreen()
        }
        composable<DownloadSettingsRoute> {
            DownloadSettingsRouteScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        onExitSettings()
                    }
                },
            )
        }
        composable<MpvPlayerSettingsRoute> {
            MpvPlayerSettingsRouteScreen()
        }
        composable<HKeyframesRoute> {
            HKeyframesRouteScreen()
        }
        composable<SharedHKeyframesRoute> {
            SharedHKeyframesRouteScreen()
        }
        composable<HKeyframeSettingsRoute> {
            HKeyframeSettingsRouteScreen(
                onNavigateToHKeyframes = { navController.navigate(HKeyframesRoute) },
                onNavigateToSharedHKeyframes = { navController.navigate(SharedHKeyframesRoute) },
            )
        }
    }
}
