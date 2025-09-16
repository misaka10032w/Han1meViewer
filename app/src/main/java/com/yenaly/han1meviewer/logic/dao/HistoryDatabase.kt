package com.yenaly.han1meviewer.logic.dao

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yenaly.han1meviewer.logic.entity.HanimeAdvancedSearchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.SearchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.yenaly_libs.utils.applicationContext

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/22 022 22:46
 */
@Database(
    entities = [SearchHistoryEntity::class,
        WatchHistoryEntity::class,
        HanimeAdvancedSearchHistoryEntity::class],
    version = 4, exportSchema = false
)
abstract class HistoryDatabase : RoomDatabase() {

    abstract val searchHistory: SearchHistoryDao

    abstract val watchHistory: WatchHistoryDao

    abstract val hanimeAdvancedSearchHistory: HanimeAdvancedSearchHistoryDao

    companion object {
        val instance by lazy {
            Room.databaseBuilder(
                applicationContext,
                HistoryDatabase::class.java,
                "history.db"
            ).addMigrations(
                Migration1To2,
                Migration2To3,
                Migration3To4
            ).build()
        }
    }

    object Migration1To2 : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {

            val cursor = db.query(
                """SELECT id, redirectLink FROM WatchHistoryEntity"""
            )
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val url = cursor.getString(cursor.getColumnIndexOrThrow("redirectLink"))
                val videoCode =
                    url.substringAfter("v=") // 不用 String.toVideoCode() 的原因是，防止該拓展函數因不可抗力改變導致 migrate 失敗
                val values = contentValuesOf("redirectLink" to videoCode)
                db.update(
                    "WatchHistoryEntity",
                    SQLiteDatabase.CONFLICT_REPLACE,
                    values,
                    "id = ?", arrayOf(id)
                )
            }
            db.execSQL(
                """ALTER TABLE WatchHistoryEntity
                   RENAME COLUMN redirectLink TO videoCode"""
            )
        }
    }
    object Migration2To3 : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 增加播放进度列，默认值为 0
            db.execSQL(
                """ALTER TABLE WatchHistoryEntity
                   ADD COLUMN progress INTEGER NOT NULL DEFAULT 0"""
            )
        }
    }
    object Migration3To4 : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `HanimeAdvancedSearchHistory` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `query` TEXT,
                    `genre` TEXT,
                    `sort` TEXT,
                    `broad` INTEGER,
                    `date` TEXT,
                    `duration` TEXT,
                    `tags` TEXT,
                    `brands` TEXT,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }
}



