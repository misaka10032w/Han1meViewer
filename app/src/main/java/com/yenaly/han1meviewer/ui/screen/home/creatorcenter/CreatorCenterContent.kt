package com.yenaly.han1meviewer.ui.screen.home.creatorcenter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.CreatorTab
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.ui.component.LoadMoreFooter
import com.yenaly.han1meviewer.ui.component.PageContent
import com.yenaly.han1meviewer.ui.component.PaginationPager
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 已上传视频 Tab 页面。
 *
 * @param uiState 页面 UI 状态
 * @param onEvent 用户事件回调
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreatorUploadedPage(
    uiState: CreatorCenterUiState,
    onEvent: (CreatorCenterEvent) -> Unit,
) {
    val items = uiState.uploadedItems
    val state = uiState.uploadedState
    val sort = uiState.uploadedSort
    val loadedPageCount = uiState.uploadedPage
    val totalPages = uiState.uploadedTotalPages
    val isLoadingMore = uiState.uploadedLoadingMore
    val gridState = rememberLazyGridState()
    val refreshState = rememberPullToRefreshState()
    val refreshing = state is PageLoadingState.Loading && items.isEmpty()
    var sortBarVisible by rememberSaveable { mutableStateOf(true) }
    val searchPagination = Preferences.searchPagination
    val scope = rememberCoroutineScope()

    LaunchedEffect(gridState.canLoadMore(state), isLoadingMore, searchPagination) {
        if (!searchPagination && gridState.canLoadMore(state) && !isLoadingMore) {
            onEvent(CreatorCenterEvent.OnLoadMore(CreatorTab.Uploaded))
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

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = sortBarVisible) {
            CreatorSortRow(
                sort = sort,
                onRefresh = { s ->
                    onEvent(
                        CreatorCenterEvent.OnSortChange(
                            CreatorTab.Uploaded,
                            s
                        )
                    )
                })
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            state = refreshState,
            onRefresh = { onEvent(CreatorCenterEvent.OnSortChange(CreatorTab.Uploaded, sort)) },
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = refreshState, isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            PageContent(
                isLoading = state is PageLoadingState.Loading && items.isEmpty(),
                isError = state is PageLoadingState.Error,
                isEmpty = state is PageLoadingState.NoMoreData && items.isEmpty(),
                onRetry = { onEvent(CreatorCenterEvent.OnRefresh(CreatorTab.Uploaded)) },
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorContent(
                            title = stringResource(R.string.creator_uploaded_load_failed),
                            onRetry = { onEvent(CreatorCenterEvent.OnRefresh(CreatorTab.Uploaded)) }
                        )
                    }
                },
                empty = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyContent(
                            hint = stringResource(R.string.creator_uploaded_empty_title),
                            subHint = stringResource(R.string.creator_uploaded_empty_description),
                        )
                    }
                },
            ) {
                val columns = rememberVideoGridColumns()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns), state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(SpacingNormal),
                    horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
                    verticalArrangement = Arrangement.spacedBy(SpacingNormal),
                ) {
                    items(items, key = { it.videoCode }) { item ->
                        VideoCardItem(
                            videoItem = item,
                            onClickVideosItem = {
                                onEvent(
                                    CreatorCenterEvent.OnOpenUploadedVideo(
                                        item
                                    )
                                )
                            },
                            onLongClickVideosItem = { _, _ -> },
                        )
                    }
                    if (items.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            if (searchPagination) {
                                PaginationPager(
                                    currentPage = loadedPageCount.coerceAtLeast(1),
                                    totalPages = totalPages,
                                    onPageSelected = { page ->
                                        onEvent(CreatorCenterEvent.OnGoToPage(CreatorTab.Uploaded, page))
                                        scope.launch { gridState.scrollToItem(0) }
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            } else {
                                LoadMoreFooter(
                                    state = state,
                                    loadedPage = loadedPageCount,
                                    isLoadingMore = isLoadingMore
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 审核中/上传中视频 Tab 页面。
 *
 * @param uiState 页面 UI 状态
 * @param onEvent 用户事件回调
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreatorUploadingPage(
    uiState: CreatorCenterUiState,
    onEvent: (CreatorCenterEvent) -> Unit,
) {
    val items = uiState.uploadingItems
    val state = uiState.uploadingState
    val sort = uiState.uploadingSort
    val loadedPageCount = uiState.uploadingPage
    val totalPages = uiState.uploadingTotalPages
    val isLoadingMore = uiState.uploadingLoadingMore
    val gridState = rememberLazyGridState()
    val refreshState = rememberPullToRefreshState()
    val refreshing = state is PageLoadingState.Loading && items.isEmpty()
    var sortBarVisible by rememberSaveable { mutableStateOf(true) }
    val searchPagination = Preferences.searchPagination
    val scope = rememberCoroutineScope()

    LaunchedEffect(gridState.canLoadMore(state), isLoadingMore, searchPagination) {
        if (!searchPagination && gridState.canLoadMore(state) && !isLoadingMore) {
            onEvent(CreatorCenterEvent.OnLoadMore(CreatorTab.Uploading))
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

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = sortBarVisible) {
            CreatorSortRow(
                sort = sort,
                onRefresh = { s ->
                    onEvent(
                        CreatorCenterEvent.OnSortChange(
                            CreatorTab.Uploading,
                            s
                        )
                    )
                })
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            state = refreshState,
            onRefresh = { onEvent(CreatorCenterEvent.OnSortChange(CreatorTab.Uploading, sort)) },
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = refreshState, isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            PageContent(
                isLoading = state is PageLoadingState.Loading && items.isEmpty(),
                isError = state is PageLoadingState.Error,
                isEmpty = state is PageLoadingState.NoMoreData && items.isEmpty(),
                onRetry = { onEvent(CreatorCenterEvent.OnRefresh(CreatorTab.Uploading)) },
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorContent(
                            title = stringResource(R.string.creator_uploading_load_failed),
                            onRetry = { onEvent(CreatorCenterEvent.OnRefresh(CreatorTab.Uploading)) }
                        )
                    }
                },
                empty = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyContent(
                            hint = stringResource(R.string.creator_uploading_empty_title),
                            subHint = stringResource(R.string.creator_uploading_empty_description),
                        )
                    }
                },
            ) {
                val columns = rememberVideoGridColumns()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns), state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(SpacingNormal),
                    horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
                    verticalArrangement = Arrangement.spacedBy(SpacingNormal),
                ) {
                    items(items, key = { it.videoCode }) { item ->
                        CreatorUploadingCard(
                            item = item,
                            onClick = { onEvent(CreatorCenterEvent.OnOpenUploadingVideo(item)) })
                    }
                    if (items.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            if (searchPagination) {
                                PaginationPager(
                                    currentPage = loadedPageCount.coerceAtLeast(1),
                                    totalPages = totalPages,
                                    onPageSelected = { page ->
                                        onEvent(CreatorCenterEvent.OnGoToPage(CreatorTab.Uploading, page))
                                        scope.launch { gridState.scrollToItem(0) }
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            } else {
                                LoadMoreFooter(
                                    state = state,
                                    loadedPage = loadedPageCount,
                                    isLoadingMore = isLoadingMore
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
