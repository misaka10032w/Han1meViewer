package com.yenaly.han1meviewer.ui.screen.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.HanimeConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HomePage
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.component.lazy.LazyRow
import com.yenaly.han1meviewer.ui.preview.fakeAnnouncements
import com.yenaly.han1meviewer.ui.preview.fakeBanner
import com.yenaly.han1meviewer.ui.preview.fakeCategories
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.yenaly_libs.utils.putSpValue
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────
// CompositionLocal：搜索历史查询函数
// ─────────────────────────────────────────────

/**
 * 通过 CompositionLocal 向下层组件提供搜索历史查询能力
 * 由 Fragment 层注入实际的数据源（Room/DAO）
 */
val LocalSearchHistoryQuery = staticCompositionLocalOf<suspend (String) -> List<String>> {
    { emptyList() }
}

// ─────────────────────────────────────────────
// 数据类：首页视频分类行
// ─────────────────────────────────────────────
// 数据类：首页视频分类行
// ─────────────────────────────────────────────

/** 首页视频分类行数据 */
data class HomeCategory(
    @param:StringRes val titleRes: Int,
    val genre: String? = null,
    val sort: String? = null,
    val tags: String? = null,
    val videos: List<HanimeInfo>
)

// ─────────────────────────────────────────────
// 首页搜索覆盖层（从上向下展开）
// ─────────────────────────────────────────────

/**
 * 搜索覆盖层 - 点击首页搜索框后，自上向下展开的全屏搜索页
 * 支持实时搜索历史建议
 *
 * @param visible 是否显示覆盖层
 * @param onDismiss 关闭覆盖层回调
 * @param onNavigateToSearch 提交搜索后的跳转回调，传入搜索关键词
 * @param onQueryHistory 搜索历史查询回调，传入关键词返回匹配的历史搜索词列表
 */
