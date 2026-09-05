package com.yenaly.han1meviewer.ui.screen.home.videogrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.ui.component.LoadMoreFooter
import com.yenaly.han1meviewer.ui.component.PaginationPager
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import kotlinx.coroutines.launch

/**
 * 视频网格 Content 层。纯 UI，不持有 ViewModel。
 *
 * 接收 [VideoGridUiState] 和单个回调集合，负责网格渲染和 LoadMoreFooter。
 *
 * @param uiState 页面 UI 状态
 * @param gridState LazyGrid 滚动状态
 * @param onOpenVideo 打开视频详情回调
 * @param onDeleteItem 删除视频项回调
 * @param onGoToPage 跳转到指定页回调
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoGridContent(
    uiState: VideoGridUiState,
    gridState: LazyGridState,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteItem: (HanimeInfo) -> Unit,
    onGoToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoColumns = rememberVideoGridColumns()
    val scope = rememberCoroutineScope()
    val searchPagination = Preferences.searchPagination
    LazyVerticalGrid(
        columns = GridCells.Fixed(videoColumns),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(SpacingNormal),
        horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
        verticalArrangement = Arrangement.spacedBy(SpacingNormal)
    ) {
        items(uiState.items, key = { it.videoCode }) { item ->
            VideoCardItem(
                videoItem = item,
                isHorizontalCard = true,
                onClickVideosItem = { onOpenVideo(item) },
                onLongClickVideosItem = { _, _ -> onDeleteItem(item) },
            )
        }
        if (uiState.items.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (searchPagination) {
                    PaginationPager(
                        currentPage = uiState.loadedPageCount.coerceAtLeast(1),
                        totalPages = uiState.totalPages,
                        onPageSelected = { page ->
                            onGoToPage(page)
                            scope.launch { gridState.scrollToItem(0) }
                        },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    LoadMoreFooter(
                        state = uiState.state,
                        loadedPage = uiState.loadedPageCount,
                        isLoadingMore = uiState.isLoadingMore
                    )
                }
            }
        }
    }
}
