package com.yenaly.han1meviewer.util

import androidx.annotation.IntRange
import java.util.Locale

private val SI_UNITS = arrayOf("B", "kB", "MB", "GB", "TB")
private val IEC_UNITS = arrayOf("B", "KiB", "MiB", "GiB", "TiB")

fun Long.formatFileSizeV2(
    useSi: Boolean = false,
    @IntRange(from = 0) decimalPlaces: Int = 1,
    stripTrailingZeros: Boolean = true,
): String {
    val unit = if (useSi) 1000 else 1024
    if (this < unit) return "$this B"

    val units = if (useSi) SI_UNITS else IEC_UNITS

    var value = this.toDouble()
    var unitIndex = 0

    while (value >= unit && unitIndex < units.size - 1) {
        value /= unit
        unitIndex++
    }

    return if (decimalPlaces == 0 || (stripTrailingZeros && value % 1 == 0.0)) {
        "%.0f %s".format(Locale.getDefault(), value, units[unitIndex])
    } else {
        "%.${decimalPlaces}f %s".format(Locale.getDefault(), value, units[unitIndex])
    }
}

fun Long.formatBytesPerSecond(
    useSi: Boolean = false,
    @IntRange(from = 0) decimalPlaces: Int = 1,
    stripTrailingZeros: Boolean = true,
): String = formatFileSizeV2(useSi, decimalPlaces, stripTrailingZeros) + "/s"
