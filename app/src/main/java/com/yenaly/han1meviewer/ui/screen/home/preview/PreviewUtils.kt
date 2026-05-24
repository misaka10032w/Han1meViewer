package com.yenaly.han1meviewer.ui.screen.home.preview

import androidx.compose.foundation.lazy.LazyListState

/**
 * 根据年份和月份生成日期码（yyyyMM 格式）。
 *
 * @param year 年份
 * @param month 月份 (1-12)
 * @return 日期码字符串，如 "202401"
 */
internal fun currentCodeFrom(year: Int, month: Int): String = "%04d%02d".format(year, month)

/**
 * 获取当前月份对应的日期码。
 *
 * @return 本月日期码字符串
 */
internal fun currentDateCode(): String {
    val now = java.time.LocalDate.now()
    return currentCodeFrom(now.year, now.monthValue)
}

/**
 * 将日期码转换为可读的标签格式（yyyy/MM）。
 *
 * @param code 日期码，如 "202401"
 * @return 标签字符串，如 "2024/1"
 */
internal fun toNormalDateLabel(code: String): String {
    val year = code.substring(0, 4).toInt()
    val month = code.substring(4, 6).toInt()
    return "$year/$month"
}

/**
 * 将日期码按指定偏移量移动月份。
 *
 * @param code 日期码，如 "202401"
 * @param delta 偏移月数（负数表示向前）
 * @return 新日期码
 */
internal fun shiftMonthCode(code: String, delta: Int): String {
    var year = code.substring(0, 4).toInt()
    var month = code.substring(4, 6).toInt() + delta
    while (month < 1) {
        month += 12
        year -= 1
    }
    while (month > 12) {
        month -= 12
        year += 1
    }
    return currentCodeFrom(year, month)
}

/**
 * 将预览游览列表居中到指定项。
 *
 * @param listState LazyList 状态
 * @param index 目标索引
 */
internal suspend fun centerPreviewTourItem(
    listState: LazyListState,
    index: Int,
) {
    if (index < 0) return
    listState.animateScrollToItem(index)
}
