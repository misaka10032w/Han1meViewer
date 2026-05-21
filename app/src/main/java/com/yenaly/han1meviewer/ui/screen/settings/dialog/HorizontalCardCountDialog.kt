package com.yenaly.han1meviewer.ui.screen.settings.dialog

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.yenaly.han1meviewer.HorizontalCardCountConfig
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.screen.settings.model.GridRangeOption


@Composable
fun HorizontalCardCountDialog(
    initialConfig: HorizontalCardCountConfig,
    onDismiss: () -> Unit,
    onConfirm: (HorizontalCardCountConfig) -> Unit,
) {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val currentWidthDp = with(density) { containerSize.width.toDp().value.toInt() }

    var narrowText by remember(initialConfig) { mutableStateOf(initialConfig.narrowCount.toString()) }
    var compactText by remember(initialConfig) { mutableStateOf(initialConfig.compactCount.toString()) }
    var mediumText by remember(initialConfig) { mutableStateOf(initialConfig.mediumCount.toString()) }
    var expandedText by remember(initialConfig) { mutableStateOf(initialConfig.expandedCount.toString()) }

    val narrowCount = narrowText.toHorizontalCardCountOrNull()
    val compactCount = compactText.toHorizontalCardCountOrNull()
    val mediumCount = mediumText.toHorizontalCardCountOrNull()
    val expandedCount = expandedText.toHorizontalCardCountOrNull()

    val currentConfig = HorizontalCardCountConfig(
        narrowCount = narrowCount ?: initialConfig.narrowCount,
        compactCount = compactCount ?: initialConfig.compactCount,
        mediumCount = mediumCount ?: initialConfig.mediumCount,
        expandedCount = expandedCount ?: initialConfig.expandedCount,
    )

    val options = listOf(
        GridRangeOption(
            label = stringResource(R.string.horizontal_card_count_range_narrow),
            value = narrowText,
            onValueChange = { narrowText = it },
            isError = narrowCount == null,
            isHighlighted = currentWidthDp < 350,
            highlightLabels = buildList {
                if (currentWidthDp < 350) add(stringResource(R.string.current_bucket))
            }
        ),
        GridRangeOption(
            label = stringResource(R.string.horizontal_card_count_range_compact),
            value = compactText,
            onValueChange = { compactText = it },
            isError = compactCount == null,
            isHighlighted = currentWidthDp in 350..599,
            highlightLabels = buildList {
                if (currentWidthDp in 350..599) add(stringResource(R.string.current_bucket))
            }

        ),
        GridRangeOption(
            label = stringResource(R.string.horizontal_card_count_range_medium),
            value = mediumText,
            onValueChange = { mediumText = it },
            isError = mediumCount == null,
            isHighlighted = currentWidthDp in 600..839,
            highlightLabels = buildList {
                if (currentWidthDp in 600..839) add(stringResource(R.string.current_bucket))
            }
        ),
        GridRangeOption(
            label = stringResource(R.string.horizontal_card_count_range_expanded),
            value = expandedText,
            onValueChange = { expandedText = it },
            isError = expandedCount == null,
            isHighlighted = currentWidthDp >= 840,
            highlightLabels = buildList {
                if (currentWidthDp >= 840) add(stringResource(R.string.current_bucket))
            }
        )
    )

    BaseGridConfigDialog(
        title = stringResource(R.string.horizontal_card_count_title),
        hintText = stringResource(R.string.horizontal_card_count_dialog_hint),
        widthHintText = stringResource(
            R.string.horizontal_card_count_current_width_hint,
            currentWidthDp
        ),
        bucketHintText = stringResource(
            R.string.horizontal_card_count_current_bucket_hint,
            horizontalCardCountBucketLabel(currentWidthDp),
            currentConfig.countForWidthDp(currentWidthDp)
        ),
        options = options,
        isDecimal = true,
        canConfirm = narrowCount != null && compactCount != null && mediumCount != null && expandedCount != null,
        onDismiss = onDismiss,
        onReset = {
            narrowText = HorizontalCardCountConfig.DEFAULT_NARROW_COUNT.toString()
            compactText = HorizontalCardCountConfig.DEFAULT_COMPACT_COUNT.toString()
            mediumText = HorizontalCardCountConfig.DEFAULT_MEDIUM_COUNT.toString()
            expandedText = HorizontalCardCountConfig.DEFAULT_EXPANDED_COUNT.toString()
        },
        onConfirm = { onConfirm(currentConfig) }
    )
}

private fun String.toHorizontalCardCountOrNull(): Float? {
    val parsed = toFloatOrNull() ?: return null
    return parsed.takeIf { it in 1f..12f }
}

@Composable
private fun horizontalCardCountBucketLabel(widthDp: Int): String {
    return when {
        widthDp < 350 -> stringResource(R.string.horizontal_card_count_range_narrow)
        widthDp < 600 -> stringResource(R.string.horizontal_card_count_range_compact)
        widthDp < 840 -> stringResource(R.string.horizontal_card_count_range_medium)
        else -> stringResource(R.string.horizontal_card_count_range_expanded)
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HorizontalCardCountDialogPreview() {
    ComponentPreview {
        Surface {
            HorizontalCardCountDialog(
                initialConfig = HorizontalCardCountConfig(),
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}