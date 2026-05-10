package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.preview.fakeTagList2
import kotlin.random.Random

/**
 * 标签组组件
 *
 * 以流式布局展示一组标签，每个标签以 [AssistChip] 形式呈现。
 * 标签颜色会根据 MaterialTheme 配色方案自动分配，并为相邻标签避免使用相同配色，
 * 以提升视觉区分度。颜色分配基于标签内容的哈希值确定，保证重组时同一标签颜色稳定。
 *
 * @param tags 要展示的标签文本列表
 * @param modifier 应用于 [FlowRow] 布局的修饰符，默认为 [Modifier]
 * @param onTagClick 标签点击回调，参数为点击的标签文本。设为 null 时标签不可点击
 *
 * @sample com.yenaly.han1meviewer.ui.component.TagChipGroupPreview
 */
@Composable
fun TagChipGroup(
    tags: List<String>,
    modifier: Modifier = Modifier,
    onTagClick: ((String) -> Unit)? = null,
) {
    val tagColors = rememberTagChipColors(tags)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            AssistChip(
                onClick = { onTagClick?.invoke(tag) ?: Unit },
                label = { Text(tag) },
                colors = tagColors[tag] ?: AssistChipDefaults.assistChipColors(),
            )
        }
    }
}

@Composable
private fun rememberTagChipColors(tags: List<String>): Map<String, ChipColors> {
    val colorScheme = MaterialTheme.colorScheme
    val palette = remember(colorScheme) {
        listOf(
            colorScheme.primaryContainer to colorScheme.onPrimaryContainer,
            colorScheme.secondaryContainer to colorScheme.onSecondaryContainer,
            colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer,
            colorScheme.errorContainer to colorScheme.onErrorContainer,
            colorScheme.surfaceContainer to colorScheme.onSurface,
            colorScheme.surfaceContainerLow to colorScheme.onSurface,
            colorScheme.surfaceContainerHigh to colorScheme.onSurface,
            colorScheme.surfaceContainerHighest to colorScheme.onSurface,
            colorScheme.inversePrimary.copy(alpha = 0.28f) to colorScheme.onSurface,
            colorScheme.primary.copy(alpha = 0.18f) to colorScheme.primary,
            colorScheme.secondary.copy(alpha = 0.18f) to colorScheme.secondary,
            colorScheme.tertiary.copy(alpha = 0.18f) to colorScheme.tertiary,
            colorScheme.error.copy(alpha = 0.16f) to colorScheme.error,
        )
    }
    val assignedPalette = remember(tags, palette) {
        assignPaletteWithoutAdjacentRepeats(tags.distinct(), palette)
    }
    return assignedPalette.mapValues { (_, pair) ->
        AssistChipDefaults.assistChipColors(
            containerColor = pair.first,
            labelColor = pair.second,
        )
    }
}

private fun assignPaletteWithoutAdjacentRepeats(
    tags: List<String>,
    palette: List<Pair<Color, Color>>,
): Map<String, Pair<Color, Color>> {
    if (tags.isEmpty()) return emptyMap()
    val shuffled = palette.shuffled(Random(tags.joinToString("|").hashCode()))
    val result = LinkedHashMap<String, Pair<Color, Color>>(tags.size)
    var paletteIndex = 0
    var previous: Pair<Color, Color>? = null

    tags.forEach { tag ->
        var chosen = shuffled[paletteIndex % shuffled.size]
        if (chosen == previous && shuffled.size > 1) {
            chosen = shuffled[(paletteIndex + 1) % shuffled.size]
            paletteIndex += 1
        }
        result[tag] = chosen
        previous = chosen
        paletteIndex += 1
    }
    return result
}

@Preview(showSystemUi = false, showBackground = true)
@Composable
fun TagChipGroupPreview(){
    TagChipGroup(
        fakeTagList2
    )
}