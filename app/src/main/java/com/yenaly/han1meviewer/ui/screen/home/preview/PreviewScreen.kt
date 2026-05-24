package com.yenaly.han1meviewer.ui.screen.home.preview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yenaly.han1meviewer.PREVIEW_COMMENT_PREFIX
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.han1meviewer.ui.preview.fakeNewHanimeInfo
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.han1meviewer.ui.viewmodel.PreviewViewModel
import com.yenaly.han1meviewer.ui.navigation.main.shiftMonthCodeForPreview
import kotlinx.coroutines.launch

/**
 * 预览页面 Screen 层。
 *
 * 持有 [PreviewViewModel] 和 [CommentViewModel]，负责状态收集、图片预加载、
 * 评论预取器生命周期、月份翻页编排和图片查看器 UI 管理。
 * 渲染委托给 [PreviewContent]。
 *
 * @param onBack 返回回调
 * @param onNavigateToPreviewComment 打开预览评论页回调
 * @param onNavigateToVideo 打开视频详情回调
 * @param previewViewModel 预览 ViewModel
 * @param commentViewModel 评论 ViewModel（需 Activity scope）
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onBack: () -> Unit,
    onNavigateToPreviewComment: (String, String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
    previewViewModel: PreviewViewModel,
    commentViewModel: CommentViewModel,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    val previewState = previewViewModel.previewFlow.collectAsStateWithLifecycle().value
    val commentCount = PreviewCommentPrefetcher.here(commentViewModel)
        .commentFlow
        .collectAsStateWithLifecycle()
        .value
        .size

    fun preloadImages(preview: HanimePreview?) {
        if (preview == null) return
        buildList {
            preview.headerPicUrl?.let(::add)
            addAll(preview.latestHanime.map { it.coverUrl })
            addAll(preview.previewInfo.mapNotNull { it.coverUrl })
        }.distinct().forEach { url ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build()
            )
        }
    }

    LaunchedEffect(previewState) {
        when (previewState) {
            is WebsiteState.Success -> preloadImages(previewState.info)
            else -> Unit
        }
    }

    DisposableEffect(Unit) {
        PreviewCommentPrefetcher.here(commentViewModel)
            .tag(PreviewCommentPrefetcher.Scope.PREVIEW_ACTIVITY)
        onDispose {
            PreviewCommentPrefetcher.bye(PreviewCommentPrefetcher.Scope.PREVIEW_ACTIVITY)
        }
    }

    var routeState by rememberSaveable(stateSaver = PreviewRouteUiState.Saver) {
        mutableStateOf(PreviewRouteUiState())
    }
    var imageViewerState by remember { mutableStateOf<PreviewImageViewerState?>(null) }
    var monthAnimationDirection by remember { mutableIntStateOf(1) }
    val currentDateCode = routeState.currentDateCode
    val selectedIndex = routeState.selectedIndex

    val currentDateLabel = remember(currentDateCode) { toNormalDateLabel(currentDateCode) }
    val prevDateCode = remember(currentDateCode) { shiftMonthCode(currentDateCode, -1) }
    val nextDateCode = remember(currentDateCode) { shiftMonthCode(currentDateCode, 1) }
    val prevDateLabel = remember(prevDateCode) { toNormalDateLabel(prevDateCode) }
    val nextDateLabel = remember(nextDateCode) { toNormalDateLabel(nextDateCode) }

    val displayState = remember(currentDateCode, previewState) {
        val cached = previewViewModel.getCachedPreview(currentDateCode)
        if (previewState is WebsiteState.Loading && cached is WebsiteState.Success) {
            cached
        } else {
            previewState
        }
    }

    val success = displayState as? WebsiteState.Success
    val previewInfoList = success?.info?.previewInfo.orEmpty()
    val previewPagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { previewInfoList.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()

    val canPrev = when (displayState) {
        is WebsiteState.Loading -> false
        is WebsiteState.Success -> displayState.info.hasPrevious
        is WebsiteState.Error -> true
    }
    val canNext = when (displayState) {
        is WebsiteState.Success -> displayState.info.hasNext
        else -> false
    }
    val monthHeaderState = remember(
        currentDateCode,
        success?.info?.headerPicUrl,
        prevDateLabel,
        nextDateLabel,
        canPrev,
        canNext,
    ) {
        PreviewMonthHeaderState(
            dateCode = currentDateCode,
            headerImageUrl = success?.info?.headerPicUrl,
            prevLabel = prevDateLabel,
            nextLabel = nextDateLabel,
            canPrev = canPrev,
            canNext = canNext,
        )
    }

    val uiState = PreviewUiState(
        routeState = routeState,
        currentDateLabel = currentDateLabel,
        prevDateLabel = prevDateLabel,
        nextDateLabel = nextDateLabel,
        monthAnimationDirection = monthAnimationDirection,
        displayState = displayState,
        commentCount = commentCount,
        canPrev = canPrev,
        canNext = canNext,
        monthHeaderState = monthHeaderState,
        imageViewerState = imageViewerState,
    )

    val handleEvent: (PreviewEvent) -> Unit = { event ->
        when (event) {
            PreviewEvent.OnBack -> onBack()
            is PreviewEvent.OnPrevMonth -> {
                monthAnimationDirection = -1
                routeState = routeState.copy(
                    currentDateCode = shiftMonthCode(event.fromDateCode, -1),
                )
            }
            is PreviewEvent.OnNextMonth -> {
                monthAnimationDirection = 1
                routeState = routeState.copy(
                    currentDateCode = shiftMonthCode(event.fromDateCode, 1),
                )
            }
            is PreviewEvent.OnSelectTourItem -> {
                if (event.index != previewPagerState.currentPage) {
                    scope.launch {
                        previewPagerState.animateScrollToPage(event.index)
                    }
                }
            }
            is PreviewEvent.OnOpenImage -> {
                imageViewerState = PreviewImageViewerState(
                    imageUrls = event.imageUrls,
                    initialPage = event.index,
                )
            }
            PreviewEvent.OnDismissImageViewer -> { imageViewerState = null }
            is PreviewEvent.OnOpenVideo -> event.videoCode?.let(onNavigateToVideo)
            is PreviewEvent.OnOpenComment -> onNavigateToPreviewComment(event.label, event.dateCode)
            PreviewEvent.OnRetryLoad -> {
                val code = uiState.routeState.currentDateCode
                previewViewModel.getHanimePreview(code)
                previewViewModel.preloadPreview(shiftMonthCodeForPreview(code, -1))
                previewViewModel.preloadPreview(shiftMonthCodeForPreview(code, 1))
                PreviewCommentPrefetcher.here(commentViewModel).fetch(PREVIEW_COMMENT_PREFIX, code)
            }
        }
    }

    LaunchedEffect(currentDateCode) {
        previewViewModel.getHanimePreview(currentDateCode)
        previewViewModel.preloadPreview(shiftMonthCodeForPreview(currentDateCode, -1))
        previewViewModel.preloadPreview(shiftMonthCodeForPreview(currentDateCode, 1))
        PreviewCommentPrefetcher.here(commentViewModel).fetch(PREVIEW_COMMENT_PREFIX, currentDateCode)
        routeState = routeState.copy(selectedIndex = 0)
    }

    LaunchedEffect(uiState.routeState.selectedIndex, previewInfoList.size) {
        if (previewInfoList.isEmpty()) return@LaunchedEffect
        val targetPage = uiState.routeState.selectedIndex.coerceIn(previewInfoList.indices)
        if (previewPagerState.currentPage != targetPage) {
            if (!previewPagerState.isScrollInProgress) {
                previewPagerState.scrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(previewPagerState.currentPage, previewInfoList.size) {
        if (previewInfoList.isEmpty()) return@LaunchedEffect
        val pagerPage = previewPagerState.currentPage.coerceIn(previewInfoList.indices)
        if (pagerPage != uiState.routeState.selectedIndex) {
            routeState = routeState.copy(selectedIndex = pagerPage)
        }
    }

    uiState.imageViewerState?.let { viewerState ->
        PreviewImageViewerDialog(
            imageUrls = viewerState.imageUrls,
            initialPage = viewerState.initialPage,
            onDismiss = { handleEvent(PreviewEvent.OnDismissImageViewer) },
        )
    }

    PreviewContent(
        uiState = uiState,
        onEvent = handleEvent,
        previewPagerState = previewPagerState,
        previewInfoList = previewInfoList,
    )
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun PreviewScreenPreview() {
    val preview = HanimePreview(
        headerPicUrl = fakeHomePageVideos.first().coverUrl,
        hasPrevious = true,
        hasNext = true,
        latestHanime = fakeHomePageVideos.take(5),
        previewInfo = fakeNewHanimeInfo
    )
    ComponentPreview {
        PreviewContent(
            uiState = PreviewUiState(
                currentDateLabel = "2024/1",
                prevDateLabel = "2023/12",
                nextDateLabel = "2024/2",
                displayState = WebsiteState.Success(preview),
                commentCount = 12,
                monthHeaderState = PreviewMonthHeaderState(
                    dateCode = "202401",
                    headerImageUrl = preview.headerPicUrl,
                    prevLabel = "2023/12",
                    nextLabel = "2024/2",
                    canPrev = true,
                    canNext = true,
                ),
                routeState = PreviewRouteUiState("202401", 0),
            ),
            onEvent = {},
            previewPagerState = rememberPagerState { 1 },
            previewInfoList = preview.previewInfo,
        )
    }
}
