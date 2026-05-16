package com.yenaly.han1meviewer.ui.screen.video

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.logic.model.HanimeInfo

@Composable
fun VideoShellContent(
    isTabletMode: Boolean,
    relatedItems: List<HanimeInfo>,
    onHideRelatedInIntroChange: (Boolean) -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    mainHostFactory: () -> View,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isTabletLandscape = isTabletMode && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isTabletLandscape) {
        onHideRelatedInIntroChange(isTabletLandscape)
    }

    if (isTabletLandscape) {
        Row(modifier = modifier.fillMaxSize().statusBarsPadding()) {
            AndroidView(
                factory = {
                    mainHostFactory().also { view ->
                        (view.parent as? ViewGroup)?.removeView(view)
                    }
                },
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
        AndroidView(
            factory = {
                mainHostFactory().also { view ->
                    (view.parent as? ViewGroup)?.removeView(view)
                }
            },
            modifier = modifier.fillMaxSize(),
        )
    }
}
