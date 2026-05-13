package com.yenaly.han1meviewer.ui.screen.main

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.exception.CloudFlareBlockedException
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.main.MainNavHost
import com.yenaly.han1meviewer.ui.navigation.main.handleMainIntent
import com.yenaly.han1meviewer.ui.navigation.main.navigateDrawerDestination
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsDestinationSpec
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
    drawerOpenRequests: Flow<Unit>,
    pendingNavigationRequests: Flow<Intent>,
    showAuthGuard: Boolean,
    onOpenSettings: (SettingsDestinationSpec) -> Unit,
    onRequireLogin: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onNavigateControllerReady: (NavHostController) -> Unit,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
) {
    HanimeTheme {
        val composeNavController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var currentMainDestination by remember { mutableStateOf(MainDestinationSpec.Home) }

        val homeState by viewModel.homePageFlow.collectAsStateWithLifecycle()
        val versionState by AppViewModel.versionFlow.collectAsStateWithLifecycle()
        val isLoggedIn by Preferences.loginStateFlow.collectAsStateWithLifecycle()
        val headerAvatarUrl = (homeState as? WebsiteState.Success)?.info?.avatarUrl
        val headerUsername = (homeState as? WebsiteState.Success)?.info?.username
        val headerIsLoading = homeState !is WebsiteState.Success && !isLoggedIn
        val selectedDrawerItemId = currentMainDestination.menuItemId

        LaunchedEffect(composeNavController) {
            onNavigateControllerReady(composeNavController)
        }
        LaunchedEffect(Unit) {
            drawerOpenRequests.collect {
                if (currentMainDestination.drawerEnabled) {
                    drawerState.open()
                }
            }
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
                    android.util.Log.e("error", "被屏蔽时的处理")
                }
            }
        }
        MainActivityScaffold(
            drawerState = drawerState,
            drawerEnabled = currentMainDestination.drawerEnabled,
            selectedItemId = selectedDrawerItemId,
            avatarUrl = headerAvatarUrl,
            username = headerUsername,
            isLoggedIn = isLoggedIn,
            isLoading = headerIsLoading,
            currentSite = Preferences.baseUrl,
            onAvatarClick = {
                if (isLoggedIn) {
                    onOpenSettings(SettingsDestinationSpec.Home)
                } else {
                    onRequireLogin()
                }
            },
            onSwitchSiteClick = onSwitchSiteClick,
            onDrawerItemSelected = { itemId ->
                val handled = composeNavController.navigateDrawerDestination(
                    itemId = itemId,
                    isLoggedIn = isLoggedIn,
                    onRequireLogin = { showShortToast(R.string.login_first) },
                    onOpenSettings = onOpenSettings,
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
                    onDestinationChanged = { destination ->
                        currentMainDestination = destination
                        onDestinationChanged(destination)
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
