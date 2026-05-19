package com.yenaly.han1meviewer.ui.screen.home.preview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.exception.HanimeNotFoundException
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.pienization
import com.yenaly.han1meviewer.ui.component.TagChipGroup
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.content.LoadingContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.component.lazy.LazyRow
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.han1meviewer.ui.preview.fakeNewHanimeInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    previewState: WebsiteState<HanimePreview>,
    getCachedPreviewState: (String) -> WebsiteState<HanimePreview>?,
    commentCount: Int,
    onBack: () -> Unit,
    onLoadDate: (String) -> Unit,
    onOpenComment: (String, String) -> Unit,
    onOpenVideo: (String) -> Unit,
) {
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
        val cached = getCachedPreviewState(currentDateCode)
        if (previewState is WebsiteState.Loading && cached is WebsiteState.Success) {
            cached
        } else {
            previewState
        }
    }

    val success = displayState as? WebsiteState.Success
    success?.info?.previewInfo?.getOrNull(selectedIndex)
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

    LaunchedEffect(currentDateCode) {
        onLoadDate(currentDateCode)
        routeState = routeState.copy(selectedIndex = 0)
    }

    LaunchedEffect(selectedIndex, previewInfoList.size) {
        if (previewInfoList.isEmpty()) return@LaunchedEffect
        val targetPage = selectedIndex.coerceIn(previewInfoList.indices)
        if (previewPagerState.currentPage != targetPage) {
            if (!previewPagerState.isScrollInProgress) {
                previewPagerState.scrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(previewPagerState.currentPage, previewInfoList.size) {
        if (previewInfoList.isEmpty()) return@LaunchedEffect
        val pagerPage = previewPagerState.currentPage.coerceIn(previewInfoList.indices)
        if (pagerPage != selectedIndex) {
            routeState = routeState.copy(selectedIndex = pagerPage)
        }
    }

    imageViewerState?.let { viewerState ->
        PreviewImageViewerDialog(
            imageUrls = viewerState.imageUrls,
            initialPage = viewerState.initialPage,
            onDismiss = { imageViewerState = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                AnimatedContent(
                    targetState = currentDateLabel,
                    transitionSpec = {
                        val forward = monthAnimationDirection >= 0
                        (slideInVertically(
                            animationSpec = tween(320, easing = LinearOutSlowInEasing),
                            initialOffsetY = { height -> if (forward) height / 2 else -height / 2 }
                        ) + fadeIn(
                            animationSpec = tween(
                                260,
                                delayMillis = 40,
                                easing = LinearOutSlowInEasing
                            )
                        )) togetherWith
                                (slideOutVertically(
                                    animationSpec = tween(220, easing = FastOutLinearInEasing),
                                    targetOffsetY = { height -> if (forward) -height / 2 else height / 2 }
                                ) + fadeOut(
                                    animationSpec = tween(170, easing = FastOutLinearInEasing)
                                ))
                    },
                    label = "preview_month_title",
                ) { animatedDateLabel ->
                    Text(stringResource(R.string.latest_hanime_list_monthly, animatedDateLabel))
                }
            },
            navigationIcon = {
                FilledIconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            actions = {
                FilledIconButton(onClick = { onOpenComment(currentDateLabel, currentDateCode) }) {
                    BadgedBox(
                        badge = {
                            if (commentCount > 0) {
                                Badge { Text(commentCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_comment_24),
                            contentDescription = stringResource(R.string.comment),
                        )
                    }
                }
            },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AnimatedContent(
                    targetState = monthHeaderState,
                    contentKey = { it.dateCode },
                    transitionSpec = {
                        val forward = monthAnimationDirection >= 0
                        (slideInHorizontally(
                            animationSpec = tween(420, easing = LinearOutSlowInEasing),
                            initialOffsetX = { width -> if (forward) width else -width }
                        ) + fadeIn(
                            animationSpec = tween(
                                320,
                                delayMillis = 70,
                                easing = LinearOutSlowInEasing
                            )
                        )) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(260, easing = FastOutLinearInEasing),
                                    targetOffsetX = { width -> if (forward) -width else width }
                                ) + fadeOut(
                                    animationSpec = tween(
                                        190,
                                        easing = FastOutLinearInEasing
                                    )
                                ))
                    },
                    label = "preview_month_header",
                ) { animatedHeaderState ->
                    PreviewHeaderSection(
                        headerImageUrl = animatedHeaderState.headerImageUrl,
                        prevLabel = animatedHeaderState.prevLabel,
                        nextLabel = animatedHeaderState.nextLabel,
                        canPrev = animatedHeaderState.canPrev,
                        canNext = animatedHeaderState.canNext,
                        onPrev = {
                            monthAnimationDirection = -1
                            routeState = routeState.copy(
                                currentDateCode = shiftMonthCode(animatedHeaderState.dateCode, -1),
                            )
                        },
                        onNext = {
                            monthAnimationDirection = 1
                            routeState = routeState.copy(
                                currentDateCode = shiftMonthCode(animatedHeaderState.dateCode, 1),
                            )
                        },
                    )
                }
            }

            when (displayState) {
                is WebsiteState.Loading -> item {
                    LoadingContent(modifier = Modifier.padding(horizontal = 16.dp))
                }

                is WebsiteState.Error -> item {
                    ErrorContent(
                        title = stringResource(R.string.hanime_list),
                        message = if (displayState.throwable is HanimeNotFoundException) {
                            stringResource(R.string.preview_page_updating)
                        } else {
                            displayState.throwable.pienization.toString()
                        },
                        onRetry = { onLoadDate(currentDateCode) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                is WebsiteState.Success -> {
                    item {
                        PreviewTourRow(
                            latestHanime = displayState.info.latestHanime,
                            selectedIndex = selectedIndex,
                            onSelect = {
                                if (it != previewPagerState.currentPage) {
                                    scope.launch {
                                        previewPagerState.animateScrollToPage(it)
                                    }
                                }
                            },
                        )
                    }

                    item {
                        if (previewInfoList.isEmpty()) {
                            EmptyContent(
                                hint = stringResource(R.string.empty_content),
                                subHint = stringResource(R.string.new_anime_trailers)
                            )
                        } else {
                            HorizontalPager(
                                state = previewPagerState,
                                beyondViewportPageCount = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 620.dp)
                                    .animateContentSize(),
                                verticalAlignment = Alignment.Top,
                            ) { page ->
                                PreviewInfoCard(
                                    previewInfo = previewInfoList[page],
                                    onOpenVideo = { code -> code?.let(onOpenVideo) },
                                    onOpenImage = { index, imageUrls ->
                                        imageViewerState = PreviewImageViewerState(
                                            imageUrls = imageUrls,
                                            initialPage = index,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PreviewRouteUiState(
    val currentDateCode: String = currentDateCode(),
    val selectedIndex: Int = 0,
) {
    companion object {
        val Saver = listSaver<PreviewRouteUiState, Any>(
            save = { listOf(it.currentDateCode, it.selectedIndex) },
            restore = {
                PreviewRouteUiState(
                    currentDateCode = it[0] as String,
                    selectedIndex = it[1] as Int,
                )
            },
        )
    }
}

private data class PreviewMonthHeaderState(
    val dateCode: String,
    val headerImageUrl: String?,
    val prevLabel: String,
    val nextLabel: String,
    val canPrev: Boolean,
    val canNext: Boolean,
)

@Composable
private fun PreviewHeaderSection(
    headerImageUrl: String?,
    prevLabel: String,
    nextLabel: String,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        AsyncImage(
            model = headerImageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(onClick = onPrev, enabled = canPrev, modifier = Modifier.weight(1f)) {
                Text(prevLabel)
            }
            FilledTonalButton(onClick = onNext, enabled = canNext, modifier = Modifier.weight(1f)) {
                Text(nextLabel)
            }
        }
    }
}

@Composable
private fun PreviewTourRow(
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

private suspend fun centerPreviewTourItem(
    listState: LazyListState,
    index: Int,
) {
    if (index < 0) return
    listState.animateScrollToItem(index)
}

@Composable
private fun PreviewInfoCard(
    previewInfo: HanimePreview.PreviewInfo,
    onOpenVideo: (String?) -> Unit,
    onOpenImage: (Int, List<String>) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                AsyncImage(
                    model = previewInfo.coverUrl,
                    contentDescription = previewInfo.videoTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = previewInfo.videoTitle.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!previewInfo.title.isNullOrBlank()) {
                        Text(
                            text = previewInfo.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!previewInfo.brand.isNullOrBlank()) {
                            Text(
                                text = "${stringResource(R.string.brand)}: ${previewInfo.brand}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (!previewInfo.releaseDate.isNullOrBlank()) {
                            Text(
                                text = "${stringResource(R.string.release_date)}: ${previewInfo.releaseDate}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (!previewInfo.videoCode.isNullOrBlank()) {
                        Button(onClick = { onOpenVideo(previewInfo.videoCode) }) {
                            Text(stringResource(R.string.play_trailer))
                        }
                    }
                }
                if (!previewInfo.introduction.isNullOrBlank()) {
                    Text(
                        text = previewInfo.introduction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (previewInfo.tags.isNotEmpty()) {
                    TagChipGroup(tags = previewInfo.tags)
                }

                if (previewInfo.relatedPicsUrl.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(
                            previewInfo.relatedPicsUrl,
                            key = { index, _ -> "$index-${previewInfo.videoCode}" }) { index, url ->
                            ElevatedCard(
                                onClick = { onOpenImage(index, previewInfo.relatedPicsUrl) },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(88.dp),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewImageViewerDialog(
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageUrls.size.coerceAtLeast(1) })
    var isCurrentImageZoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isCurrentImageZoomed,
                    verticalAlignment = Alignment.CenterVertically,
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        AsyncImage(
                            model = imageUrls[page],
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = 0.24f },
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                                        )
                                    )
                                )
                        )
                        AsyncImage(
                            model = imageUrls[page],
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center),
                            contentScale = ContentScale.Fit,
                        )
                        ZoomablePreviewImage(
                            imageUrl = imageUrls[page],
                            isActivePage = pagerState.currentPage == page,
                            onZoomStateChange = { zoomed ->
                                if (pagerState.currentPage == page) {
                                    isCurrentImageZoomed = zoomed
                                }
                            },
                            onDismiss = onDismiss,
                        )
                    }
                }

                LaunchedEffect(pagerState.currentPage) {
                    isCurrentImageZoomed = false
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            CircleShape
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(imageUrls.size) { index ->
                            val selected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 8.dp else 6.dp)
                                    .background(
                                        color = if (selected) Color.White else Color.White.copy(
                                            alpha = 0.45f
                                        ),
                                        shape = CircleShape,
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomablePreviewImage(
    imageUrl: String,
    isActivePage: Boolean,
    onZoomStateChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale, isActivePage) {
        if (isActivePage) {
            onZoomStateChange(scale > 1.01f)
        }
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imageUrl, scale) {
                detectTapGestures(
                    onTap = {
                        if (scale <= 1.01f) onDismiss()
                    },
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2f
                        }
                    },
                )
            }
            .then(
                if (scale > 1.01f) {
                    Modifier.pointerInput(imageUrl) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            offset = if (newScale <= 1f) {
                                Offset.Zero
                            } else {
                                offset + pan
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentScale = ContentScale.Fit,
    )
}

private data class PreviewImageViewerState(
    val imageUrls: List<String>,
    val initialPage: Int,
)

private fun currentDateCode(): String = currentCodeFrom(2024, 1).let {
    val now = java.time.LocalDate.now()
    currentCodeFrom(now.year, now.monthValue)
}

private fun currentCodeFrom(year: Int, month: Int): String = "%04d%02d".format(year, month)

private fun toNormalDateLabel(code: String): String {
    val year = code.substring(0, 4).toInt()
    val month = code.substring(4, 6).toInt()
    return "$year/$month"
}

private fun shiftMonthCode(code: String, delta: Int): String {
    var year = code.substring(0, 4).toInt()
    var month = code.substring(4, 6).toInt() + delta
    while (month < 1) {
        month += 12
        year -= 1
    }
    while (month > 12) {
        month -= 12
        year += 1
    }
    return currentCodeFrom(year, month)
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
        PreviewScreen(
            previewState = WebsiteState.Success(preview),
            getCachedPreviewState = { null },
            commentCount = 12,
            onBack = {},
            onLoadDate = {},
            onOpenComment = { _, _ -> },
            onOpenVideo = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun PreviewScreenEmptyPreview() {
    val preview = HanimePreview(
        headerPicUrl = null,
        hasPrevious = true,
        hasNext = false,
        latestHanime = emptyList(),
        previewInfo = emptyList()
    )
    ComponentPreview {
        PreviewScreen(
            previewState = WebsiteState.Success(preview),
            getCachedPreviewState = { null },
            commentCount = 12,
            onBack = {},
            onLoadDate = {},
            onOpenComment = { _, _ -> },
            onOpenVideo = {},
        )
    }
}
