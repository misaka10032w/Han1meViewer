package com.yenaly.han1meviewer.ui.fragment.home.subscription

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.MySubscriptions
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.fragment.ArtistItem
import com.yenaly.han1meviewer.ui.fragment.EmptyView
import com.yenaly.han1meviewer.ui.fragment.VideoCardItem
import com.yenaly.han1meviewer.ui.fragment.fakeArtists
import com.yenaly.han1meviewer.ui.fragment.fakeVideos
import com.yenaly.han1meviewer.ui.viewmodel.MySubscriptionsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        viewModel.refreshCompleted.collect {
            isRefreshing = false
        }
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.loadMySubscriptions(forceReload = true)
    }

    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(refreshState.distanceFraction).coerceIn(0f, 1f)
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
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text(
                        stringResource(R.string.my_subscribe),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back button",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
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
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
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
                        EmptyView(
                            "${stringResource(R.string.load_failed_retry)}: ${result.throwable.message}",
                            R.drawable.h_chan_sad
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

            if (isRefreshing || scaleFraction() > 0f) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            scaleX = scaleFraction()
                            scaleY = scaleFraction()
                        }
                        .zIndex(1f)
                ) {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = refreshState,
                        isRefreshing = isRefreshing
                    )
                }
            }
        }
    }
}
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

        if (canLoadMore) {
            item(span = { GridItemSpan(videoColumns) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionAppPreviewBody() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val isRefreshing = false
    val refreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .pullToRefresh(
                state = refreshState,
                isRefreshing = isRefreshing,
                onRefresh = {}
            ),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.my_subscribe)) },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
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
                onLongClickVideosItem = {_,_->},
                onLongClickArtist = {_->}
            )
        }
    }
}