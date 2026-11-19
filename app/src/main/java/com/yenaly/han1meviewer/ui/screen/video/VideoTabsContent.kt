package com.yenaly.han1meviewer.ui.screen.video

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 播放页标签的种类。
 *
 * 页面分发不再依赖下标（评论关闭、本地下载、相关影片开关都会改变 tab 数量），
 * 而是根据 [VideoTabItem.kind] 映射到对应的页面内容。
 */
enum class VideoTabKind {
    INTRODUCTION,
    COMMENT,
    RELATED,
}

data class VideoTabItem(
    val kind: VideoTabKind,
    @param:StringRes val titleRes: Int,
    val badgeCount: Int = 0,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoTabsContent(
    tabs: List<VideoTabItem>,
    selectedTabIndex: Int,
    onSelectedTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pageContent: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
        pageCount = { tabs.size.coerceAtLeast(1) },
    )

    LaunchedEffect(selectedTabIndex, tabs.size) {
        if (tabs.isEmpty()) return@LaunchedEffect
        val targetPage = selectedTabIndex.coerceIn(0, tabs.lastIndex)
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage, tabs.size) {
        if (tabs.isEmpty()) return@LaunchedEffect
        val currentPage = pagerState.currentPage.coerceIn(0, tabs.lastIndex)
        if (currentPage != selectedTabIndex) {
            onSelectedTabChange(currentPage)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (tabs.isNotEmpty()) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, item ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            // 不用 BadgedBox：它把角标叠在文字右上角，会遮住「评论」二字的尾部；
                            // 改为文字后跟随角标（如「评论 49」），不会互相遮挡。
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(item.titleRes),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (item.badgeCount > 0) {
                                    Badge(
                                        modifier = Modifier.padding(top = 1.dp),
                                    ) {
                                        Text(item.badgeCount.toString())
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }

        // 不设 beyondViewportPageCount：相邻页（评论页较重：Scaffold + PTR + 列表）
        // 预组合会把整页塞进进场帧，造成导航动画掉帧；各页滚动状态经
        // pager 的 SaveableStateHolder 保留，首次切换时才组合。
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                pageContent(page)
            }
        }
    }
}
