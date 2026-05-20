package com.yenaly.han1meviewer

data class SearchGridColumnsConfig(
    val compactColumns: Int = DEFAULT_COMPACT_COLUMNS,
    val mediumColumns: Int = DEFAULT_MEDIUM_COLUMNS,
    val expandedColumns: Int = DEFAULT_EXPANDED_COLUMNS,
    val largeColumns: Int = DEFAULT_LARGE_COLUMNS,
) {
    fun columnsForWidthDp(screenWidthDp: Int): Int {
        return when {
            screenWidthDp <= 600 -> compactColumns
            screenWidthDp <= 900 -> mediumColumns
            screenWidthDp <= 1200 -> expandedColumns
            else -> largeColumns
        }.coerceAtLeast(1)
    }

    companion object {
        const val DEFAULT_COMPACT_COLUMNS = 2
        const val DEFAULT_MEDIUM_COLUMNS = 3
        const val DEFAULT_EXPANDED_COLUMNS = 4
        const val DEFAULT_LARGE_COLUMNS = 5
    }
}
