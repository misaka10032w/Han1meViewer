package com.yenaly.han1meviewer.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.OnlineWatchHistorySort
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.LoadMoreFooter
import com.yenaly.han1meviewer.ui.component.PageContent
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WatchHistoryTabScreen(
    localHistoriesFlow: Flow<List<WatchHistoryEntity>>,
    onlineItems: StateFlow<List<HanimeInfo>>,
    onlineState: StateFlow<PageLoadingState<*>>,
    onlineSort: StateFlow<OnlineWatchHistorySort>,
    onlineLoadedPageCount: StateFlow<Int>,
    onlineIsLoadingMore: StateFlow<Boolean>,
    onlineRefreshing: () -> Boolean,
    onlineDeleteStateFlow: SharedFlow<WebsiteState<Boolean>>,
    onBack: () -> Unit,
    onOpenLocalVideo: (WatchHistoryEntity) -> Unit,
    onDeleteLocalHistory: (WatchHistoryEntity) -> Unit,
    onDeleteAllLocalHistories: () -> Unit,
    onOpenOnlineVideo: (HanimeInfo) -> Unit,
    onDeleteOnlineVideo: (HanimeInfo) -> Unit,
    onRefreshOnline: (OnlineWatchHistorySort) -> Unit,
    onLoadMoreOnline: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val localHistories by localHistoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentOnlineItems by onlineItems.collectAsState()
    val currentOnlineState by onlineState.collectAsState()
    val currentOnlineSort by onlineSort.collectAsState()
    val currentOnlineLoadedPageCount by onlineLoadedPageCount.collectAsState()
    val currentOnlineIsLoadingMore by onlineIsLoadingMore.collectAsState()
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllLocalDialog by rememberSaveable { mutableStateOf(false) }

    val helpMessage = if (pagerState.currentPage == 1) {
        stringResource(R.string.watch_history_online_help)
    } else {
        stringResource(R.string.long_press_to_delete_all_histories)
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1 && currentOnlineItems.isEmpty() && currentOnlineLoadedPageCount == 0 && currentOnlineState is PageLoadingState.Loading) {
            onRefreshOnline(currentOnlineSort)
        }
    }

    ConfirmDialog(
        visible = showHelpDialog,
        title = stringResource(R.string.help),
        message = helpMessage,
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.close),
        onConfirm = { showHelpDialog = false },
        onDismiss = { showHelpDialog = false },
    )

    ConfirmDialog(
        visible = showDeleteAllLocalDialog,
        title = stringResource(R.string.watch_history_delete_all_title),
        message = stringResource(R.string.sure_to_delete_all_histories),
        confirmText = stringResource(R.string.watch_history_clear_all),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            onDeleteAllLocalHistories()
            showDeleteAllLocalDialog = false
        },
        onDismiss = { showDeleteAllLocalDialog = false },
    )

    HanimeScaffold(
        title = stringResource(R.string.watch_history),
        onBack = onBack,
        actions = {
            FilledIconButton(onClick = { showHelpDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_help_24),
                    contentDescription = stringResource(R.string.help),
                )
            }
            if (pagerState.currentPage == 0) {
                FilledIconButton(
                    onClick = { showDeleteAllLocalDialog = true },
                    enabled = localHistories.isNotEmpty(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_delete_24),
                        contentDescription = stringResource(R.string.watch_history_clear_all),
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(R.string.local)) },
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(R.string.online)) },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> WatchHistoryScreen(
                        histories = localHistories,
                        onBack = onBack,
                        onOpenVideo = onOpenLocalVideo,
                        onDeleteHistory = onDeleteLocalHistory,
                        onDeleteAllHistories = onDeleteAllLocalHistories,
                        useScaffold = false,
                        showHelpAction = false,
                        showDeleteAllAction = false,
                    )

                    else -> OnlineWatchHistoryScreen(
                        items = currentOnlineItems,
                        state = currentOnlineState,
                        sort = currentOnlineSort,
                        loadedPageCount = currentOnlineLoadedPageCount,
                        isLoadingMore = currentOnlineIsLoadingMore,
                        refreshing = onlineRefreshing(),
                        deleteStateFlow = onlineDeleteStateFlow,
                        onOpenVideo = onOpenOnlineVideo,
                        onDeleteVideo = onDeleteOnlineVideo,
                        onRefresh = onRefreshOnline,
                        onLoadMore = onLoadMoreOnline,
                    )
                }
            }
        }
    }
}

