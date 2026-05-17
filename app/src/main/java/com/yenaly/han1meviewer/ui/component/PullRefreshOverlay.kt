package com.yenaly.han1meviewer.ui.component

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoxScope.PullRefreshOverlay(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    containerColor: Color = Color.Unspecified,
) {
    val scaleFraction = remember(isRefreshing, state.distanceFraction) {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(state.distanceFraction).coerceIn(0f, 1f)
    }

    if (isRefreshing || scaleFraction > 0f) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    scaleX = scaleFraction
                    scaleY = scaleFraction
                }
                .zIndex(1f)
        ) {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = isRefreshing,
//                containerColor = containerColor,
            )
        }
    }
}
