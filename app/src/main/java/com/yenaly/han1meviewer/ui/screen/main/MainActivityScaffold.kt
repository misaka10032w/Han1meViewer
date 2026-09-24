package com.yenaly.han1meviewer.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.adaptive.LocalContentWidthDp
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
    onAvatarLongClick: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onDrawerItemSelected: (MainDrawerDestination) -> Boolean,
    useRail: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (useRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            MainNavigationRail(
                selectedDestination = selectedDestination,
                avatarUrl = avatarUrl,
                onAvatarClick = onAvatarClick,
                onAvatarLongClick = onAvatarLongClick,
                onSwitchSiteClick = onSwitchSiteClick,
                onDrawerItemSelected = onDrawerItemSelected,
            )
            VerticalDivider()
            ScaffoldContentPane(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                content = content,
            )
        }
        return
    }

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
                    onAvatarLongClick = onAvatarLongClick,
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
                            MainDrawerDestination.CreatorCenter,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
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
                ScaffoldContentPane(content = content)
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
private fun ScaffoldContentPane(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalContentWidthDp provides maxWidth) {
            content()
        }
    }
}

@Composable
private fun MainNavigationRail(
    selectedDestination: MainDrawerDestination?,
    avatarUrl: String?,
    onAvatarClick: () -> Unit,
    onAvatarLongClick: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onDrawerItemSelected: (MainDrawerDestination) -> Boolean,
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onAvatarClick,
                    onLongClick = onAvatarLongClick,
                ),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.bg_default_header),
            fallback = painterResource(id = R.drawable.bg_default_header),
            error = painterResource(id = R.drawable.bg_default_header),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            RailDestinationGroup(
                items = listOf(
                    MainDrawerDestination.Home,
                    MainDrawerDestination.Settings,
                    MainDrawerDestination.DailyCheckIn,
                ),
                selectedDestination = selectedDestination,
                onDrawerItemSelected = onDrawerItemSelected,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
            RailDestinationGroup(
                items = listOf(
                    MainDrawerDestination.WatchLater,
                    MainDrawerDestination.FavVideo,
                    MainDrawerDestination.Playlist,
                    MainDrawerDestination.Subscription,
                    MainDrawerDestination.CreatorCenter,
                ),
                selectedDestination = selectedDestination,
                onDrawerItemSelected = onDrawerItemSelected,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
            RailDestinationGroup(
                items = listOf(
                    MainDrawerDestination.WatchHistory,
                    MainDrawerDestination.Download,
                ),
                selectedDestination = selectedDestination,
                onDrawerItemSelected = onDrawerItemSelected,
            )
        }
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onSwitchSiteClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_switch_24),
                contentDescription = stringResource(R.string.switch_site),
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.switch_site),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun RailDestinationGroup(
    items: List<MainDrawerDestination>,
    selectedDestination: MainDrawerDestination?,
    onDrawerItemSelected: (MainDrawerDestination) -> Boolean,
) {
    items.forEach { item ->
        val selected = selectedDestination == item
        val label = stringResource(item.titleRes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent,
                )
                .clickable { onDrawerItemSelected(item) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp),
            )
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
            onAvatarLongClick = {},
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