@Composable
fun WatchHistoryScreen(
    historiesFlow: Flow<List<WatchHistoryEntity>>,
    onBack: () -> Unit,
    onOpenVideo: (WatchHistoryEntity) -> Unit,
    onDeleteHistory: (WatchHistoryEntity) -> Unit,
    onDeleteAllHistories: () -> Unit,
    useScaffold: Boolean = true,
    showHelpAction: Boolean = true,
    showDeleteAllAction: Boolean = true,
) {
    val histories by historiesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    WatchHistoryScreen(
        histories = histories,
        onBack = onBack,
        onOpenVideo = onOpenVideo,
        onDeleteHistory = onDeleteHistory,
        onDeleteAllHistories = onDeleteAllHistories,
        useScaffold = useScaffold,
        showHelpAction = showHelpAction,
        showDeleteAllAction = showDeleteAllAction,
    )
}

@Composable
private fun WatchHistoryScreen(
    histories: List<WatchHistoryEntity>,
    onBack: () -> Unit,
    onOpenVideo: (WatchHistoryEntity) -> Unit,
    onDeleteHistory: (WatchHistoryEntity) -> Unit,
    onDeleteAllHistories: () -> Unit,
    useScaffold: Boolean,
    showHelpAction: Boolean,
    showDeleteAllAction: Boolean,
) {
    var pendingDelete by remember { mutableStateOf<WatchHistoryEntity?>(null) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }

    ConfirmDialog(
        visible = pendingDelete != null,
        title = stringResource(R.string.delete_history),
        message = stringResource(R.string.sure_to_delete_s, pendingDelete?.title.orEmpty()),
        confirmText = stringResource(R.string.delete),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            pendingDelete?.let(onDeleteHistory)
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null },
    )

    ConfirmDialog(
        visible = showDeleteAllDialog,
        title = stringResource(R.string.watch_history_delete_all_title),
        message = stringResource(R.string.sure_to_delete_all_histories),
        confirmText = stringResource(R.string.watch_history_clear_all),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            onDeleteAllHistories()
            showDeleteAllDialog = false
        },
        onDismiss = { showDeleteAllDialog = false },
    )

    ConfirmDialog(
        visible = showHelpDialog,
        title = stringResource(R.string.help),
        message = stringResource(R.string.long_press_to_delete_all_histories),
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.close),
        onConfirm = { showHelpDialog = false },
        onDismiss = { showHelpDialog = false },
    )

    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        if (histories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                EmptyContent(
                    hint = stringResource(R.string.watch_history_empty_title),
                    subHint = stringResource(R.string.watch_history_empty_description),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(histories, key = { it.id }) { history ->
                    WatchHistoryCard(
                        history = history,
                        onClick = { onOpenVideo(history) },
                        onLongClick = { pendingDelete = history },
                    )
                }
            }
        }
    }

    if (useScaffold) {
        HanimeScaffold(
            title = stringResource(R.string.watch_history),
            subtitle = {
                Text(
                    text = stringResource(R.string.watch_history_total_count, histories.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onBack = onBack,
            actions = {
                if (showHelpAction) {
                    FilledIconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_help_24),
                            contentDescription = stringResource(R.string.help),
                        )
                    }
                }
                if (showDeleteAllAction) {
                    FilledIconButton(
                        onClick = { showDeleteAllDialog = true },
                        enabled = histories.isNotEmpty()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_delete_24),
                            contentDescription = stringResource(R.string.watch_history_clear_all),
                        )
                    }
                }
            },
        ) { paddingValues ->
            content(paddingValues)
        }
    } else {
        content(PaddingValues())
    }
}

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun OnlineWatchHistoryScreen(
    items: List<HanimeInfo>,
    state: PageLoadingState<*>,
    sort: OnlineWatchHistorySort,
    loadedPageCount: Int,
    isLoadingMore: Boolean,
    refreshing: Boolean,
    deleteStateFlow: SharedFlow<WebsiteState<Boolean>>,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteVideo: (HanimeInfo) -> Unit,
    onRefresh: (OnlineWatchHistorySort) -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val refreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<HanimeInfo?>(null) }
    var sortBarVisible by rememberSaveable { mutableStateOf(true) }
    val deleteFailedText = stringResource(R.string.delete_failed)
    val deleteSuccessText = stringResource(R.string.delete_success)

    LaunchedEffect(deleteStateFlow, deleteFailedText, deleteSuccessText) {
        deleteStateFlow.collect { deleteState ->
            when (deleteState) {
                is WebsiteState.Error -> snackbarHostState.showSnackbar(message = deleteFailedText)
                is WebsiteState.Success -> snackbarHostState.showSnackbar(message = deleteSuccessText)
                WebsiteState.Loading -> Unit
            }
        }
    }

    LaunchedEffect(gridState.canLoadMore(items, state), isLoadingMore) {
        if (gridState.canLoadMore(items, state) && !isLoadingMore) {
            onLoadMore()
        }
    }

    LaunchedEffect(gridState) {
        var previousIndex = 0
        var previousOffset = 0
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (currentIndex, currentOffset) ->
                sortBarVisible = when {
                    !gridState.canScrollBackward -> true
                    currentIndex < previousIndex -> true
                    currentIndex > previousIndex -> false
                    currentOffset < previousOffset -> true
                    currentOffset > previousOffset -> false
                    else -> sortBarVisible
                }
                previousIndex = currentIndex
                previousOffset = currentOffset
            }
    }

    ConfirmDialog(
        visible = pendingDelete != null,
        title = stringResource(R.string.delete_history),
        message = stringResource(R.string.sure_to_delete_s, pendingDelete?.title.orEmpty()),
        confirmText = stringResource(R.string.delete),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            pendingDelete?.let(onDeleteVideo)
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null },
    )

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = sortBarVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OnlineHistorySortChip(
                    text = stringResource(R.string.sort_by_newest),
                    selected = sort == OnlineWatchHistorySort.Latest,
                    onClick = { onRefresh(OnlineWatchHistorySort.Latest) },
                )
                OnlineHistorySortChip(
                    text = stringResource(R.string.popular),
                    selected = sort == OnlineWatchHistorySort.Popular,
                    onClick = { onRefresh(OnlineWatchHistorySort.Popular) },
                )
                OnlineHistorySortChip(
                    text = stringResource(R.string.sort_by_oldest),
                    selected = sort == OnlineWatchHistorySort.Oldest,
                    onClick = { onRefresh(OnlineWatchHistorySort.Oldest) },
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            state = refreshState,
            onRefresh = { onRefresh(sort) },
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = refreshState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            PageContent(
                isLoading = state is PageLoadingState.Loading && items.isEmpty(),
                isError = state is PageLoadingState.Error,
                isEmpty = state is PageLoadingState.NoMoreData && items.isEmpty(),
                onRetry = { onRefresh(sort) },
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorContent(
                            title = stringResource(R.string.load_failed_retry),
                            onRetry = { onRefresh(sort) },
                        )
                    }
                },
                empty = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyContent(
                            hint = stringResource(R.string.watch_history_empty_title),
                            subHint = stringResource(R.string.watch_history_empty_description),
                        )
                    }
                },
            ) {
                OnlineWatchHistoryGrid(
                    items = items,
                    gridState = gridState,
                    loadedPageCount = loadedPageCount,
                    state = state,
                    isLoadingMore = isLoadingMore,
                    snackbarHostState = snackbarHostState,
                    onOpenVideo = onOpenVideo,
                    onDeleteVideo = { pendingDelete = it },
                )
            }
        }
    }
}

