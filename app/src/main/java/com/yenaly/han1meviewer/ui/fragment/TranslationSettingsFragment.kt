package com.yenaly.han1meviewer.ui.fragment.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.*
import com.google.gson.Gson
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.TranslationManager
import com.yenaly.han1meviewer.logic.TranslationMigrationHelper
import com.yenaly.yenaly_libs.base.settings.YenalySettingsFragment
import kotlinx.coroutines.launch

class TranslationSettingsFragment : YenalySettingsFragment() {
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_translation, rootKey)
        
        // API Keys preference
        val apiKeysPref = findPreference<EditTextPreference>("translation_api_keys")
        apiKeysPref?.setOnPreferenceChangeListener { _, newValue ->
            // Update API keys
            val keys = (newValue as String).split("\n").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            Preferences.translationApiKeys = keys
            true
        }
        
        // Monthly limit
        val monthlyLimitPref = findPreference<EditTextPreference>("translation_monthly_limit")
        monthlyLimitPref?.setOnPreferenceChangeListener { _, newValue ->
            val limit = (newValue as String).toIntOrNull() ?: 500000
            Preferences.translationMonthlyLimit = limit
            true
        }
        
        // Batch size
        val batchSizePref = findPreference<EditTextPreference>("translation_batch_size")
        batchSizePref?.setOnPreferenceChangeListener { _, newValue ->
            val size = (newValue as String).toIntOrNull() ?: 30000
            Preferences.translationBatchSize = size
            true
        }
        
        // Target language
        val targetLangPref = findPreference<ListPreference>("translation_target_lang")
        targetLangPref?.setOnPreferenceChangeListener { _, newValue ->
            Preferences.translationTargetLang = newValue as String
            true
        }
        
        // What to translate
        val translateTitlesPref = findPreference<SwitchPreferenceCompat>("translate_titles")
        translateTitlesPref?.setOnPreferenceChangeListener { _, newValue ->
            Preferences.translateTitles = newValue as Boolean
            true
        }
        
        val translateDescriptionsPref = findPreference<SwitchPreferenceCompat>("translate_descriptions")
        translateDescriptionsPref?.setOnPreferenceChangeListener { _, newValue ->
            Preferences.translateDescriptions = newValue as Boolean
            true
        }
        
        val translateCommentsPref = findPreference<SwitchPreferenceCompat>("translate_comments")
        translateCommentsPref?.setOnPreferenceChangeListener { _, newValue ->
            Preferences.translateComments = newValue as Boolean
            true
        }
        
        val translateTagsPref = findPreference<SwitchPreferenceCompat>("translate_tags")
        translateTagsPref?.setOnPreferenceChangeListener { _, newValue ->
            Preferences.translateTags = newValue as Boolean
            true
        }
        
        // Clear cache button
        val clearCachePref = findPreference<Preference>("clear_translation_cache")
        clearCachePref?.setOnPreferenceClickListener {
            lifecycleScope.launch {
                // Get TranslationManager instance and clear cache
                TranslationManager.getInstance(requireContext()).clearCache()
                // Show snackbar using your app's utility
                // showSnackbar("Cache cleared")
            }
            true
        }
        
        // View cache button
        val viewCachePref = findPreference<Preference>("view_translation_cache")
        viewCachePref?.setOnPreferenceClickListener {
            // Navigate to Cache Management Fragment
            // This will be implemented
            true
        }
        
        // Add migration options
        val migratePref = findPreference<Preference>("migrate_settings")
        migratePref?.setOnPreferenceClickListener {
            showMigrationDialog()
            true
        }
        
        val resetPref = findPreference<Preference>("reset_translation_settings")
        resetPref?.setOnPreferenceClickListener {
            showResetConfirmationDialog()
            true
        }
        
        val exportPref = findPreference<Preference>("export_settings")
        exportPref?.setOnPreferenceClickListener {
            exportSettings()
            true
        }
        
        val importPref = findPreference<Preference>("import_settings")
        importPref?.setOnPreferenceClickListener {
            importSettings()
            true
        }
    }
    
    private fun showMigrationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Migrate Settings")
            .setMessage("Check for and migrate any existing translation settings from previous versions?")
            .setPositiveButton("Migrate") { _, _ ->
                lifecycleScope.launch {
                    TranslationMigrationHelper.migrateIfNeeded(requireContext())
                    // showSnackbar("Migration completed")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset All Settings")
            .setMessage("Are you sure you want to reset all translation settings to defaults? This cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                TranslationMigrationHelper.resetToDefaults(requireContext())
                // showSnackbar("Settings reset to defaults")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportSettings() {
        val settings = TranslationMigrationHelper.exportSettings()
        val json = Gson().toJson(settings)
        
        // Share the settings
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Han1meViewer Translation Settings Backup")
        intent.putExtra(Intent.EXTRA_TEXT, json)
        startActivity(Intent.createChooser(intent, "Export Settings"))
    }
    
    private fun importSettings() {
        // For simplicity, we'll show a dialog to paste JSON
        // In a real implementation, you might want to read from a file
        val editText = EditText(requireContext())
        editText.hint = "Paste settings JSON here"
        
        AlertDialog.Builder(requireContext())
            .setTitle("Import Settings")
            .setView(editText)
            .setPositiveButton("Import") { _, _ ->
                try {
                    val json = editText.text.toString()
                    val settings = Gson().fromJson(json, Map::class.java) as Map<String, Any>
                    TranslationMigrationHelper.importSettings(settings)
                    // showSnackbar("Settings imported successfully")
                } catch (e: Exception) {
                    // showSnackbar("Import failed: ${e.message}")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
