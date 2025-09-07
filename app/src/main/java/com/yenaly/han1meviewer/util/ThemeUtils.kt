package com.yenaly.han1meviewer.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
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

    @Suppress("DEPRECATION")
    fun Activity.setStatusBarIcons() {
        val isDarkTheme: Boolean =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (!isDarkTheme) APPEARANCE_LIGHT_STATUS_BARS else 0,
                APPEARANCE_LIGHT_STATUS_BARS
            )
        } else
            window.decorView.systemUiVisibility =
                if (!isDarkTheme) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
    }
}