package com.yenaly.han1meviewer.logic.model

import androidx.annotation.Keep
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@Keep
data class Announcement(
    @JvmField val title: String = "",
    @JvmField val content: String = "",
    @JvmField val timestamp: Long = 0,
    @JvmField val priority: Int = 2,
    @JvmField val imageUrl: String = "",
    @JvmField val isActive: Boolean = false
) {
    fun getFormattedDate(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(timestamp * 1000))
    }
}