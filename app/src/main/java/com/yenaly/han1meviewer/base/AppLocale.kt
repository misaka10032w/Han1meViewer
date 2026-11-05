package com.yenaly.han1meviewer.base

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.preference.PreferenceManager
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
     * 根据设置中的应用语言包装 [context]。
     *
     * 由 Activity 的 `attachBaseContext` 调用，使资源按用户选择加载。
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
        return context.createConfigurationContext(config)
    }
}
