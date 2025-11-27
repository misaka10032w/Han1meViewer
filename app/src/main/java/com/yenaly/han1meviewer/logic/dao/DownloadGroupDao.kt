package com.yenaly.han1meviewer.logic.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import kotlinx.coroutines.flow.Flow

/**
 * 已下载分组的DAO
 *
 * @project Han1meViewer
 *
 * @author Misaka10032w - 创建 (2025/11/27)
 * 初始版本
 * 实现分组展示和展开/折叠功能
 * 实现分组移动、重命名等
 */

@Dao
interface DownloadGroupDao {
    /**
     * 插入默认分组
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultGroup(group: DownloadGroupEntity = DownloadGroupEntity(
        name = DownloadGroupEntity.DEFAULT_GROUP_NAME,
        orderIndex = 0,
        id = DownloadGroupEntity.DEFAULT_GROUP_ID
    )): Long

    /**
     * 插入新的分组
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: DownloadGroupEntity): Long

    /**
     * 更新现有分组
     */
    @Update
    suspend fun update(group: DownloadGroupEntity)

    /**
     * 删除指定分组，配合[resetVideosGroupToDefault]食用
     */
    @Delete
    suspend fun delete(group: DownloadGroupEntity)

    /**
     * 获取所有自定义分组，并按 orderIndex 排序。
     */
    @Query("SELECT * FROM download_groups ORDER BY orderIndex ASC")
    fun getAllGroups(): Flow<List<DownloadGroupEntity>>

    /**
     * 通过 ID 获取分组
     */
    @Query("SELECT * FROM download_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Int): DownloadGroupEntity?

    /**
     * 获取最大分组ID
     */
    @Query("SELECT MAX(orderIndex) FROM download_groups")
    suspend fun getMaxOrderIndex(): Int?

    /**
     * 将某个分组下的所有视频移到默认分组 (DEFAULT_GROUP_ID) 下。
     * 用于删除分组时的清理操作。
     */
    @Query("UPDATE HanimeDownloadEntity SET groupId = ${DownloadGroupEntity.DEFAULT_GROUP_ID} WHERE groupId = :oldGroupId")
    suspend fun resetVideosGroupToDefault(oldGroupId: Int)
}