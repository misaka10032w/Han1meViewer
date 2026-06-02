package com.yenaly.han1meviewer.util

import com.yenaly.yenaly_libs.utils.LanguageHelper
import java.math.BigDecimal
import java.util.Locale

object DisplayTextLocalizer {

    private val viewsRegex = Regex("^(.+?)(万次|萬次|次)$")
    private val relativeTimeRegex = Regex("^(?:ge)?(.+?)(分钟|分鐘|小时|小時|天|周|週|个月|個月|年)前$")

    fun localizeViews(text: String): String {
        val match = viewsRegex.matchEntire(text.trim()) ?: return text
        val count = match.groupValues[1]
        val unit = match.groupValues[2]
        return when (language()) {
            Locale.SIMPLIFIED_CHINESE.language -> when (unit) {
                "万次", "萬次" -> "${count}万次"
                else -> "${count}次"
            }

            Locale.ENGLISH.language -> when (unit) {
                "万次", "萬次" -> "${count.toKViews()} views"
                else -> "$count views"
            }

            Locale.JAPANESE.language -> when (unit) {
                "万次", "萬次" -> "${count}万回"
                else -> "${count}回"
            }

            else -> when (unit) {
                "万次", "萬次" -> "${count}萬次"
                else -> "${count}次"
            }
        }
    }

    fun localizeRelativeTime(text: String): String {
        val match = relativeTimeRegex.matchEntire(text.trim()) ?: return text
        val count = match.groupValues[1]
        val unit = match.groupValues[2]
        return when (language()) {
            Locale.SIMPLIFIED_CHINESE.language -> "$count${unit.toSimplifiedUnit()}前"
            Locale.ENGLISH.language -> "$count ${unit.toEnglishUnit(count)} ago"
            Locale.JAPANESE.language -> "$count${unit.toJapaneseUnit()}前"
            else -> "$count${unit.toTraditionalUnit()}前"
        }
    }

    private fun language(): String = LanguageHelper.preferredLanguage.language

    private fun String.toSimplifiedUnit(): String = when (this) {
        "分鐘", "分钟" -> "分钟"
        "小時", "小时" -> "小时"
        "週", "周" -> "周"
        "個月", "个月" -> "个月"
        else -> this
    }

    private fun String.toTraditionalUnit(): String = when (this) {
        "分钟", "分鐘" -> "分鐘"
        "小时", "小時" -> "小時"
        "周", "週" -> "週"
        "个月", "個月" -> "個月"
        else -> this
    }

    private fun String.toJapaneseUnit(): String = when (this) {
        "分钟", "分鐘" -> "分"
        "小时", "小時" -> "時間"
        "天" -> "日"
        "周", "週" -> "週間"
        "个月", "個月" -> "か月"
        "年" -> "年"
        else -> this
    }

    private fun String.toEnglishUnit(count: String): String {
        val singular = count == "1"
        return when (this) {
            "分钟", "分鐘" -> if (singular) "minute" else "minutes"
            "小时", "小時" -> if (singular) "hour" else "hours"
            "天" -> if (singular) "day" else "days"
            "周", "週" -> if (singular) "week" else "weeks"
            "个月", "個月" -> if (singular) "month" else "months"
            "年" -> if (singular) "year" else "years"
            else -> this
        }
    }

    private fun String.toKViews(): String {
        return runCatching {
            BigDecimal(this).multiply(BigDecimal.TEN).stripTrailingZeros().toPlainString() + "K"
        }.getOrElse { "${this}0K" }
    }
}
