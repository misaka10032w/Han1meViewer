package com.yenaly.han1meviewer.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.R

enum class ThemeColorPreset(
    val displayNameRes: Int,
    val key: String,
) {
    SYSTEM(R.string.theme_color_system, "system"),
    DEFAULT(R.string.theme_color_default, "default"),
    BLUE(R.string.theme_color_blue, "blue"),
    LIGHT_GREEN(R.string.theme_color_light_green, "light_green"),
    PURPLE(R.string.theme_color_purple, "purple"),
    PINK(R.string.theme_color_pink, "pink"),
    ORANGE(R.string.theme_color_orange,"orange"),
    TEAL(R.string.theme_color_teal,"teal"),
    YELLOW(R.string.theme_color_yellow,"yellow"),
    HIGH_CONTRAST(R.string.theme_color_high_contrast, "high_contrast");

    companion object {
        fun fromKey(key: String?): ThemeColorPreset =
            entries.find { it.key == key } ?: DEFAULT
    }
}

private val redLight = lightColorScheme(
    primary = Color(0xFFBC0100), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEB0000), onPrimaryContainer = Color(0xFFFFFBFF),
    secondary = Color(0xFFB72114), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFF5541), onSecondaryContainer = Color(0xFF5C0000),
    tertiary = Color(0xFF805200), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA16900), onTertiaryContainer = Color(0xFFFFFBFF),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F6), onBackground = Color(0xFF2B1613),
    surface = Color(0xFFFFF8F6), onSurface = Color(0xFF2B1613),
    surfaceVariant = Color(0xFFFFDAD4), onSurfaceVariant = Color(0xFF603E39),
    outline = Color(0xFF956D67), outlineVariant = Color(0xFFEBBBB4),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFF422A27),
    inverseOnSurface = Color(0xFFFFEDEA), inversePrimary = Color(0xFFFFB4A8),
    surfaceDim = Color(0xFFF8D1CB), surfaceBright = Color(0xFFFFF8F6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0EE),
    surfaceContainer = Color(0xFFFFE9E6),
    surfaceContainerHigh = Color(0xFFFFE2DD),
    surfaceContainerHighest = Color(0xFFFFDAD4),
)

private val redDark = darkColorScheme(
    primary = Color(0xFFFFB4A8), onPrimary = Color(0xFF690100),
    primaryContainer = Color(0xFFFF5540), onPrimaryContainer = Color(0xFF360000),
    secondary = Color(0xFFFFB4A8), onSecondary = Color(0xFF690100),
    secondaryContainer = Color(0xFF970100), onSecondaryContainer = Color(0xFFFF9F90),
    tertiary = Color(0xFFFFB956), onTertiary = Color(0xFF462B00),
    tertiaryContainer = Color(0xFFC4831A), onTertiaryContainer = Color(0xFF211200),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF210E0B), onBackground = Color(0xFFFFDAD4),
    surface = Color(0xFF210E0B), onSurface = Color(0xFFFFDAD4),
    surfaceVariant = Color(0xFF603E39), onSurfaceVariant = Color(0xFFEBBBB4),
    outline = Color(0xFFB18780), outlineVariant = Color(0xFF603E39),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFFFFDAD4),
    inverseOnSurface = Color(0xFF422A27), inversePrimary = Color(0xFFC00100),
    surfaceDim = Color(0xFF210E0B), surfaceBright = Color(0xFF4C332F),
    surfaceContainerLowest = Color(0xFF1B0907),
    surfaceContainerLow = Color(0xFF2B1613),
    surfaceContainer = Color(0xFF2F1A17),
    surfaceContainerHigh = Color(0xFF3B2420),
    surfaceContainerHighest = Color(0xFF472F2B),
)

// ── Blue preset ──────────────────────────────────────────────
private val blueLight = lightColorScheme(
    primary = Color(0xFF005FAE), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4D95EE), onPrimaryContainer = Color(0xFF002C57),
    secondary = Color(0xFF485F82), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBED6FE), onSecondaryContainer = Color(0xFF455D7F),
    tertiary = Color(0xFF87409A), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC276D4), onTertiaryContainer = Color(0xFF4E0063),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9F9FF), onBackground = Color(0xFF191C21),
    surface = Color(0xFFF9F9FF), onSurface = Color(0xFF191C21),
    surfaceVariant = Color(0xFFDDE2F0), onSurfaceVariant = Color(0xFF414752),
    outline = Color(0xFF717783), outlineVariant = Color(0xFFC1C6D4),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFF2D3036),
    inverseOnSurface = Color(0xFFEFF0F8), inversePrimary = Color(0xFFA5C8FF),
    surfaceDim = Color(0xFFD8DAE1), surfaceBright = Color(0xFFF9F9FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FB),
    surfaceContainer = Color(0xFFECEDF5),
    surfaceContainerHigh = Color(0xFFE6E8F0),
    surfaceContainerHighest = Color(0xFFE1E2EA),
)

