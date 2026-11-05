package com.yenaly.han1meviewer.ui.screen

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.theme.SpacingLarge
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import com.yenaly.han1meviewer.ui.theme.VideoNormalCardMinWidth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun RetryableImage(
    model: Any,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    retryLimit: Int = 1,
    placeholder: Painter,
    error: Painter,
    contentScale: ContentScale? = ContentScale.Fit
) {
    val context = LocalContext.current
    var retryCount by remember { mutableIntStateOf(0) }
    var currentModel by remember { mutableStateOf(model) }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(currentModel)
            .crossfade(true)
            .listener(
                onError = { _, result ->
                    Log.e("CoilError", "Image load failed", result.throwable)
                }
            ).build(),
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = error,
        modifier = modifier,
        onError = {
            if (retryCount < retryLimit) {
                retryCount++
                currentModel = "$model?retry=$retryCount"
            }
        },
        contentScale = contentScale ?: ContentScale.Fit
    )
}

/**
 * 视频/播放列表网格所在容器的可用宽度。
 *
 * 双栏、多栏布局中应在详情容器外层通过 [CompositionLocalProvider] 提供实际面板宽度；
 * 未提供时回退到整个窗口宽度。
 */
val LocalVideoGridAvailableWidth = compositionLocalOf<Dp?> { null }

/**
 * 双栏详情面板中允许的最小视频卡片宽度。
 *
 * 比全屏单栏的 145dp 略小，避免在 300dp 左右的面板里直接退化成单列，
 * 同时仍能阻止三列或更多列被挤进狭窄区域。
 */
private val SplitPaneMinVideoCardWidth = 120.dp

@Composable
fun getColumnCount(
    itemWidth: Int,
    contentPadding: Dp = SpacingNormal,
    itemSpacing: Dp = SpacingNormal,
): Int {
    val availableWidthDp = currentGridAvailableWidthDp()
    val usableWidth = (availableWidthDp - contentPadding * 2).coerceAtLeast(0.dp)
    return maxOf(
        1,
        ((usableWidth + itemSpacing) / (itemWidth.dp + itemSpacing)).toInt(),
    )
}

@Composable
fun rememberCardResponsiveWidth(
    horizontalPadding: Dp = SpacingLarge,
    itemSpacing: Dp = SpacingNormal
): Pair<Dp, Float> {
    val containerWidth = LocalWindowInfo.current.containerSize.width
    val density = LocalDensity.current
    val currentWidthDp = with(density) { containerWidth.toDp() }

    val isPreview = LocalInspectionMode.current
    val itemsToShow = if (!isPreview) {
        Preferences.horizontalCardCountConfig.countForWidthDp(currentWidthDp.value.toInt())
    } else {
        val estimatedCardWidth = 160.dp
        maxOf(1f, ((currentWidthDp - (horizontalPadding * 2)) / (estimatedCardWidth + itemSpacing)))
    }

    val safeItemsToShow = maxOf(1f, itemsToShow)
    val cardWidth = (currentWidthDp - (horizontalPadding * 2) - (itemSpacing * (safeItemsToShow - 1))) / safeItemsToShow

    return Pair(cardWidth, safeItemsToShow)
}

/**
 * 计算视频网格列数。
 *
 * 设置中的网格列数继续作为个性化偏好，但会结合**实际可用宽度**进行修正：
 * - 手机宽度继续使用自适应列数；
 * - 平板单栏按窗口宽度取偏好列数；
 * - 双栏等窄容器按面板宽度取偏好列数，并按最小卡片宽度限制最大列数，
 *   避免三列甚至更多列被挤进狭窄的详情面板。
 */
@Composable
fun rememberVideoGridColumns(minCardWidth: Dp = VideoNormalCardMinWidth): Int {
    val windowWidthDp = currentWindowWidthDp()
    val availableWidthDp = LocalVideoGridAvailableWidth.current ?: windowWidthDp
    val isPreview = LocalInspectionMode.current
    val isNarrowedContainer = availableWidthDp < windowWidthDp
    val fittingMinWidth = if (isNarrowedContainer) {
        minOf(minCardWidth, SplitPaneMinVideoCardWidth)
    } else {
        minCardWidth
    }
    val maxFittingColumns = videoGridColumnsForWidth(availableWidthDp, fittingMinWidth)

    if (isPreview) return maxOf(2, maxFittingColumns)

    val useTabletColumns = windowWidthDp.value >=
            WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    if (!useTabletColumns) return maxOf(2, maxFittingColumns)

    val preferredColumns = Preferences.searchGridColumnsConfig
        .columnsForWidthDp(availableWidthDp.value.toInt())
        .coerceAtLeast(1)
    return if (isNarrowedContainer) {
        minOf(preferredColumns, maxFittingColumns).coerceAtLeast(1)
    } else {
        preferredColumns
    }
}

@Composable
private fun currentWindowWidthDp(): Dp = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.width.toDp()
}

@Composable
private fun currentGridAvailableWidthDp(): Dp {
    val windowWidthDp = currentWindowWidthDp()
    return LocalVideoGridAvailableWidth.current ?: windowWidthDp
}

private fun videoGridColumnsForWidth(width: Dp, minCardWidth: Dp): Int {
    val usableWidth = (width - SpacingNormal * 2).coerceAtLeast(0.dp)
    return maxOf(
        1,
        ((usableWidth + SpacingNormal) / (minCardWidth + SpacingNormal)).toInt(),
    )
}

@Composable
fun rememberRandomLoadingHint(): String {
    val placeholders = stringArrayResource(R.array.loading_hints)
    return remember(placeholders) { placeholders.random() }
}

/**
 * 等待界面完成两次帧刷新后执行 [block]。
 *
 * 适用于先关闭弹窗、再重建 Activity 或重启应用的场景，
 * 避免阻塞主线程导致弹窗停留在屏幕上。
 */
fun CoroutineScope.runAfterUiSettles(block: () -> Unit) {
    launch {
        withFrameNanos { }
        withFrameNanos { }
        block()
    }
}