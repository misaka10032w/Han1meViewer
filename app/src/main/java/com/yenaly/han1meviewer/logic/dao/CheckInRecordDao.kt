package com.yenaly.han1meviewer.logic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity

@Dao
interface CheckInRecordDao {
    @Query("SELECT * FROM check_in_records WHERE date = :date")
    suspend fun getRecord(date: String): CheckInRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: CheckInRecordEntity)

    @Query("SELECT COUNT(*) FROM check_in_records WHERE count > 0 AND date LIKE :yearMonth || '%'")
    suspend fun getMonthlyCheckedDays(yearMonth: String): Int

    @Query("SELECT * FROM check_in_records WHERE date BETWEEN :start AND :end")
    suspend fun getRecordsBetween(start: String, end: String): List<CheckInRecordEntity>

    @Query("SELECT SUM(count) FROM check_in_records WHERE date LIKE :yearMonth || '%'")
    suspend fun getMonthlyCheckInTotal(yearMonth: String): Int?

}