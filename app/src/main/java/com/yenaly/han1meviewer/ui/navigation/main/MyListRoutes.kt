package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.ui.screen.home.MyFavVideoScreen
import com.yenaly.han1meviewer.ui.screen.home.MyWatchLaterScreen
import com.yenaly.han1meviewer.ui.viewmodel.MyListViewModel

@Composable
fun MyFavVideoRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyListViewModel = viewModel()
    MyFavVideoScreen(
        favVideoFlow = viewModel.fav.favVideoFlow,
        favVideoStateFlow = viewModel.fav.favVideoStateFlow,
        deleteStateFlow = viewModel.fav.deleteMyFavVideoFlow,
        loadedPageCountFlow = viewModel.fav.loadedPageCount,
        isLoadingMoreFlow = viewModel.fav.isLoadingMore,
        onBack = onBack,
        onOpenVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteFavorite = { item ->
            val position =
                viewModel.fav.favVideoFlow.value.indexOfFirst { it.videoCode == item.videoCode }
            if (position >= 0) {
                viewModel.fav.deleteMyFavVideo(item.videoCode, position)
            }
        },
        onRefresh = {
            viewModel.fav.favVideoPage = 1
            viewModel.fav.clearMyListItems()
            viewModel.fav.getMyFavVideoItems(Preferences.savedUserId, 1)
            viewModel.fav.favVideoPage = 2
        },
        onLoadMore = {
            val page = viewModel.fav.favVideoPage
            viewModel.fav.getMyFavVideoItems(Preferences.savedUserId, page)
            viewModel.fav.favVideoPage = page + 1
        },
    )
}

@Composable
fun MyWatchLaterRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyListViewModel = viewModel()
    MyWatchLaterScreen(
        watchLaterFlow = viewModel.watchLater.watchLaterFlow,
        watchLaterStateFlow = viewModel.watchLater.watchLaterStateFlow,
        deleteStateFlow = viewModel.watchLater.deleteMyWatchLaterFlow,
        loadedPageCountFlow = viewModel.watchLater.loadedPageCount,
        isLoadingMoreFlow = viewModel.watchLater.isLoadingMore,
        onBack = onBack,
        onOpenVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteWatchLater = { item ->
            val position =
                viewModel.watchLater.watchLaterFlow.value.indexOfFirst { it.videoCode == item.videoCode }
            if (position >= 0) {
                viewModel.watchLater.deleteMyWatchLater(item.videoCode, position)
            }
        },
        onRefresh = {
            viewModel.watchLater.watchLaterPage = 1
            viewModel.watchLater.clearMyListItems()
            viewModel.watchLater.getMyWatchLaterItems(1)
            viewModel.watchLater.watchLaterPage = 2
        },
        onLoadMore = {
            val page = viewModel.watchLater.watchLaterPage
            viewModel.watchLater.getMyWatchLaterItems(page)
            viewModel.watchLater.watchLaterPage = page + 1
        },
    )
}
