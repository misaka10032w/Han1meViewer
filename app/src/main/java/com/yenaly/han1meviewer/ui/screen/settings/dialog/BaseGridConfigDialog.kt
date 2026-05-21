package com.yenaly.han1meviewer.ui.screen.settings.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.screen.settings.model.GridRangeOption

@Composable
fun BaseGridConfigDialog(
    title: String,
    hintText: String,
    widthHintText: String,
    bucketHintText: String,
    options: List<GridRangeOption>,
    isDecimal: Boolean,
    canConfirm: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = hintText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = widthHintText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = bucketHintText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                options.forEach { option ->
                    GridConfigInputRow(option = option, isDecimal = isDecimal)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canConfirm) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onReset,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
private fun GridConfigInputRow(option: GridRangeOption, isDecimal: Boolean) {
    val containerColor = if (option.isHighlighted) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val borderStroke = if (option.isHighlighted) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    } else {
        null
    }

    val step = if (isDecimal) 0.1f else 1.0f
    val convertAndStep = { isIncrement: Boolean ->
        val currentValue = option.value.toFloatOrNull() ?: 0f
        val newValue = if (isIncrement) currentValue + step else currentValue - step
        val finalValue = newValue.coerceAtLeast(0f)

        val textValue = if (isDecimal) {
            String.format(java.util.Locale.US, "%.1f", finalValue)
        } else {
            finalValue.toInt().toString()
        }
        option.onValueChange(textValue)
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        border = borderStroke,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(if (option.isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (option.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        color = if (option.isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )

                    if (option.highlightLabels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            option.highlightLabels.forEach { text ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp) // 控件之间的间距
                ) {
                    FilledIconButton(
                        onClick = { convertAndStep(false) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Remove,
                            contentDescription = "减少",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    BasicTextField(
                        value = option.value,
                        onValueChange = { input ->
                            val isValid = if (isDecimal) {
                                input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d*)?$"))
                            } else {
                                input.isEmpty() || input.all(Char::isDigit)
                            }
                            if (isValid) option.onValueChange(input)
                        },
                        singleLine = true,
                        maxLines = 1,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(32.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (option.isError) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                        },
                                        shape = MaterialTheme.shapes.extraSmall
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                innerTextField()
                            }
                        }
                    )

                    FilledIconButton(
                        onClick = { convertAndStep(true) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Add,
                            contentDescription = "增加",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BaseGridConfigDialogPreview() {
    ComponentPreview {
        BaseGridConfigDialog(
            title = "标题",
            hintText = "Hint",
            widthHintText = "100px",
            bucketHintText = "200",
            options = listOf(
                GridRangeOption(
                    label = "300px",
                    value = "2",
                    onValueChange = { },
                    isError = false,
                    isHighlighted = false,
                    highlightLabels = listOf("hints")
                ),
                GridRangeOption(
                    label = "100px",
                    value = "2",
                    onValueChange = { },
                    isError = true,
                    isHighlighted = true,
                    highlightLabels = listOf("hints")
                )
            ),
            isDecimal = true,
            canConfirm = true,
            onDismiss = {},
            onReset = {},
            onConfirm = {}
        )
    }
}