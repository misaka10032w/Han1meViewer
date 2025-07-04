package com.yenaly.han1meviewer.ui.fragment.home.subscription

import android.util.Log
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.MySubscriptions
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.viewmodel.MySubscriptionsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionApp(
    navigateBack: () -> Unit,
    viewModel: MySubscriptionsViewModel,
    onClickArtist: (String) -> Unit,
    onClickVideosItem: (String) -> Unit
) {
    val state by viewModel.subscriptionsState.collectAsStateWithLifecycle()
    val cachedArtists = rememberSaveable { mutableStateOf<List<SubscriptionItem>>(emptyList()) }
    val cachedVideos =
        rememberSaveable { mutableStateOf<List<SubscriptionVideosItem>>(emptyList()) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        viewModel.refreshCompleted.collect {
            isRefreshing = false
        }
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.loadMySubscriptions(forceReload = true)
    }

    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(refreshState.distanceFraction).coerceIn(0f, 1f)
    }

    LaunchedEffect(state) {
        if (state is WebsiteState.Success) {
            cachedArtists.value =
                (state as WebsiteState.Success<MySubscriptions>).info.subscriptions.toList()
            cachedVideos.value =
                (state as WebsiteState.Success<MySubscriptions>).info.subscriptionsVideos.toList()
        } else if (state is WebsiteState.Loading && cachedArtists.value.isEmpty()) {
            viewModel.loadMySubscriptions()
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        stringResource(R.string.my_subscribe),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pullToRefresh(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
        ) {
            when (val result = state) {
                is WebsiteState.Loading -> {
                    if (cachedArtists.value.isEmpty() || cachedVideos.value.isEmpty()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else {
                        // 显示旧数据，刷新中不替换内容
                        AnimatedVideoContent(
                            gridState = gridState,
                            artists = cachedArtists.value,
                            videos = cachedVideos.value,
                            onClickArtist = onClickArtist,
                            onClickVideosItem = onClickVideosItem,
                            onLoadMore = { viewModel.loadMySubscriptions() },
                            canLoadMore = viewModel.canLoadMore()
                        )
                    }
                }

                is WebsiteState.Error -> {
                    if (cachedArtists.value.isEmpty()) {
                        Text(
                            "加载失败: ${result.throwable.message}",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        // 显示旧缓存内容（保持体验）
                        AnimatedVideoContent(
                            gridState = gridState,
                            artists = cachedArtists.value,
                            videos = cachedVideos.value,
                            onClickArtist = onClickArtist,
                            onClickVideosItem = onClickVideosItem,
                            onLoadMore = { viewModel.loadMySubscriptions() },
                            canLoadMore = viewModel.canLoadMore()
                        )
                    }
                }

                else -> {
                    // 统一渲染缓存（成功后缓存已更新）
                    AnimatedVideoContent(
                        gridState = gridState,
                        artists = cachedArtists.value,
                        videos = cachedVideos.value,
                        onClickArtist = onClickArtist,
                        onClickVideosItem = onClickVideosItem,
                        onLoadMore = { viewModel.loadMySubscriptions() },
                        canLoadMore = viewModel.canLoadMore()
                    )
                }
            }

            Log.i("VideoCard", cachedVideos.value.toString())
            // 下拉加载指示器
            if (isRefreshing || scaleFraction() > 0f) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            scaleX = scaleFraction()
                            scaleY = scaleFraction()
                        }
                        .zIndex(1f)
                ) {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = refreshState,
                        isRefreshing = isRefreshing
                    )
                }
            }
        }
    }
}
