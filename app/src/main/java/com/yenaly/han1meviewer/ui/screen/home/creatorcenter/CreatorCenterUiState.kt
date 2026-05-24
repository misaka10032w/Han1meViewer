package com.yenaly.han1meviewer.ui.screen.home.creatorcenter

import com.yenaly.han1meviewer.logic.model.CreatorSort
import com.yenaly.han1meviewer.logic.model.CreatorTab
import com.yenaly.han1meviewer.logic.model.CreatorUploadingItem
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.state.PageLoadingState

/**
 * 创作者中心页面 UI 状态。
 *
 * @param selectedTab 当前选中的 Tab
 * @param uploadedItems 已上传视频列表
 * @param uploadingItems 审核中视频列表
 * @param uploadedState 已上传 Tab 加载状态
 * @param uploadingState 审核中 Tab 加载状态
 * @param uploadedSort 已上传排序方式
 * @param uploadingSort 审核中排序方式
 * @param uploadedPage 已上传已加载页数
 * @param uploadingPage 审核中已加载页数
 * @param uploadedLoadingMore 是否正在加载更多已上传
 * @param uploadingLoadingMore 是否正在加载更多审核中
 */
data class CreatorCenterUiState(
    val selectedTab: CreatorTab = CreatorTab.Uploaded,
    val uploadedItems: List<HanimeInfo> = emptyList(),
    val uploadingItems: List<CreatorUploadingItem> = emptyList(),
    val uploadedState: PageLoadingState<*> = PageLoadingState.Loading,
    val uploadingState: PageLoadingState<*> = PageLoadingState.Loading,
    val uploadedSort: CreatorSort = CreatorSort.Latest,
    val uploadingSort: CreatorSort = CreatorSort.Latest,
    val uploadedPage: Int = 0,
    val uploadingPage: Int = 0,
    val uploadedLoadingMore: Boolean = false,
    val uploadingLoadingMore: Boolean = false,
)

/**
 * 创作者中心用户交互事件。
 */
sealed interface CreatorCenterEvent {
    /** 切换 Tab */
    data class OnTabChange(val tab: CreatorTab) : CreatorCenterEvent

    /** 切换排序并刷新 */
    data class OnSortChange(val tab: CreatorTab, val sort: CreatorSort) : CreatorCenterEvent

    /** 加载更多 */
    data class OnLoadMore(val tab: CreatorTab) : CreatorCenterEvent

    /** 刷新 */
    data class OnRefresh(val tab: CreatorTab) : CreatorCenterEvent

    /** 打开视频 */
    data class OnOpenUploadedVideo(val item: HanimeInfo) : CreatorCenterEvent

    /** 打开审核中视频 */
    data class OnOpenUploadingVideo(val item: CreatorUploadingItem) : CreatorCenterEvent

    /** 返回 */
    data object OnBack : CreatorCenterEvent
}
