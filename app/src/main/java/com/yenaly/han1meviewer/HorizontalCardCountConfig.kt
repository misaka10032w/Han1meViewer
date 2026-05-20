package com.yenaly.han1meviewer

data class HorizontalCardCountConfig(
    val narrowCount: Float = DEFAULT_NARROW_COUNT,
    val compactCount: Float = DEFAULT_COMPACT_COUNT,
    val mediumCount: Float = DEFAULT_MEDIUM_COUNT,
    val expandedCount: Float = DEFAULT_EXPANDED_COUNT,
) {
    fun countForWidthDp(screenWidthDp: Int): Float {
        return when {
            screenWidthDp < 350 -> narrowCount
            screenWidthDp < 600 -> compactCount
            screenWidthDp < 840 -> mediumCount
            else -> expandedCount
        }
    }

    companion object {
        const val DEFAULT_NARROW_COUNT = 1.5f
        const val DEFAULT_COMPACT_COUNT = 2.1f
        const val DEFAULT_MEDIUM_COUNT = 4.1f
        const val DEFAULT_EXPANDED_COUNT = 5.1f
    }
}
