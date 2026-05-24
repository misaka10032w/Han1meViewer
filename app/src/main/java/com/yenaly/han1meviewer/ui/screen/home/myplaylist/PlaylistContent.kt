package com.yenaly.han1meviewer.ui.screen.home.myplaylist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.LoadMoreFooter
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.screen.getColumnCount

/**
 * 播放列表页 Content 层。纯 UI，不持有 ViewModel。
 *
 * 接收 [PlaylistUiState] + [PlaylistEvent] 回调，负责网格展示和动画切换。
 *
 * @param uiState 页面 UI 状态
 * @param onEvent 用户事件回调
 * @param rawState 原始网络状态（用于 Error 的重试和空状态判断）
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistContent(
    uiState: PlaylistUiState,
    onEvent: (PlaylistEvent) -> Unit,
    rawState: WebsiteState<Playlists>,
) {
    val gridState = rememberLazyGridState()
    val noMore = uiState.noMorePlaylists
    val loadingMore = uiState.isLoadingMore

    LaunchedEffect(gridState, noMore, loadingMore) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 3 && uiState.playlists.isNotEmpty()
        }.collect { shouldLoad ->
            if (shouldLoad && !loadingMore && !noMore) {
                onEvent(PlaylistEvent.OnLoadMore)
            }
        }
    }

    AnimatedContent(
        targetState = rawState,
        label = "playlist-content-animation",
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(200))
        }
    ) { target ->
        when (target) {
            is WebsiteState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }

            is WebsiteState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(
                                R.string.load_failed_with_reason,
                                target.throwable.message.orEmpty()
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onEvent(PlaylistEvent.OnRefresh) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            is WebsiteState.Success -> {
                if (target.info.playlists.isEmpty() && uiState.playlists.isEmpty()) {
                    EmptyContent(stringResource(R.string.empty_content))
                    return@AnimatedContent
                }
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(getColumnCount(180)),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.playlists) { playlist ->
                        PlaylistItem(
                            playlist = playlist,
                            modifier = Modifier.height(140.dp)
                        ) {
                            onEvent(
                                PlaylistEvent.OnPlaylistClick(
                                    playlist.listCode,
                                    playlist.title
                                )
                            )
                        }
                    }
                    if (uiState.playlists.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LoadMoreFooter(
                                state = if (uiState.noMorePlaylists) PageLoadingState.NoMoreData
                                else if (uiState.isLoadingMore) PageLoadingState.Loading
                                else PageLoadingState.Success(Unit),
                                loadedPage = uiState.playlistPage - 1,
                                isLoadingMore = uiState.isLoadingMore
                            )
                        }
                    }
                }
            }
        }
    }
}
