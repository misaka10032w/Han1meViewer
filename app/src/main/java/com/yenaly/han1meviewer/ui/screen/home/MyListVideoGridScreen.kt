package com.yenaly.han1meviewer.ui.screen.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.PageContent
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.han1meviewer.ui.screen.home.videogrid.VideoGridContent
import com.yenaly.han1meviewer.ui.screen.home.videogrid.VideoGridUiState
import com.yenaly.han1meviewer.ui.screen.home.videogrid.canLoadMore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 通用视频网格页面 Screen 层。
 *
 * 为"稍后观看"、"收藏视频"等列表页面提供统一的 Scaffold + 下拉刷新 + 删除确认逻辑，
 * 渲染委托给 [VideoGridContent]。
 *
 * @param items 视频列表
 * @param state 加载状态
 * @param deleteStateFlow 删除操作结果流
 * @param loadedPageCount 已加载页数
 * @param isLoadingMore 是否正在加载更多
 * @param titleRes 标题资源 ID
 * @param helpMessageRes 帮助信息资源 ID
 * @param deleteTitleRes 删除确认标题资源 ID
 * @param onBack 返回回调
 * @param onOpenVideo 打开视频详情回调
 * @param onDeleteItem 删除视频回调
 * @param onRefresh 下拉刷新回调
 * @param onLoadMore 加载更多回调
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoGridScreen(
    items: List<HanimeInfo>,
    state: PageLoadingState<*>,
    deleteStateFlow: Flow<WebsiteState<Boolean>>,
    loadedPageCount: Int,
    isLoadingMore: Boolean,
    titleRes: Int,
    helpMessageRes: Int,
    deleteTitleRes: Int,
    onBack: () -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteItem: (HanimeInfo) -> Unit,
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
    val refreshingState = rememberPullToRefreshState()
    val isError = state is PageLoadingState.Error && items.isEmpty()
    val isEmpty = state is PageLoadingState.NoMoreData && items.isEmpty()
    val shouldBootstrap = items.isEmpty() && state is PageLoadingState.Loading && loadedPageCount == 0

    LaunchedEffect(shouldBootstrap) {
        if (shouldBootstrap) {
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
                is WebsiteState.Error -> snackbarHostState.showSnackbar(message = deleteFailedText)
                is WebsiteState.Success -> snackbarHostState.showSnackbar(message = deleteSuccessText)
                WebsiteState.Loading -> Unit
            }
        }
    }

    ConfirmDialog(
        visible = pendingDelete != null,
        title = stringResource(deleteTitleRes),
        message = stringResource(R.string.sure_to_delete_s, pendingDelete?.title.orEmpty()),
        confirmText = stringResource(R.string.delete),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            pendingDelete?.let(onDeleteItem)
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null },
    )

    ConfirmDialog(
        visible = showHelpDialog,
        title = stringResource(R.string.help),
        message = stringResource(helpMessageRes),
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.close),
        onConfirm = { showHelpDialog = false },
        onDismiss = { showHelpDialog = false },
    )

    val uiState = VideoGridUiState(
        items = items,
        state = state,
        loadedPageCount = loadedPageCount,
        isLoadingMore = isLoadingMore,
        isRefreshing = refreshing,
        isError = isError,
        isEmpty = isEmpty,
    )

    HanimeScaffold(
        title = stringResource(titleRes),
        subtitle = {
            Text(
                text = stringResource(R.string.video_count, items.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onBack = onBack,
        actions = {
            FilledIconButton(onClick = { showHelpDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_help_24),
                    contentDescription = stringResource(R.string.help),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            state = refreshingState,
            onRefresh = {
                pendingRefresh = true
                onRefresh()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = refreshingState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            PageContent(
                isLoading = false,
                isError = isError,
                isEmpty = isEmpty,
                onRetry = {
                    pendingRefresh = true
                    onRefresh()
                },
                error = {
                    ErrorContent(
                        title = stringResource(R.string.load_failed_retry),
                        onRetry = {
                            pendingRefresh = true
                            onRefresh()
                        },
                        modifier = Modifier.align(Alignment.Center),
                    )
                },
                empty = {
                    EmptyContent(hint = stringResource(R.string.empty_content))
                },
            ) {
                VideoGridContent(
                    uiState = uiState,
                    gridState = gridState,
                    onOpenVideo = onOpenVideo,
                    onDeleteItem = { pendingDelete = it },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun VideoGridScreenPreview() {
    ComponentPreview {
        VideoGridScreen(
            items = fakeHomePageVideos.take(6),
            state = PageLoadingState.Success(Unit),
            deleteStateFlow = flowOf(WebsiteState.Success(true)),
            loadedPageCount = 2,
            isLoadingMore = false,
            titleRes = R.string.fav_video,
            helpMessageRes = R.string.long_press_to_cancel_fav,
            deleteTitleRes = R.string.delete_fav,
            onBack = {},
            onOpenVideo = {},
            onDeleteItem = {},
            onRefresh = {},
            onLoadMore = {},
        )
    }
}
