package com.yenaly.han1meviewer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.Preferences

@Composable
fun HanimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val preset = ThemeColorPreset.fromKey(Preferences.themeColor)
    val colorScheme = when {
        preset == ThemeColorPreset.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicColorScheme(darkTheme)

        else -> preset.colorScheme(darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content,
    )
}