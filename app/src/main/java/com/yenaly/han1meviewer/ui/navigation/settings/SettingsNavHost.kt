package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.adaptive.LocalTabletRailVisible
import com.yenaly.han1meviewer.ui.adaptive.TabletEmptyDetail
import com.yenaly.han1meviewer.ui.adaptive.TabletListDetail
import com.yenaly.han1meviewer.ui.adaptive.shouldUseListDetail
import com.yenaly.han1meviewer.ui.adaptive.tabletReadableWidth
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.util.logScreenViewEvent
import com.yenaly.yenaly_libs.utils.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    navController: NavController,
    fallbackDestination: Any,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity<MainActivity>()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = SettingsDestinationSpec.fromDestination(backStackEntry?.destination)
        ?: SettingsDestinationSpec.Home

    fun navigateBack() {
        if (!navController.popBackStack()) {
            navController.navigate(fallbackDestination)
        }
    }

    LaunchedEffect(currentDestination) {
        activity.logScreenViewEvent(currentDestination.screenClassName)
    }

    if (shouldUseListDetail()) {
        val parent = currentDestination.parent()
        if (parent == null) {
            TabletListDetail(
                showDetail = false,
                listWidth = 360.dp,
                list = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(16.dp),
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            content()
                        }
                    }
                },
                detail = {},
                emptyDetail = {
                    TabletEmptyDetail(stringResource(R.string.tablet_select_settings))
                },
            )
        } else {
            TabletListDetail(
                showDetail = true,
                listWidth = 360.dp,
                list = {
                    SettingsParentPane(
                        parent = parent,
                        activity = activity,
                        navController = navController,
                    )
                },
                detail = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (currentDestination.showToolbar) {
                            TopAppBar(
                                title = { Text(stringResource(currentDestination.titleRes)) },
                                navigationIcon = {
                                    FilledIconButton(onClick = ::navigateBack) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.back),
                                        )
                                    }
                                },
                                actions = { actions() },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            content()
                        }
                    }
                },
                emptyDetail = {},
            )
        }
        return
    }

    val hideHomeBack = LocalTabletRailVisible.current &&
        currentDestination == SettingsDestinationSpec.Home
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentDestination.showToolbar) {
                TopAppBar(
                    title = { Text(stringResource(currentDestination.titleRes)) },
                    navigationIcon = {
                        if (!hideHomeBack) {
                            FilledIconButton(onClick = ::navigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        }
                    },
                    actions = { actions() },
                    modifier = Modifier.statusBarsPadding(),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .tabletReadableWidth(840.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsParentPane(
    parent: SettingsDestinationSpec,
    activity: MainActivity,
    navController: NavController,
) {
    when (parent) {
        SettingsDestinationSpec.Home -> HomeSettingsRouteScreen(
            activity = activity,
            onNavigateToPlayerSettings = { navController.navigateSafely(PlayerSettingsRoute) },
            onNavigateToHKeyframeSettings = { navController.navigateSafely(HKeyframeSettingsRoute) },
            onNavigateToDownloadSettings = { navController.navigateSafely(DownloadSettingsRoute) },
            onNavigateToNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
        )
        SettingsDestinationSpec.Player -> PlayerSettingsRouteScreen(
            onNavigateToMpvSettings = { navController.navigateSafely(MpvPlayerSettingsRoute) },
        )
        SettingsDestinationSpec.HKeyframeSettings -> HKeyframeSettingsRouteScreen(
            onNavigateToHKeyframes = { navController.navigateSafely(HKeyframesRoute) },
            onNavigateToSharedHKeyframes = { navController.navigateSafely(SharedHKeyframesRoute) },
        )
        else -> Unit
    }
}
