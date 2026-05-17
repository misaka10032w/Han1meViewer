package com.yenaly.han1meviewer.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.MySubscriptions
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.ArtistItem
import com.yenaly.han1meviewer.ui.component.LoadMoreFooter
import com.yenaly.han1meviewer.ui.component.PullRefreshOverlay
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.preview.fakeArtists
import com.yenaly.han1meviewer.ui.preview.fakeVideos
import com.yenaly.han1meviewer.ui.viewmodel.MySubscriptionsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionApp(
    navigateBack: () -> Unit,
    viewModel: MySubscriptionsViewModel,
    onClickArtist: (String) -> Unit,
    onLongClickArtist: (String) -> Unit,
    onClickVideosItem: (String) -> Unit,
    onLongClickVideosItem: (String, String) -> Unit,
) {
    val state by viewModel.subscriptionsState.collectAsStateWithLifecycle()
    val cachedArtists = rememberSaveable { mutableStateOf<List<SubscriptionItem>>(emptyList()) }
    val cachedVideos =
        rememberSaveable { mutableStateOf<List<SubscriptionVideosItem>>(emptyList()) }
    val scrollBehavior = pinnedScrollBehavior(rememberTopAppBarState())
    val gridState = rememberLazyGridState()

    val refreshState = rememberPullToRefreshState()
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    var expandedItem by rememberSaveable { mutableIntStateOf(-1) }

    if (expandedItem == -1 && cachedVideos.value.isNotEmpty()) {
        expandedItem = 0
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.loadMySubscriptions(forceReload = true)
    }

    LaunchedEffect(state) {
        if (state is WebsiteState.Success) {
            cachedArtists.value =
                (state as WebsiteState.Success<MySubscriptions>).info.subscriptions.toList()
            cachedVideos.value =
                (state as WebsiteState.Success<MySubscriptions>).info.subscriptionsVideos.toList()
        } else if (state is WebsiteState.Loading && cachedArtists.value.isEmpty()) {
            viewModel.loadMySubscriptions()
        }
        if (state !is WebsiteState.Loading) {
            isRefreshing = false
        }
    }

    HanimeScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        title = stringResource(R.string.my_subscribe),
        onBack = navigateBack,
        scrollBehavior = scrollBehavior,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pullToRefresh(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val result = state) {
                is WebsiteState.Loading -> {
                    if (cachedArtists.value.isEmpty() || cachedVideos.value.isEmpty()) {
                        LoadingIndicator(Modifier.align(Alignment.Center))
                    } else {
                        // 显示旧数据，刷新中不替换内容
                        AnimatedPageContent(
                            gridState = gridState,
                            artists = cachedArtists.value,
                            videos = cachedVideos.value,
                            onClickArtist = onClickArtist,
                            onLongClickArtist = onLongClickArtist,
                            onClickVideosItem = onClickVideosItem,
                            onLongClickVideosItem = onLongClickVideosItem,
                            onLoadMore = { viewModel.loadMySubscriptions() },
                            canLoadMore = viewModel.canLoadMore()
                        )
                    }
                }

                is WebsiteState.Error -> {
                    if (cachedArtists.value.isEmpty()) {
                        EmptyContent(
                            hint = stringResource(
                                R.string.load_failed_with_reason,
                                result.throwable.message.orEmpty()
                            ),
                            picRes = R.drawable.h_chan_sad
                        )
                    } else {
                        // 显示旧缓存内容（保持体验）
                        AnimatedPageContent(
                            gridState = gridState,
                            artists = cachedArtists.value,
                            videos = cachedVideos.value,
                            onClickArtist = onClickArtist,
                            onLongClickArtist = onLongClickArtist,
                            onClickVideosItem = onClickVideosItem,
                            onLongClickVideosItem = onLongClickVideosItem,
                            onLoadMore = { viewModel.loadMySubscriptions() },
                            canLoadMore = viewModel.canLoadMore()
                        )
                    }
                }

                else -> {
                    // 统一渲染缓存（成功后缓存已更新）
                    AnimatedPageContent(
                        gridState = gridState,
                        artists = cachedArtists.value,
                        videos = cachedVideos.value,
                        onClickArtist = onClickArtist,
                        onLongClickArtist = onLongClickArtist,
                        onClickVideosItem = onClickVideosItem,
                        onLongClickVideosItem = onLongClickVideosItem,
                        onLoadMore = { viewModel.loadMySubscriptions() },
                        canLoadMore = viewModel.canLoadMore()
                    )
                }
            }

            PullRefreshOverlay(
                state = refreshState,
                isRefreshing = isRefreshing,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionPageContent(
    gridState: LazyGridState,
    videos: List<SubscriptionVideosItem>,
    artists: List<SubscriptionItem>,
    onClickVideosItem: (String) -> Unit,
    onLongClickVideosItem: (String, String) -> Unit,
    onClickArtist: (String) -> Unit,
    onLongClickArtist: (String) -> Unit,
    onLoadMore: () -> Unit,
    canLoadMore: Boolean
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidthPx = windowInfo.containerSize.width
    val screenWidthDp = with(density) { screenWidthPx.toDp() }
    val videoColumns = maxOf(2, (screenWidthDp / 180.dp).toInt())
    val artistColumns = maxOf(4, (screenWidthDp / 72.dp).toInt())
    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 60

    LaunchedEffect(gridState, videos.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
            .map { it.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= videos.size - 4 &&
                    videos.size >= currentPage * pageSize &&
                    canLoadMore
                ) {
                    currentPage += 1
                    onLoadMore()
                }
            }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(videoColumns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 艺术家
        items(artists.chunked(artistColumns), span = { GridItemSpan(videoColumns) }) { artistRow ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                artistRow.forEach { artist ->
                    ArtistItem(
                        artist = artist,
                        modifier = Modifier.weight(1f),
                        onClickArtist = onClickArtist,
                        onLongClickArtist = onLongClickArtist
                    )
                }
                // 添加空白占位防止最后一列被压扁
                val emptySlots = artistColumns - artistRow.size
                repeat(emptySlots) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        //分割线
        item(span = { GridItemSpan(videoColumns) }) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        // 视频列
        items(videos) { video ->
            VideoCardItem(
                videoItem = video,
                onClickVideosItem = onClickVideosItem,
                onLongClickVideosItem = onLongClickVideosItem
            )
        }
        if (videos.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                var loadState by remember {
                    mutableStateOf<PageLoadingState<*>>(
                        PageLoadingState.Success(
                            emptyList<String>()
                        )
                    )
                }
                LoadMoreFooter(
                    state = loadState,
                    isLoadingMore = canLoadMore,
                    loadedPage = currentPage
                )
            }
        }
    }
}

@Composable
fun AnimatedPageContent(
    gridState: LazyGridState,
    artists: List<SubscriptionItem>,
    videos: List<SubscriptionVideosItem>,
    onClickArtist: (String) -> Unit,
    onLongClickArtist: (String) -> Unit,
    onClickVideosItem: (String) -> Unit,
    onLongClickVideosItem: (String, String) -> Unit,
    onLoadMore: () -> Unit,
    canLoadMore: Boolean
) {
    AnimatedContent(
        targetState = Pair(artists, videos),
        label = "video-content-animation",
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(200))
        }
    ) { (a, v) ->
        SubscriptionPageContent(
            gridState = gridState,
            artists = a,
            videos = v,
            onClickArtist = onClickArtist,
            onLongClickArtist = onLongClickArtist,
            onClickVideosItem = onClickVideosItem,
            onLongClickVideosItem = onLongClickVideosItem,
            onLoadMore = onLoadMore,
            canLoadMore = canLoadMore
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun SubscriptionAppPreview() {
    MaterialTheme { SubscriptionAppPreviewBody() }
}

@Composable
fun SubscriptionAppPreviewBody() {
    val scrollBehavior = pinnedScrollBehavior(rememberTopAppBarState())
    val isRefreshing = false
    val refreshState = rememberPullToRefreshState()

    HanimeScaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .pullToRefresh(
                state = refreshState,
                isRefreshing = isRefreshing,
                onRefresh = {}
            ),
        title = stringResource(R.string.my_subscribe),
        onBack = {},
        scrollBehavior = scrollBehavior,
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            SubscriptionPageContent(
                gridState = LazyGridState(),
                videos = fakeVideos,
                onClickVideosItem = {},
                onLoadMore = { },
                canLoadMore = false,
                artists = fakeArtists,
                onClickArtist = {},
                onLongClickVideosItem = { _, _ -> },
                onLongClickArtist = { _ -> }
            )
        }
    }
}