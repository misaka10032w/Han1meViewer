package com.yenaly.han1meviewer.ui.screen.home.myplaylist

import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.state.PageLoadingState

/**
 * 播放列表页面 UI 状态。
 *
 * @param playlists 播放列表项
 * @param isRefreshing 是否正在下拉刷新
 * @param showSheet 是否显示详情底部弹窗
 * @param selectedListCode 选中的播放列表代码
 * @param selectedListTitle 选中的播放列表标题
 * @param isLoadingMore 是否正在加载更多列表
 * @param noMorePlaylists 是否无更多播放列表
 * @param playlistPage 当前列表页码
 */
data class PlaylistUiState(
    val playlists: List<Playlists.Playlist> = emptyList(),
    val isRefreshing: Boolean = false,
    val showSheet: Boolean = false,
    val selectedListCode: String = "",
    val selectedListTitle: String = "",
    val isLoadingMore: Boolean = false,
    val noMorePlaylists: Boolean = false,
    val playlistPage: Int = 1,
)

/**
 * 播放列表页面用户交互事件。
 */
sealed interface PlaylistEvent {
    /** 下拉刷新 */
    data object OnRefresh : PlaylistEvent

    /** 加载更多播放列表 */
    data object OnLoadMore : PlaylistEvent

    /** 点击播放列表 → 打开详情弹窗 */
    data class OnPlaylistClick(val listCode: String, val title: String) : PlaylistEvent

    /** 关闭详情弹窗 */
    data object OnDismissSheet : PlaylistEvent

    /** 返回上一页 */
    data object OnBack : PlaylistEvent

    /** 创建新播放列表 */
    data class OnCreatePlaylist(val title: String, val desc: String) : PlaylistEvent
}

/**
 * 播放列表详情弹窗的回调集合。
 *
 * 将弹窗所需的 ViewModel 交互抽象为回调，避免弹窗直接持有 ViewModel。
 */
data class PlaylistSheetCallbacks(
    val loadItems: (page: Int, listCode: String, refresh: Boolean) -> Unit,
    val modifyPlaylist: (listCode: String, title: String, desc: String, delete: Boolean) -> Unit,
    val deleteFromPlaylist: (listCode: String, videoCode: String, position: Int) -> Unit,
    val updateScrollState: (listCode: String, firstVisibleIndex: Int, scrollOffset: Int) -> Unit,
    val getScrollState: (listCode: String) -> Pair<Int, Int>,
    val onLoadPlaylists: () -> Unit,
)

/**
 * 播放列表详情弹窗的 UI 状态。
 */
data class PlaylistSheetUiState(
    val playlistItems: List<HanimeInfo> = emptyList(),
    val playlistState: PageLoadingState<*> = PageLoadingState.Loading,
    val playlistDesc: String? = null,
    val currentListCode: String = "",
    val currentListTitle: String = "",
    val currentPage: Int = 1,
)
