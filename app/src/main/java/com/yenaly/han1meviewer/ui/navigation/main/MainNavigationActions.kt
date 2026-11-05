package com.yenaly.han1meviewer.ui.navigation.main

import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import kotlinx.serialization.json.Json

fun NavHostController.navigateTopLevelDestination(
    destination: MainTopLevelDestination,
    isLoggedIn: Boolean,
    onRequireLogin: () -> Unit,
): Boolean {
    if (destination.requiresLogin && !isLoggedIn) {
        onRequireLogin()
        return false
    }

    navigateSafely(destination.route) {
        popUpTo(HomeRoute) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
    return true
}

/**
 * 设置页内部跳转时只保留“设置首页 + 当前分类”，避免平板双栏反复点击后不断入栈。
 */
fun NavController.navigateSettingsDestination(route: Any) {
    navigateSafely(route) {
        popUpTo(HomeSettingsRoute) {
            inclusive = false
        }
        launchSingleTop = true
    }
}

fun NavHostController.handleMainIntent(intent: Intent) {
    if (intent.action == Intent.ACTION_VIEW) {
        val uri = intent.data ?: return
        when (uri.scheme) {
            "http", "https" -> {
                val videoCode = uri.getQueryParameter("v")
                if (videoCode != null) {
                    navigateSafely(VideoRoute(videoCode))
                }
            }

            "file", "content" -> {
                navigateSafely(VideoRoute("-1", uri.toString()))
            }
        }
        return
    }

    intent.getStringExtra("startSearchFromTag")?.let { tag ->
        intent.removeExtra("startSearchFromTag")
        navigateSafely(SearchRoute(query = tag))
        return
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    val map = intent.getSerializableExtra("startSearchFromMap") as? HashMap<String, String>
    if (map != null) {
        intent.removeExtra("startSearchFromMap")
        navigateSafely(SearchRoute(advancedSearchJson = Json.encodeToString(map)))
        return
    }

    val videoCode = intent.getStringExtra("startVideoCode")
    if (!videoCode.isNullOrEmpty()) {
        intent.removeExtra("startVideoCode")
        navigateSafely(VideoRoute(videoCode))
    }
}
