package com.yenaly.han1meviewer.util

import android.os.LocaleList
import java.util.Locale

object LanguageHelper {
    val preferredLanguage: Locale
        get() = LocaleList.getDefault()[0]
}
