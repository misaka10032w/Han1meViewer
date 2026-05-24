package com.yenaly.han1meviewer.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
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
    var chipContentHeightTarget by remember(
        tags,
        collapsible,
        collapsedMaxLines
    ) { mutableIntStateOf(0) }
//    val collapseText = stringResource(R.string.collapse)
//    val expandText = stringResource(R.string.expand)
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

        data class FlowLayoutResult(
            val positions: List<ChipPosition>,
            val lineHeights: List<Int>,
            val lineTops: List<Int>,
            val contentWidth: Int,
        ) {
            val lineCount: Int
                get() = lineHeights.size

            val height: Int
                get() = if (lineCount > 0) lineTops.last() + lineHeights.last() else 0
        }

        fun calculateFlowLayout(placeables: List<Placeable>): FlowLayoutResult {
            if (placeables.isEmpty()) {
                return FlowLayoutResult(
                    positions = emptyList(),
                    lineHeights = emptyList(),
                    lineTops = emptyList(),
                    contentWidth = 0,
                )
            }

            val positions = mutableListOf<ChipPosition>()
            val lineHeights = mutableListOf(0)
            val lineTops = mutableListOf(0)
            var line = 0
            var x = 0
            var y = 0

            placeables.forEachIndexed { index, placeable ->
                val shouldWrap =
                    x > 0 && maxWidth != Int.MAX_VALUE && x + placeable.width > maxWidth
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

            val contentWidth = positions.maxOfOrNull { position ->
                position.x + placeables[position.index].width
            } ?: 0

            return FlowLayoutResult(
                positions = positions,
                lineHeights = lineHeights,
                lineTops = lineTops,
                contentWidth = contentWidth,
            )
        }

        val chipLayout = calculateFlowLayout(chipPlaceables)
        val hasOverflow = collapsible && chipLayout.lineCount > collapsedMaxLines
        val togglePlaceable = if (hasOverflow) {
            subcompose(TagChipSlot.Toggle) {
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = arrowRotation
                        },
                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text(text = if (expanded) collapseText else expandText)
                }
            }.single().measure(contentConstraints)
        } else {
            null
        }

        val contentPlaceables: List<Placeable>
        val contentLayout: FlowLayoutResult

        if (togglePlaceable != null) {
            if (expanded) {
                contentPlaceables = chipPlaceables + togglePlaceable
                contentLayout = calculateFlowLayout(contentPlaceables)
            } else {
                var visibleChipCount = chipPlaceables.size
                var bestPlaceables: List<Placeable>? = null
                var bestLayout: FlowLayoutResult? = null

                while (visibleChipCount >= 0) {
                    val candidatePlaceables =
                        chipPlaceables.take(visibleChipCount) + togglePlaceable
                    val candidateLayout = calculateFlowLayout(candidatePlaceables)
                    if (candidateLayout.lineCount <= collapsedMaxLines) {
                        bestPlaceables = candidatePlaceables
                        bestLayout = candidateLayout
                        break
                    }
                    visibleChipCount -= 1
                }

                contentPlaceables = bestPlaceables ?: listOf(togglePlaceable)
                contentLayout = bestLayout ?: calculateFlowLayout(contentPlaceables)
            }
        } else {
            contentPlaceables = chipPlaceables
            contentLayout = chipLayout
        }

        val chipContentHeight = contentLayout.height
        if (chipContentHeightTarget != chipContentHeight) {
            chipContentHeightTarget = chipContentHeight
        }

        val headerPlaceables = if (!headerTitle.isNullOrBlank()) {
            subcompose(TagChipSlot.Header) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleLarge,
                )
            }.map { measurable -> measurable.measure(contentConstraints) }
        } else {
            emptyList()
        }

        val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
        val headerSpacing =
            if (headerPlaceables.isNotEmpty() && animatedChipContentHeight > 0) spacingY else 0
        val contentWidth = contentLayout.contentWidth
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
            contentLayout.positions.forEach { position ->
                contentPlaceables[position.index].placeRelative(position.x, position.y + yOffset)
            }
        }
    }
}

private enum class TagChipSlot {
    Header,
    Chips,
    Toggle,
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

@Preview(showSystemUi = true, showBackground = true)
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