private val blueDark = darkColorScheme(
    primary = Color(0xFFA5C8FF), onPrimary = Color(0xFF00315E),
    primaryContainer = Color(0xFF4D95EE), onPrimaryContainer = Color(0xFF002C57),
    secondary = Color(0xFFB0C8EF), onSecondary = Color(0xFF183151),
    secondaryContainer = Color(0xFF304869), onSecondaryContainer = Color(0xFF9FB6DD),
    tertiary = Color(0xFFF2AFFF), onTertiary = Color(0xFF530768),
    tertiaryContainer = Color(0xFFC276D4), onTertiaryContainer = Color(0xFF4E0063),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101319), onBackground = Color(0xFFE1E2EA),
    surface = Color(0xFF101319), onSurface = Color(0xFFE1E2EA),
    surfaceVariant = Color(0xFF414752), onSurfaceVariant = Color(0xFFC1C6D4),
    outline = Color(0xFF8B919D), outlineVariant = Color(0xFF414752),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFFE1E2EA),
    inverseOnSurface = Color(0xFF2D3036), inversePrimary = Color(0xFF005FAE),
    surfaceDim = Color(0xFF101319), surfaceBright = Color(0xFF36393F),
    surfaceContainerLowest = Color(0xFF0B0E14),
    surfaceContainerLow = Color(0xFF191C21),
    surfaceContainer = Color(0xFF1D2025),
    surfaceContainerHigh = Color(0xFF272A30),
    surfaceContainerHighest = Color(0xFF32353B),
)

// ── Purple preset ────────────────────────────────────────────
private val purpleLight = lightColorScheme(
    primary = Color(0xFF7A1994), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9638AE), onPrimaryContainer = Color(0xFFFCD2FF),
    secondary = Color(0xFF794F80), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF9C6FF), onSecondaryContainer = Color(0xFF774E7F),
    tertiary = Color(0xFF911448), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB13060), onTertiaryContainer = Color(0xFFFFD4DD),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF7FA), onBackground = Color(0xFF201921),
    surface = Color(0xFFFFF7FA), onSurface = Color(0xFF201921),
    surfaceVariant = Color(0xFFF0DDEE), onSurfaceVariant = Color(0xFF4F4350),
    outline = Color(0xFF817281), outlineVariant = Color(0xFFD3C1D2),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFF362E36),
    inverseOnSurface = Color(0xFFFBEDF8), inversePrimary = Color(0xFFF4AEFF),
    surfaceDim = Color(0xFFE4D6E1), surfaceBright = Color(0xFFFFF7FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFEF0FA),
    surfaceContainer = Color(0xFFF8EAF5),
    surfaceContainerHigh = Color(0xFFF2E4EF),
    surfaceContainerHighest = Color(0xFFECDFE9),
)

private val purpleDark = darkColorScheme(
    primary = Color(0xFFF4AEFF), onPrimary = Color(0xFF55006A),
    primaryContainer = Color(0xFF9638AE), onPrimaryContainer = Color(0xFFFCD2FF),
    secondary = Color(0xFFE8B6EE), onSecondary = Color(0xFF46214F),
    secondaryContainer = Color(0xFF5F3867), onSecondaryContainer = Color(0xFFD5A5DC),
    tertiary = Color(0xFFFFB1C4), onTertiary = Color(0xFF65002F),
    tertiaryContainer = Color(0xFFB13060), onTertiaryContainer = Color(0xFFFFD4DD),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF181118), onBackground = Color(0xFFECDFE9),
    surface = Color(0xFF181118), onSurface = Color(0xFFECDFE9),
    surfaceVariant = Color(0xFF4F4350), onSurfaceVariant = Color(0xFFD3C1D2),
    outline = Color(0xFF9C8C9B), outlineVariant = Color(0xFF4F4350),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFFECDFE9),
    inverseOnSurface = Color(0xFF362E36), inversePrimary = Color(0xFF9133A9),
    surfaceDim = Color(0xFF181118), surfaceBright = Color(0xFF3F373F),
    surfaceContainerLowest = Color(0xFF120C13),
    surfaceContainerLow = Color(0xFF201921),
    surfaceContainer = Color(0xFF241D25),
    surfaceContainerHigh = Color(0xFF2F282F),
    surfaceContainerHighest = Color(0xFF3A323A),
)

