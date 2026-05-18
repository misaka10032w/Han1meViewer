package com.yenaly.han1meviewer.ui.screen.video

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.logic.model.HanimeInfo

@Composable
fun VideoShellContent(
    modifier: Modifier = Modifier,
    isTabletMode: Boolean,
    relatedItems: List<HanimeInfo>,
    onHideRelatedInIntroChange: (Boolean) -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    playerView: View,
    playerHeightDp: Dp = 250.dp,
    isPlaying: Boolean = false,
    onPlayClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onFullscreenClick: () -> Unit = {},
    progress: Float = 0f,
    currentTime: String = "",
    totalTime: String = "",
    onProgressChange: (Float) -> Unit = {},
    tabsContent: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isTabletLandscape =
        isTabletMode && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isTabletLandscape) {
        onHideRelatedInIntroChange(isTabletLandscape)
    }

    if (isTabletLandscape) {
        Row(modifier = modifier.fillMaxSize().statusBarsPadding()) {
            AndroidView(
                factory = { playerView },
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                RelatedVideosSection(
                    videos = relatedItems,
                    onOpenVideo = onOpenVideo,
                )
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
            VideoPlayerUi(
                playerView = playerView,
                isPlaying = isPlaying,
                progress = progress,
                currentTime = currentTime,
                totalTime = totalTime,
                onProgressChange = onProgressChange,
                showControls = true,
                onPlayClick = onPlayClick,
                onBackClick = onBackClick,
                onFullscreenClick = onFullscreenClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(playerHeightDp),
            )
            tabsContent()
        }
    }
}
