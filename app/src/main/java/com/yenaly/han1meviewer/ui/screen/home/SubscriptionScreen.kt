package com.yenaly.han1meviewer.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.PullRefreshOverlay
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.screen.home.subscription.SubscriptionContent
import com.yenaly.han1meviewer.ui.screen.home.subscription.SubscriptionEvent
import com.yenaly.han1meviewer.ui.screen.home.subscription.SubscriptionUiState
import com.yenaly.han1meviewer.ui.viewmodel.MySubscriptionsViewModel

/**
 * 订阅页面 Screen 层。
 *
 * 持有 [MySubscriptionsViewModel]，管理缓存、下拉刷新、加载更多等状态编排。
 * 渲染委托给 [SubscriptionContent]。
 *
 * @param navigateBack 返回回调
 * @param viewModel 订阅 ViewModel
 * @param onClickArtist 点击作者 → 跳转搜索
 * @param onLongClickArtist 长按作者 → 复制分享文本
 * @param onClickVideosItem 点击视频 → 跳转详情
 * @param onLongClickVideosItem 长按视频 → 复制分享文本
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionScreen(
    navigateBack: () -> Unit,
    viewModel: MySubscriptionsViewModel,
    onClickArtist: (String) -> Unit,
    onLongClickArtist: (String) -> Unit,
    onClickVideosItem: (String) -> Unit,
    onLongClickVideosItem: (String, String) -> Unit,
) {
    val state by viewModel.subscriptionsState.collectAsStateWithLifecycle()
    val cachedArtists = rememberSaveable { mutableStateOf<List<SubscriptionItem>>(emptyList()) }
    val cachedVideos = rememberSaveable { mutableStateOf<List<SubscriptionVideosItem>>(emptyList()) }
    val scrollBehavior = pinnedScrollBehavior(rememberTopAppBarState())
    val gridState = rememberLazyGridState()

    val refreshState = rememberPullToRefreshState()
    var isRefreshing by rememberSaveable { mutableStateOf(false) }

    val canLoadMore = viewModel.canLoadMore()

    val showCached = state is WebsiteState.Loading && cachedArtists.value.isNotEmpty() ||
            state is WebsiteState.Error && cachedArtists.value.isNotEmpty()

    LaunchedEffect(state) {
        when (val s = state) {
            is WebsiteState.Success -> {
                cachedArtists.value = s.info.subscriptions.toList()
                cachedVideos.value = s.info.subscriptionsVideos.toList()
            }
            is WebsiteState.Loading -> {
                if (cachedArtists.value.isEmpty()) viewModel.loadMySubscriptions()
            }
            else -> Unit
        }
        if (state !is WebsiteState.Loading) {
            isRefreshing = false
        }
    }

    val uiState = SubscriptionUiState(
        artists = cachedArtists.value,
        videos = cachedVideos.value,
        isRefreshing = isRefreshing,
        canLoadMore = canLoadMore,
        error = (state as? WebsiteState.Error)?.throwable,
        showCached = showCached,
    )

    val handleEvent: (SubscriptionEvent) -> Unit = { event ->
        when (event) {
            SubscriptionEvent.OnBack -> navigateBack()
            is SubscriptionEvent.OnClickArtist -> onClickArtist(event.artistName)
            is SubscriptionEvent.OnLongClickArtist -> onLongClickArtist(event.artistName)
            is SubscriptionEvent.OnClickVideo -> onClickVideosItem(event.videoCode)
            is SubscriptionEvent.OnLongClickVideo -> onLongClickVideosItem(event.videoCode, event.title)
            SubscriptionEvent.OnRefresh -> {
                isRefreshing = true
                viewModel.loadMySubscriptions(forceReload = true)
            }
            SubscriptionEvent.OnLoadMore -> viewModel.loadMySubscriptions()
        }
    }

    HanimeScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        title = stringResource(R.string.my_subscribe),
        onBack = navigateBack,
        scrollBehavior = scrollBehavior,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pullToRefresh(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = { handleEvent(SubscriptionEvent.OnRefresh) }
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (state) {
                is WebsiteState.Loading -> {
                    if (cachedArtists.value.isEmpty() || cachedVideos.value.isEmpty()) {
                        LoadingIndicator(Modifier.align(Alignment.Center))
                    } else {
                        SubscriptionContent(
                            uiState = uiState,
                            onEvent = handleEvent,
                            gridState = gridState,
                        )
                    }
                }

                is WebsiteState.Error -> {
                    if (cachedArtists.value.isEmpty()) {
                        EmptyContent(
                            hint = stringResource(
                                R.string.load_failed_with_reason,
                                (state as WebsiteState.Error).throwable.message.orEmpty()
                            ),
                            picRes = R.drawable.h_chan_sad
                        )
                    } else {
                        SubscriptionContent(
                            uiState = uiState,
                            onEvent = handleEvent,
                            gridState = gridState,
                        )
                    }
                }

                is WebsiteState.Success -> {
                    SubscriptionContent(
                        uiState = uiState,
                        onEvent = handleEvent,
                        gridState = gridState,
                    )
                }
            }

            PullRefreshOverlay(
                state = refreshState,
                isRefreshing = isRefreshing,
            )
        }
    }
}
