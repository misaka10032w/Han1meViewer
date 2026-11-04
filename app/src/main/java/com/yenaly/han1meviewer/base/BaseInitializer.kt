package com.yenaly.han1meviewer.base

import android.content.Context
import androidx.startup.Initializer
import com.yenaly.han1meviewer.util.applicationContext

open class BaseInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        applicationContext = context
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
