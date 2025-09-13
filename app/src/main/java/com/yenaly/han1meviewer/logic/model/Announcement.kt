package com.yenaly.han1meviewer.logic.model

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.Keep
import com.yenaly.han1meviewer.LOCAL_DATE_TIME_FORMAT
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

@Keep
data class Announcement(
    @JvmField val title: String,
    @JvmField val content: String,
    @JvmField val positiveText: String ? = null,
    @JvmField val negativeText: String ? = null,
    @JvmField val timestamp: Long = 0,
    @JvmField val priority: Int = 1,
    @JvmField val imageUrl: String ? = null,
    @JvmField val isActive: Boolean = false
) {
    //firebase初始化需要一个空的构造函数
    constructor() : this("", "", null, null, 0L, 1, null, false)
    @OptIn(ExperimentalTime::class)
    fun getFormattedDate(): String {
        return kotlin.time.Instant
            .fromEpochSeconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LOCAL_DATE_TIME_FORMAT)
    }
    fun getHighlightedContent(): Spanned {
        val regex = "(https?://[\\w-]+(\\.[\\w-]+)+([/?%&=]*)?)".toRegex()
        val spannableContent = SpannableString(content)
        val matches = regex.findAll(content)

        for (match in matches) {
            spannableContent.setSpan(
                ForegroundColorSpan(Color.BLUE),
                match.range.first, match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableContent
    }
}