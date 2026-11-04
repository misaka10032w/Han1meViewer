package com.yenaly.han1meviewer.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URLDecoder
import java.net.URLEncoder

private fun Context.sp(
    name: String = packageName,
    mode: Int = Context.MODE_PRIVATE
): SharedPreferences = getSharedPreferences(name, mode)

fun <T> putSpValue(
    key: String,
    value: T,
    name: String = applicationContext.packageName
) {
    applicationContext.sp(name = name).edit {
        when (value) {
            is Long -> putLong(key, value)
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            else -> putString(key, serialize(value))
        }
    }
}

fun <T> getSpValue(
    key: String,
    default: T,
    name: String = applicationContext.packageName
): T {
    return applicationContext.sp(name = name).run {
        val result = when (default) {
            is Long -> getLong(key, default)
            is String -> getString(key, default)
            is Int -> getInt(key, default)
            is Boolean -> getBoolean(key, default)
            is Float -> getFloat(key, default)
            else -> deSerialization(getString(key, serialize(default)))
        }
        @Suppress("UNCHECKED_CAST")
        result as T
    }
}

private fun <T> serialize(obj: T): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
    objectOutputStream.writeObject(obj)
    var serStr = byteArrayOutputStream.toString("ISO-8859-1")
    serStr = URLEncoder.encode(serStr, "UTF-8")
    objectOutputStream.close()
    byteArrayOutputStream.close()
    return serStr
}

private fun <T> deSerialization(str: String?): T {
    val redStr = URLDecoder.decode(str, "UTF-8")
    val byteArrayInputStream = ByteArrayInputStream(redStr.toByteArray(charset("ISO-8859-1")))
    val objectInputStream = ObjectInputStream(byteArrayInputStream)
    @Suppress("UNCHECKED_CAST")
    val obj = objectInputStream.readObject() as T
    objectInputStream.close()
    byteArrayInputStream.close()
    return obj
}