@Composable
private fun OnlineWatchHistoryGrid(
    items: List<HanimeInfo>,
    gridState: LazyGridState,
    loadedPageCount: Int,
    state: PageLoadingState<*>,
    isLoadingMore: Boolean,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteVideo: (HanimeInfo) -> Unit,
) {
    val videoColumns = rememberVideoGridColumns()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(videoColumns),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SpacingNormal),
            horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.watch_history_total_count, items.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            items(items, key = { it.videoCode }) { item ->
                VideoCardItem(
                    videoItem = item,
                    onClickVideosItem = { onOpenVideo(item) },
                    onLongClickVideosItem = { _, _ -> onDeleteVideo(item) },
                )
            }
            if (items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadMoreFooter(
                        state = state,
                        loadedPage = loadedPageCount,
                        isLoadingMore = isLoadingMore,
                    )
                }
            }
        }
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun LazyGridState.canLoadMore(
    items: List<HanimeInfo>,
    state: PageLoadingState<*>,
): Boolean {
    if (items.isEmpty()) return false
    if (state is PageLoadingState.Loading || state is PageLoadingState.NoMoreData || state is PageLoadingState.Error) {
        return false
    }
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    return lastVisible >= layoutInfo.totalItemsCount - 4
}

@Composable
private fun OnlineHistorySortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            labelColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WatchHistoryCard(
    history: WatchHistoryEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val fixTimestamp = { ts: Long -> if (ts < 9999999999L) ts * 1000 else ts }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val watchDate =
        remember(history.watchDate) { dateFormatter.format(Date(fixTimestamp(history.watchDate))) }
    val releaseDate =
        remember(history.releaseDate) { dateFormatter.format(Date(fixTimestamp(history.releaseDate))) }
    val progressMinutes = remember(history.progress) { history.progress / 60_000 }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = history.coverUrl,
                    contentDescription = history.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (progressMinutes > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(topEnd = 4.dp),
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.watch_history_minutes_short,
                                progressMinutes
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = history.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    FilledIconButton(
                        onClick = onLongClick,
                        modifier = Modifier.size(25.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete_history),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                WatchHistoryMeta(
                    iconRes = R.drawable.ic_baseline_access_time_24,
                    label = stringResource(R.string.watch_history_watched_at, watchDate),
                )
                WatchHistoryMeta(
                    iconRes = R.drawable.ic_baseline_play_circle_outline_24,
                    label = stringResource(R.string.watch_history_released_at, releaseDate),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    AssistChip(
                        onClick = onClick,
                        label = {
                            Text(
                                stringResource(R.string.watch_history_resume_watch),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_history_24),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer, // 改用 primary 强化引导
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchHistoryMeta(
    iconRes: Int,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun WatchHistoryScreenPreview() {
    val previews = fakeHomePageVideos.take(3).mapIndexed { index, item ->
        WatchHistoryEntity(
            id = index + 1,
            title = item.title,
            coverUrl = item.coverUrl,
            videoCode = item.videoCode,
            releaseDate = System.currentTimeMillis() - (index + 10) * 86_400_000L,
            watchDate = System.currentTimeMillis() - index * 3_600_000L,
            progress = (index + 1) * 12L * 60_000L,
        )
    }
    ComponentPreview {
        WatchHistoryScreen(
            histories = previews,
            onBack = {},
            onOpenVideo = {},
            onDeleteHistory = {},
            onDeleteAllHistories = {},
            useScaffold = true,
            showHelpAction = true,
            showDeleteAllAction = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchHistoryEmptyPreview() {
    ComponentPreview {
        WatchHistoryScreen(
            histories = emptyList<WatchHistoryEntity>(),
            onBack = {},
            onOpenVideo = {},
            onDeleteHistory = {},
            onDeleteAllHistories = {},
            useScaffold = true,
            showHelpAction = true,
            showDeleteAllAction = true,
        )
    }
}
