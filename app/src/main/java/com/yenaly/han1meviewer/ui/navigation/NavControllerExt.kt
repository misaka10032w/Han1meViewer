package com.yenaly.han1meviewer.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

fun NavController.canNavigateSafely(): Boolean {
    return currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED
}

fun <T : Any> NavController.navigateSafely(
    route: T,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    if (!canNavigateSafely()) return
    navigate(route) {
        launchSingleTop = false
        builder()
    }
}
