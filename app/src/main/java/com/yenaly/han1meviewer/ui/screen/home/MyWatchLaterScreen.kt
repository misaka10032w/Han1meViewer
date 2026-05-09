package com.yenaly.han1meviewer.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MyWatchLaterScreen(
    watchLaterFlow: StateFlow<List<HanimeInfo>>,
    watchLaterStateFlow: StateFlow<PageLoadingState<*>>,
    deleteStateFlow: Flow<WebsiteState<Boolean>>,
    loadedPageCountFlow: StateFlow<Int>,
    isLoadingMoreFlow: StateFlow<Boolean>,
    onBack: () -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteWatchLater: (HanimeInfo) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val items = watchLaterFlow.collectAsStateWithLifecycle().value
    val state = watchLaterStateFlow.collectAsStateWithLifecycle().value
    val loadedPageCount = loadedPageCountFlow.collectAsStateWithLifecycle().value
    val isLoadingMore = isLoadingMoreFlow.collectAsStateWithLifecycle().value

    MyListVideoGridScreen(
        items = items,
        state = state,
        deleteStateFlow = deleteStateFlow,
        loadedPageCount = loadedPageCount,
        isLoadingMore = isLoadingMore,
        titleRes = R.string.watch_later,
        helpMessageRes = R.string.long_press_to_cancel_watch_later,
        deleteTitleRes = R.string.delete_watch_later,
        onBack = onBack,
        onOpenVideo = onOpenVideo,
        onDeleteItem = onDeleteWatchLater,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
    )
}
