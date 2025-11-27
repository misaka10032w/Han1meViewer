package com.yenaly.han1meviewer.logic.entity.download

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 已下载分组的Entity
 *
 * @project Han1meViewer
 *
 * @author Misaka10032w - 创建 (2025/11/27)
 * 初始版本
 * 实现分组展示和展开/折叠功能
 * 实现分组移动、重命名等
 */

@Entity(tableName = "download_groups")
data class DownloadGroupEntity(
    val name: String,

    val orderIndex: Int,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
) {
    companion object {
        const val DEFAULT_GROUP_ID = 1
        const val DEFAULT_GROUP_NAME = "Default Group"
    }
}