package com.yenaly.han1meviewer.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

@Composable
fun <T> WheelLikeColumn(
    title: String,
    values: List<T>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 240.dp)
                .fadingEdges(fadeHeight = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(values, key = { label(it) }) { value ->
                val isSelected = value == selectedValue
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.96f)
                    },
                    tonalElevation = if (isSelected) 2.dp else 0.dp,
                    onClick = { onSelect(value) },
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label(value),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

fun Modifier.fadingEdges(fadeHeight: Dp): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadeGradient = Brush.verticalGradient(
            0.0f to Color.Transparent,
            (fadeHeight.toPx() / size.height) to Color.Black,
            1.0f - (fadeHeight.toPx() / size.height) to Color.Black,
            1.0f to Color.Transparent
        )
        drawRect(
            brush = fadeGradient,
            blendMode = BlendMode.DstIn
        )
    }

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WheelLikeColumnPreview(){
    ComponentPreview {
        Row {
            WheelLikeColumn(
                modifier = Modifier.weight(1f),
                title = "Year",
                values = (1990..2026).toList(),
                selectedValue = 1992,
                label = { value -> value.toString() },
                onSelect = { },
            )
            WheelLikeColumn(
                modifier = Modifier.weight(1f),
                title = "Month",
                values = (1..12).toList(),
                selectedValue = 2,
                label = { value -> value.toString() },
                onSelect = { },
            )
        }
    }
}