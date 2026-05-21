package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.ui.screen.home.WatchHistoryTabScreen
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.han1meviewer.ui.viewmodel.OnlineWatchHistoryViewModel

@Composable
fun WatchHistoryRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val localViewModel: MainViewModel = viewModel()
    val onlineViewModel: OnlineWatchHistoryViewModel = viewModel()
    WatchHistoryTabScreen(
        localHistoriesFlow = localViewModel.loadAllWatchHistories(),
        onlineItems = onlineViewModel.items,
        onlineState = onlineViewModel.state,
        onlineSort = onlineViewModel.selectedSort,
        onlineLoadedPageCount = onlineViewModel.loadedPageCount,
        onlineIsLoadingMore = onlineViewModel.isLoadingMore,
        onlineRefreshing = onlineViewModel::isRefreshing,
        onlineDeleteStateFlow = onlineViewModel.deleteFlow,
        onBack = onBack,
        onOpenLocalVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteLocalHistory = localViewModel::deleteWatchHistory,
        onDeleteAllLocalHistories = localViewModel::deleteAllWatchHistories,
        onOpenOnlineVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteOnlineVideo = onlineViewModel::deleteItem,
        onRefreshOnline = onlineViewModel::refresh,
        onLoadMoreOnline = onlineViewModel::loadNextPage,
    )
}
