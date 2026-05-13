package com.yenaly.han1meviewer.ui.screen.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.DrawerValue
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
import com.yenaly.han1meviewer.ui.component.ComponentPreview
import kotlinx.coroutines.launch

@Composable
fun MainActivityScaffold(
    drawerState: DrawerState,
    drawerEnabled: Boolean,
    selectedItemId: Int?,
    avatarUrl: String?,
    username: String?,
    isLoggedIn: Boolean,
    isLoading: Boolean,
    currentSite: String,
    onAvatarClick: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onDrawerItemSelected: (Int) -> Boolean,
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
                modifier = Modifier.navigationBarsPadding(),
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
                MainDrawerPrimaryItems(selectedItemId, onDrawerItemSelected)
                MainDrawerSection(
                    titleRes = R.string.my_list,
                    items = listOf(
                        MainDrawerItemSpec(R.id.nv_watch_later, R.drawable.ic_baseline_watch_later_24, R.string.watch_later),
                        MainDrawerItemSpec(R.id.nv_fav_video, R.drawable.ic_baseline_favorite_24, R.string.fav_video),
                        MainDrawerItemSpec(R.id.nv_playlist, R.drawable.ic_baseline_list_24, R.string.play_list),
                        MainDrawerItemSpec(R.id.nv_subscription, R.drawable.ic_subscribtion, R.string.my_subscribe),
                    ),
                    selectedItemId = selectedItemId,
                    onItemClick = { id ->
                        if (onDrawerItemSelected(id)) {
                            scope.launch { drawerState.close() }
                        }
                    },
                )
                MainDrawerSection(
                    titleRes = R.string.video,
                    items = listOf(
                        MainDrawerItemSpec(R.id.nv_watch_history, R.drawable.ic_baseline_history_24, R.string.watch_history),
                        MainDrawerItemSpec(R.id.nv_download, R.drawable.ic_baseline_download_24, R.string.download),
                    ),
                    selectedItemId = selectedItemId,
                    onItemClick = { id ->
                        if (onDrawerItemSelected(id)) {
                            scope.launch { drawerState.close() }
                        }
                    },
                )
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
    }
}

@Composable
private fun MainDrawerPrimaryItems(
    selectedItemId: Int?,
    onDrawerItemSelected: (Int) -> Boolean,
) {
    val primaryItems = listOf(
        MainDrawerItemSpec(R.id.nv_home_page, R.drawable.ic_baseline_home_24, R.string.home_page),
        MainDrawerItemSpec(R.id.nv_settings, R.drawable.ic_baseline_settings_24, R.string.settings),
        MainDrawerItemSpec(R.id.nv_daily_check_in, R.drawable.ic_baseline_thumb_up_alt_24, R.string.has_mastur),
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        primaryItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(stringResource(item.titleRes)) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = stringResource(item.titleRes),
                    )
                },
                selected = selectedItemId == item.id,
                onClick = {
                    onDrawerItemSelected(item.id)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}

@Composable
private fun MainDrawerSection(
    titleRes: Int,
    items: List<MainDrawerItemSpec>,
    selectedItemId: Int?,
    onItemClick: (Int) -> Unit,
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            NavigationDrawerItem(
                label = { Text(stringResource(item.titleRes)) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = stringResource(item.titleRes),
                    )
                },
                selected = selectedItemId == item.id,
                onClick = { onItemClick(item.id) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}

private data class MainDrawerItemSpec(
    val id: Int,
    val iconRes: Int,
    val titleRes: Int,
)

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun MainActivityScaffoldPreview() {
    ComponentPreview {
        MainActivityScaffold(
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
            drawerEnabled = true,
            selectedItemId = R.id.nv_home_page,
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
