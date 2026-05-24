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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.exception.HanimeNotFoundException
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.pienization
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.content.LoadingContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PreviewContent(
    uiState: PreviewUiState,
    onEvent: (PreviewEvent) -> Unit,
    previewPagerState: PagerState,
    previewInfoList: List<HanimePreview.PreviewInfo>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                AnimatedContent(
                    targetState = uiState.currentDateLabel,
                    transitionSpec = {
                        val forward = uiState.monthAnimationDirection >= 0
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
                FilledIconButton(onClick = { onEvent(PreviewEvent.OnBack) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            actions = {
                FilledIconButton(onClick = {
                    onEvent(PreviewEvent.OnOpenComment(
                        uiState.currentDateLabel,
                        uiState.routeState.currentDateCode
                    ))
                }) {
                    BadgedBox(
                        badge = {
                            if (uiState.commentCount > 0) {
                                Badge { Text(uiState.commentCount.toString()) }
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
                    targetState = uiState.monthHeaderState,
                    contentKey = { it.dateCode },
                    transitionSpec = {
                        val forward = uiState.monthAnimationDirection >= 0
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
                        onPrev = { onEvent(PreviewEvent.OnPrevMonth(animatedHeaderState.dateCode)) },
                        onNext = { onEvent(PreviewEvent.OnNextMonth(animatedHeaderState.dateCode)) },
                    )
                }
            }

            when (uiState.displayState) {
                is WebsiteState.Loading -> item {
                    LoadingContent(modifier = Modifier.padding(horizontal = 16.dp))
                }

                is WebsiteState.Error -> item {
                    ErrorContent(
                        title = stringResource(R.string.hanime_list),
                        message = if (uiState.displayState.throwable is HanimeNotFoundException) {
                            stringResource(R.string.preview_page_updating)
                        } else {
                            uiState.displayState.throwable.pienization.toString()
                        },
                        onRetry = { onEvent(PreviewEvent.OnRetryLoad) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                is WebsiteState.Success -> {
                    item {
                        PreviewTourRow(
                            latestHanime = uiState.displayState.info.latestHanime,
                            selectedIndex = uiState.routeState.selectedIndex,
                            onSelect = { onEvent(PreviewEvent.OnSelectTourItem(it)) },
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
                                    onOpenVideo = { code ->
                                        onEvent(PreviewEvent.OnOpenVideo(code))
                                    },
                                    onOpenImage = { index, imageUrls ->
                                        onEvent(PreviewEvent.OnOpenImage(index, imageUrls))
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
