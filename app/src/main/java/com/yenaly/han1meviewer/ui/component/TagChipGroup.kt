package com.yenaly.han1meviewer.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.fakeTagList2
import kotlin.random.Random

/**
 * 标签组组件。
 *
 * 支持流式排列、点击回调，以及可选的“超过两行折叠/展开”模式。
 */
@Composable
fun TagChipGroup(
    tags: List<String>,
    modifier: Modifier = Modifier,
    onTagClick: ((String) -> Unit)? = null,
    collapsible: Boolean = false,
    collapsedMaxLines: Int = 2,
    headerTitle: String? = null,
) {
    require(!collapsible || collapsedMaxLines > 0)

    val tagColors = rememberTagChipColors(tags)
    var expanded by rememberSaveable(tags, collapsible, collapsedMaxLines) { mutableStateOf(false) }
    var overflowDetected by remember(tags, collapsible, collapsedMaxLines) { mutableStateOf(false) }
    var chipContentHeightTarget by remember(
        tags,
        collapsible,
        collapsedMaxLines
    ) { mutableIntStateOf(0) }
    val collapseText = stringResource(R.string.collapse)
    val expandText = stringResource(R.string.expand)
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "tag-arrow-rotation",
    )
    val animatedChipContentHeight by animateIntAsState(
        targetValue = chipContentHeightTarget,
        animationSpec = tween(durationMillis = 240),
        label = "tag-content-height",
    )

    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val contentConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val spacingX = 8.dp.roundToPx()
        val spacingY = 8.dp.roundToPx()
        val maxWidth =
            constraints.maxWidth.takeUnless { it == Constraints.Infinity } ?: Int.MAX_VALUE

        val chipPlaceables = subcompose(TagChipSlot.Chips) {
            tags.forEach { tag ->
                TagChip(
                    tag = tag,
                    onTagClick = onTagClick,
                    colors = tagColors[tag] ?: AssistChipDefaults.assistChipColors(),
                )
            }
        }.map { measurable -> measurable.measure(contentConstraints) }

        data class ChipPosition(
            val index: Int,
            val line: Int,
            val x: Int,
            val y: Int,
        )

        val positions = mutableListOf<ChipPosition>()
        val lineHeights = mutableListOf<Int>()
        val lineTops = mutableListOf<Int>()
        var line = 0
        var x = 0
        var y = 0

        if (chipPlaceables.isNotEmpty()) {
            lineHeights += 0
            lineTops += 0
        }

        chipPlaceables.forEachIndexed { index, placeable ->
            val shouldWrap = x > 0 && maxWidth != Int.MAX_VALUE && x + placeable.width > maxWidth
            if (shouldWrap) {
                y += lineHeights[line] + spacingY
                line += 1
                x = 0
                lineHeights += 0
                lineTops += y
            }

            positions += ChipPosition(index = index, line = line, x = x, y = y)
            lineHeights[line] = maxOf(lineHeights[line], placeable.height)
            x += placeable.width + spacingX
        }

        val totalLines = if (chipPlaceables.isEmpty()) 0 else line + 1
        val hasOverflowNow = collapsible && totalLines > collapsedMaxLines
        if (hasOverflowNow && !overflowDetected) {
            overflowDetected = true
        }
        val showToggle = collapsible && (expanded || overflowDetected)
        val visibleLines = if (showToggle && !expanded) collapsedMaxLines else totalLines
        val visibleCount = positions.indexOfFirst { it.line >= visibleLines }
            .let { if (it == -1) positions.size else it }
        val chipContentHeight = if (visibleLines > 0) {
            lineTops[visibleLines - 1] + lineHeights[visibleLines - 1]
        } else {
            0
        }
        if (chipContentHeightTarget != chipContentHeight) {
            chipContentHeightTarget = chipContentHeight
        }

        val headerPlaceables = if (!headerTitle.isNullOrBlank() || collapsible) {
            subcompose(TagChipSlot.Header) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!headerTitle.isNullOrBlank()) {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }

                    if (showToggle) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = arrowRotation
                                },
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (expanded) collapseText else expandText
                            )
                        }
                    }
                }
            }.map { measurable -> measurable.measure(contentConstraints) }
        } else {
            emptyList()
        }

        val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
        val headerSpacing =
            if (headerPlaceables.isNotEmpty() && animatedChipContentHeight > 0) spacingY else 0
        val contentWidth = positions.take(visibleCount)
            .maxOfOrNull { position -> position.x + chipPlaceables[position.index].width }
            ?: 0
        val headerWidth = headerPlaceables.maxOfOrNull { it.width } ?: 0
        val layoutWidth = (constraints.maxWidth.takeUnless { it == Constraints.Infinity }
            ?: maxOf(contentWidth, headerWidth))
            .coerceAtLeast(constraints.minWidth)
        val layoutHeight = (headerHeight + headerSpacing + animatedChipContentHeight)
            .coerceAtLeast(constraints.minHeight)

        layout(layoutWidth, layoutHeight) {
            headerPlaceables.forEach { placeable ->
                placeable.placeRelative(0, 0)
            }
            val yOffset = headerHeight + headerSpacing
            positions.take(visibleCount).forEach { position ->
                chipPlaceables[position.index].placeRelative(position.x, position.y + yOffset)
            }
        }
    }
}

private enum class TagChipSlot {
    Header,
    Chips,
}

@Composable
private fun TagChip(
    tag: String,
    onTagClick: ((String) -> Unit)?,
    colors: ChipColors,
) {
    AssistChip(
        onClick = { onTagClick?.invoke(tag) ?: Unit },
        label = { Text(tag) },
        colors = colors,
    )
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
fun TagChipGroupPreview() {
    ComponentPreview {
        TagChipGroup(
            tags = fakeTagList2,
            collapsible = true,
            headerTitle = "TAGs",
        )
    }
}
