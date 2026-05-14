package com.yenaly.han1meviewer.ui.navigation.settings

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
