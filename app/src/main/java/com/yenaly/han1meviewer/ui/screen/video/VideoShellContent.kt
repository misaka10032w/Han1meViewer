package com.yenaly.han1meviewer.ui.screen.video

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.logic.model.HanimeInfo

@Composable
fun VideoShellContent(
    isTabletMode: Boolean,
    isInPipMode: Boolean,
    relatedItems: List<HanimeInfo>,
    onHideRelatedInIntroChange: (Boolean) -> Unit,
    onSideRelatedCollapsedChange: (Boolean) -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    mainHostFactory: () -> View,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isTabletLandscape =
        isTabletMode && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showSideRelated = isTabletLandscape && !isInPipMode
    var isSideRelatedCollapsed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showSideRelated) {
        onHideRelatedInIntroChange(showSideRelated)
    }

    LaunchedEffect(showSideRelated, isSideRelatedCollapsed) {
        onSideRelatedCollapsedChange(showSideRelated && isSideRelatedCollapsed)
    }

    if (showSideRelated) {
        val indicatorWidth = 28.dp
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val sideWidth by animateDpAsState(
                targetValue = if (isSideRelatedCollapsed) indicatorWidth else maxWidth * 0.38f,
                animationSpec = tween(durationMillis = 300),
                label = "sideRelatedWidth",
            )
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                AndroidView(
                    factory = {
                        mainHostFactory().also { view ->
                            (view.parent as? ViewGroup)?.removeView(view)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                Row(
                    modifier = Modifier
                        .width(sideWidth)
                        .fillMaxHeight()
                ) {
                    RelatedCollapseIndicator(
                        collapsed = isSideRelatedCollapsed,
                        onClick = { isSideRelatedCollapsed = !isSideRelatedCollapsed },
                        modifier = Modifier
                            .width(indicatorWidth)
                            .fillMaxHeight(),
                    )
                    if (!isSideRelatedCollapsed) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            RelatedVideosSection(
                                videos = relatedItems,
                                onOpenVideo = onOpenVideo,
                            )
                        }
                    }
                }
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

@Composable
private fun RelatedCollapseIndicator(
    collapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (collapsed) {
                Icons.AutoMirrored.Filled.KeyboardArrowLeft
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        )
    }
}
