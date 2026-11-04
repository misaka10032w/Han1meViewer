package com.yenaly.han1meviewer.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.ThemeUtils

/**
 * 当前 Compose 主题的主色桥接，供仍使用 View/Adapter 的组件读取主题色。
 */
object AppThemeState {
    /** 当前主题主色的 ARGB 值，0 表示尚未初始化。 */
    var primaryColorArgb: Int = 0
        internal set
}

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

    SideEffect {
        AppThemeState.primaryColorArgb = colorScheme.primary.toArgb()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content,
    )
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
