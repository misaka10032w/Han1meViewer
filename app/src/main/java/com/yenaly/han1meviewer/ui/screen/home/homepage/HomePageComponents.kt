package com.yenaly.han1meviewer.ui.screen.home.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HomePage
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.component.lazy.LazyRow
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeAnnouncements
import com.yenaly.han1meviewer.ui.preview.fakeBanner
import com.yenaly.han1meviewer.ui.preview.fakeCategories
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.ui.screen.rememberCardResponsiveWidth
import com.yenaly.han1meviewer.ui.theme.SpacingLarge
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 渲染首页顶部栏，包含抽屉入口、搜索入口和新番列表入口。
 *
 * @param onOpenDrawer 点击抽屉按钮时调用。
 * @param onSearchClick 点击搜索框时调用。
 * @param onNavigateToPreview 点击新番按钮时调用。
 * @param modifier 应用于顶部栏根布局的修饰符。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageTopBar(
    onOpenDrawer: () -> Unit,
    onSearchClick: () -> Unit,
    onNavigateToPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val placeholders = stringArrayResource(R.array.search_placeholders)
    val randomHint = placeholders.random()
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.open_menu),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSearchClick() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        randomHint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            IconButton(onClick = onNavigateToPreview) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_newspaper_24),
                    contentDescription = stringResource(R.string.hanime_list),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 显示首页 Banner 轮播图。
 *
 * @param banner Banner 数据，为空时不渲染内容。
 * @param onBannerClick 点击 Banner 时调用，参数为视频编号。
 * @param modifier 应用于轮播图根布局的修饰符。
 */
@Composable
fun BannerCarousel(
    banner: HomePage.Banner?,
    onBannerClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banner == null) return

    val banners = listOf(banner)
    val pagerState = rememberPagerState(pageCount = { banners.size.coerceAtLeast(1) })

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val item = banners[page.coerceIn(banners.indices)]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onBannerClick(item.videoCode) }
                ) {
                    RetryableImage(
                        model = item.picUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.h_chan_loading),
                        error = painterResource(R.drawable.h_chan_load_failed)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        item.description?.let { desc ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (banners.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(banners.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                        )
                        if (index < banners.lastIndex) Spacer(Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}

/**
 * 显示横向滚动的视频分类行。
 *
 * @param title 分类标题。
 * @param videos 当前分类下的视频列表。
 * @param onMoreClick 点击更多按钮时调用。
 * @param onVideoClick 点击视频卡片时调用，参数为视频编号。
 * @param onVideoLongClick 长按视频卡片时调用，参数为视频编号和标题。
 * @param modifier 应用于分类行根布局的修饰符。
 */
@Composable
fun CategoryRow(
    title: String,
    videos: List<HanimeInfo>,
    onMoreClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    onVideoLongClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.more),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onMoreClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        val uniqueVideos = videos.distinctBy { it.videoCode }
        val (cardWidth, _) = rememberCardResponsiveWidth()
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
            contentPadding = PaddingValues(horizontal = SpacingLarge)
        ) {
            items(uniqueVideos, key = { it.videoCode }) { video ->
                VideoCardItem(
                    modifier = Modifier.width(cardWidth),
                    videoItem = video,
                    isHorizontalCard = true,
                    onClickVideosItem = onVideoClick,
                    onLongClickVideosItem = onVideoLongClick
                )
            }
        }
    }
}

/**
 * 显示完整公告列表弹窗。
 *
 * @param announcements 可供选择的公告列表。
 * @param onDismiss 关闭弹窗时调用。
 */
@Composable
fun AnnouncementListDialog(
    announcements: List<Announcement>,
    onDismiss: () -> Unit
) {
    var selectedAnnouncement by remember { mutableStateOf<Announcement?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.announcement_list),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(announcements.size) { index ->
                    val item = announcements[index]
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAnnouncement = item }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Text(
                stringResource(R.string.close),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }
    )

    selectedAnnouncement?.let { announcement ->
        AnnouncementDialog(
            announcementData = announcement,
            onDismiss = { selectedAnnouncement = null }
        )
    }
}

/**
 * 显示首页紧凑公告轮播卡片。
 *
 * @param announcements 要展示的公告列表。
 * @param onAnnouncementClick 点击公告时调用，参数为被点击公告。
 * @param onClose 点击关闭按钮时调用。
 * @param modifier 应用于公告卡片根布局的修饰符。
 */