// ── Orange preset ────────────────────────────────────────────
private val orangeLight = lightColorScheme(
    primary = Color(0xFF984700), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE57A2C), onPrimaryContainer = Color(0xFF502200),
    secondary = Color(0xFF845333), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFBE97), onSecondaryContainer = Color(0xFF7A4B2C),
    tertiary = Color(0xFF5D6300), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF959E09), onTertiaryContainer = Color(0xFF2F3200),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F5), onBackground = Color(0xFF231914),
    surface = Color(0xFFFFF8F5), onSurface = Color(0xFF231914),
    surfaceVariant = Color(0xFFFADDCD), onSurfaceVariant = Color(0xFF564338),
    outline = Color(0xFF897266), outlineVariant = Color(0xFFDCC1B2),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFF392E28),
    inverseOnSurface = Color(0xFFFFEDE5), inversePrimary = Color(0xFFFFB68A),
    surfaceDim = Color(0xFFE9D6CD), surfaceBright = Color(0xFFFFF8F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EA),
    surfaceContainer = Color(0xFFFEEAE0),
    surfaceContainerHigh = Color(0xFFF8E4DB),
    surfaceContainerHighest = Color(0xFFF2DFD5),
)

private val orangeDark = darkColorScheme(
    primary = Color(0xFFFFB68A), onPrimary = Color(0xFF522300),
    primaryContainer = Color(0xFFE57A2C), onPrimaryContainer = Color(0xFF502200),
    secondary = Color(0xFFF9B891), onSecondary = Color(0xFF4D260A),
    secondaryContainer = Color(0xFF6B3E20), onSecondaryContainer = Color(0xFFEAAB84),
    tertiary = Color(0xFFC5CF42), onTertiary = Color(0xFF303300),
    tertiaryContainer = Color(0xFF959E09), onTertiaryContainer = Color(0xFF2F3200),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1B110C), onBackground = Color(0xFFF2DFD5),
    surface = Color(0xFF1B110C), onSurface = Color(0xFFF2DFD5),
    surfaceVariant = Color(0xFF564338), onSurfaceVariant = Color(0xFFDCC1B2),
    outline = Color(0xFFA48C7E), outlineVariant = Color(0xFF564338),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFFF2DFD5),
    inverseOnSurface = Color(0xFF392E28), inversePrimary = Color(0xFF984700),
    surfaceDim = Color(0xFF1B110C), surfaceBright = Color(0xFF433730),
    surfaceContainerLowest = Color(0xFF150C07),
    surfaceContainerLow = Color(0xFF231914),
    surfaceContainer = Color(0xFF281D18),
    surfaceContainerHigh = Color(0xFF332821),
    surfaceContainerHighest = Color(0xFF3E322C),
)

// ── Teal preset ──────────────────────────────────────────────
private val tealLight = lightColorScheme(
    primary = Color(0xFF006A64), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF39C5BB), onPrimaryContainer = Color(0xFF004D49),
    secondary = Color(0xFF3A6662), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBAE9E3), onSecondaryContainer = Color(0xFF3E6A66),
    tertiary = Color(0xFF714D9E), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8A0F8), onTertiaryContainer = Color(0xFF563181),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF5FBF9), onBackground = Color(0xFF171D1C),
    surface = Color(0xFFF5FBF9), onSurface = Color(0xFF171D1C),
    surfaceVariant = Color(0xFFD7E5E3), onSurfaceVariant = Color(0xFF3C4948),
    outline = Color(0xFF6C7A78), outlineVariant = Color(0xFFBBC9C7),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFF2B3231),
    inverseOnSurface = Color(0xFFECF2F0), inversePrimary = Color(0xFF55DAD0),
    surfaceDim = Color(0xFFD5DBD9), surfaceBright = Color(0xFFF5FBF9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F3),
    surfaceContainer = Color(0xFFE9EFED),
    surfaceContainerHigh = Color(0xFFE3E9E8),
    surfaceContainerHighest = Color(0xFFDEE4E2),
)

