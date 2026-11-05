package com.yenaly.han1meviewer.ui.navigation.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.yenaly.han1meviewer.R

/**
 * 主界面的一级导航目的地。
 *
 * 这些入口会显示在底部导航栏或平板/折叠屏的导航轨中。
 */
enum class MainTopLevelDestination(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    val route: Any,
    val requiresLogin: Boolean = false,
) {
    Home(
        icon = Icons.Filled.Home,
        titleRes = R.string.home_page,
        route = HomeRoute,
    ),
    Subscription(
        icon = Icons.Filled.Subscriptions,
        titleRes = R.string.my_subscribe,
        route = SubscriptionRoute,
        requiresLogin = true,
    ),
    ContentLibrary(
        icon = Icons.Filled.VideoLibrary,
        titleRes = R.string.content_library,
        route = ContentLibraryRoute,
    ),
    Profile(
        icon = Icons.Filled.Person,
        titleRes = R.string.my_account,
        route = ProfileRoute,
    ),
}
