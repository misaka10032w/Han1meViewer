package com.yenaly.han1meviewer.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.SeekBarPreference
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * 为 [SeekBarPreference] 设置数值与摘要之间的转换逻辑。
 *
 * @param defValue 默认值
 * @param converter 将数值转换为摘要文本
 * @param action 数值变化时的额外回调
 */
fun SeekBarPreference.setSummaryConverter(
    defValue: Int,
    converter: (Int) -> CharSequence?,
    action: ((Int) -> Unit)? = null
) {
    setDefaultValue(defValue)
    summary = converter(value)
    setOnPreferenceChangeListener { _, newValue ->
        summary = converter(newValue as Int)
        action?.invoke(newValue)
        true
    }
}

private fun Context.sp(
    name: String = packageName,
    mode: Int = Context.MODE_PRIVATE
): SharedPreferences = getSharedPreferences(name, mode)

/**
 * 将值写入 SharedPreferences。
 *
 * 支持 Long、String、Int、Boolean、Float，其他类型会先序列化再存储。
 *
 * @param key 储存键
 * @param value 储存值
 * @param name SharedPreferences 名称，默认为包名
 */
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

/**
 * 从 SharedPreferences 读取值。
 *
 * @param key 储存键
 * @param default 缺省值，同时用于推断存储类型
 * @param name SharedPreferences 名称，默认为包名
 */
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
