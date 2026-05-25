package com.yenaly.han1meviewer.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.PullRefreshOverlay
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.screen.home.homepage.HomePageContent
import com.yenaly.han1meviewer.ui.screen.home.homepage.HomePageTopBar
import com.yenaly.han1meviewer.ui.screen.home.homepage.buildCategoryList
import com.yenaly.han1meviewer.ui.screen.home.homepage.toAdvancedSearchParams
import com.yenaly.han1meviewer.ui.screen.home.homepage.toHomePageErrorMessageRes
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.yenaly_libs.utils.putSpValue

/**
 * 首页容器屏幕，负责连接 ViewModel 状态与导航回调。
 *
 * @param viewModel 提供首页数据与公告数据的 ViewModel。
 * @param isDrawerOpen 侧边抽屉是否已打开。
 * @param onOpenDrawer 顶部栏抽屉按钮点击回调。
 * @param onNavigateToPreview 跳转到新番列表的回调。
 * @param onNavigateToSearch 携带搜索关键词跳转搜索页的回调。
 * @param onOpenSearchPage 打开完整搜索页的回调。
 * @param onNavigateToSearchAdvanced 携带高级搜索参数跳转的回调。
 * @param onOpenVideo 打开视频详情的回调。
 * @param onLongPressVideoCopy 长按视频复制分享文案的回调。
 * @param onShowExitDialog 返回键触发退出确认弹窗的回调。
 * @param onShowAnnouncementDialog 点击公告后展示详情的回调。
 * @param modifier 作用于屏幕根布局的修饰符。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePageScreen(
    viewModel: MainViewModel,
    isDrawerOpen: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onOpenSearchPage: () -> Unit,
    onNavigateToSearchAdvanced: (Map<String, String>) -> Unit,
    onOpenVideo: (String) -> Unit,
    onLongPressVideoCopy: (String, String) -> Unit,
    onShowExitDialog: () -> Unit,
    onShowAnnouncementDialog: (Announcement) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.homePageFlow.collectAsStateWithLifecycle()
    val refreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(viewModel.announcements, lifecycleOwner) {
        val observer = Observer<List<Announcement>> { list -> announcements = list }
        viewModel.announcements.observe(lifecycleOwner, observer)
        onDispose { viewModel.announcements.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (viewModel.homePageFlow.value !is WebsiteState.Success) {
            viewModel.getHomePage()
        }
        if (viewModel.announcements.value == null) {
            viewModel.loadAnnouncements(true)
        }
    }

    LaunchedEffect(state) {
        if (state !is WebsiteState.Loading) {
            isRefreshing = false
        }
    }

    BackHandler(enabled = !isDrawerOpen) {
        onShowExitDialog()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomePageTopBar(
                onOpenDrawer = onOpenDrawer,
                onSearchClick = onOpenSearchPage,
                onNavigateToPreview = onNavigateToPreview
            )
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
                    is WebsiteState.Loading -> HomePageLoading()
                    is WebsiteState.Success -> AnimatedContent(
                        targetState = currentState.info,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                        }
                    ) { page ->
                        HomePageContent(
                            banner = page.banner,
                            announcements = announcements.filter { it.isActive },
                            categories = buildCategoryList(page),
                            onBannerClick = { videoCode -> videoCode?.let(onOpenVideo) },
                            onAnnouncementClick = onShowAnnouncementDialog,
                            onCloseAnnouncement = {
                                putSpValue(
                                    "last_dismiss_time",
                                    System.currentTimeMillis(),
                                    "setting_pref"
                                )
                            },
                            onMoreClick = { category ->
                                val params = category.toAdvancedSearchParams()
                                if (params.isNotEmpty()) {
                                    onNavigateToSearchAdvanced(params)
                                }
                            },
                            onVideoClick = onOpenVideo,
                            onVideoLongClick = onLongPressVideoCopy
                        )
                    }

                    is WebsiteState.Error -> HomePageError(
                        message = stringResource(currentState.throwable.toHomePageErrorMessageRes()),
                        onRetry = {
                            isRefreshing = true
                            viewModel.getHomePage()
                            viewModel.loadAnnouncements(true)
                        }
                    )
                }

                PullRefreshOverlay(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomePageLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun HomePageError(
    message: String?,
    onRetry: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = maxHeight * 0.2f)
        ) {
            ErrorContent(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                message = message,
                onRetry = onRetry
            )
        }
    }
}