private val tealDark = darkColorScheme(
    primary = Color(0xFF5DE1D7), onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF39C5BB), onPrimaryContainer = Color(0xFF004D49),
    secondary = Color(0xFFA1CFCA), onSecondary = Color(0xFF023734),
    secondaryContainer = Color(0xFF204E4A), onSecondaryContainer = Color(0xFF90BEB9),
    tertiary = Color(0xFFDEC1FF), onTertiary = Color(0xFF411B6C),
    tertiaryContainer = Color(0xFFC8A0F8), onTertiaryContainer = Color(0xFF563181),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1514), onBackground = Color(0xFFDEE4E2),
    surface = Color(0xFF0E1514), onSurface = Color(0xFFDEE4E2),
    surfaceVariant = Color(0xFF3C4948), onSurfaceVariant = Color(0xFFBBC9C7),
    outline = Color(0xFF869491), outlineVariant = Color(0xFF3C4948),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFFDEE4E2),
    inverseOnSurface = Color(0xFF2B3231), inversePrimary = Color(0xFF006A64),
    surfaceDim = Color(0xFF0E1514), surfaceBright = Color(0xFF343A39),
    surfaceContainerLowest = Color(0xFF090F0F),
    surfaceContainerLow = Color(0xFF171D1C),
    surfaceContainer = Color(0xFF1B2120),
    surfaceContainerHigh = Color(0xFF252B2A),
    surfaceContainerHighest = Color(0xFF303635),
)
// ── Yellow preset ────────────────────────────────────────────
private val yellowLight = lightColorScheme(
    primary = Color(0xFF795900), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2BC3A), onPrimaryContainer = Color(0xFF684C00),
    secondary = Color(0xFF725B2A), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBDC9F), onSecondaryContainer = Color(0xFF765F2E),
    tertiary = Color(0xFF526600), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB4CF5D), onTertiaryContainer = Color(0xFF465700),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F2), onBackground = Color(0xFF201B12),
    surface = Color(0xFFFFF8F2), onSurface = Color(0xFF201B12),
    surfaceVariant = Color(0xFFEFE1C9), onSurfaceVariant = Color(0xFF4F4634),
    outline = Color(0xFF817662), outlineVariant = Color(0xFFD3C5AE),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFF353026),
    inverseOnSurface = Color(0xFFFAEFE1), inversePrimary = Color(0xFFF5BE3C),
    surfaceDim = Color(0xFFE3D9CB), surfaceBright = Color(0xFFFFF8F2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDF2E4),
    surfaceContainer = Color(0xFFF7ECDE),
    surfaceContainerHigh = Color(0xFFF1E7D8),
    surfaceContainerHighest = Color(0xFFEBE1D3),
)

private val yellowDark = darkColorScheme(
    primary = Color(0xFFFFDC95), onPrimary = Color(0xFF402D00),
    primaryContainer = Color(0xFFF2BC3A), onPrimaryContainer = Color(0xFF684C00),
    secondary = Color(0xFFE1C388), onSecondary = Color(0xFF3F2E01),
    secondaryContainer = Color(0xFF584415), onSecondaryContainer = Color(0xFFCEB278),
    tertiary = Color(0xFFD0EC76), onTertiary = Color(0xFF293500),
    tertiaryContainer = Color(0xFFB4CF5D), onTertiaryContainer = Color(0xFF465700),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF17130B), onBackground = Color(0xFFEBE1D3),
    surface = Color(0xFF17130B), onSurface = Color(0xFFEBE1D3),
    surfaceVariant = Color(0xFF4F4634), onSurfaceVariant = Color(0xFFD3C5AE),
    outline = Color(0xFF9B8F7A), outlineVariant = Color(0xFF4F4634),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFFEBE1D3),
    inverseOnSurface = Color(0xFF353026), inversePrimary = Color(0xFF795900),
    surfaceDim = Color(0xFF17130B), surfaceBright = Color(0xFF3E382F),
    surfaceContainerLowest = Color(0xFF120E06),
    surfaceContainerLow = Color(0xFF201B12),
    surfaceContainer = Color(0xFF241F16),
    surfaceContainerHigh = Color(0xFF2E2920),
    surfaceContainerHighest = Color(0xFF3A342A),
)

