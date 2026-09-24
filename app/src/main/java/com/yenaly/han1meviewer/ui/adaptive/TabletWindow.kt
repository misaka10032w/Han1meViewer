package com.yenaly.han1meviewer.ui.adaptive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Preferences

fun isTabletWindow(screenWidthDp: Int, smallestScreenWidthDp: Int, tabletMode: Boolean): Boolean =
    screenWidthDp >= 600 && (smallestScreenWidthDp >= 600 || tabletMode)

fun shouldUseListDetail(contentWidthDp: Int): Boolean = contentWidthDp >= 840

fun shouldUseYoutubeSplit(
    contentWidthDp: Int,
    contentHeightDp: Int,
    isInPip: Boolean,
    isFullscreen: Boolean,
): Boolean = !isInPip && !isFullscreen && contentWidthDp >= 680 && contentHeightDp >= 480

val LocalContentWidthDp = compositionLocalOf { 0.dp }

val LocalTabletRailVisible = compositionLocalOf { false }

@Composable
fun isTabletWindow(): Boolean {
    val configuration = LocalConfiguration.current
    return isTabletWindow(
        screenWidthDp = configuration.screenWidthDp,
        smallestScreenWidthDp = configuration.smallestScreenWidthDp,
        tabletMode = Preferences.tabletMode,
    )
}

@Composable
fun rememberAvailableWidthDp(): Dp {
    val provided = LocalContentWidthDp.current
    if (provided > 0.dp) return provided
    val density = LocalDensity.current
    val widthPx = LocalWindowInfo.current.containerSize.width
    return with(density) { widthPx.toDp() }
}

@Composable
fun shouldUseListDetail(): Boolean = shouldUseListDetail(rememberAvailableWidthDp().value.toInt())

fun Modifier.tabletReadableWidth(max: Dp): Modifier =
    fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max)
