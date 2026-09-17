package com.yenaly.han1meviewer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.yenaly.han1meviewer.Preferences

@Composable
fun HanimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
