package com.yenaly.han1meviewer.ui.screen.video

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

data class VideoTabItem(
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
                            if (item.badgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text(item.badgeCount.toString()) }
                                    }
                                ) {
                                    Text(
                                        text = androidx.compose.ui.res.stringResource(item.titleRes),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            } else {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(item.titleRes),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        },
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                pageContent(page)
            }
        }
    }
}
