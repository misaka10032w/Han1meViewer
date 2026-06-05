package com.yenaly.han1meviewer.logic

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.dao.HistoryDatabase
import com.yenaly.han1meviewer.logic.dao.MiscellanyDatabase
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.SideDishEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.download.DownloadCategoryEntity
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeCategoryCrossRef
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.OutputStream

object BackupManager {
    private const val BACKUP_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    @Serializable
    private data class BackupData(
        val version: Int = BACKUP_VERSION,
        val appVersionCode: Int = BuildConfig.VERSION_CODE,
        val appVersionName: String = BuildConfig.VERSION_NAME,
        val exportedAt: Long = System.currentTimeMillis(),
        val settings: Map<String, PreferenceValue>? = null,
        val hKeyframes: List<HKeyframeEntity>? = null,
        val checkInRecords: List<CheckInRecordEntity>? = null,
        val sideDishes: List<SideDishEntity>? = null,
        val watchHistories: List<WatchHistoryEntity>? = null,
        val downloadGroups: List<DownloadGroupEntity>? = null,
        val downloads: List<HanimeDownloadEntity>? = null,
        val downloadCategories: List<DownloadCategoryEntity>? = null,
        val downloadCategoryCrossRefs: List<HanimeCategoryCrossRef>? = null,
    )

    @Serializable
    private sealed interface PreferenceValue {
        @Serializable
        data class BooleanValue(val value: Boolean) : PreferenceValue

        @Serializable
        data class FloatValue(val value: Float) : PreferenceValue

        @Serializable
        data class IntValue(val value: Int) : PreferenceValue

        @Serializable
        data class LongValue(val value: Long) : PreferenceValue

        @Serializable
        data class StringValue(val value: String) : PreferenceValue

        @Serializable
        data class StringSetValue(val value: Set<String>) : PreferenceValue
    }

