package com.yenaly.han1meviewer.util

import android.util.Base64

fun String.decodeFromStringByBase64(flag: Int = Base64.DEFAULT): String {
    return String(Base64.decode(this.toByteArray(), flag))
}

inline fun <I, reified O> List<I>.mapToArray(transform: (I) -> O): Array<O> {
    return Array(size) { i -> transform(this[i]) }
}

fun <T> unsafeLazy(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)
