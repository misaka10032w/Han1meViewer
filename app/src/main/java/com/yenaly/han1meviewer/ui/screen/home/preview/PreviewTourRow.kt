package com.yenaly.han1meviewer.ui.screen.home.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.ui.component.lazy.LazyRow
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import kotlinx.coroutines.launch

/**
 * 预览游览行。水平滚动展示当月最新影片缩略图，选中项高亮。
 *
 * @param latestHanime 影片列表
 * @param selectedIndex 当前选中项索引
 * @param onSelect 选中回调
 */
@Composable
fun PreviewTourRow(
    latestHanime: List<HanimeInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val edgePadding = remember(windowInfo.containerSize) {
        with(density) {
            val containerWidthDp = windowInfo.containerSize.width.toDp()
            ((containerWidthDp - 92.dp) / 2).coerceAtLeast(16.dp)
        }
    }

    LaunchedEffect(selectedIndex, latestHanime.size) {
        if (latestHanime.isEmpty()) return@LaunchedEffect
        centerPreviewTourItem(listState, selectedIndex)
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = edgePadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(
            latestHanime,
            key = { index, item -> item.videoCode.ifBlank { "tour-$index" } },
        ) { index, item ->
            OutlinedCard(
                onClick = {
                    onSelect(index)
                    scope.launch {
                        centerPreviewTourItem(listState, index)
                    }
                },
                shape = RoundedCornerShape(18.dp),
            ) {
                Box {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .width(92.dp)
                            .height(132.dp),
                        contentScale = ContentScale.Crop,
                    )
                    if (index == selectedIndex) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun PreviewTourRowPreview() {
    ComponentPreview {
        PreviewTourRow(
            latestHanime = fakeHomePageVideos.take(5),
            selectedIndex = 1,
            onSelect = {},
        )
    }
}
