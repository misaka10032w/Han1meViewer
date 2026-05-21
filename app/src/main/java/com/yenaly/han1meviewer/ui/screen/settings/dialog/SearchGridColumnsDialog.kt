package com.yenaly.han1meviewer.ui.screen.settings.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SearchGridColumnsConfig
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.screen.settings.model.GridRangeOption
import kotlin.math.max
import kotlin.math.min

@Composable
fun SearchGridColumnsDialog(
    initialConfig: SearchGridColumnsConfig,
    onDismiss: () -> Unit,
    onConfirm: (SearchGridColumnsConfig) -> Unit,
) {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val currentWidthDp = with(density) { containerSize.width.toDp().value.toInt() }
    val currentHeightDp = with(density) { containerSize.height.toDp().value.toInt() }
    val portraitWidthDp = min(currentWidthDp, currentHeightDp)
    val landscapeWidthDp = max(currentWidthDp, currentHeightDp)

    var compactText by remember(initialConfig) { mutableStateOf(initialConfig.compactColumns.toString()) }
    var mediumText by remember(initialConfig) { mutableStateOf(initialConfig.mediumColumns.toString()) }
    var expandedText by remember(initialConfig) { mutableStateOf(initialConfig.expandedColumns.toString()) }
    var largeText by remember(initialConfig) { mutableStateOf(initialConfig.largeColumns.toString()) }

    val compactCols = compactText.toSearchGridColumnsOrNull()
    val mediumCols = mediumText.toSearchGridColumnsOrNull()
    val expandedCols = expandedText.toSearchGridColumnsOrNull()
    val largeCols = largeText.toSearchGridColumnsOrNull()

    val currentConfig = SearchGridColumnsConfig(
        compactColumns = compactCols ?: initialConfig.compactColumns,
        mediumColumns = mediumCols ?: initialConfig.mediumColumns,
        expandedColumns = expandedCols ?: initialConfig.expandedColumns,
        largeColumns = largeCols ?: initialConfig.largeColumns,
    )

    val options = listOf(
        GridRangeOption(
            label = stringResource(R.string.search_grid_columns_range_compact),
            value = compactText,
            onValueChange = { compactText = it },
            isError = compactCols == null,
            isHighlighted = portraitWidthDp <= 600 || landscapeWidthDp <= 600,
            highlightLabels = buildList {
                if (portraitWidthDp <= 600) add(stringResource(R.string.search_grid_columns_current_portrait))
                if (landscapeWidthDp <= 600) add(stringResource(R.string.search_grid_columns_current_landscape))
            }
        ),
        GridRangeOption(
            label = stringResource(R.string.search_grid_columns_range_medium),
            value = mediumText,
            onValueChange = { mediumText = it },
            isError = mediumCols == null,
            isHighlighted = (portraitWidthDp in 601..900) || (landscapeWidthDp in 601..900),
            highlightLabels = buildList {
                if (portraitWidthDp in 601..900) add(stringResource(R.string.search_grid_columns_current_portrait))
                if (landscapeWidthDp in 601..900) add(stringResource(R.string.search_grid_columns_current_landscape))
            }
        ),
        GridRangeOption(
            label = stringResource(R.string.search_grid_columns_range_expanded),
            value = expandedText,
            onValueChange = { expandedText = it },
            isError = expandedCols == null,
            isHighlighted = (portraitWidthDp in 901..1200) || (landscapeWidthDp in 901..1200),
            highlightLabels = buildList {
                if (portraitWidthDp in 901..1200) add(stringResource(R.string.search_grid_columns_current_portrait))
                if (landscapeWidthDp in 901..1200) add(stringResource(R.string.search_grid_columns_current_landscape))
            }
        ),
        GridRangeOption(
            label = stringResource(R.string.search_grid_columns_range_large),
            value = largeText,
            onValueChange = { largeText = it },
            isError = largeCols == null,
            isHighlighted = portraitWidthDp >= 1201 || landscapeWidthDp >= 1201,
            highlightLabels = buildList {
                if (portraitWidthDp >= 1201) add(stringResource(R.string.search_grid_columns_current_portrait))
                if (landscapeWidthDp >= 1201) add(stringResource(R.string.search_grid_columns_current_landscape))
            }
        )
    )

    BaseGridConfigDialog(
        title = stringResource(R.string.search_grid_columns_title),
        hintText = stringResource(R.string.search_grid_columns_dialog_hint),
        widthHintText = stringResource(
            R.string.search_grid_columns_current_width_hint,
            portraitWidthDp,
            landscapeWidthDp
        ),
        bucketHintText = stringResource(
            R.string.search_grid_columns_current_bucket_hint,
            searchGridColumnsBucketLabel(portraitWidthDp),
            currentConfig.columnsForWidthDp(portraitWidthDp),
            searchGridColumnsBucketLabel(landscapeWidthDp),
            currentConfig.columnsForWidthDp(landscapeWidthDp)
        ),
        options = options,
        isDecimal = false, // 只允许整数
        canConfirm = compactCols != null && mediumCols != null && expandedCols != null && largeCols != null,
        onDismiss = onDismiss,
        onReset = {
            compactText = SearchGridColumnsConfig.DEFAULT_COMPACT_COLUMNS.toString()
            mediumText = SearchGridColumnsConfig.DEFAULT_MEDIUM_COLUMNS.toString()
            expandedText = SearchGridColumnsConfig.DEFAULT_EXPANDED_COLUMNS.toString()
            largeText = SearchGridColumnsConfig.DEFAULT_LARGE_COLUMNS.toString()
        },
        onConfirm = { onConfirm(currentConfig) }
    )
}

private fun String.toSearchGridColumnsOrNull(): Int? {
    val parsed = toIntOrNull() ?: return null
    return parsed.takeIf { it in 1..12 }
}

@Composable
private fun searchGridColumnsBucketLabel(widthDp: Int): String {
    return when {
        widthDp <= 600 -> stringResource(R.string.search_grid_columns_range_compact)
        widthDp <= 900 -> stringResource(R.string.search_grid_columns_range_medium)
        widthDp <= 1200 -> stringResource(R.string.search_grid_columns_range_expanded)
        else -> stringResource(R.string.search_grid_columns_range_large)
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SearchGridColumnsDialogPreview() {
    ComponentPreview {
        SearchGridColumnsDialog(
            initialConfig = SearchGridColumnsConfig(),
            onDismiss = {},
            onConfirm = {}
        )
    }
}