package com.yenaly.han1meviewer.ui.theme

import android.os.Build
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.base.AppDisplayDensity
import com.yenaly.han1meviewer.base.AppFontScale
import com.yenaly.han1meviewer.base.displayRealWidthPx
import com.yenaly.han1meviewer.util.ThemeUtils

@Composable
fun HanimeTheme(
    darkTheme: Boolean = ThemeUtils.isDarkTheme(LocalContext.current),
    content: @Composable () -> Unit,
) {
    val presetColorScheme = when (val preset = ThemeColorPreset.fromKey(Preferences.themeColor)) {
        ThemeColorPreset.SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicColorScheme(darkTheme)
            } else {
                ThemeColorPreset.DEFAULT.colorScheme(darkTheme)
            }
        }

        else -> preset.colorScheme(darkTheme)
    }
    val colorScheme =
        if (darkTheme && Preferences.pureBlackDarkMode) presetColorScheme.toPureBlackColorScheme()
        else presetColorScheme

    val systemDensity = LocalDensity.current
    val fontScale = AppFontScale.resolve(systemDensity.fontScale, Preferences.appFontScale)
    val compatEnabled = Preferences.ignoreSystemDisplaySize && !LocalInspectionMode.current

    // 密度以 AppLocale.wrap 写进 Configuration 的值为准（Dialog 等独立窗口只能靠那层）；
    // 这里再算一次只处理 Activity 不重建的窗口尺寸变化（折叠、分屏、自由窗口），
    // 输入通常已修正过，只会保持不变或继续限宽，不会来回放大。
    // 两种模式走同一条路径，避免结构变化导致 rememberSaveable 状态丢失。
    BoxWithConstraints {
        val resolvedDensity = if (compatEnabled) {
            // 折叠 / 分屏不重建 Activity，以此刷新屏幕物理宽度
            val displayWidthPx = remember(LocalConfiguration.current) {
                displayRealWidthPx()
            }
            AppDisplayDensity.resolve(
                currentDensity = systemDensity.density,
                windowWidthPx = constraints.maxWidth,
                displayWidthPx = displayWidthPx,
                enabled = true,
            )
        } else {
            systemDensity.density
        }
        val appDensity = remember(resolvedDensity, fontScale) {
            Density(resolvedDensity, fontScale)
        }
        AppMaterialTheme(colorScheme = colorScheme, density = appDensity, content = content)
    }
}

/**
 * 用覆盖后的 [density] 提供 `LocalDensity`；对已覆盖的输入再算一次结果不变，嵌套调用安全。
 */
@Composable
private fun AppMaterialTheme(
    colorScheme: ColorScheme,
    density: Density,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDensity provides density) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            shapes = Shapes(),
            content = content,
        )
    }
}

/**
 * 纯黑（OLED）模式：保留主题色的同时，将背景与表面色替换为纯黑，
 * 以在 OLED 屏幕上实现熄屏省电效果。
 */
private fun ColorScheme.toPureBlackColorScheme(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF232323),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF1B1B1B),
    surfaceContainerHighest = Color(0xFF242424),
)
