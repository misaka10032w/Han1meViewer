package com.yenaly.han1meviewer.ui.screen.video

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.ui.adaptive.shouldUseYoutubeSplit

@Composable
fun VideoShellContent(
    isInPipMode: Boolean,
    isFullscreen: Boolean,
    playlistItems: List<HanimeInfo>,
    relatedItems: List<HanimeInfo>,
    childCommentId: String?,
    onHideRelatedInIntroChange: (Boolean) -> Unit,
    onHidePlaylistInIntroChange: (Boolean) -> Unit,
    onSideRelatedCollapsedChange: (Boolean) -> Unit,
    onYoutubeSplitChange: (Boolean) -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    onPrepareSplit: (Int) -> Unit,
    onPrepareCoordinator: () -> Unit,
    onPrepareFullscreen: () -> Unit,
    mainHostFactory: () -> View,
    playerHostFactory: () -> View,
    tabsHostFactory: () -> View,
    fullscreenHostFactory: () -> View,
    childCommentPane: @Composable (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val showSplit = shouldUseYoutubeSplit(
            contentWidthDp = maxWidth.value.toInt(),
            contentHeightDp = maxHeight.value.toInt(),
            isInPip = isInPipMode,
            isFullscreen = isFullscreen,
        )
        LaunchedEffect(showSplit, playlistItems.isNotEmpty()) {
            onHideRelatedInIntroChange(showSplit)
            onHidePlaylistInIntroChange(showSplit && playlistItems.isNotEmpty())
            onSideRelatedCollapsedChange(false)
            onYoutubeSplitChange(showSplit)
        }
        val rightWidth = minOf(maxWidth * 0.28f, 320.dp).coerceAtLeast(260.dp)
        val leftWidth = (maxWidth - rightWidth - 1.dp).coerceAtLeast(0.dp)
        val naturalHeight = leftWidth * 9f / 16f
        val playerHeight = minOf(naturalHeight, (maxHeight - 112.dp).coerceAtLeast(0.dp))
        val density = LocalDensity.current
        val splitPlayerHeightPx = with(density) { playerHeight.roundToPx() }
        LaunchedEffect(showSplit, isFullscreen, splitPlayerHeightPx) {
            when {
                isFullscreen -> onPrepareFullscreen()
                showSplit -> onPrepareSplit(splitPlayerHeightPx)
                else -> onPrepareCoordinator()
            }
        }

        if (isFullscreen) {
            AndroidView(
                factory = { fullscreenHostFactory().detachFromParent() },
                modifier = Modifier.fillMaxSize(),
            )
            return@BoxWithConstraints
        }

        if (!showSplit) {
            AndroidView(
                factory = { mainHostFactory().detachFromParent() },
                modifier = Modifier.fillMaxSize(),
            )
            return@BoxWithConstraints
        }

        val playerWidth = leftWidth

        Row(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            AndroidView(
                factory = { mainHostFactory().detachFromParent() },
                modifier = Modifier
                    .width(playerWidth)
                    .fillMaxHeight(),
            )
            VerticalDivider()
            Box(
                modifier = Modifier
                    .width(rightWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                if (childCommentId != null) {
                    childCommentPane(childCommentId)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (playlistItems.isNotEmpty()) {
                            RelatedVideosSection(
                                videos = playlistItems,
                                onOpenVideo = onOpenVideo,
                                titleRes = R.string.series_video,
                                horizontalPadding = 0.dp,
                                forcedColumns = 2,
                            )
                        }
                        if (relatedItems.isNotEmpty()) {
                            RelatedVideosSection(
                                videos = relatedItems,
                                onOpenVideo = onOpenVideo,
                                horizontalPadding = 0.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun View.detachFromParent(): View {
    (parent as? ViewGroup)?.removeView(this)
    return this
}

