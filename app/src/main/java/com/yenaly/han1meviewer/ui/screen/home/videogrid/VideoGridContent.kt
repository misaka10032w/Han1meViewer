package com.yenaly.han1meviewer.ui.screen.home.videogrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.ui.component.LoadMoreFooter
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.ui.theme.SpacingNormal

/**
 * 视频网格 Content 层。纯 UI，不持有 ViewModel。
 *
 * 接收 [VideoGridUiState] 和单个回调集合，负责网格渲染和 LoadMoreFooter。
 *
 * @param uiState 页面 UI 状态
 * @param gridState LazyGrid 滚动状态
 * @param onOpenVideo 打开视频详情回调
 * @param onDeleteItem 删除视频项回调
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoGridContent(
    uiState: VideoGridUiState,
    gridState: LazyGridState,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteItem: (HanimeInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoColumns = rememberVideoGridColumns()
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
                LoadMoreFooter(
                    state = uiState.state,
                    loadedPage = uiState.loadedPageCount,
                    isLoadingMore = uiState.isLoadingMore
                )
            }
        }
    }
}
