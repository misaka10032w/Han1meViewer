package com.yenaly.han1meviewer.logic.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/22 022 18:16
 */
@Entity
data class SearchHistoryEntity(
    val query: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)

@Entity(tableName = "HanimeAdvancedSearchHistory")
data class HanimeAdvancedSearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String? = null,
    val genre: String? = null,
    val sort: String? = null,
    val broad: Boolean? = null,
    val date: String? = null,
    val duration: String? = null,
    val tags: String? = null,
    val brands: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
