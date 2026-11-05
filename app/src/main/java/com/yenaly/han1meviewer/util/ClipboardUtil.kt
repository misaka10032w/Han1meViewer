package com.yenaly.han1meviewer.util

import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.content.getSystemService

/**
 * 将文本复制到系统剪贴板。
 *
 * @param text 要复制的文本
 * @param label 剪贴板条目的用户可见标签
 */
fun copyTextToClipboard(
    text: CharSequence?,
    label: CharSequence? = null,
) {
    val clipboardManager = applicationContext.getSystemService<ClipboardManager>()
    clipboardManager?.setPrimaryClip(ClipData.newPlainText(label, text))
}

/**
 * 将字符序列复制到系统剪贴板。
 *
 * @see copyTextToClipboard
 */
fun CharSequence?.copyToClipboard(label: CharSequence? = null) {
    copyTextToClipboard(this, label)
}

/**
 * 读取系统剪贴板中的第一段文本。
 *
 * @return 剪贴板内容，无法读取时返回 null
 */
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