@Composable
fun SearchOverlay(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onDismiss: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onQueryHistory: (suspend (String) -> List<String>)? = null
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var historySuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // 覆盖层可见时自动聚焦搜索输入框
    LaunchedEffect(visible) {
        if (visible) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            searchQuery = ""
            historySuggestions = emptyList()
        }
    }

    // 实时搜索历史建议（防抖 300ms）
    LaunchedEffect(searchQuery) {
        historySuggestions = if (searchQuery.isNotBlank() && onQueryHistory != null) {
            delay(300)
            try {
                onQueryHistory(searchQuery)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { -it }
        ) + fadeIn(tween(300)),
        exit = slideOutVertically(
            animationSpec = tween(250),
            targetOffsetY = { -it }
        ) + fadeOut(tween(200)),
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 搜索栏
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    // 返回按钮
                    IconButton(onClick = {
                        searchQuery = ""
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 搜索输入框
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        val query = searchQuery.trim()
                                        if (query.isNotBlank()) {
                                            onNavigateToSearch(query)
                                            onDismiss()
                                        }
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                stringResource(R.string.search_video_hint),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }

                    // 清除文本按钮
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_checkin),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 搜索提交按钮
                    IconButton(onClick = {
                        val query = searchQuery.trim()
                        if (query.isNotBlank()) {
                            onNavigateToSearch(query)
                            onDismiss()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 搜索建议/历史区域
                if (historySuggestions.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            stringResource(R.string.search_suggestions),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        historySuggestions.forEach { suggestion ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchQuery = suggestion
                                        onNavigateToSearch(suggestion)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (searchQuery.isNotBlank()) {
                                stringResource(R.string.no_matching_search_history)
                            } else {
                                stringResource(R.string.search_enter_keywords)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 首页顶部搜索栏
// ─────────────────────────────────────────────

/**
 * 首页顶部搜索栏 - MD3 风格，包含汉堡菜单按钮、搜索框、新番按钮
 *
 * @param onOpenDrawer 点击汉堡图标时打开侧边抽屉
 * @param onSearchClick 点击搜索框时展开搜索覆盖层
 * @param onNavigateToPreview 点击新番按钮跳转到新番列表
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
            // 左侧：汉堡菜单按钮
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.open_menu),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // 中间：搜索框（点击展开搜索页）
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

            // 右侧：新番列表按钮
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

// ─────────────────────────────────────────────
// 轮播封面图
// ─────────────────────────────────────────────

/**
 * Banner 轮播组件 - 使用 HorizontalPager 实现封面图轮播展示
 *
 * @param banner 首页 Banner 数据，为 null 时不显示
 * @param onBannerClick 点击 Banner 时的回调，传入 videoCode
 */
@Composable
fun BannerCarousel(
    banner: HomePage.Banner?,
    onBannerClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banner == null) return

    // TODO: 当前仅有一条 Banner，轮播结构已预留，后续接入多条 Banner 数据时自动启用
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
                    // Banner 封面图片
                    RetryableImage(
                        model = item.picUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.akarin),
                        error = painterResource(R.drawable.baseline_error_outline_24)
                    )

                    // 底部渐变遮罩
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

                    // Banner 文字信息
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

            // 页面指示器（多条 Banner 时显示）
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
                                    if (isSelected) Color.White
                                    else Color.White.copy(alpha = 0.5f)
                                )
                        )
                        if (index < banners.lastIndex) {
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 视频分类横向滚动行
// ─────────────────────────────────────────────

/**
 * 视频分类横向滚动行 - 显示一行视频卡片，包含标题和"更多"按钮
 *
 * @param title 分类标题
 * @param videos 该分类下的视频列表
 * @param onMoreClick 点击"更多"按钮回调
 * @param onVideoClick 点击视频卡片回调，传入 videoCode
 * @param onVideoLongClick 长按视频卡片回调，传入 videoCode 和 title
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
        // 标题栏
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

        // 横向视频列表
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(videos, key = { it.videoCode }) { video ->
                VideoCardItem(
                    videoItem = video,
                    isHorizontalCard = true,
                    onClickVideosItem = onVideoClick,
                    onLongClickVideosItem = onVideoLongClick
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 公告卡片
// ─────────────────────────────────────────────

/**
 * 公告列表全屏弹窗 - 点击"查看全部"后弹出，支持展开/收起单条公告详情
 *
 * @param announcements 全部公告列表
 * @param onDismiss 关闭弹窗回调
 */
@Composable
fun AnnouncementListDialog(
    announcements: List<Announcement>,
    onDismiss: () -> Unit
) {
    var expandedIndex by remember { mutableIntStateOf(-1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.announcement_list), style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(announcements.size) { index ->
                    val item = announcements[index]
                    val isExpanded = expandedIndex == index

                    Surface(
                        color = if (isExpanded) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = if (isExpanded) 2.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedIndex = if (isExpanded) -1 else index
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isExpanded) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = item.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
}

/**
 * 公告轮播卡片 - 使用 HorizontalPager 支持滑动浏览多条公告
 * 底部有页面指示器和"查看全部"按钮
 *
 * @param announcements 公告列表
 * @param onAnnouncementClick 点击某条公告回调
 * @param onClose 关闭公告（24小时内不再显示）
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
            // 可滑动的公告内容
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val item = announcements[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(onClick = { onAnnouncementClick(item) })
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(end = 40.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.announcement),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 关闭按钮（始终在最上层）
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
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

        // 底部行：页面指示器 + "查看全部"按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            // 页面指示器
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                // 用占位保持居中，右侧按钮对齐到末尾
            }
            if (announcements.size > 1) {
                Row(
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
                        if (index < announcements.lastIndex) {
                            Spacer(Modifier.width(4.dp))
                        }
                    }
                }
            }

            // "查看全部"按钮
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

    // 公告列表弹窗
    if (showAllDialog) {
        AnnouncementListDialog(
            announcements = announcements,
            onDismiss = { showAllDialog = false }
        )
    }
}

// ─────────────────────────────────────────────
// 首页主内容区
// ─────────────────────────────────────────────

/**
 * 首页可滚动内容区 - 包含 Banner 轮播、公告、各分类视频行
 *
 * @param banner 首页 Banner 数据
 * @param announcements 公告列表
 * @param categories 视频分类行列表
 * @param onBannerClick Banner 点击回调
 * @param onAnnouncementClick 公告点击回调
 * @param onCloseAnnouncement 关闭公告回调
 * @param onMoreClick 某分类"更多"按钮回调
 * @param onVideoClick 视频卡片点击回调
 * @param onVideoLongClick 视频卡片长按回调
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
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        // 轮播封面（两侧留边距）
        item(key = "banner") {
            BannerCarousel(
                banner = banner,
                onBannerClick = onBannerClick,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // 公告区域
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

        // 各分类视频行
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

// ─────────────────────────────────────────────
// 首页主屏幕
// ─────────────────────────────────────────────

/**
 * 首页主 Composable 屏幕 - MD3 风格首页
 *
 * 包含：
 * - Search App Bar（汉堡菜单 + 搜索框 + 新番按钮）
 * - Banner 轮播封面图
 * - 公告卡片
 * - 多个视频分类横向滚动行
 * - 搜索覆盖层（点击搜索框展开）
 *
 * @param viewModel 首页 ViewModel
 * @param onOpenDrawer 打开侧边抽屉回调
 * @param onNavigateToPreview 跳转新番列表回调
 * @param onNavigateToSearch 跳转搜索页回调
 * @param onNavigateToSearchAdvanced 跳转高级搜索结果页回调
 * @param onOpenVideo 打开视频详情回调
 * @param onLongPressVideoCopy 长按视频复制分享文本回调
 * @param onShowExitDialog 显示退出确认对话框回调
 * @param onShowAnnouncementDialog 显示公告详情对话框回调
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePageScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onOpenSearchPage: () -> Unit,
    onNavigateToSearchAdvanced: (Map<String, String>) -> Unit,
    onOpenVideo: (String) -> Unit,
    onLongPressVideoCopy: (String, String) -> Unit,
    onShowExitDialog: () -> Unit,
    onShowAnnouncementDialog: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.homePageFlow.collectAsStateWithLifecycle()
    val refreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    // 观测公告 LiveData，转为 Compose State
    val lifecycleOwner = LocalLifecycleOwner.current
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    DisposableEffect(viewModel.announcements, lifecycleOwner) {
        val observer = Observer<List<Announcement>> { list ->
            announcements = list
        }
        viewModel.announcements.observe(lifecycleOwner, observer)
        onDispose { }
    }

    // 仅在首次加载（无缓存数据时）才自动请求首页数据，避免每次返回都刷新
    LaunchedEffect(Unit) {
        if (viewModel.homePageFlow.value !is WebsiteState.Success) {
            viewModel.getHomePage()
        }
        if (viewModel.announcements.value == null) {
            viewModel.loadAnnouncements(true)
        }
    }

    // 数据加载完成后重置刷新状态
    LaunchedEffect(state) {
        if (state !is WebsiteState.Loading) {
            isRefreshing = false
        }
    }

    // Expressive 指示器缩放动画
    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(refreshState.distanceFraction).coerceIn(0f, 1f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部搜索栏
            HomePageTopBar(
                onOpenDrawer = onOpenDrawer,
                onSearchClick = onOpenSearchPage,
                onNavigateToPreview = onNavigateToPreview
            )

            // 主内容区（下拉刷新，M3 Expressive 风格指示器）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullToRefresh(
                        state = refreshState,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            viewModel.getHomePage()
                            viewModel.loadAnnouncements(true)
                        }
                    )
            ) {
                when (val currentState = state) {
                    is WebsiteState.Loading -> {
                        if (viewModel.homePageFlow.collectAsState().value !is WebsiteState.Success) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(Modifier.align(Alignment.Center))
                            }
                        }
                    }

                    is WebsiteState.Success -> {
                        val homePage = currentState.info
                        AnimatedContent(
                            targetState = homePage,
                            transitionSpec = {
                                fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                            }
                        ) { page ->
                            HomePageContent(
                                banner = page.banner,
                                announcements = announcements.filter { it.isActive },
                                categories = buildCategoryList(page),
                                onBannerClick = { videoCode ->
                                    videoCode?.let { onOpenVideo(it) }
                                },
                                onAnnouncementClick = { announcement ->
                                    onShowAnnouncementDialog(
                                        announcement.title,
                                        announcement.content,
                                        announcement.imageUrl
                                    )
                                },
                                onCloseAnnouncement = {
                                    putSpValue(
                                        "last_dismiss_time",
                                        System.currentTimeMillis(),
                                        "setting_pref"
                                    )
                                },
                                onMoreClick = { category ->
                                    val params = mutableMapOf<String, String>()
                                    category.genre?.let { params["genre"] = it }
                                    category.sort?.let { params["sort"] = it }
                                    category.tags?.let { params["tags"] = it }
                                    if (params.isNotEmpty()) {
                                        onNavigateToSearchAdvanced(params)
                                    }
                                },
                                onVideoClick = onOpenVideo,
                                onVideoLongClick = onLongPressVideoCopy
                            )
                        }
                    }

                    is WebsiteState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(
                                    R.string.load_failed_with_reason,
                                    currentState.throwable.message.orEmpty()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // M3 Expressive 下拉刷新指示器（顶部居中，缩放动画）
                if (isRefreshing || scaleFraction() > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                scaleX = scaleFraction()
                                scaleY = scaleFraction()
                            }
                            .zIndex(1f)
                    ) {
                        PullToRefreshDefaults.LoadingIndicator(
                            state = refreshState,
                            isRefreshing = isRefreshing,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                }
            }
        }

        // 搜索覆盖层已移除：点击搜索框直接导航到搜索页，SearchScreen 内置完整搜索功能
    }
}

// ─────────────────────────────────────────────
// 辅助函数
// ─────────────────────────────────────────────

/** 将 HomePage 数据转换为分类行列表 */
private fun buildCategoryList(homePage: HomePage): List<HomeCategory> {
    val isAVSite =
        Preferences.baseUrl == HanimeConstants.HANIME_URL[3]

    return listOfNotNull(
        HomeCategory(
            titleRes = if (isAVSite) R.string.latest_av else R.string.latest_hanime,
            genre = if (isAVSite) "日本AV" else "裏番",
            videos = homePage.ecchiAnime
        ),
        HomeCategory(
            titleRes = R.string.latest_release,
            sort = "最新上市",
            videos = homePage.latestRelease
        ),
        HomeCategory(
            titleRes = R.string.latest_upload,
            sort = "最新上傳",
            videos = homePage.latestHanime
        ),
        HomeCategory(
            titleRes = R.string.they_watched,
            sort = "他們在看",
            videos = homePage.watchingNow
        ),
        HomeCategory(
            titleRes = if (isAVSite) R.string.amateur_nomask else R.string.category_instant_noodle,
            genre = if (isAVSite) "素人業餘" else "泡麵番",
            sort = "最新上傳",
            videos = homePage.shortEpisodeAnime
        ),
        HomeCategory(
            titleRes = if (isAVSite) R.string.hd_uncensored else R.string.category_motion_anime,
            genre = if (isAVSite) "高清無碼" else "Motion Anime",
            sort = "最新上傳",
            videos = homePage.motionAnime
        ),
        HomeCategory(
            titleRes = if (isAVSite) R.string.ai_decensored else R.string.category_3d_animation,
            genre = if (isAVSite) "AI解碼" else "3DCG",
            sort = "最新上傳",
            videos = homePage.threeDCG
        ),
        HomeCategory(
            titleRes = if (isAVSite) R.string.china_av else R.string.animation_2_5d,
            genre = if (isAVSite) "國產AV" else "2.5D",
            sort = "最新上傳",
            videos = homePage.twoPointFiveDAnime
        ),
        HomeCategory(
            titleRes = if (isAVSite) R.string.chinese_amateur else R.string.animation_2d,
            genre = if (isAVSite) "國產素人" else "2D動畫",
            sort = "最新上傳",
            videos = homePage.twoDAnime
        ),
        HomeCategory(
            titleRes = if (isAVSite) R.string.chinese_subtitle else R.string.ai_generated,
            genre = if (isAVSite) null else "AI生成",
            tags = if (isAVSite) "中文字幕" else null,
            sort = "最新上傳",
            videos = homePage.aiGenerated
        ),
        HomeCategory(
            titleRes = if (isAVSite) R.string.ranking_today else R.string.mmd,
            genre = if (isAVSite) null else "MMD",
            sort = if (isAVSite) "本日排行" else "最新上傳",
            videos = homePage.mmd
        ),
        HomeCategory(
            titleRes = if (isAVSite) R.string.ranking_this_month else R.string.category_cosplay,
            genre = if (isAVSite) null else "Cosplay",
            sort = if (isAVSite) "本月排行" else "最新上傳",
            videos = homePage.cosplay
        )
    ).filter { it.videos.isNotEmpty() }
}

// ─────────────────────────────────────────────
// Preview（所有预览使用 MaterialTheme，假数据来自 ComposePreviewDataSource）
// ─────────────────────────────────────────────

@Preview(showBackground = true, name = "搜索覆盖层")
@Composable
private fun SearchOverlayPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SearchOverlay(
                visible = true,
                onDismiss = {},
                onNavigateToSearch = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "首页顶栏")
@Composable
private fun HomePageTopBarPreview() {
    MaterialTheme {
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
    MaterialTheme {
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
    MaterialTheme {
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
    MaterialTheme {
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
    MaterialTheme {
        AnnouncementListDialog(
            announcements = fakeAnnouncements,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "视频分类行")
@Composable
private fun CategoryRowPreview() {
    MaterialTheme {
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

@Preview(showBackground = true, name = "首页主内容", device = "spec:width=411dp,height=891dp")
@Composable
private fun HomePageContentPreview() {
    MaterialTheme {
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
