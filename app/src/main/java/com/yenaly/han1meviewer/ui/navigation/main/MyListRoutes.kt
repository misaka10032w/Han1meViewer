package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.screen.home.VideoGridScreen
import com.yenaly.han1meviewer.ui.viewmodel.MyListViewModel

@Composable
fun FavVideoRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyListViewModel = viewModel()
    val fav = viewModel.fav
    val items = fav.favVideoFlow.collectAsStateWithLifecycle().value
    val state = fav.favVideoStateFlow.collectAsStateWithLifecycle().value
    val loadedPageCount = fav.loadedPageCount.collectAsStateWithLifecycle().value
    val isLoadingMore = fav.isLoadingMore.collectAsStateWithLifecycle().value

    VideoGridScreen(
        items = items,
        state = state,
        deleteStateFlow = fav.deleteMyFavVideoFlow,
        loadedPageCount = loadedPageCount,
        isLoadingMore = isLoadingMore,
        titleRes = R.string.fav_video,
        helpMessageRes = R.string.long_press_to_cancel_fav,
        deleteTitleRes = R.string.delete_fav,
        onBack = onBack,
        onOpenVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteItem = { item ->
            val position = items.indexOfFirst { it.videoCode == item.videoCode }
            if (position >= 0) fav.deleteMyFavVideo(item.videoCode, position)
        },
        onRefresh = {
            fav.favVideoPage = 1
            fav.clearMyListItems()
            fav.getMyFavVideoItems(Preferences.savedUserId, 1)
            fav.favVideoPage = 2
        },
        onLoadMore = {
            val page = fav.favVideoPage
            fav.getMyFavVideoItems(Preferences.savedUserId, page)
            fav.favVideoPage = page + 1
        },
    )
}

@Composable
fun WatchLaterRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyListViewModel = viewModel()
    val wl = viewModel.watchLater
    val items = wl.watchLaterFlow.collectAsStateWithLifecycle().value
    val state = wl.watchLaterStateFlow.collectAsStateWithLifecycle().value
    val loadedPageCount = wl.loadedPageCount.collectAsStateWithLifecycle().value
    val isLoadingMore = wl.isLoadingMore.collectAsStateWithLifecycle().value

    VideoGridScreen(
        items = items,
        state = state,
        deleteStateFlow = wl.deleteMyWatchLaterFlow,
        loadedPageCount = loadedPageCount,
        isLoadingMore = isLoadingMore,
        titleRes = R.string.watch_later,
        helpMessageRes = R.string.long_press_to_cancel_watch_later,
        deleteTitleRes = R.string.delete_watch_later,
        onBack = onBack,
        onOpenVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteItem = { item ->
            val position = items.indexOfFirst { it.videoCode == item.videoCode }
            if (position >= 0) wl.deleteMyWatchLater(item.videoCode, position)
        },
        onRefresh = {
            wl.watchLaterPage = 1
            wl.clearMyListItems()
            wl.getMyWatchLaterItems(1)
            wl.watchLaterPage = 2
        },
        onLoadMore = {
            val page = wl.watchLaterPage
            wl.getMyWatchLaterItems(page)
            wl.watchLaterPage = page + 1
        },
    )
}
