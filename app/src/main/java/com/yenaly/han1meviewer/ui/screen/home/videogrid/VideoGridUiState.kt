package com.yenaly.han1meviewer.ui.screen.home.videogrid

import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.state.PageLoadingState

/**
 * 视频网格页面的 UI 状态。
 *
 * @param items 视频列表
 * @param state 加载状态
 * @param loadedPageCount 已加载的页数
 * @param isLoadingMore 是否正在加载更多
 * @param isRefreshing 是否正在下拉刷新
 * @param isError 是否处于错误状态（且列表为空）
 * @param isEmpty 是否处于空状态
 */
data class VideoGridUiState(
    val items: List<HanimeInfo> = emptyList(),
    val state: PageLoadingState<*> = PageLoadingState.Loading,
    val loadedPageCount: Int = 0,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val isError: Boolean = false,
    val isEmpty: Boolean = false,
)
