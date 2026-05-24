package com.yenaly.han1meviewer.ui.screen.home.creatorcenter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.CreatorSort
import com.yenaly.han1meviewer.logic.model.CreatorTab
import com.yenaly.han1meviewer.logic.model.CreatorUploadingItem
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.ui.component.LoadMoreFooter
import com.yenaly.han1meviewer.ui.component.PageContent
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import kotlinx.coroutines.flow.distinctUntilChanged

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
    val isLoadingMore = uiState.uploadedLoadingMore
    val gridState = rememberLazyGridState()
    val refreshState = rememberPullToRefreshState()
    val refreshing = state is PageLoadingState.Loading && items.isEmpty()
    var sortBarVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(gridState.canLoadMore(state), isLoadingMore) {
        if (gridState.canLoadMore(state) && !isLoadingMore) {
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
    val isLoadingMore = uiState.uploadingLoadingMore
    val gridState = rememberLazyGridState()
    val refreshState = rememberPullToRefreshState()
    val refreshing = state is PageLoadingState.Loading && items.isEmpty()
    var sortBarVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(gridState.canLoadMore(state), isLoadingMore) {
        if (gridState.canLoadMore(state) && !isLoadingMore) {
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

/**
 * 排序筛选行：最新/热门/最旧。
 */
@Composable
fun CreatorSortRow(sort: CreatorSort, onRefresh: (CreatorSort) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CreatorSortChip(
            text = stringResource(R.string.sort_by_newest),
            selected = sort == CreatorSort.Latest
        ) { onRefresh(CreatorSort.Latest) }
        CreatorSortChip(
            text = stringResource(R.string.popular),
            selected = sort == CreatorSort.Popular
        ) { onRefresh(CreatorSort.Popular) }
        CreatorSortChip(
            text = stringResource(R.string.sort_by_oldest),
            selected = sort == CreatorSort.Oldest
        ) { onRefresh(CreatorSort.Oldest) }
    }
}

@Composable
private fun CreatorSortChip(text: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick, label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/**
 * 审核中视频卡片。
 */
@Composable
fun CreatorUploadingCard(
    item: CreatorUploadingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = item.reviewStatus.toReviewStatusColor()
    Card(
        onClick = onClick, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.22f)), modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.h_chan_loading),
                    error = painterResource(R.drawable.h_chan_load_failed),
                    fallback = painterResource(R.drawable.h_chan_load_failed),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(
                            color = statusColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.reviewStatus.toLocalizedReviewStatus(),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                item.duration?.let {
                    Text(
                        text = it, color = Color.White, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(
                                Color.Black.copy(alpha = 0.65f),
                                RoundedCornerShape(topStart = 8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.currentArtist.orEmpty() + if (!item.uploadTime.isNullOrBlank()) " · ${item.uploadTime}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fileName = item.remoteVideoUrl.substringAfterLast('/').substringBefore('?')
                    Text(
                        text = fileName.ifBlank { "Unknown Source" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun PreviewCreatorUploadingCard() {
    ComponentPreview {
        CreatorUploadingCard(
            item = CreatorUploadingItem(
                title = "Sample", coverUrl = "https://picsum.photos/400/240", videoCode = "5103",
                duration = "04:54", currentArtist = "_shinobu_", uploadTime = "1min ago",
                remoteVideoUrl = "https://example.com/video.mp4", reviewStatus = "Pending",
            ), onClick = {})
    }
}