private val pinkLight = lightColorScheme(
    primary = Color(0xFF9A4152), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEE8395), onPrimaryContainer = Color(0xFF6B1C2F),
    secondary = Color(0xFF805258), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEC2CA), onSecondaryContainer = Color(0xFF7A4D54),
    tertiary = Color(0xFF7C5800), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD09A29), onTertiaryContainer = Color(0xFF4D3500),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F7), onBackground = Color(0xFF22191A),
    surface = Color(0xFFFFF8F7), onSurface = Color(0xFF22191A),
    surfaceVariant = Color(0xFFF7DCDE), onSurfaceVariant = Color(0xFF544244),
    outline = Color(0xFF877274), outlineVariant = Color(0xFFDAC0C3),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFF382E2F),
    inverseOnSurface = Color(0xFFFEEDEE), inversePrimary = Color(0xFFFFB2BC),
    surfaceDim = Color(0xFFE7D6D7), surfaceBright = Color(0xFFFFF8F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0F1),
    surfaceContainer = Color(0xFFFBEAEB),
    surfaceContainerHigh = Color(0xFFF5E4E5),
    surfaceContainerHighest = Color(0xFFF0DEDF),
)

private val pinkDark = darkColorScheme(
    primary = Color(0xFFFFB2BC), onPrimary = Color(0xFF5F1226),
    primaryContainer = Color(0xFFEE8395), onPrimaryContainer = Color(0xFF6B1C2F),
    secondary = Color(0xFFF2B7BF), onSecondary = Color(0xFF4B252B),
    secondaryContainer = Color(0xFF673D43), onSecondaryContainer = Color(0xFFE3AAB1),
    tertiary = Color(0xFFF7BD4A), onTertiary = Color(0xFF422D00),
    tertiaryContainer = Color(0xFFD09A29), onTertiaryContainer = Color(0xFF4D3500),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191112), onBackground = Color(0xFFF0DEDF),
    surface = Color(0xFF191112), onSurface = Color(0xFFF0DEDF),
    surfaceVariant = Color(0xFF544244), onSurfaceVariant = Color(0xFFDAC0C3),
    outline = Color(0xFFA28B8D), outlineVariant = Color(0xFF544244),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFFF0DEDF),
    inverseOnSurface = Color(0xFF382E2F), inversePrimary = Color(0xFF9A4152),
    surfaceDim = Color(0xFF191112), surfaceBright = Color(0xFF413738),
    surfaceContainerLowest = Color(0xFF140C0D),
    surfaceContainerLow = Color(0xFF22191A),
    surfaceContainer = Color(0xFF261D1E),
    surfaceContainerHigh = Color(0xFF312829),
    surfaceContainerHighest = Color(0xFF3C3233),
)

private val lightGreenLight = lightColorScheme(
    primary = Color(0xFF426900), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF94C355), onPrimaryContainer = Color(0xFF304F00),
    secondary = Color(0xFF526439), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E7B0), onSecondaryContainer = Color(0xFF56693C),
    tertiary = Color(0xFF006C4D), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF3BCB98), onTertiaryContainer = Color(0xFF005139),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9FAED), onBackground = Color(0xFF1A1D15),
    surface = Color(0xFFF9FAED), onSurface = Color(0xFF1A1D15),
    surfaceVariant = Color(0xFFDFE5D0), onSurfaceVariant = Color(0xFF43493A),
    outline = Color(0xFF737968), outlineVariant = Color(0xFFC3C9B5),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFF2F3129),
    inverseOnSurface = Color(0xFFF0F2E5), inversePrimary = Color(0xFFA5D565),
    surfaceDim = Color(0xFFD9DBCE), surfaceBright = Color(0xFFF9FAED),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F5E8),
    surfaceContainer = Color(0xFFEDEFE2),
    surfaceContainerHigh = Color(0xFFE8E9DC),
    surfaceContainerHighest = Color(0xFFE2E4D7),
)

private val lightGreenDark = darkColorScheme(
    primary = Color(0xFFAFDF6E), onPrimary = Color(0xFF203600),
    primaryContainer = Color(0xFF94C355), onPrimaryContainer = Color(0xFF304F00),
    secondary = Color(0xFFB9CE99), onSecondary = Color(0xFF25350F),
    secondaryContainer = Color(0xFF3F5127), onSecondaryContainer = Color(0xFFAEC38F),
    tertiary = Color(0xFF5EE8B2), onTertiary = Color(0xFF003826),
    tertiaryContainer = Color(0xFF3BCB98), onTertiaryContainer = Color(0xFF005139),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF11140D), onBackground = Color(0xFFE2E4D7),
    surface = Color(0xFF11140D), onSurface = Color(0xFFE2E4D7),
    surfaceVariant = Color(0xFF43493A), onSurfaceVariant = Color(0xFFC3C9B5),
    outline = Color(0xFF8D9381), outlineVariant = Color(0xFF43493A),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFFE2E4D7),
    inverseOnSurface = Color(0xFF2F3129), inversePrimary = Color(0xFF426900),
    surfaceDim = Color(0xFF11140D), surfaceBright = Color(0xFF373A31),
    surfaceContainerLowest = Color(0xFF0C0F08),
    surfaceContainerLow = Color(0xFF1A1D15),
    surfaceContainer = Color(0xFF1E2119),
    surfaceContainerHigh = Color(0xFF282B23),
    surfaceContainerHighest = Color(0xFF33362D),
)

