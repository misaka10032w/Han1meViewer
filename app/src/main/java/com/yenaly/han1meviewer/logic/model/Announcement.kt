package com.yenaly.han1meviewer.logic.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Announcement(
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val priority: Int = 2,
    val imageUrl: String = "",
    @JvmField val isActive: Boolean = false
) {
    fun getFormattedDate(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(timestamp * 1000))
    }
}