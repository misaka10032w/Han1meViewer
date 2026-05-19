package com.yenaly.han1meviewer.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

fun Context.showToast(@StringRes resId: Int, vararg formatArgs: Any, duration: Int = Toast.LENGTH_SHORT) {
    val text = this.resources.getString(resId, *formatArgs)
    Toast.makeText(this, text, duration).show()
}