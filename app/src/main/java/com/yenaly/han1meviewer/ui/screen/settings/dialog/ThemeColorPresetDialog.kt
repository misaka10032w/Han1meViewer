package com.yenaly.han1meviewer.ui.screen.settings.dialog

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.theme.ThemeColorPreset
import com.yenaly.han1meviewer.ui.theme.colorScheme
import com.yenaly.han1meviewer.ui.theme.dynamicColorScheme
import com.yenaly.han1meviewer.util.ThemeUtils

/**
 * 预设配色选择弹窗。
 *
 * 使用四色圆盘预览每套配色，点击即可应用。
 */
@Composable
fun ThemeColorPresetDialog(
    presets: List<ThemeColorPreset>,
    selectedKey: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    visible: Boolean = true,
) {
    if (!visible) return

    val context = LocalContext.current
    val darkTheme = remember(context) { ThemeUtils.isDarkTheme(context) }
    // “跟随系统”预览必须基于系统动态取色，而不是当前已选主题。
    val systemPreviewScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicColorScheme(darkTheme)
    } else {
        ThemeColorPreset.DEFAULT.colorScheme(darkTheme)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.theme_color_preset_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.theme_color_preset_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 72.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = presets,
                    key = { it.key },
                ) { preset ->
                    val previewScheme = if (preset == ThemeColorPreset.SYSTEM) {
                        systemPreviewScheme
                    } else {
                        preset.colorScheme(darkTheme)
                    }
                    ThemePresetItem(
                        preset = preset,
                        selected = preset.key == selectedKey,
                        previewColors = listOf(
                            previewScheme.primaryContainer,
                            previewScheme.secondaryContainer,
                            previewScheme.tertiaryContainer,
                            previewScheme.primary,
                        ),
                        onClick = { onSelect(preset.key) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ThemePresetItem(
    preset: ThemeColorPreset,
    selected: Boolean,
    previewColors: List<Color>,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                },
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                ThemeColorPie(
                    colors = previewColors,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }
        }
        Text(
            text = stringResource(preset.displayNameRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ThemeColorPie(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val segmentSize = Size(size.width, size.height)
        colors.forEachIndexed { index, color ->
            drawArc(
                color = color,
                startAngle = -90f + index * (360f / colors.size),
                sweepAngle = 360f / colors.size,
                useCenter = true,
                topLeft = Offset.Zero,
                size = segmentSize,
            )
        }
    }
}
