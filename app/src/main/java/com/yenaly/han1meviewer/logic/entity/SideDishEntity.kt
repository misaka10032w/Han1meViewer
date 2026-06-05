package com.yenaly.han1meviewer.logic.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "sidedishes")
data class SideDishEntity(
    @PrimaryKey val videoCode: String,
    val title: String,
    val coverUrl: String,
)
