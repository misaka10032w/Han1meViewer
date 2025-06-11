package com.yenaly.han1meviewer.util

import android.content.Context

object HStorageModeManager {
    const val PREF_NAME = "setting_pref"
    const val KEY_USE_PRIVATE = "use_private_storage"

    fun setUsePrivateDownloadFolder(context: Context, usePrivate: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_USE_PRIVATE, usePrivate)
            .apply()
    }

    fun isUsingPrivateDownloadFolder(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_PRIVATE, false)
    }
}