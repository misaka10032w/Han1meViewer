package com.yenaly.han1meviewer.logic.dao

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yenaly.han1meviewer.logic.dao.download.DownloadCategoryDao
import com.yenaly.han1meviewer.logic.dao.download.HanimeDownloadDao
import com.yenaly.han1meviewer.logic.entity.download.DownloadCategoryEntity
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeCategoryCrossRef
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.state.DownloadState
import com.yenaly.yenaly_libs.utils.applicationContext

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/08/07 007 18:26
 */
@Database(
    entities = [HanimeDownloadEntity::class, DownloadCategoryEntity::class, HanimeCategoryCrossRef::class, DownloadGroupEntity::class],
    version = 5, exportSchema = false
)
abstract class DownloadDatabase : RoomDatabase() {

    abstract val hanimeDownloadDao: HanimeDownloadDao
    abstract val downloadCategoryDao: DownloadCategoryDao
    abstract val downloadGroupDao: DownloadGroupDao

    companion object {
        val instance by lazy {
            Room.databaseBuilder(
                applicationContext,
                DownloadDatabase::class.java,
                "download.db"
            ).addMigrations(Migration1To2, Migration2To3, Migration3To4, Migration4To5).build()
        }
    }

    object Migration1To2 : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `HanimeDownloadEntity`(
                    `coverUrl` TEXT NOT NULL, `title` TEXT NOT NULL,
                    `addDate` INTEGER NOT NULL, `videoCode` TEXT NOT NULL,
                    `videoUri` TEXT NOT NULL, `quality` TEXT NOT NULL,
                    `videoUrl` TEXT NOT NULL, `length` INTEGER NOT NULL,
                    `downloadedLength` INTEGER NOT NULL, `isDownloading` INTEGER NOT NULL,
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)""".trimIndent()
            )
            db.execSQL(
                """INSERT INTO `HanimeDownloadEntity`(
                        `coverUrl`, `title`, `addDate`,
                        `videoCode`, `videoUri`, `quality`,
                        `videoUrl`, `length`, `downloadedLength`, `isDownloading`, `id`)
                     SELECT `coverUrl`, `title`, `addDate`, `videoCode`, `videoUri`, `quality`,
                        '' AS `videoUrl`, 1 AS `length`, 1 AS `downloadedLength`, 0 AS `isDownloading`,
                        `id`
                     FROM `HanimeDownloadedEntity`""".trimIndent()
            )
            db.execSQL("""DROP TABLE IF EXISTS HanimeDownloadedEntity""")
        }
    }

    object Migration2To3 : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `DownloadCategoryEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `HanimeCategoryCrossRef` (`videoId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, PRIMARY KEY(`videoId`, `categoryId`))"""
            )
            db.execSQL("""CREATE INDEX IF NOT EXISTS `index_HanimeCategoryCrossRef_categoryId` ON `HanimeCategoryCrossRef` (`categoryId`)""")
            // Add coverUri column
            db.execSQL("""ALTER TABLE `HanimeDownloadEntity` ADD COLUMN `coverUri` TEXT NULL""")

            // Add state column with default value (convert from isDownloading)
            db.execSQL("""ALTER TABLE `HanimeDownloadEntity` ADD COLUMN `state` INTEGER NOT NULL DEFAULT ${DownloadState.Mask.UNKNOWN}""")

            // Update state values based on isDownloading
            // If isDownloading=1, set state to DOWNLOADING (2)
            // If isDownloading=0,
            //                     if downloadedLength=length, set state to FINISHED (4)
            //                     else set state to PAUSED (3)
            db.execSQL(
                """UPDATE `HanimeDownloadEntity` SET `state` = 
                    |CASE WHEN `isDownloading` = 1 THEN ${DownloadState.Mask.DOWNLOADING} ELSE 
                    |CASE WHEN `downloadedLength` = `length` THEN ${DownloadState.Mask.FINISHED} 
                    |ELSE ${DownloadState.Mask.PAUSED} END END""".trimMargin()
            )
        }
    }

    object Migration3To4 : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `HanimeDownloadEntity_new` (
                `coverUrl` TEXT NOT NULL,
                `coverUri` TEXT,
                `title` TEXT NOT NULL,
                `addDate` INTEGER NOT NULL,
                `videoCode` TEXT NOT NULL,
                `videoUri` TEXT NOT NULL,
                `quality` TEXT NOT NULL,
                `videoUrl` TEXT NOT NULL DEFAULT '',
                `length` INTEGER NOT NULL DEFAULT 1,
                `downloadedLength` INTEGER NOT NULL DEFAULT 0,
                `state` INTEGER NOT NULL DEFAULT ${DownloadState.Mask.UNKNOWN},
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
            )
            """.trimIndent()
            )
            db.execSQL(
                """
            INSERT INTO `HanimeDownloadEntity_new` (
                coverUrl, coverUri, title, addDate,
                videoCode, videoUri, quality, videoUrl,
                length, downloadedLength, state, id
            )
            SELECT 
                coverUrl, coverUri, title, addDate,
                videoCode, videoUri, quality,
                videoUrl, length, downloadedLength, state, id
            FROM `HanimeDownloadEntity`
            """.trimIndent()
            )
            db.execSQL("DROP TABLE `HanimeDownloadEntity`")
            db.execSQL("ALTER TABLE `HanimeDownloadEntity_new` RENAME TO `HanimeDownloadEntity`")
        }
    }
    object Migration4To5 : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `download_groups` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `name` TEXT NOT NULL,
                    `orderIndex` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO `download_groups` (`id`, `name`, `orderIndex`) 
                VALUES (${DownloadGroupEntity.DEFAULT_GROUP_ID}, '${DownloadGroupEntity.DEFAULT_GROUP_NAME}', 0)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE `HanimeDownloadEntity_new` (
                    `coverUrl` TEXT NOT NULL,
                    `coverUri` TEXT,
                    `title` TEXT NOT NULL,
                    `addDate` INTEGER NOT NULL,
                    `videoCode` TEXT NOT NULL,
                    `videoUri` TEXT NOT NULL,
                    `quality` TEXT NOT NULL,
                    `videoUrl` TEXT NOT NULL DEFAULT '',
                    `length` INTEGER NOT NULL DEFAULT 1,
                    `downloadedLength` INTEGER NOT NULL DEFAULT 0,
                    `state` INTEGER NOT NULL DEFAULT ${DownloadState.Mask.UNKNOWN},
                    `groupId` INTEGER NOT NULL DEFAULT ${DownloadGroupEntity.DEFAULT_GROUP_ID},
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    FOREIGN KEY(`groupId`) REFERENCES `download_groups`(`id`) ON UPDATE NO ACTION ON DELETE SET DEFAULT
                )
                """.trimIndent()
            )

            db.execSQL("""CREATE INDEX IF NOT EXISTS `index_HanimeDownloadEntity_groupId` ON `HanimeDownloadEntity_new` (`groupId`)""")

            db.execSQL(
                """
                INSERT INTO `HanimeDownloadEntity_new` (
                    coverUrl, coverUri, title, addDate,
                    videoCode, videoUri, quality, videoUrl,
                    length, downloadedLength, state, id,
                    groupId
                )
                SELECT 
                    coverUrl, coverUri, title, addDate,
                    videoCode, videoUri, quality,
                    videoUrl, length, downloadedLength, state, id,
                    ${DownloadGroupEntity.DEFAULT_GROUP_ID} AS groupId
                FROM `HanimeDownloadEntity`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `HanimeDownloadEntity`")
            db.execSQL("ALTER TABLE `HanimeDownloadEntity_new` RENAME TO `HanimeDownloadEntity`")
        }
    }

}