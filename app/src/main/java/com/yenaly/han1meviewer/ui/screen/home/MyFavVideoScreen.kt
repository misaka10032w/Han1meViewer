package com.yenaly.han1meviewer.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.ComponentPreview
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.EmptyContent
import com.yenaly.han1meviewer.ui.component.ErrorContent
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun MyFavVideoScreen(
    favVideoFlow: Flow<List<HanimeInfo>>,
    favVideoStateFlow: Flow<PageLoadingState<*>>,
    deleteStateFlow: Flow<WebsiteState<Boolean>>,
    loadedPageCountFlow: Flow<Int>,
    isLoadingMoreFlow: Flow<Boolean>,
    onBack: () -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteFavorite: (HanimeInfo) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val items by favVideoFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val state by favVideoStateFlow.collectAsStateWithLifecycle(initialValue = PageLoadingState.Loading)
    val loadedPageCount by loadedPageCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val isLoadingMore by isLoadingMoreFlow.collectAsStateWithLifecycle(initialValue = false)
    MyFavVideoScreen(
        items = items,
        state = state,
        deleteStateFlow = deleteStateFlow,
        loadedPageCount = loadedPageCount,
        isLoadingMore = isLoadingMore,
        onBack = onBack,
        onOpenVideo = onOpenVideo,
        onDeleteFavorite = onDeleteFavorite,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyFavVideoScreen(
    items: List<HanimeInfo>,
    state: PageLoadingState<*>,
    deleteStateFlow: Flow<WebsiteState<Boolean>>,
    loadedPageCount: Int,
    isLoadingMore: Boolean,
    onBack: () -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteFavorite: (HanimeInfo) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<HanimeInfo?>(null) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    var pendingRefresh by rememberSaveable { mutableStateOf(false) }
    val deleteFailedText = stringResource(R.string.delete_failed)
    val deleteSuccessText = stringResource(R.string.delete_success)

    val refreshing = state is PageLoadingState.Loading && pendingRefresh
    val isError = state is PageLoadingState.Error && items.isEmpty()
    val isEmpty = state is PageLoadingState.NoMoreData && items.isEmpty()

    LaunchedEffect(Unit) {
        if (items.isEmpty()) {
            pendingRefresh = true
            onRefresh()
        }
    }

    LaunchedEffect(state) {
        if (state !is PageLoadingState.Loading) {
            pendingRefresh = false
        }
    }

    LaunchedEffect(gridState.canLoadMore(items, state), pendingRefresh, isLoadingMore) {
        if (gridState.canLoadMore(items, state) && !pendingRefresh && !isLoadingMore) {
            onLoadMore()
        }
    }

    LaunchedEffect(deleteStateFlow, deleteFailedText, deleteSuccessText) {
        deleteStateFlow.collect { deleteState ->
            when (deleteState) {
                is WebsiteState.Error -> {
                    snackbarHostState.showSnackbar(message = deleteFailedText)
                }

                is WebsiteState.Success -> {
                    snackbarHostState.showSnackbar(message = deleteSuccessText)
                }

                WebsiteState.Loading -> Unit
            }
        }
    }

    ConfirmDialog(
        visible = pendingDelete != null,
        title = stringResource(R.string.delete_fav),
        message = stringResource(R.string.sure_to_delete_s, pendingDelete?.title.orEmpty()),
        confirmText = stringResource(R.string.delete),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            pendingDelete?.let(onDeleteFavorite)
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null },
    )

    ConfirmDialog(
        visible = showHelpDialog,
        title = stringResource(R.string.help),
        message = stringResource(R.string.long_press_to_cancel_fav),
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.close),
        onConfirm = { showHelpDialog = false },
        onDismiss = { showHelpDialog = false },
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.fav_video))
                        Text(
                            text = stringResource(R.string.video_count, items.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    FilledIconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    FilledIconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_help_24),
                            contentDescription = stringResource(R.string.help),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                pendingRefresh = true
                onRefresh()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                isError -> ErrorContent(
                    title = stringResource(R.string.load_failed_retry),
                    onRetry = {
                        pendingRefresh = true
                        onRefresh()
                    },
                    modifier = Modifier.align(Alignment.Center),
                )

                isEmpty -> EmptyContent(
                    title = stringResource(R.string.empty_content),
                    description = stringResource(R.string.fav_video),
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> FavoriteVideoGrid(
                    items = items,
                    gridState = gridState,
                    loadedPageCount = loadedPageCount,
                    onOpenVideo = onOpenVideo,
                    onDeleteFavorite = { pendingDelete = it },
                    state = state,
                    loadingMore = isLoadingMore,
                )
            }
        }
    }
}

@Composable
private fun FavoriteVideoGrid(
    items: List<HanimeInfo>,
    gridState: LazyGridState,
    loadedPageCount: Int,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteFavorite: (HanimeInfo) -> Unit,
    state: PageLoadingState<*>,
    loadingMore: Boolean,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 172.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(items, key = { it.videoCode }) { item ->
            VideoCardItem(
                videoItem = item,
                isHorizontalCard = true,
                onClickVideosItem = { onOpenVideo(item) },
                onLongClickVideosItem = { _, _ -> onDeleteFavorite(item) },
            )
        }

        if (loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(strokeWidth = 2.5.dp)
                    Text(
                        text = stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (items.isNotEmpty() && state is PageLoadingState.NoMoreData) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.load_complete_with_pages, loadedPageCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun MyFavVideoScreenPreview() {
    ComponentPreview {
        MyFavVideoScreen(
            items = fakeHomePageVideos.take(6),
            state = PageLoadingState.Success(Unit),
            deleteStateFlow = flowOf(WebsiteState.Success(true)),
            loadedPageCount = 2,
            isLoadingMore = false,
            onBack = {},
            onOpenVideo = {},
            onDeleteFavorite = {},
            onRefresh = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyFavVideoEmptyPreview() {
    ComponentPreview {
        MyFavVideoScreen(
            items = emptyList(),
            state = PageLoadingState.NoMoreData,
            deleteStateFlow = flowOf(WebsiteState.Success(true)),
            loadedPageCount = 0,
            isLoadingMore = false,
            onBack = {},
            onOpenVideo = {},
            onDeleteFavorite = {},
            onRefresh = {},
            onLoadMore = {},
        )
    }
}
