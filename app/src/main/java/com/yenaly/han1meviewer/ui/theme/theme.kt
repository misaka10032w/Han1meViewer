package com.yenaly.han1meviewer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    // 主色调 - 柔和的蓝紫色
    primary = Color(0xFF9A86FC),
    onPrimary = Color(0xFF1A1A2E),

    // 主容器 - 更暗的紫色调
    primaryContainer = Color(0xFF4A4458),
    onPrimaryContainer = Color(0xFFE6DFFF),

    // 次要色调 - 柔和的青绿色
    secondary = Color(0xFF7FD6D6),
    onSecondary = Color(0xFF1C2B2B),

    // 次要容器
    secondaryContainer = Color(0xFF3A4B4B),
    onSecondaryContainer = Color(0xFFC2F0F0),

    // 第三色调 - 柔和的珊瑚色
    tertiary = Color(0xFFF4A582),
    onTertiary = Color(0xFF2B1A15),

    // 第三容器
    tertiaryContainer = Color(0xFF5C443A),
    onTertiaryContainer = Color(0xFFFFDBCF),

    // 背景色 - 深灰蓝色（非纯黑）
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),

    // 表面色 - 稍亮于背景的灰色
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),

    // 表面变体 - 用于区分表面层次
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFC7C7C7),

    // 错误色 - 柔和的红色
    error = Color(0xFFCF6679),
    onError = Color(0xFF2D1B1F),

    // 轮廓色 - 用于边框和分割线
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF3A3A3A),

    // 逆色 - 用于特殊情况
    inversePrimary = Color(0xFF6B5B95),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF121212)
)
val LightColorScheme = lightColorScheme(
    // 主色调 - 柔和的蓝紫色
    primary = Color(0xFF5E4FA2),
    onPrimary = Color(0xFFFFFFFF),

    // 主容器 - 浅紫色调
    primaryContainer = Color(0xFFE8DEF8),
    onPrimaryContainer = Color(0xFF1D192B),

    // 次要色调 - 柔和的青绿色
    secondary = Color(0xFF4A7B7B),
    onSecondary = Color(0xFFFFFFFF),     // 白色文字

    // 次要容器
    secondaryContainer = Color(0xFFCCE8E6),
    onSecondaryContainer = Color(0xFF05201C),

    // 第三色调 - 柔和的珊瑚色
    tertiary = Color(0xFF9C4A2C),
    onTertiary = Color(0xFFFFFFFF),

    // 第三容器
    tertiaryContainer = Color(0xFFFFDBCF),
    onTertiaryContainer = Color(0xFF3A0A00),

    // 背景色 - 柔和的灰白色
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1C1E),

    // 表面色 - 纯白色（稍亮于背景）
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),

    // 表面变体 - 用于区分表面层次
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),

    // 错误色 - 柔和的红色
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),

    // 错误容器
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // 轮廓色 - 用于边框和分割线
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFC4C7C5),

    // 逆色 - 用于特殊情况
    inversePrimary = Color(0xFFD0BCFF),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4)
)
@Composable
fun HanimeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme)DarkColorScheme else LightColorScheme,
        typography = Typography(), // 可以自定义
        shapes = Shapes(),
        content = content
    )
}