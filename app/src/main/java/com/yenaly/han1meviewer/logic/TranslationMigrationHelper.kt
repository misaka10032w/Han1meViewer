package com.yenaly.han1meviewer.logic

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.yenaly.han1meviewer.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class to migrate existing translation settings
 * from older versions or different implementations
 */
object TranslationMigrationHelper {
    
    // Old preference keys that might exist in previous versions
    private const val OLD_ENABLE_TRANSLATION = "enable_translation"
    private const val OLD_TRANSLATION_API_KEY = "translation_api_key"
    private const val OLD_TARGET_LANGUAGE = "translation_target_language"
    private const val OLD_TRANSLATE_CONTENT_TYPES = "translate_content_types"
    
    /**
     * Check for and migrate any existing translation settings
     */
    suspend fun migrateIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        try {
            val sharedPrefs = context.getSharedPreferences("translation_prefs", Context.MODE_PRIVATE)
            migrateFromSharedPrefs(sharedPrefs)
            
            // Also check for any web translation settings that might exist
            migrateWebTranslationSettings()
            
            // Initialize translation manager after migration
            TranslationManager.getInstance(context).initialize()
            
            Log.d("TranslationMigrationHelper", "Migration completed successfully")
        } catch (e: Exception) {
            Log.e("TranslationMigrationHelper", "Migration failed: ${e.message}", e)
        }
    }
    
    /**
     * Migrate from old shared preferences format
     */
    private fun migrateFromSharedPrefs(sharedPrefs: SharedPreferences) {
        // Check if old preferences exist
        if (!sharedPrefs.contains(OLD_ENABLE_TRANSLATION)) {
            return // No old settings to migrate
        }
        
        Log.d("TranslationMigrationHelper", "Found old translation settings, migrating...")
        
        // Migrate enable/disable setting
        val oldEnabled = sharedPrefs.getBoolean(OLD_ENABLE_TRANSLATION, false)
        Preferences.isTranslationEnabled = oldEnabled
        
        // Migrate API key
        val oldApiKey = sharedPrefs.getString(OLD_TRANSLATION_API_KEY, "")
        if (!oldApiKey.isNullOrBlank()) {
            Preferences.translationApiKeys = setOf(oldApiKey)
        }
        
        // Migrate target language
        val oldTargetLang = sharedPrefs.getString(OLD_TARGET_LANGUAGE, "EN")
        if (!oldTargetLang.isNullOrBlank()) {
            Preferences.translationTargetLang = oldTargetLang
        }
        
        // Migrate content type preferences
        val oldContentTypes = sharedPrefs.getStringSet(OLD_TRANSLATE_CONTENT_TYPES, null)
        oldContentTypes?.let { types ->
            Preferences.translateTitles = types.contains("titles") || types.isEmpty()
            Preferences.translateDescriptions = types.contains("descriptions") || types.isEmpty()
            Preferences.translateComments = types.contains("comments") || types.isEmpty()
            Preferences.translateTags = types.contains("tags") || types.isEmpty()
        }
        
        // Clear old preferences after migration
        sharedPrefs.edit().clear().apply()
        Log.d("TranslationMigrationHelper", "Old preferences cleared after migration")
    }
    
    /**
     * Migrate from any web-based translation settings
     * (if the app previously used webpage translation)
     */
    private fun migrateWebTranslationSettings() {
        // Check for any web translation related settings in main preferences
        val oldWebTranslationKey = "web_translation_enabled"
        val oldWebTranslationUrl = "web_translation_url"
        
        // You can check your existing Preferences for any web translation settings
        // and migrate them to the new DeepL system
        // This is just an example - adjust based on your actual implementation
        
        Log.d("TranslationMigrationHelper", "Web translation settings migration attempted")
    }
    
    /**
     * Reset all translation settings to defaults
     */
    fun resetToDefaults(context: Context) {
        Preferences.isTranslationEnabled = false
        Preferences.translationApiKeys = emptySet()
        Preferences.translationMonthlyLimit = 500000
        Preferences.translationTargetLang = "EN"
        Preferences.translationBatchSize = 30000
        Preferences.translateTitles = true
        Preferences.translateDescriptions = true
        Preferences.translateComments = true
        Preferences.translateTags = true
        
        // Clear cache
        TranslationManager.getInstance(context).clearCache()
    }
    
    /**
     * Export translation settings for backup
     */
    fun exportSettings(): Map<String, Any> {
        return mapOf(
            "version" to 1,
            "isTranslationEnabled" to Preferences.isTranslationEnabled,
            "translationApiKeys" to Preferences.translationApiKeys.toList(),
            "translationMonthlyLimit" to Preferences.translationMonthlyLimit,
            "translationTargetLang" to Preferences.translationTargetLang,
            "translationBatchSize" to Preferences.translationBatchSize,
            "translateTitles" to Preferences.translateTitles,
            "translateDescriptions" to Preferences.translateDescriptions,
            "translateComments" to Preferences.translateComments,
            "translateTags" to Preferences.translateTags
        )
    }
    
    /**
     * Import translation settings from backup
     */
    fun importSettings(settings: Map<String, Any>) {
        (settings["isTranslationEnabled"] as? Boolean)?.let {
            Preferences.isTranslationEnabled = it
        }
        
        (settings["translationApiKeys"] as? List<*>)?.let { keys ->
            Preferences.translationApiKeys = keys.filterIsInstance<String>().toSet()
        }
        
        (settings["translationMonthlyLimit"] as? Int)?.let {
            Preferences.translationMonthlyLimit = it
        }
        
        (settings["translationTargetLang"] as? String)?.let {
            Preferences.translationTargetLang = it
        }
        
        (settings["translationBatchSize"] as? Int)?.let {
            Preferences.translationBatchSize = it
        }
        
        (settings["translateTitles"] as? Boolean)?.let {
            Preferences.translateTitles = it
        }
        
        (settings["translateDescriptions"] as? Boolean)?.let {
            Preferences.translateDescriptions = it
        }
        
        (settings["translateComments"] as? Boolean)?.let {
            Preferences.translateComments = it
        }
        
        (settings["translateTags"] as? Boolean)?.let {
            Preferences.translateTags = it
        }
    }
}
