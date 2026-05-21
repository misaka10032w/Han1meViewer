package com.yenaly.han1meviewer.ui.navigation

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

fun NavController.canNavigateSafely(): Boolean {
    val currentState = currentBackStackEntry?.lifecycle?.currentState
    Log.i("nav_state", currentState?.toString() ?: "BackStack is empty (null)")
    return currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true
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
