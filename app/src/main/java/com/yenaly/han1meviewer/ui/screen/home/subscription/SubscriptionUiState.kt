package com.yenaly.han1meviewer.ui.screen.home.subscription

import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem

/**
 * 订阅页面 UI 状态。
 *
 * @param artists 已订阅作者列表
 * @param videos 订阅视频列表
 * @param isRefreshing 是否正在下拉刷新
 * @param canLoadMore 是否可加载更多
 * @param currentPage 当前页码
 * @param error 错误信息，null 表示无错误
 * @param showCached 是否只展示缓存数据（Loading/Error 时保留旧数据）
 */
data class SubscriptionUiState(
    val artists: List<SubscriptionItem> = emptyList(),
    val videos: List<SubscriptionVideosItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = false,
    val currentPage: Int = 1,
    val error: Throwable? = null,
    val showCached: Boolean = false,
)

/**
 * 订阅页面用户交互事件。
 */
sealed interface SubscriptionEvent {
    /** 点击作者 */
    data class OnClickArtist(val artistName: String) : SubscriptionEvent

    /** 长按作者 */
    data class OnLongClickArtist(val artistName: String) : SubscriptionEvent

    /** 点击视频 */
    data class OnClickVideo(val videoCode: String) : SubscriptionEvent

    /** 长按视频 */
    data class OnLongClickVideo(val videoCode: String, val title: String) : SubscriptionEvent

    /** 下拉刷新 */
    data object OnRefresh : SubscriptionEvent

    /** 加载更多 */
    data object OnLoadMore : SubscriptionEvent

    /** 返回 */
    data object OnBack : SubscriptionEvent
}
