package com.yenaly.han1meviewer.ui.screen.settings.model

data class GridRangeOption(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit,
    val isError: Boolean,
    val isHighlighted: Boolean,
    val highlightLabels: List<String> = emptyList()
)