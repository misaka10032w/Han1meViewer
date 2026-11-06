package com.yenaly.han1meviewer.ui.screen.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.ExpressiveEntryCard
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.screen.LocalVideoGridAvailableWidth
import com.yenaly.han1meviewer.ui.navigation.main.DownloadRouteScreen
import com.yenaly.han1meviewer.ui.navigation.main.FavVideoRouteScreen
import com.yenaly.han1meviewer.ui.navigation.main.MyPlaylistRouteScreen
import com.yenaly.han1meviewer.ui.navigation.main.WatchHistoryRouteScreen
import com.yenaly.han1meviewer.ui.navigation.main.WatchLaterRouteScreen
import kotlinx.coroutines.launch

/**
 * 内容库一级页面。
 *
 * 手机使用单栏，平板/折叠屏展开宽度使用列表-详情双栏。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ContentLibraryScreen(
    isLoggedIn: Boolean,
    onRequireLogin: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToLocalVideo: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<ContentLibraryEntry>()
    val scope = rememberCoroutineScope()
    val defaultEntry = remember(isLoggedIn) {
        ContentLibraryEntry.entries.firstOrNull { !it.requiresLogin || isLoggedIn }
            ?: ContentLibraryEntry.entries.first()
    }
    // 双栏模式下切换分类只替换详情内容，不再向 navigator 追加新的 Detail 页面。
    var selectedEntryName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedEntry = selectedEntryName?.let { name ->
        ContentLibraryEntry.entries.firstOrNull { it.name == name }
    }
    val detailEntry = selectedEntry
        ?: navigator.currentDestination?.contentKey
        ?: defaultEntry
    val useTwoPane = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    val closeDetail: () -> Unit = {
        selectedEntryName = null
        scope.launch {
            if (!navigator.navigateBack(BackNavigationBehavior.PopUntilContentChange)) {
                navigator.navigateTo(
                    pane = ListDetailPaneScaffoldRole.List,
                    contentKey = null,
                )
            }
        }
    }

    BackHandler(
        enabled = !useTwoPane && navigator.currentDestination?.contentKey != null,
    ) {
        closeDetail()
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                ContentLibraryListPane(
                    isLoggedIn = isLoggedIn,
                    useTwoPane = useTwoPane,
                    onEntryClick = { entry ->
                        if (entry.requiresLogin && !isLoggedIn) {
                            onRequireLogin()
                        } else {
                            selectedEntryName = entry.name
                            if (navigator.currentDestination?.pane !=
                                ListDetailPaneScaffoldRole.Detail
                            ) {
                                scope.launch {
                                    navigator.navigateTo(
                                        pane = ListDetailPaneScaffoldRole.Detail,
                                        contentKey = entry,
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // 双栏时视频网格使用详情面板的实际宽度，而不是整个窗口宽度。
                    CompositionLocalProvider(
                        LocalVideoGridAvailableWidth provides maxWidth,
                    ) {
                        ContentLibraryDetailPane(
                            entry = detailEntry,
                            onBack = closeDetail,
                            onNavigateToVideo = onNavigateToVideo,
                            onNavigateToLocalVideo = onNavigateToLocalVideo,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ContentLibraryListPane(
    isLoggedIn: Boolean,
    useTwoPane: Boolean,
    onEntryClick: (ContentLibraryEntry) -> Unit,
) {
    if (useTwoPane) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 20.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.content_library),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.content_library_pane_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ContentLibraryEntryList(
                isLoggedIn = isLoggedIn,
                onEntryClick = onEntryClick,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
            )
        }
    } else {
        HanimeScaffold(
            title = stringResource(R.string.content_library),
            onBack = null,
        ) { paddingValues ->
            ContentLibraryEntryList(
                isLoggedIn = isLoggedIn,
                onEntryClick = onEntryClick,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 24.dp,
                ),
            )
        }
    }
}

@Composable
private fun ContentLibraryEntryList(
    isLoggedIn: Boolean,
    onEntryClick: (ContentLibraryEntry) -> Unit,
    contentPadding: PaddingValues,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layoutDirection = LocalLayoutDirection.current
        val horizontalPadding =
            contentPadding.calculateStartPadding(layoutDirection) +
                contentPadding.calculateEndPadding(layoutDirection)
        val itemSpacing = 12.dp
        val minCardWidth = 150.dp
        val columns = maxOf(
            2,
            ((maxWidth - horizontalPadding + itemSpacing) /
                (minCardWidth + itemSpacing)).toInt(),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            items(
                items = ContentLibraryEntry.entries,
                key = { it.name },
            ) { entry ->
                ExpressiveEntryCard(
                    icon = entry.icon,
                    title = stringResource(entry.titleRes),
                    subtitle = stringResource(entry.descriptionRes),
                    subtitleMinLines = 2,
                    locked = entry.requiresLogin && !isLoggedIn,
                    onClick = { onEntryClick(entry) },
                )
            }
        }
    }
}

@Composable
private fun ContentLibraryDetailPane(
    entry: ContentLibraryEntry,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToLocalVideo: (String, String?) -> Unit,
) {
    when (entry) {
        ContentLibraryEntry.Favorite -> FavVideoRouteScreen(
            onBack = onBack,
            onNavigateToVideo = onNavigateToVideo,
        )

        ContentLibraryEntry.WatchLater -> WatchLaterRouteScreen(
            onBack = onBack,
            onNavigateToVideo = onNavigateToVideo,
        )

        ContentLibraryEntry.Playlist -> MyPlaylistRouteScreen(
            onBack = onBack,
            onNavigateToVideo = onNavigateToVideo,
        )

        ContentLibraryEntry.History -> WatchHistoryRouteScreen(
            onBack = onBack,
            onNavigateToVideo = onNavigateToVideo,
        )

        ContentLibraryEntry.Download -> DownloadRouteScreen(
            onBack = onBack,
            onNavigateToVideo = onNavigateToVideo,
            onNavigateToLocalVideo = onNavigateToLocalVideo,
        )
    }
}
