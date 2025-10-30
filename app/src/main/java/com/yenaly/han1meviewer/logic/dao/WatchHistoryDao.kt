package com.yenaly.han1meviewer.logic.dao

import androidx.room.*
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/07/02 002 16:47
 */
@Dao
abstract class WatchHistoryDao {

    @Query("SELECT * FROM WatchHistoryEntity ORDER BY id DESC")
    abstract fun loadAll(): Flow<MutableList<WatchHistoryEntity>>

    @Delete
    abstract suspend fun delete(history: WatchHistoryEntity)

    @Query("DELETE FROM WatchHistoryEntity")
    abstract suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(history: WatchHistoryEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(history: WatchHistoryEntity)

    @Query("SELECT * FROM WatchHistoryEntity WHERE (`videoCode` = :videoCode) LIMIT 1")
    abstract suspend fun findBy(videoCode: String): WatchHistoryEntity?

    @Query("SELECT videoCode FROM WatchHistoryEntity WHERE videoCode IN (:codes)")
    abstract suspend fun getWatchedCodes(codes: List<String>): List<String>

    @Query("UPDATE WatchHistoryEntity SET progress = :progress WHERE videoCode = :videoCode")
    abstract suspend fun updateProgress(videoCode: String, progress: Long)

    @Transaction
    open suspend fun insertOrUpdate(history: WatchHistoryEntity) {
        val dbEntity = findBy(history.videoCode)
        if (dbEntity != null) {
            val merged = history.copy(
                id = dbEntity.id,
                progress = if (history.progress == 0L) dbEntity.progress else history.progress
            )
            update(merged)
        } else {
            insert(history)
        }
    }
}