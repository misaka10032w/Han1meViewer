package com.yenaly.han1meviewer.util

import android.util.Log
import com.yenaly.han1meviewer.Preferences
import okhttp3.Cookie

@JvmInline
value class CookieString(val cookie: String)

/**
 * 主要用於 [HCookieJar][com.yenaly.han1meviewer.logic.network.HCookieJar]，最好不要用到其他地方。
 */
fun CookieString.toLoginCookieList(domain: String): List<Cookie> {
    val cookieList = mutableListOf<Cookie>().also {
        it += preferencesCookieList(domain)
    }
    cookie.split(';').forEach { cookie ->
        if (cookie.isNotBlank()) {
            val name = cookie.substringBefore('=').trim()
            val value = cookie.substringAfter('=').trim()
            val cleanedName = name.filter { it.code in 0x20..0x7E && it != '\n' && it != '\r' }
            val cleanValue = value.filter { it.code in 0x20..0x7E && it != '\n' && it != '\r' }
            if (cleanedName.isNotEmpty()) {
                try {
                    cookieList += Cookie.Builder()
                        .domain(domain)
                        .name(cleanedName)
                        .value(cleanValue)
                        .build()
                } catch (e: IllegalArgumentException) {
                    Log.w(
                        "CookieString",
                        "无效Cookie: $cleanedName=$cleanValue, error=${e.message}"
                    )
                }
            } else {
                Log.w("CookieString", "无效键值: $cookie")
            }
        }
    }
    return cookieList.also {
        Log.d("CookieString", "toCookieList: $it")
    }
}

/**
 * 每次退出登入後都會清除cookie，但是這樣可能會清除掉很多保存在cookie中的偏好，比如影片語言之類。
 *
 * 讓[preferencesCookieList]成爲 存在偏好設置 但不存在個人信息 的[emptyList]
 */
private fun preferencesCookieList(domain: String): List<Cookie> {
    val videoLanguage = Preferences.videoLanguage
    val videoLanguageCookie = Cookie.Builder().domain(domain)
        .name("user_lang")
        .value(videoLanguage)
        .build()
    return listOf(videoLanguageCookie)
}