@Composable
fun AnnouncementCard(
    announcements: List<Announcement>,
    onAnnouncementClick: (Announcement) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (announcements.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { announcements.size })
    var showAllDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val item = announcements[page]
                val time = formatTimestamp(item.timestamp)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(onClick = { onAnnouncementClick(item) })
                        .padding(start = 12.dp, end = 40.dp, top = 10.dp, bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.TopStart)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = time,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (announcements.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(announcements.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 6.dp else 4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                        if (index < announcements.lastIndex) Spacer(Modifier.width(4.dp))
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.view_all),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showAllDialog = true }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }

    if (showAllDialog) {
        AnnouncementListDialog(
            announcements = announcements,
            onDismiss = { showAllDialog = false }
        )
    }
}

/**
 * 渲染首页可滚动内容区域。
 *
 * @param banner Banner 数据。
 * @param announcements 当前要展示的公告列表。
 * @param categories 视频分类行列表。
 * @param onBannerClick 点击 Banner 时调用。
 * @param onAnnouncementClick 点击公告时调用。
 * @param onCloseAnnouncement 关闭公告时调用。
 * @param onMoreClick 点击分类更多按钮时调用，参数为对应分类。
 * @param onVideoClick 点击视频卡片时调用，参数为视频编号。
 * @param onVideoLongClick 长按视频卡片时调用，参数为视频编号和标题。
 * @param modifier 应用于列表根布局的修饰符。
 */
@Composable
fun HomePageContent(
    banner: HomePage.Banner?,
    announcements: List<Announcement>,
    categories: List<HomeCategory>,
    onBannerClick: (String?) -> Unit,
    onAnnouncementClick: (Announcement) -> Unit,
    onCloseAnnouncement: () -> Unit,
    onMoreClick: (HomeCategory) -> Unit,
    onVideoClick: (String) -> Unit,
    onVideoLongClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "banner") {
            BannerCarousel(
                banner = banner,
                onBannerClick = onBannerClick,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        if (announcements.isNotEmpty()) {
            item(key = "announcement") {
                AnnouncementCard(
                    announcements = announcements,
                    onAnnouncementClick = onAnnouncementClick,
                    onClose = onCloseAnnouncement,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        categories.forEach { category ->
            item(key = "category_${category.titleRes}") {
                CategoryRow(
                    title = stringResource(category.titleRes),
                    videos = category.videos,
                    onMoreClick = { onMoreClick(category) },
                    onVideoClick = onVideoClick,
                    onVideoLongClick = onVideoLongClick,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 将公告秒级时间戳格式化为本地时间字符串。
 *
 * @param timestamp 秒级 Unix 时间戳。
 * @return 本地日期时间字符串。
 */
fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochSecond(timestamp)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

@Preview(showBackground = true, name = "首页顶栏")
@Composable
private fun HomePageTopBarPreview() {
    ComponentPreview {
        HomePageTopBar(
            onOpenDrawer = {},
            onSearchClick = {},
            onNavigateToPreview = {}
        )
    }
}

@Preview(showBackground = true, name = "Banner 轮播")
@Composable
private fun BannerCarouselPreview() {
    ComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            BannerCarousel(
                banner = fakeBanner,
                onBannerClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "公告卡片（单条）")
@Composable
private fun AnnouncementCardSinglePreview() {
    ComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            AnnouncementCard(
                announcements = fakeAnnouncements.take(1),
                onAnnouncementClick = {},
                onClose = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "公告卡片（多条可滑动）")
@Composable
private fun AnnouncementCardMultiplePreview() {
    ComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            AnnouncementCard(
                announcements = fakeAnnouncements,
                onAnnouncementClick = {},
                onClose = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "公告列表弹窗")
@Composable
private fun AnnouncementListDialogPreview() {
    ComponentPreview {
        AnnouncementListDialog(
            announcements = fakeAnnouncements,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "视频分类行", showSystemUi = false)
@Composable
private fun CategoryRowPreview() {
    ComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            CategoryRow(
                title = "最新裏番",
                videos = fakeHomePageVideos,
                onMoreClick = {},
                onVideoClick = {},
                onVideoLongClick = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "首页主内容", showSystemUi = false
)
@Composable
private fun HomePageContentPreview() {
    ComponentPreview {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomePageContent(
                banner = fakeBanner,
                announcements = fakeAnnouncements,
                categories = fakeCategories,
                onBannerClick = {},
                onAnnouncementClick = {},
                onCloseAnnouncement = {},
                onMoreClick = {},
                onVideoClick = {},
                onVideoLongClick = { _, _ -> }
            )
        }
    }
}
