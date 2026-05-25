package com.yenaly.han1meviewer.logic.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity

@Dao
interface CheckInRecordDao {
    @Insert
    suspend fun insert(record: CheckInRecordEntity): Long

    @Insert
    suspend fun insertAll(records: List<CheckInRecordEntity>)

    @Delete
    suspend fun delete(record: CheckInRecordEntity)

    @Query("SELECT * FROM check_in_records WHERE date = :date ORDER BY id ASC")
    suspend fun getRecordsByDate(date: String): List<CheckInRecordEntity>

    @Query("SELECT COUNT(*) FROM check_in_records WHERE date = :date")
    suspend fun getCountByDate(date: String): Int

    @Query("SELECT DISTINCT date FROM check_in_records WHERE date LIKE :yearMonth || '%'")
    suspend fun getMonthlyCheckedDates(yearMonth: String): List<String>

    @Query("SELECT COUNT(*) FROM check_in_records WHERE date LIKE :yearMonth || '%'")
    suspend fun getMonthlyCheckInTotal(yearMonth: String): Int

    @Query("SELECT * FROM check_in_records WHERE date BETWEEN :start AND :end ORDER BY date ASC, id ASC")
    suspend fun getRecordsBetween(start: String, end: String): List<CheckInRecordEntity>

    @Query("SELECT * FROM check_in_records WHERE date LIKE :year || '%' ORDER BY date ASC, id ASC")
    suspend fun getYearlyRecords(year: String): List<CheckInRecordEntity>

    @Query("SELECT * FROM check_in_records ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentRecords(limit: Int): List<CheckInRecordEntity>

    @Query("SELECT DISTINCT sideDishes FROM check_in_records WHERE sideDishes != '' ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentSideDishesTexts(limit: Int): List<String>

    @Query("SELECT * FROM check_in_records ORDER BY id ASC")
    suspend fun getAllRecords(): List<CheckInRecordEntity>

    @Query("DELETE FROM check_in_records")
    suspend fun deleteAll()
}
