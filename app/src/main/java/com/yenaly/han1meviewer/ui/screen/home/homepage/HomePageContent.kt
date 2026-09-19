package com.yenaly.han1meviewer.ui.screen.home.homepage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeAnnouncements
import com.yenaly.han1meviewer.ui.preview.fakeHomePage
import com.yenaly.han1meviewer.ui.screen.home.homepage.component.AnnouncementCard
import com.yenaly.han1meviewer.ui.screen.home.homepage.component.AnnouncementPanel
import com.yenaly.han1meviewer.ui.screen.home.homepage.component.BannerCarousel
import com.yenaly.han1meviewer.ui.screen.home.homepage.component.CategoryRow
import com.yenaly.han1meviewer.ui.screen.home.homepage.component.HeroQueue
import kotlinx.coroutines.launch

/**
 * 渲染首页可滚动内容区域。
 *
 * 轮播数据由官网运营位与下方分类行中的视频混排而成，见 [buildHomeHeroItems]。平板大屏下
 * 轮播会与右侧内容（公告优先，否则为待播队列）以左右分栏的形式组成 Hero 区域，详见
 * [rememberHomeHeroSpec]。
 *
 * @param data 主页数据
 * @param onEvent 主页事件回调
 * @param onCloseAnnouncement 关闭公告时调用。
 * @param modifier 应用于列表根布局的修饰符。
 */
@Composable
fun HomePageContent(
    data: HomeData,
    onEvent: (HomeUiEvent) -> Unit,
    onCloseAnnouncement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val announcements = remember(data.announcements) {
        data.announcements.filter { it.isActive }
    }
    val categories = remember(data.page) {
        buildCategoryList(data.page)
    }
    val heroItems = remember(data.page, categories) {
        buildHomeHeroItems(data.page, categories)
    }
    val heroSpec = rememberHomeHeroSpec(
        hasSideContent = announcements.isNotEmpty() || heroItems.size > 1
    )
    val heroPagerState = rememberPagerState(pageCount = { heroItems.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()
    val onHeroItemClick: (String?) -> Unit = { videoCode ->
        videoCode?.let { onEvent(HomeUiEvent.OpenVideo(it)) }
    }
    val onAnnouncementClick: (Announcement) -> Unit = { announcement ->
        onEvent(HomeUiEvent.ShowAnnouncementDialog(announcement))
    }
    val sidePanelWidth = (heroSpec as? HomeHeroSpec.Constrained)?.panelWidth
    val showAnnouncementInSidePanel = sidePanelWidth != null && announcements.isNotEmpty()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "banner") {
            when (heroSpec) {
                HomeHeroSpec.Default -> BannerCarousel(
                    items = heroItems,
                    onItemClick = onHeroItemClick,
                    modifier = Modifier.padding(
                        horizontal = HomeHeroHorizontalPadding,
                        vertical = 6.dp
                    ),
                    pagerState = heroPagerState
                )

                is HomeHeroSpec.Constrained -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HomeHeroHorizontalPadding, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BannerCarousel(
                        items = heroItems,
                        onItemClick = onHeroItemClick,
                        size = heroSpec.bannerSize,
                        pagerState = heroPagerState
                    )
                    if (sidePanelWidth != null) {
                        Spacer(Modifier.width(HomeHeroPanelSpacing))
                        val sidePanelModifier = Modifier
                            .width(sidePanelWidth)
                            .height(heroSpec.bannerSize.height)
                        if (showAnnouncementInSidePanel) {
                            AnnouncementPanel(
                                announcements = announcements,
                                onAnnouncementClick = onAnnouncementClick,
                                onClose = onCloseAnnouncement,
                                modifier = sidePanelModifier
                            )
                        } else {
                            HeroQueue(
                                items = heroItems,
                                currentIndex = heroPagerState.currentPage,
                                onItemClick = { index ->
                                    scope.launch { heroPagerState.animateScrollToPage(index) }
                                },
                                modifier = sidePanelModifier
                            )
                        }
                    }
                }
            }
        }
        if (announcements.isNotEmpty() && !showAnnouncementInSidePanel) {
            item(key = "announcement") {
                AnnouncementCard(
                    announcements = announcements,
                    onAnnouncementClick = onAnnouncementClick,
                    onClose = onCloseAnnouncement,
                    modifier = Modifier.padding(
                        horizontal = HomeHeroHorizontalPadding,
                        vertical = 4.dp
                    )
                )
            }
        }
        categories.forEach { category ->
            item(key = "category_${category.titleRes}") {
                CategoryRow(
                    title = stringResource(category.titleRes),
                    videos = category.videos,
                    onMoreClick = {
                        val params = category.toAdvancedSearchParams()
                        if (params.isNotEmpty()) {
                            onEvent(HomeUiEvent.NavigateToSearchAdvanced(params))
                        }
                    },
                    onVideoClick = { code ->
                        onEvent(HomeUiEvent.OpenVideo(code))
                    },
                    onVideoLongClick = { _, _ ->
                       // onEvent(HomeUiEvent.LongPressVideoCopy(code, title))
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "首页主内容")
@Composable
private fun HomePageContentPreview() {
    ComponentPreview {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomePageContent(
                data = HomeData(
                    page = fakeHomePage,
                    announcements = fakeAnnouncements,
                ),
                onEvent = {},
                onCloseAnnouncement = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "首页主内容（平板）", widthDp = 1280, heightDp = 800)
@Composable
private fun HomePageContentTabletPreview() {
    ComponentPreview {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomePageContent(
                data = HomeData(
                    page = fakeHomePage,
                    announcements = fakeAnnouncements,
                ),
                onEvent = {},
                onCloseAnnouncement = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "首页主内容（平板·待播队列）", widthDp = 1280, heightDp = 800)
@Composable
private fun HomePageContentTabletPreviewWithQueue() {
    ComponentPreview {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomePageContent(
                data = HomeData(
                    page = fakeHomePage,
                    announcements = emptyList(),
                ),
                onEvent = {},
                onCloseAnnouncement = {},
            )
        }
    }
}