private val highContrastBlueLight = lightColorScheme(
    primary = Color(0xFF0031A6), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE1FF), onPrimaryContainer = Color(0xFF000729),
    secondary = Color(0xFF23305A), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E2EC), onSecondaryContainer = Color(0xFF000A27),
    tertiary = Color(0xFF4A0080), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3DAFF), onTertiaryContainer = Color(0xFF1B0036),
    error = Color(0xFF8C0009), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF360001),
    background = Color(0xFFFFFFFF), onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE2E2EC), onSurfaceVariant = Color(0xFF1A1B23),
    outline = Color(0xFF45464F), outlineVariant = Color(0xFF757680),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFF1A1B20),
    inverseOnSurface = Color(0xFFF1F0F7), inversePrimary = Color(0xFFB7C4FF),
    surfaceDim = Color(0xFFD9D9E0), surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3FA),
    surfaceContainer = Color(0xFFEEEDF4),
    surfaceContainerHigh = Color(0xFFE8E7EE),
    surfaceContainerHighest = Color(0xFFE2E2E9),
)
private val highContrastBlueDark = darkColorScheme(
    primary = Color(0xFFB7C4FF), onPrimary = Color(0xFF00114B),
    primaryContainer = Color(0xFF00227B), onPrimaryContainer = Color(0xFFE1E6FF),
    secondary = Color(0xFFBCC3FF), onSecondary = Color(0xFF00154B),
    secondaryContainer = Color(0xFF23305A), onSecondaryContainer = Color(0xFFE0E2EC),
    tertiary = Color(0xFFE2B6FF), onTertiary = Color(0xFF26004C),
    tertiaryContainer = Color(0xFF4A0080), onTertiaryContainer = Color(0xFFF3DAFF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A0B10), onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A0B10), onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF45464F), onSurfaceVariant = Color(0xFFE2E2EC),
    outline = Color(0xFFC6C6D0), outlineVariant = Color(0xFF45464F),
    scrim = Color(0xFF000000), inverseSurface = Color(0xFFE4E2E9),
    inverseOnSurface = Color(0xFF000000), inversePrimary = Color(0xFF0031A6),
    surfaceDim = Color(0xFF0A0B10), surfaceBright = Color(0xFF33343A),
    surfaceContainerLowest = Color(0xFF020307),
    surfaceContainerLow = Color(0xFF121318),
    surfaceContainer = Color(0xFF16171D),
    surfaceContainerHigh = Color(0xFF202228),
    surfaceContainerHighest = Color(0xFF2B2D33),
)

fun ThemeColorPreset.colorScheme(darkTheme: Boolean): ColorScheme = when (this) {
    ThemeColorPreset.SYSTEM -> error("Use dynamicColorScheme for SYSTEM preset")
    ThemeColorPreset.DEFAULT -> if (darkTheme) redDark else redLight
    ThemeColorPreset.BLUE -> if (darkTheme) blueDark else blueLight
    ThemeColorPreset.PURPLE -> if (darkTheme) purpleDark else purpleLight
    ThemeColorPreset.ORANGE -> if (darkTheme) orangeDark else orangeLight
    ThemeColorPreset.TEAL -> if (darkTheme) tealDark else tealLight
    ThemeColorPreset.YELLOW -> if (darkTheme) yellowDark else yellowLight
    ThemeColorPreset.PINK -> if (darkTheme) pinkDark else pinkLight
    ThemeColorPreset.LIGHT_GREEN -> if (darkTheme) lightGreenDark else lightGreenLight
    ThemeColorPreset.HIGH_CONTRAST -> if (darkTheme) highContrastBlueDark else highContrastBlueLight
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun dynamicColorScheme(darkTheme: Boolean): ColorScheme {
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
