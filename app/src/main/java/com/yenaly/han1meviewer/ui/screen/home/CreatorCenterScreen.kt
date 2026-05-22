package com.yenaly.han1meviewer.ui.screen.home

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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.yenaly.han1meviewer.logic.model.CreatorSort
import com.yenaly.han1meviewer.logic.model.CreatorTab
import com.yenaly.han1meviewer.logic.model.CreatorUploadingItem
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.LoadMoreFooter
import com.yenaly.han1meviewer.ui.component.PageContent
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import com.yenaly.han1meviewer.ui.viewmodel.CreatorCenterViewModel
import kotlinx.coroutines.launch

@Composable
fun CreatorCenterScreen(
    viewModel: CreatorCenterViewModel,
    onBack: () -> Unit,
    onOpenUploadedVideo: (HanimeInfo) -> Unit,
    onOpenUploadingVideo: (CreatorUploadingItem) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    val uploadedItems by viewModel.uploadedItems.collectAsStateWithLifecycle()
    val uploadingItems by viewModel.uploadingItems.collectAsStateWithLifecycle()
    val uploadedState by viewModel.uploadedState.collectAsStateWithLifecycle()
    val uploadingState by viewModel.uploadingState.collectAsStateWithLifecycle()
    val uploadedSort by viewModel.uploadedSort.collectAsStateWithLifecycle()
    val uploadingSort by viewModel.uploadingSort.collectAsStateWithLifecycle()
    val uploadedPage by viewModel.uploadedPage.collectAsStateWithLifecycle()
    val uploadingPage by viewModel.uploadingPage.collectAsStateWithLifecycle()
    val uploadedLoadingMore by viewModel.uploadedLoadingMore.collectAsStateWithLifecycle()
    val uploadingLoadingMore by viewModel.uploadingLoadingMore.collectAsStateWithLifecycle()
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(if (pagerState.currentPage == 0) CreatorTab.Uploaded else CreatorTab.Uploading)
        if (pagerState.currentPage == 0 && uploadedItems.isEmpty() && uploadedPage == 0 && uploadedState is PageLoadingState.Loading) {
            viewModel.refreshUploaded(uploadedSort)
        }
        if (pagerState.currentPage == 1 && uploadingItems.isEmpty() && uploadingPage == 0 && uploadingState is PageLoadingState.Loading) {
            viewModel.refreshUploading(uploadingSort)
        }
    }

    ConfirmDialog(
        visible = showHelpDialog,
        title = stringResource(R.string.help),
        message = stringResource(R.string.creator_center_help),
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.close),
        onConfirm = { showHelpDialog = false },
        onDismiss = { showHelpDialog = false },
    )

    HanimeScaffold(
        title = stringResource(R.string.creator_center),
        onBack = onBack,
        actions = {
            FilledIconButton(onClick = { showHelpDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_help_24),
                    contentDescription = stringResource(R.string.help),
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(R.string.uploaded_videos)) },
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(R.string.uploading_videos)) },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> CreatorUploadedPage(
                        items = uploadedItems,
                        state = uploadedState,
                        sort = uploadedSort,
                        loadedPageCount = uploadedPage,
                        isLoadingMore = uploadedLoadingMore,
                        onRefresh = viewModel::refreshUploaded,
                        onLoadMore = viewModel::loadMoreUploaded,
                        onOpenVideo = onOpenUploadedVideo,
                    )

                    else -> CreatorUploadingPage(
                        items = uploadingItems,
                        state = uploadingState,
                        sort = uploadingSort,
                        loadedPageCount = uploadingPage,
                        isLoadingMore = uploadingLoadingMore,
                        onRefresh = viewModel::refreshUploading,
                        onLoadMore = viewModel::loadMoreUploading,
                        onOpenVideo = onOpenUploadingVideo,
                    )
                }
            }
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun CreatorUploadedPage(
    items: List<HanimeInfo>,
    state: PageLoadingState<*>,
    sort: CreatorSort,
    loadedPageCount: Int,
    isLoadingMore: Boolean,
    onRefresh: (CreatorSort) -> Unit,
    onLoadMore: () -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val refreshState = rememberPullToRefreshState()
    val refreshing = state is PageLoadingState.Loading && items.isEmpty()

    LaunchedEffect(gridState.canLoadMore(items, state), isLoadingMore) {
        if (gridState.canLoadMore(items, state) && !isLoadingMore) {
            onLoadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CreatorSortRow(sort = sort, onRefresh = onRefresh)
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
                            title = stringResource(R.string.creator_uploaded_load_failed),
                            onRetry = { onRefresh(sort) }
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
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(SpacingNormal),
                    horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
                    verticalArrangement = Arrangement.spacedBy(SpacingNormal),
                ) {
                    items(items, key = { it.videoCode }) { item ->
                        VideoCardItem(
                            videoItem = item,
                            onClickVideosItem = { onOpenVideo(item) },
                            onLongClickVideosItem = { _, _ -> },
                        )
                    }
                    if (items.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LoadMoreFooter(state = state, loadedPage = loadedPageCount, isLoadingMore = isLoadingMore)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun CreatorUploadingPage(
    items: List<CreatorUploadingItem>,
    state: PageLoadingState<*>,
    sort: CreatorSort,
    loadedPageCount: Int,
    isLoadingMore: Boolean,
    onRefresh: (CreatorSort) -> Unit,
    onLoadMore: () -> Unit,
    onOpenVideo: (CreatorUploadingItem) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val refreshState = rememberPullToRefreshState()
    val refreshing = state is PageLoadingState.Loading && items.isEmpty()

    LaunchedEffect(gridState.canLoadMoreUploading(items, state), isLoadingMore) {
        if (gridState.canLoadMoreUploading(items, state) && !isLoadingMore) {
            onLoadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CreatorSortRow(sort = sort, onRefresh = onRefresh)
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
                            title = stringResource(R.string.creator_uploading_load_failed),
                            onRetry = { onRefresh(sort) }
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
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(SpacingNormal),
                    horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
                    verticalArrangement = Arrangement.spacedBy(SpacingNormal),
                ) {
                    items(items, key = { it.videoCode }) { item ->
                        CreatorUploadingCard(item = item, onClick = { onOpenVideo(item) })
                    }
                    if (items.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LoadMoreFooter(state = state, loadedPage = loadedPageCount, isLoadingMore = isLoadingMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorSortRow(
    sort: CreatorSort,
    onRefresh: (CreatorSort) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CreatorSortChip(text = stringResource(R.string.sort_by_newest), selected = sort == CreatorSort.Latest) {
            onRefresh(CreatorSort.Latest)
        }
        CreatorSortChip(text = stringResource(R.string.popular), selected = sort == CreatorSort.Popular) {
            onRefresh(CreatorSort.Popular)
        }
        CreatorSortChip(text = stringResource(R.string.sort_by_oldest), selected = sort == CreatorSort.Oldest) {
            onRefresh(CreatorSort.Oldest)
        }
    }
}

@Composable
private fun CreatorSortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun CreatorUploadingCard(
    item: CreatorUploadingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = item.reviewStatus.toReviewStatusColor()
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.22f)),
        modifier = modifier
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
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                item.duration?.let {
                    Text(
                        text = it,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(topStart = 8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = item.currentArtist.orEmpty() + if (!item.uploadTime.isNullOrBlank()) " • ${item.uploadTime}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val fileName = item.remoteVideoUrl.substringAfterLast('/').substringBefore('?')
                    Text(
                        text = fileName.ifBlank { "Unknown Source" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun String.toLocalizedReviewStatus(): String {
    return when {
        contains("已上傳") -> stringResource(R.string.creator_status_uploaded)
        contains("排隊") -> stringResource(R.string.creator_status_queued)
        contains("待處理") -> stringResource(R.string.creator_status_pending)
        contains("轉檔") -> stringResource(R.string.creator_status_transcoding)
        else -> this
    }
}

@Composable
private fun String.toReviewStatusColor(): Color {
    return when {
        contains("已上傳") -> Color(0xFF27C93F)
        contains("排隊") -> Color(0xFFF9A825)
        contains("待處理") -> Color(0xFFFF9800)
        contains("轉檔") -> Color(0xFF42A5F5)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}



private fun LazyGridState.canLoadMore(
    items: List<HanimeInfo>,
    state: PageLoadingState<*>,
): Boolean {
    if (items.isEmpty()) return false
    if (state is PageLoadingState.Loading || state is PageLoadingState.NoMoreData || state is PageLoadingState.Error) return false
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    return lastVisible >= layoutInfo.totalItemsCount - 4
}

private fun LazyGridState.canLoadMoreUploading(
    items: List<CreatorUploadingItem>,
    state: PageLoadingState<*>,
): Boolean {
    if (items.isEmpty()) return false
    if (state is PageLoadingState.Loading || state is PageLoadingState.NoMoreData || state is PageLoadingState.Error) return false
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    return lastVisible >= layoutInfo.totalItemsCount - 4
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun CreatorUploadingCardPreview() {
    ComponentPreview {
        CreatorUploadingCard(
            item = CreatorUploadingItem(
                title = "[3Dimm Animations] 優菈的丘丘人大危機",
                coverUrl = "https://picsum.photos/400/240",
                videoCode = "5103",
                duration = "04:54",
                currentArtist = "_shinobu_",
                uploadTime = "1分鐘前",
                remoteVideoUrl = "https://example.com/video.mp4",
                reviewStatus = "排隊中",
            ),
            onClick = {},
        )
    }
}
