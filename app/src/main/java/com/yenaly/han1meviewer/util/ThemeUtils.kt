package com.yenaly.han1meviewer.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.yenaly.han1meviewer.Preferences

/**
 * 应用主题相关工具。
 */
object ThemeUtils {

    /**
     * 根据用户设置判断当前是否应使用深色主题。
     *
     * `follow_system` 时读取系统 uiMode，其余情况直接使用用户选择，
     * 保证 Compose 与 View 体系使用同一套判断结果。
     */
    fun isDarkTheme(context: Context): Boolean = when (Preferences.useDarkMode) {
        "always_on" -> true
        "always_off" -> false
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * 将设置中的深色模式同步到 AppCompat，使 View 体系与 Compose 保持一致。
     */
    fun applyDarkModeFromPreferences(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            when (Preferences.useDarkMode) {
                "always_on" -> AppCompatDelegate.MODE_NIGHT_YES
                "always_off" -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
