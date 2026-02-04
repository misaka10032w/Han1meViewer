package com.yenaly.han1meviewer.logic

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.exception.TranslationException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

// ♧¥ separator for text blocks, { for tags
const val TEXT_SEPARATOR = "♧¥"
const val TAG_SEPARATOR = "{"

// Entity for translation cache
@Entity(tableName = "translation_cache")
data class TranslationCache(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalText: String,
    val translatedText: String,
    val sourceLang: String = "auto",
    val targetLang: String = "EN",
    val contentType: ContentType, // TITLE, DESCRIPTION, COMMENT, TAG, etc.
    val videoCode: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val apiKeyUsed: String = "",
    val charsConsumed: Int = 0
) {
    enum class ContentType {
        TITLE, DESCRIPTION, COMMENT, TAG, ARTIST_NAME, OTHER
    }
}

// Dao for translation cache
@Dao
interface TranslationCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: TranslationCache)
    
    @Query("SELECT * FROM translation_cache WHERE originalText = :original AND targetLang = :targetLang AND contentType = :contentType")
    suspend fun get(original: String, targetLang: String, contentType: TranslationCache.ContentType): TranslationCache?
    
    @Query("SELECT * FROM translation_cache WHERE videoCode = :videoCode ORDER BY timestamp DESC")
    suspend fun getByVideoCode(videoCode: String): List<TranslationCache>
    
    @Query("SELECT * FROM translation_cache ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TranslationCache>>
    
    @Query("DELETE FROM translation_cache WHERE id = :id")
    suspend fun delete(id: Int)
    
    @Query("DELETE FROM translation_cache WHERE contentType = :contentType")
    suspend fun deleteByType(contentType: TranslationCache.ContentType)
    
    @Query("DELETE FROM translation_cache")
    suspend fun deleteAll()
    
    @Query("SELECT SUM(charsConsumed) FROM translation_cache WHERE apiKeyUsed = :apiKey AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getCharsConsumed(apiKey: String, startTime: Long, endTime: Long): Long
}

// Database
@Database(entities = [TranslationCache::class], version = 1)
abstract class TranslationDatabase : RoomDatabase() {
    abstract fun cacheDao(): TranslationCacheDao
    
    companion object {
        @Volatile
        private var INSTANCE: TranslationDatabase? = null
        
        fun getInstance(context: Context): TranslationDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TranslationDatabase::class.java,
                    "translation.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}

// API Key with limits
data class TranslationApiKey(
    val key: String,
    val monthlyLimit: Int = 500000, // Default DeepL Free limit
    var charsUsedThisMonth: AtomicInteger = AtomicInteger(0),
    val isActive: Boolean = true,
    val lastReset: Long = System.currentTimeMillis()
) {
    fun resetIfNeeded(): Boolean {
        val now = System.currentTimeMillis()
        // Reset on 1st of every month
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = lastReset
        val lastMonth = calendar.get(java.util.Calendar.MONTH)
        calendar.timeInMillis = now
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        
        if (currentMonth != lastMonth) {
            charsUsedThisMonth.set(0)
            return true
        }
        return false
    }
    
    fun hasQuota(chars: Int): Boolean {
        return charsUsedThisMonth.get() + chars <= monthlyLimit
    }
    
    fun consume(chars: Int) {
        charsUsedThisMonth.addAndGet(chars)
    }
}

// Main Translation Manager
class TranslationManager private constructor(context: Context) {
    private val cacheDao = TranslationDatabase.getInstance(context).cacheDao()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var apiKeys = mutableListOf<TranslationApiKey>()
    private var currentApiKeyIndex = 0
    private var isEnabled = false
    private var batchSize = 30000 // 30KB limit per request
    private var targetLang = "EN"
    private var translateTitles = true
    private var translateDescriptions = true
    private var translateComments = true
    private var translateTags = true
    
    companion object {
        @Volatile
        private var INSTANCE: TranslationManager? = null
        
        fun getInstance(context: Context): TranslationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TranslationManager(context).also { INSTANCE = it }
            }
        }
    }
    
    // Initialize from preferences
    fun initialize() {
        val apiKeyStrings = Preferences.translationApiKeys
        apiKeys.clear()
        apiKeyStrings.forEach { key ->
            if (key.isNotBlank()) {
                apiKeys.add(TranslationApiKey(key, Preferences.translationMonthlyLimit))
            }
        }
        isEnabled = Preferences.isTranslationEnabled && apiKeys.isNotEmpty()
        targetLang = Preferences.translationTargetLang
        batchSize = Preferences.translationBatchSize
        translateTitles = Preferences.translateTitles
        translateDescriptions = Preferences.translateDescriptions
        translateComments = Preferences.translateComments
        translateTags = Preferences.translateTags
        
        // Reset monthly quotas if needed
        apiKeys.forEach { it.resetIfNeeded() }
    }
    
    // Get next available API key
    private fun getNextApiKey(requestChars: Int): TranslationApiKey? {
        if (apiKeys.isEmpty()) return null
        
        // Try current key first
        for (i in 0 until apiKeys.size) {
            val index = (currentApiKeyIndex + i) % apiKeys.size
            val apiKey = apiKeys[index]
            
            if (apiKey.isActive && apiKey.hasQuota(requestChars)) {
                currentApiKeyIndex = index
                return apiKey
            }
        }
        
        // If no key has quota, try any active key (will exceed limit)
        for (apiKey in apiKeys) {
            if (apiKey.isActive) {
                return apiKey
            }
        }
        
        return null
    }
    
    // Make DeepL API call
    private suspend fun callDeepLApi(
        texts: List<String>,
        targetLang: String,
        sourceLang: String? = null
    ): List<String> {
        val apiKey = getNextApiKey(texts.sumOf { it.length }) ?: throw TranslationException("No available API key")
        
        val formBody = FormBody.Builder()
            .add("auth_key", apiKey.key)
            .add("target_lang", targetLang)
        
        sourceLang?.let { formBody.add("source_lang", it) }
        
        // Join texts with separator
        val joinedText = texts.joinToString(TEXT_SEPARATOR)
        formBody.add("text", joinedText)
        
        val request = Request.Builder()
            .url("https://api-free.deepl.com/v2/translate")
            .post(formBody.build())
            .build()
        
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw TranslationException("API call failed: ${response.code}")
                }
                
                val body = response.body?.string() ?: throw TranslationException("Empty response")
                val json = JSONObject(body)
                val translations = json.getJSONArray("translations")
                
                val results = mutableListOf<String>()
                for (i in 0 until translations.length()) {
                    results.add(translations.getJSONObject(i).getString("text"))
                }
                
                // Split back using separator
                val allTranslated = if (results.isNotEmpty()) {
                    results.flatMap { it.split(TEXT_SEPARATOR) }
                } else {
                    emptyList()
                }
                
                // Record usage
                apiKey.consume(joinedText.length)
                
                allTranslated
            }
        }
    }
    
    // Smart batching
    private fun createBatches(texts: List<String>, maxBatchSize: Int = batchSize): List<List<String>> {
        val batches = mutableListOf<List<String>>()
        var currentBatch = mutableListOf<String>()
        var currentSize = 0
        
        for (text in texts) {
            val textSize = text.length
            
            // If single text is larger than batch size, split it
            if (textSize > maxBatchSize) {
                // First, add current batch if not empty
                if (currentBatch.isNotEmpty()) {
                    batches.add(currentBatch.toList())
                    currentBatch.clear()
                    currentSize = 0
                }
                
                // Split the large text
                val chunks = text.chunked(maxBatchSize - TEXT_SEPARATOR.length)
                for (chunk in chunks) {
                    if (currentSize + chunk.length + TEXT_SEPARATOR.length > maxBatchSize) {
                        batches.add(currentBatch.toList())
                        currentBatch.clear()
                        currentSize = 0
                    }
                    currentBatch.add(chunk)
                    currentSize += chunk.length + TEXT_SEPARATOR.length
                }
            } else {
                if (currentSize + textSize + TEXT_SEPARATOR.length > maxBatchSize) {
                    batches.add(currentBatch.toList())
                    currentBatch.clear()
                    currentSize = 0
                }
                currentBatch.add(text)
                currentSize += textSize + TEXT_SEPARATOR.length
            }
        }
        
        if (currentBatch.isNotEmpty()) {
            batches.add(currentBatch)
        }
        
        return batches
    }
    
    // Translate with caching
    suspend fun translate(
        originalText: String,
        contentType: TranslationCache.ContentType,
        videoCode: String? = null,
        forceFresh: Boolean = false
    ): String {
        if (!isEnabled || originalText.isBlank()) return originalText
        
        // Check cache first
        if (!forceFresh) {
            val cached = cacheDao.get(originalText, targetLang, contentType)
            if (cached != null) {
                return cached.translatedText
            }
        }
        
        // Skip if content type is disabled
        when (contentType) {
            TranslationCache.ContentType.TITLE -> if (!translateTitles) return originalText
            TranslationCache.ContentType.DESCRIPTION -> if (!translateDescriptions) return originalText
            TranslationCache.ContentType.COMMENT -> if (!translateComments) return originalText
            TranslationCache.ContentType.TAG -> if (!translateTags) return originalText
            else -> {}
        }
        
        return try {
            val translated = callDeepLApi(listOf(originalText), targetLang).firstOrNull() ?: originalText
            
            // Cache the result
            cacheDao.insert(
                TranslationCache(
                    originalText = originalText,
                    translatedText = translated,
                    targetLang = targetLang,
                    contentType = contentType,
                    videoCode = videoCode,
                    apiKeyUsed = apiKeys.getOrNull(currentApiKeyIndex)?.key ?: "",
                    charsConsumed = originalText.length
                )
            )
            
            translated
        } catch (e: Exception) {
            // Fallback: return original text
            e.printStackTrace()
            originalText
        }
    }
    
    // Batch translate multiple texts
    suspend fun translateBatch(
        texts: List<String>,
        contentType: TranslationCache.ContentType,
        videoCode: String? = null
    ): List<String> {
        if (!isEnabled || texts.isEmpty()) return texts
        
        val results = mutableListOf<String>()
        val toTranslate = mutableListOf<Pair<Int, String>>() // (index, text)
        
        // Check cache and collect texts that need translation
        texts.forEachIndexed { index, text ->
            if (text.isBlank()) {
                results.add(index, text)
            } else {
                val cached = cacheDao.get(text, targetLang, contentType)
                if (cached != null) {
                    results.add(index, cached.translatedText)
                } else {
                    toTranslate.add(index to text)
                }
            }
        }
        
        if (toTranslate.isEmpty()) return results
        
        // Prepare batches
        val textsToTranslate = toTranslate.map { it.second }
        val batches = createBatches(textsToTranslate)
        
        // Translate each batch
        for (batch in batches) {
            try {
                val translatedBatch = callDeepLApi(batch, targetLang)
                
                // Process results
                for ((batchIndex, translated) in translatedBatch.withIndex()) {
                    val originalIndex = toTranslate[batchIndex].first
                    results.add(originalIndex, translated)
                    
                    // Cache result
                    cacheDao.insert(
                        TranslationCache(
                            originalText = batch[batchIndex],
                            translatedText = translated,
                            targetLang = targetLang,
                            contentType = contentType,
                            videoCode = videoCode,
                            apiKeyUsed = apiKeys.getOrNull(currentApiKeyIndex)?.key ?: "",
                            charsConsumed = batch[batchIndex].length
                        )
                    )
                }
            } catch (e: Exception) {
                // If batch fails, add original texts
                e.printStackTrace()
                for ((index, text) in batch.withIndex()) {
                    results.add(toTranslate[index].first, text)
                }
            }
        }
        
        return results
    }
    
    // Translate tags (special handling)
    suspend fun translateTags(tags: List<String>, videoCode: String? = null): List<String> {
        if (!isEnabled || !translateTags || tags.isEmpty()) return tags
        
        // Join tags with separator
        val joinedTags = tags.joinToString(TAG_SEPARATOR)
        
        // Check cache for joined tags
        val cached = cacheDao.get(joinedTags, targetLang, TranslationCache.ContentType.TAG)
        if (cached != null) {
            return cached.translatedText.split(TAG_SEPARATOR)
        }
        
        return try {
            val translated = callDeepLApi(listOf(joinedTags), targetLang).firstOrNull() ?: joinedTags
            
            // Cache result
            cacheDao.insert(
                TranslationCache(
                    originalText = joinedTags,
                    translatedText = translated,
                    targetLang = targetLang,
                    contentType = TranslationCache.ContentType.TAG,
                    videoCode = videoCode,
                    apiKeyUsed = apiKeys.getOrNull(currentApiKeyIndex)?.key ?: "",
                    charsConsumed = joinedTags.length
                )
            )
            
            translated.split(TAG_SEPARATOR)
        } catch (e: Exception) {
            e.printStackTrace()
            tags
        }
    }
    
    // Get cache statistics
    suspend fun getStats(): Map<String, Any> {
        val allCache = cacheDao.getAll().first()
        val totalChars = allCache.sumOf { it.charsConsumed }
        val totalItems = allCache.size
        
        val byType = allCache.groupBy { it.contentType }
            .mapValues { it.value.size }
        
        val byApiKey = allCache.groupBy { it.apiKeyUsed }
            .mapValues { it.value.sumOf { cache -> cache.charsConsumed } }
        
        return mapOf(
            "totalChars" to totalChars,
            "totalItems" to totalItems,
            "byType" to byType,
            "byApiKey" to byApiKey,
            "apiKeys" to apiKeys.map { key ->
                mapOf(
                    "key" to key.key.take(8) + "..." + key.key.takeLast(4),
                    "monthlyLimit" to key.monthlyLimit,
                    "charsUsed" to key.charsUsedThisMonth.get(),
                    "remaining" to key.monthlyLimit - key.charsUsedThisMonth.get(),
                    "isActive" to key.isActive
                )
            }
        )
    }

    // In TranslationManager class, add:
    suspend fun getAllCacheItems(): List<TranslationCache> {
        return cacheDao.getAll().first()
    }

    // Clear cache
    suspend fun clearCache() {
        cacheDao.deleteAll()
    }

    // Clear cache by type
    suspend fun clearCacheByType(contentType: TranslationCache.ContentType) {
        cacheDao.deleteByType(contentType)
    }

    // Delete specific cache item
    suspend fun deleteCacheItem(id: Int) {
        cacheDao.delete(id)
    }

    // Update settings
    fun updateSettings() {
        initialize()
    }
}
