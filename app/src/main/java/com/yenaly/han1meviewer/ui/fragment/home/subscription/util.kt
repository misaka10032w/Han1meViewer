package com.yenaly.han1meviewer.ui.fragment.home.subscription

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem

@Composable
fun AnimatedVideoContent(
    gridState: LazyGridState,
    artists: List<SubscriptionItem>,
    videos: List<SubscriptionVideosItem>,
    onClickArtist: (String) -> Unit,
    onClickVideosItem: (String) -> Unit,
    onLoadMore: () -> Unit,
    canLoadMore: Boolean
) {
    AnimatedContent(
        targetState = Pair(artists, videos),
        label = "video-content-animation",
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(200))
        }
    ) { (a, v) ->
        VideoGrid(
            gridState = gridState,
            artists = a,
            videos = v,
            onClickArtist = onClickArtist,
            onClickVideosItem = onClickVideosItem,
            onLoadMore = onLoadMore,
            canLoadMore = canLoadMore
        )
    }
}

@Composable
fun RetryableImage(
    model: Any,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    retryLimit: Int = 1,
    placeholder: Painter,
    error: Painter
) {
    val context = LocalContext.current
    var retryCount by remember { mutableIntStateOf(0) }
    var currentModel by remember { mutableStateOf(model) }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(currentModel)
            .crossfade(true)
            .listener(
                onError = { _, result ->
                    Log.e("CoilError", "Image load failed", result.throwable)
                }
            )
            .build(),
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = error,
        modifier = modifier,
        onError = {
            if (retryCount < retryLimit) {
                retryCount++
                currentModel = "$model?retry=$retryCount"
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionAppPreviewBody(
    artists: List<SubscriptionItem>,
    videos: List<SubscriptionVideosItem>,
    navigateBack: () -> Unit = {},
    onClickArtist: (String) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val isRefreshing = false
    val refreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .pullToRefresh(
                state = refreshState,
                isRefreshing = isRefreshing,
                onRefresh = {}
            ),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的订阅") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            VideoGrid(
                gridState = LazyGridState(),
                artists = artists,
                onClickArtist = onClickArtist,
                videos = videos,
                onClickVideosItem = {},
                onLoadMore = { },
                canLoadMore = false
            )
        }
    }
}