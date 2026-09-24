package com.yenaly.han1meviewer.ui.screen.home.homepage

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.ui.adaptive.isTabletWindow
import com.yenaly.han1meviewer.ui.adaptive.rememberAvailableWidthDp

/**
 * 首页 Hero 区域（Banner 与公告）在平板模式下的尺寸。
 */
sealed interface HomeHeroSpec {
    /**
     * 默认布局：Banner 按 16:9 占满可用宽度，公告依次显示在下方。
     */
    data object Default : HomeHeroSpec

    /**
     * 大屏布局：Banner 保持 16:9，但高度受窗口高度约束。
     *
     * @param bannerSize Banner 渲染尺寸。
     * @param panelWidth 公告面板宽度，非空时公告显示在 Banner 右侧；为空时公告仍在下方。
     */
    data class Constrained(
        val bannerSize: DpSize,
        val panelWidth: Dp? = null
    ) : HomeHeroSpec
}

/** Banner 与公告面板之间的间距。 */
val HomeHeroPanelSpacing = 12.dp

/** Hero 区域两侧的留白。 */
val HomeHeroHorizontalPadding = 12.dp

/** 大屏下 Banner 高度占窗口高度的最大比例。 */
private const val HeroHeightWindowRatio = 0.42f

/** 公告面板可用的最小宽度，剩余宽度不足时公告回落到 Banner 下方。 */
private val HeroPanelMinWidth = 260.dp

/** 公告面板的宽度上限，避免在超宽屏幕上被过度拉伸。 */
private val HeroPanelMaxWidth = 420.dp

/**
 * 计算首页 Hero 区域的渲染尺寸。
 *
 * 手机竖屏下 16:9 的 Banner 不会占据过多高度，保持占满宽度即可。平板模式（或大屏横屏）下
 * 16:9 的 Banner 高度会随宽度线性增长，在平板上足以撑满整个屏幕，因此此时将其高度限制为
 * 窗口高度的 [HeroHeightWindowRatio]；Banner 仍保持 16:9 不裁剪，收缩后空出的横向空间
 * 交给右侧内容（公告或待播队列），避免出现大片空白。竖屏平板上 Banner 本就不到窗口高度的
 * 四成，不会触发该布局。
 *
 * @param hasSideContent 是否有内容需要占用 Banner 右侧的空间。
 */
@Composable
fun rememberHomeHeroSpec(hasSideContent: Boolean): HomeHeroSpec {
    val isPreview = LocalInspectionMode.current
    if (!isPreview && !Preferences.tabletMode && !isTabletWindow()) return HomeHeroSpec.Default

    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val containerWidth = rememberAvailableWidthDp()
    val containerHeight = with(density) { containerSize.height.toDp() }

    val naturalHeight = containerWidth * 9f / 16f
    val heroHeight = minOf(naturalHeight, containerHeight * HeroHeightWindowRatio)
    if (heroHeight >= naturalHeight) return HomeHeroSpec.Default

    val bannerWidth = heroHeight * 16f / 9f
    val remainingWidth = containerWidth - bannerWidth -
            HomeHeroHorizontalPadding * 2 - HomeHeroPanelSpacing
    val panelWidth = when {
        !hasSideContent -> null
        remainingWidth < HeroPanelMinWidth -> null
        else -> remainingWidth.coerceAtMost(HeroPanelMaxWidth)
    }
    return HomeHeroSpec.Constrained(DpSize(bannerWidth, heroHeight), panelWidth)
}
