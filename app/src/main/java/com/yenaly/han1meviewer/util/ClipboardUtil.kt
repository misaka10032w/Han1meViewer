package com.yenaly.han1meviewer.util

import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.content.getSystemService

fun copyTextToClipboard(
    text: CharSequence?,
    label: CharSequence? = null,
) {
    val clipboardManager = applicationContext.getSystemService<ClipboardManager>()
    clipboardManager?.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun CharSequence?.copyToClipboard(label: CharSequence? = null) {
    copyTextToClipboard(this, label)
}

val textFromClipboard: CharSequence?
    get() {
        val context = applicationContext
        val clipboardManager = context.getSystemService<ClipboardManager>()
        val clipData = clipboardManager?.primaryClip ?: return null
        if (clipData.itemCount > 0) {
            clipData.getItemAt(0)?.let { item ->
                return item.coerceToText(context)
            }
        }
        return null
    }
