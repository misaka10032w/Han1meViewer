package com.yenaly.han1meviewer.ui.screen.video

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.logic.model.HanimeInfo

/**
 * 播放页最外层壳布局。
 *
 * 手机/竖屏：整棵 View 结构（CoordinatorLayout：播放器 AppBar + tabs）直接铺满。
 *
 * 平板横屏（分栏，无论开关与否）：标签页整体移至右栏（可折叠），与原先
 * 「右栏相关影片 + 左栏 tabs」调换位置；左栏按「相关影片独立标签页」开关区分：
 * - 开关开启：相关影片进标签页，左栏仅剩播放器并垂直居中；
 * - 开关关闭：左栏为播放器（上）+ 相关影片列表（下）。
 *
 * 分栏左栏的播放器区域衬黑色背景，与视频画面融合，避免主题色衬底显得突兀。
 */
@Composable
fun VideoShellContent(
    isExpandedLayout: Boolean,
    isInPipMode: Boolean,
    relatedItems: List<HanimeInfo>,
    relatedAsTab: Boolean,
    onHideRelatedInIntroChange: (Boolean) -> Unit,
    onSidePaneCollapsedChange: (Boolean) -> Unit,
    onTabsBesidePlayerChange: (Boolean) -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    mainHostFactory: () -> View,
    playerHostFactory: () -> View,
    tabsHostFactory: () -> View,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isTabletLandscape =
        isExpandedLayout && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showSplitLayout = isTabletLandscape && !isInPipMode
    var isSidePaneCollapsed by rememberSaveable { mutableStateOf(false) }

    // 简介底部不再内嵌相关影片列表的情况：
    // 1. 开关开启：相关影片已作为独立标签页展示；
    // 2. 平板横屏：相关影片要么在标签页中（开关开），要么在播放器下方（开关关）。
    LaunchedEffect(relatedAsTab, showSplitLayout) {
        onHideRelatedInIntroChange(relatedAsTab || showSplitLayout)
    }

    LaunchedEffect(showSplitLayout, isSidePaneCollapsed) {
        onSidePaneCollapsedChange(showSplitLayout && isSidePaneCollapsed)
    }

    // 分栏时 tabs 位于播放器右侧而非下方，「避让 AppBar」的底部内边距应清零。
    LaunchedEffect(showSplitLayout) {
        onTabsBesidePlayerChange(showSplitLayout)
    }

    if (showSplitLayout) {
        val indicatorWidth = 28.dp
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val sideWidth by animateDpAsState(
                targetValue = if (isSidePaneCollapsed) indicatorWidth else maxWidth * 0.38f,
                animationSpec = tween(durationMillis = 300),
                label = "sideTabsWidth",
            )
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                if (relatedAsTab) {
                    // 开关开启：左栏仅剩播放器，垂直居中；
                    // 播放器四周衬黑色，避免主题色背景使播放器显得突兀
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        AndroidView(
                            factory = { playerHostFactory() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    // 开关关闭：左栏为播放器 + 下方相关影片列表
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black),
                        ) {
                            AndroidView(
                                factory = { playerHostFactory() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (relatedItems.isNotEmpty()) {
                                RelatedVideosSection(
                                    videos = relatedItems,
                                    onOpenVideo = onOpenVideo,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .width(sideWidth)
                        .fillMaxHeight()
                ) {
                    RelatedCollapseIndicator(
                        collapsed = isSidePaneCollapsed,
                        onClick = { isSidePaneCollapsed = !isSidePaneCollapsed },
                        modifier = Modifier
                            .width(indicatorWidth)
                            .fillMaxHeight(),
                    )
                    if (!isSidePaneCollapsed) {
                        AndroidView(
                            factory = { tabsHostFactory() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
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
