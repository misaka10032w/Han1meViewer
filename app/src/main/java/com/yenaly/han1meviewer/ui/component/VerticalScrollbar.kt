package com.yenaly.han1meviewer.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = Color.Unspecified,
    fadeDelayMillis: Long = 1500,
    fadeOutDurationMillis: Int = 500
): Modifier = composed {
    val resolvedColor = if (color == Color.Unspecified)
        LocalContentColor.current.copy(alpha = 0.4f)
    else color
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            alpha.snapTo(1f)
        } else {
            delay(fadeDelayMillis)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = fadeOutDurationMillis)
            )
        }
    }

    drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val visibleItemsInfo = layoutInfo.visibleItemsInfo

        if (visibleItemsInfo.isNotEmpty() && layoutInfo.totalItemsCount > visibleItemsInfo.size) {
            val elementHeight = size.height / layoutInfo.totalItemsCount
            val firstVisibleElementIndex = visibleItemsInfo.first().index

            val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
            val scrollbarHeight = visibleItemsInfo.size * elementHeight

            drawRoundRect(
                color = resolvedColor.copy(alpha = resolvedColor.alpha * alpha.value),
                topLeft = Offset(size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
                cornerRadius = CornerRadius(width.toPx() / 2)
            )
        }
    }
}