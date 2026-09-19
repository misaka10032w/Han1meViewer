package com.yenaly.han1meviewer.ui.screen.home.homepage.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHeroItems
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.ui.screen.home.homepage.HomeHeroItem

/** 队列中单行的高度。固定行高使条目数量变化时排版稳定，超出面板高度时列表可滚动。 */
private val QueueItemHeight = 56.dp

/**
 * 平板大屏下与 Banner 并排显示的待播队列。
 *
 * 与主轮播共用同一份数据，点击某一行即可切换主图，从而让限高后的 Hero 依然能一眼看到
 * 后续内容。当前项以主题色高亮；自动翻页或滑动主图把当前项移出可视区时，队列会跟随滚动，
 * 保证高亮行始终可见。
 *
 * @param items 与主轮播一致的轮播数据。
 * @param currentIndex 当前主图所在的索引。
 * @param onItemClick 点击某一行时调用，参数为该行索引。
 * @param modifier 应用于队列根布局的修饰符，需要由调用方给定宽高。
 */
@Composable
fun HeroQueue(
    items: List<HomeHeroItem>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex, items.size) {
        val target = currentIndex.coerceIn(items.indices)
        // 当前项仍在可视区内就不打扰，避免用户翻看其他条目时被强行拉回
        val isTargetVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == target }
        if (!isTargetVisible) {
            listState.animateScrollToItem(target)
        }
    }

    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = items.size,
            key = { index -> items[index].videoCode ?: index }
        ) { index ->
            HeroQueueItem(
                item = items[index],
                isCurrent = index == currentIndex,
                onClick = { onItemClick(index) },
                modifier = Modifier.height(QueueItemHeight)
            )
        }
    }
}

/**
 * 待播队列中的单行。
 *
 * @param item 当前行的轮播数据。
 * @param isCurrent 是否为当前主图。
 * @param onClick 点击当前行时调用。
 * @param modifier 应用于当前行根布局的修饰符，需要由调用方给定高度。
 */
@Composable
private fun HeroQueueItem(
    item: HomeHeroItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
        ) {
            RetryableImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                placeholder = painterResource(R.drawable.h_chan_loading),
                error = painterResource(R.drawable.h_chan_load_failed),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, name = "待播队列", widthDp = 420, heightDp = 340)
@Composable
private fun HeroQueuePreview() {
    ComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            HeroQueue(
                items = fakeHeroItems,
                currentIndex = 0,
                onItemClick = {},
                modifier = Modifier.fillMaxWidth().height(320.dp)
            )
        }
    }
}
