package com.yenaly.han1meviewer.base

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.preference.PreferenceManager
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys
import java.util.Locale

/**
 * 应用语言相关工具。
 */
object AppLocale {

    /**
     * 系统当前首选语言。
     */
    val preferredLanguage: Locale
        get() = LocaleList.getDefault()[0]

    /**
     * 按设置包装 [context]：应用语言、字体缩放上限、「显示大小」兼容密度。
     *
     * 字体与密度都要写进 [Configuration]，否则 Dialog 等独立窗口会各自按自身 context 解析。
     */
    fun wrap(context: Context): Context {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lang = prefs.getString("app_language", "system") ?: "system"

        val newLocale = when (lang) {
            "zh-rCN" -> Locale.SIMPLIFIED_CHINESE
            "zh" -> Locale.TRADITIONAL_CHINESE
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            else -> Resources.getSystem().configuration.locales[0]
        }

        Locale.setDefault(newLocale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(newLocale)

        // 系统字体缩放限幅
        val fontScalePref = AppFontScale.fromKey(
            prefs.getString(SettingsPreferenceKeys.APP_FONT_SCALE, AppFontScale.DEFAULT.key)
        )
        config.fontScale = AppFontScale.resolve(config.fontScale, fontScalePref)

        // 「显示大小」兼容：写进 Configuration 才能让独立窗口共用同一 density
        AppDisplayDensity.applyTo(
            config = config,
            metrics = context.resources.displayMetrics,
            displayWidthPx = displayRealWidthPx(),
            enabled = prefs.getBoolean(
                SettingsPreferenceKeys.IGNORE_SYSTEM_DISPLAY_SIZE,
                AppDisplayDensity.DEFAULT_ENABLED,
            ),
        )

        return context.createConfigurationContext(config)
    }
}
