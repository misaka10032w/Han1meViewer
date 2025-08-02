package com.yenaly.han1meviewer.logic.model

import androidx.annotation.Keep
import com.yenaly.han1meviewer.LOCAL_DATE_TIME_FORMAT
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

@Keep
data class Announcement(
    @JvmField val title: String = "",
    @JvmField val content: String = "",
    @JvmField val timestamp: Long = 0,
    @JvmField val priority: Int = 2,
    @JvmField val imageUrl: String = "",
    @JvmField val isActive: Boolean = false
) {
    @OptIn(ExperimentalTime::class)
    fun getFormattedDate(): String {
        return kotlin.time.Instant
            .fromEpochSeconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LOCAL_DATE_TIME_FORMAT)
    }
}