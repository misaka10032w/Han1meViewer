package com.yenaly.han1meviewer.util

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * 将文本中的繁体中文转换为简体中文，用于让搜索同时匹配繁简体。
 *
 * 例如「飛鳥」→「飞鸟」。若系统不支持（API < 29），原样返回。
 */
fun String.toSimplified(): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return this
    return try {
        ChineseTransliterator.simplify(this)
    } catch (_: Throwable) {
        this
    }
}

/**
 * 独立持有 [android.icu.text.Transliterator] 引用，仅在 API 29+ 时被加载，
 * 避免低版本设备因类不存在导致类加载失败。
 */
private object ChineseTransliterator {
    @RequiresApi(Build.VERSION_CODES.Q)
    private val transliterator =
        android.icu.text.Transliterator.getInstance("Traditional-Simplified")

    @RequiresApi(Build.VERSION_CODES.Q)
    fun simplify(text: String): String = transliterator.transliterate(text)
}
