package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.ui.screen.home.WatchHistoryScreen
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel

@Composable
fun WatchHistoryRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MainViewModel = viewModel()
    WatchHistoryScreen(
        historiesFlow = viewModel.loadAllWatchHistories(),
        onBack = onBack,
        onOpenVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteHistory = viewModel::deleteWatchHistory,
        onDeleteAllHistories = viewModel::deleteAllWatchHistories,
    )
}
