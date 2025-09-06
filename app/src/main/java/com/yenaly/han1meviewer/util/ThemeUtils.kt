package com.yenaly.han1meviewer.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.yenaly.han1meviewer.Preferences

object ThemeUtils {
    fun applyDarkModeFromPreferences(context: Context) {
        val mode = when (Preferences.useDarkMode) {
            "follow_system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            "always_off" -> AppCompatDelegate.MODE_NIGHT_NO
            "always_on" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}