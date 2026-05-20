package com.yenaly.han1meviewer.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.navigation.main.MainDrawerDestination
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import kotlinx.coroutines.launch

@Composable
fun MainActivityScaffold(
    drawerState: DrawerState,
    drawerEnabled: Boolean,
    selectedDestination: MainDrawerDestination?,
    avatarUrl: String?,
    username: String?,
    isLoggedIn: Boolean,
    isLoading: Boolean,
    currentSite: String,
    onAvatarClick: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onDrawerItemSelected: (MainDrawerDestination) -> Boolean,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drawerFraction by animateFloatAsState(
        targetValue = if (drawerState.currentValue == DrawerValue.Open || drawerState.targetValue == DrawerValue.Open) 1f else 0f,
        label = "drawer_fraction",
    )

    ModalNavigationDrawer(
        gesturesEnabled = drawerEnabled,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                MainDrawerHeader(
                    avatarUrl = avatarUrl,
                    username = username,
                    isLoggedIn = isLoggedIn,
                    isLoading = isLoading,
                    currentSite = currentSite,
                    onAvatarClick = onAvatarClick,
                    onSwitchSiteClick = onSwitchSiteClick,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    MainDrawerPrimaryItems(selectedDestination, onDrawerItemSelected)
                    MainDrawerSection(
                        titleRes = R.string.my_list,
                        items = listOf(
                            MainDrawerDestination.WatchLater,
                            MainDrawerDestination.FavVideo,
                            MainDrawerDestination.Playlist,
                            MainDrawerDestination.Subscription,
                        ),
                        selectedDestination = selectedDestination,
                        onItemClick = { destination ->
                            if (onDrawerItemSelected(destination)) {
                                scope.launch { drawerState.close() }
                            }
                        },
                    )
                    MainDrawerSection(
                        titleRes = R.string.video,
                        items = listOf(
                            MainDrawerDestination.WatchHistory,
                            MainDrawerDestination.Download,
                        ),
                        selectedDestination = selectedDestination,
                        onItemClick = { destination ->
                            if (onDrawerItemSelected(destination)) {
                                scope.launch { drawerState.close() }
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 1f - (0.03f * drawerFraction)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (0.08f * drawerFraction)
                    },
            ) {
                content()
                if (drawerFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.14f * drawerFraction)),
                    )
                }
            }
        }

        BackHandler(
            enabled = drawerState.currentValue == DrawerValue.Open ||
                drawerState.targetValue == DrawerValue.Open,
        ) {
            scope.launch { drawerState.close() }
        }
    }
}

@Composable
private fun MainDrawerPrimaryItems(
    selectedDestination: MainDrawerDestination?,
    onDrawerItemSelected: (MainDrawerDestination) -> Boolean,
) {
    val primaryItems = listOf(
        MainDrawerDestination.Home,
        MainDrawerDestination.Settings,
        MainDrawerDestination.DailyCheckIn,
    )
    Column {
        primaryItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(stringResource(item.titleRes)) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = stringResource(item.titleRes),
                    )
                },
                selected = selectedDestination == item,
                onClick = {
                    onDrawerItemSelected(item)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}

@Composable
private fun MainDrawerSection(
    titleRes: Int,
    items: List<MainDrawerDestination>,
    selectedDestination: MainDrawerDestination?,
    onItemClick: (MainDrawerDestination) -> Unit,
) {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp),
    )
    Column {
        items.forEach { item ->
            NavigationDrawerItem(
                label = { Text(stringResource(item.titleRes)) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = stringResource(item.titleRes),
                    )
                },
                selected = selectedDestination == item,
                onClick = { onItemClick(item) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 600)
@Composable
private fun MainActivityScaffoldPreview() {
    ComponentPreview {
        MainActivityScaffold(
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
            drawerEnabled = true,
            selectedDestination = MainDrawerDestination.Home,
            avatarUrl = null,
            username = "Han1meViewer",
            isLoggedIn = true,
            isLoading = false,
            currentSite = "https://hanime1.me/",
            onAvatarClick = {},
            onSwitchSiteClick = {},
            onDrawerItemSelected = { true },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        }
    }
}
