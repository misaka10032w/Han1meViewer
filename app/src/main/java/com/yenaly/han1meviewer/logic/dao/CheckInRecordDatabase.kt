package com.yenaly.han1meviewer.logic.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity

@Database(
    entities = [CheckInRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CheckInRecordDatabase : RoomDatabase() {
    abstract fun checkInDao(): CheckInRecordDao

    companion object {
        @Volatile
        private var INSTANCE: CheckInRecordDatabase? = null

        fun getDatabase(context: Context): CheckInRecordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CheckInRecordDatabase::class.java,
                    "check_in_records"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}