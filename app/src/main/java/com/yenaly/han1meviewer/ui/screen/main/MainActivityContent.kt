package com.yenaly.han1meviewer.ui.screen.main

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.exception.CloudFlareBlockedException
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.main.MainNavHost
import com.yenaly.han1meviewer.ui.navigation.main.VideoRoute
import com.yenaly.han1meviewer.ui.navigation.main.handleMainIntent
import com.yenaly.han1meviewer.ui.navigation.main.navigateDrawerDestination
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.han1meviewer.util.showUpdateDialog
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun MainActivityContent(
    activity: MainActivity,
    viewModel: MainViewModel,
    pendingNavigationRequests: Flow<Intent>,
    showAuthGuard: Boolean,
    onLogoutClick: () -> Unit,
    onRequireLogin: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onNavigateControllerReady: (NavHostController) -> Unit,
    onCurrentVideoRouteChanged: (VideoRoute?) -> Unit,
) {
    HanimeTheme {
        val composeNavController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var currentMainDestination by remember { mutableStateOf(MainDestinationSpec.Home) }
        val backStackEntry by composeNavController.currentBackStackEntryAsState()
        val currentVideoRoute = backStackEntry?.takeIf {
            it.destination.hasRoute(VideoRoute::class)
        }?.toRoute<VideoRoute>()

        val homeState by viewModel.homePageFlow.collectAsStateWithLifecycle()
        val versionState by AppViewModel.versionFlow.collectAsStateWithLifecycle()
        val isLoggedIn by Preferences.loginStateFlow.collectAsStateWithLifecycle()
        val headerAvatarUrl = if (isLoggedIn) {
            (homeState as? WebsiteState.Success)?.info?.avatarUrl
        } else {
            null
        }
        val headerUsername = if (isLoggedIn) {
            (homeState as? WebsiteState.Success)?.info?.username
        } else {
            null
        }
        val headerIsLoading = isLoggedIn && homeState is WebsiteState.Loading
        val selectedDrawerDestination = currentMainDestination.drawerDestination

        LaunchedEffect(composeNavController) {
            onNavigateControllerReady(composeNavController)
        }
        LaunchedEffect(currentVideoRoute) {
            onCurrentVideoRouteChanged(currentVideoRoute)
        }
        LaunchedEffect(Unit) {
            pendingNavigationRequests.collect { intent ->
                composeNavController.handleMainIntent(intent)
            }
        }
        LaunchedEffect(versionState) {
            if (versionState is WebsiteState.Success && Preferences.isUpdateDialogVisible) {
                (versionState as WebsiteState.Success).info?.let { release ->
                    Preferences.lastUpdatePopupTime = kotlin.time.Clock.System.now().epochSeconds
                    activity.showUpdateDialog(release)
                }
            }
        }
        LaunchedEffect(homeState) {
            if (homeState is WebsiteState.Error) {
                val throwable = (homeState as WebsiteState.Error).throwable
                if (throwable is CloudFlareBlockedException) {
                    Log.e("error", "被屏蔽时的处理")
                }
            }
        }
        MainActivityScaffold(
            drawerState = drawerState,
            drawerEnabled = currentMainDestination.drawerEnabled,
            selectedDestination = selectedDrawerDestination,
            avatarUrl = headerAvatarUrl,
            username = headerUsername,
            isLoggedIn = isLoggedIn,
            isLoading = headerIsLoading,
            currentSite = Preferences.baseUrl,
            onAvatarClick = {
                if (isLoggedIn) {
                    onLogoutClick()
                } else {
                    onRequireLogin()
                }
            },
            onSwitchSiteClick = onSwitchSiteClick,
            onDrawerItemSelected = { destination ->
                val handled = composeNavController.navigateDrawerDestination(
                    destination = destination,
                    isLoggedIn = isLoggedIn,
                    onRequireLogin = { showShortToast(R.string.login_first) },
                )
                if (handled) {
                    scope.launch { drawerState.close() }
                }
                handled
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MainNavHost(
                    activity = activity,
                    navController = composeNavController,
                    onOpenDrawer = {
                        if (currentMainDestination.drawerEnabled) {
                            scope.launch { drawerState.open() }
                        }
                    },
                    onDestinationChanged = { destination ->
                        currentMainDestination = destination
                    },
                )
                if (showAuthGuard) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                    )
                }
            }
        }
    }
}