    suspend fun exportTo(context: Context, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            exportTo(context, outputStream)
        } ?: error("Unable to open backup file")
    }

    suspend fun importFrom(context: Context, uri: Uri) {
        val backup = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            json.decodeFromString<BackupData>(inputStream.bufferedReader().readText())
        } ?: error("Unable to open backup file")

        backup.hKeyframes?.let { hKeyframes ->
            MiscellanyDatabase.instance.hKeyframeDao.apply {
                deleteAll()
                insertAll(hKeyframes)
            }
        }

        backup.checkInRecords?.let { checkInRecords ->
            CheckInRecordDatabase.getDatabase(context).checkInDao().apply {
                deleteAll()
                insertAll(checkInRecords.map { it.normalizeSideDishes() })
            }
        }

        backup.sideDishes?.let { sideDishes ->
            CheckInRecordDatabase.getDatabase(context).sideDishDao().apply {
                deleteAll()
                insertAll(sideDishes)
            }
        }

        backup.watchHistories?.let { watchHistories ->
            HistoryDatabase.instance.watchHistory.apply {
                deleteAll()
                insertAll(watchHistories)
            }
        }

        if (backup.downloadGroups != null || backup.downloads != null ||
            backup.downloadCategories != null || backup.downloadCategoryCrossRefs != null
        ) {
            val downloadGroups = backup.downloadGroups.orEmpty()
            val groupIds = downloadGroups.mapTo(mutableSetOf()) { it.id } +
                    DownloadGroupEntity.DEFAULT_GROUP_ID
            val downloads = backup.downloads.orEmpty().map { download ->
                if (download.groupId in groupIds) {
                    download
                } else {
                    download.copy(groupId = DownloadGroupEntity.DEFAULT_GROUP_ID)
                }
            }
            val downloadCategories = backup.downloadCategories.orEmpty()
            val downloadIds = downloads.mapTo(mutableSetOf()) { it.id }
            val categoryIds = downloadCategories.mapTo(mutableSetOf()) { it.id }
            val crossRefs = backup.downloadCategoryCrossRefs.orEmpty().filter { crossRef ->
                crossRef.videoId in downloadIds && crossRef.categoryId in categoryIds
            }

            DownloadDatabase.instance.apply {
                downloadCategoryDao.deleteAllCrossRefs()
                hanimeDownloadDao.deleteAll()
                downloadCategoryDao.deleteAllCategories()
                downloadGroupDao.deleteAll()
                downloadGroupDao.insertAll(downloadGroups)
                downloadGroupDao.insertDefaultGroup()
                downloadCategoryDao.insertAllCategories(downloadCategories)
                hanimeDownloadDao.insertAll(downloads)
                downloadCategoryDao.insertAllCrossRefs(crossRefs)
            }
        }

        backup.settings?.let { settings ->
            Preferences.preferenceSp.edit {
                settings.forEach { (key, value) ->
                    when (value) {
                        is PreferenceValue.BooleanValue -> putBoolean(key, value.value)
                        is PreferenceValue.FloatValue -> putFloat(key, value.value)
                        is PreferenceValue.IntValue -> putInt(key, value.value)
                        is PreferenceValue.LongValue -> putLong(key, value.value)
                        is PreferenceValue.StringSetValue -> putStringSet(key, value.value)
                        is PreferenceValue.StringValue -> putString(key, value.value)
                    }
                }
            }
        }
    }

    private suspend fun exportTo(context: Context, outputStream: OutputStream) {
        val backup = BackupData(
            settings = Preferences.preferenceSp.all.mapValuesNotNull { (_, value) ->
                value.toPreferenceValue()
            },
            hKeyframes = MiscellanyDatabase.instance.hKeyframeDao.getAll(),
            checkInRecords = CheckInRecordDatabase.getDatabase(context).checkInDao().getAllRecords(),
            sideDishes = CheckInRecordDatabase.getDatabase(context).sideDishDao().getAll(),
            watchHistories = HistoryDatabase.instance.watchHistory.getAll(),
            downloadGroups = DownloadDatabase.instance.downloadGroupDao.getAllGroupsOnce(),
            downloads = DownloadDatabase.instance.hanimeDownloadDao.getAll(),
            downloadCategories = DownloadDatabase.instance.downloadCategoryDao.getAllCategoriesOnce(),
            downloadCategoryCrossRefs = DownloadDatabase.instance.downloadCategoryDao.getAllCrossRefs(),
        )
        outputStream.bufferedWriter().use { writer ->
            writer.write(json.encodeToString(backup))
        }
    }

    private inline fun <K, V, R : Any> Map<K, V>.mapValuesNotNull(
        transform: (Map.Entry<K, V>) -> R?
    ): Map<K, R> {
        return mapNotNull { entry -> transform(entry)?.let { entry.key to it } }.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.toPreferenceValue(): PreferenceValue? {
        return when (this) {
            is Boolean -> PreferenceValue.BooleanValue(this)
            is Float -> PreferenceValue.FloatValue(this)
            is Int -> PreferenceValue.IntValue(this)
            is Long -> PreferenceValue.LongValue(this)
            is String -> PreferenceValue.StringValue(this)
            is Set<*> -> PreferenceValue.StringSetValue(this.filterIsInstance<String>().toSet())
            else -> null
        }
    }

    private fun CheckInRecordEntity.normalizeSideDishes(): CheckInRecordEntity {
        if (sideDishes.isBlank()) return this
        val normalized = sideDishes
            .replace("\\u001E", "\u001E")
            .replace("\\u001e", "\u001E")
            .split(",")
            .joinToString(",") { item ->
                if (item.contains("\u001E") || !item.contains("|")) {
                    item
                } else {
                    val title = item.substringBefore("|")
                    val videoCode = item.substringAfter("|", "")
                    if (videoCode.isBlank()) title else "$title\u001E$videoCode"
                }
            }
        return copy(sideDishes = normalized)
    }
}
