package com.yenaly.han1meviewer.logic.network

import android.util.Log
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.CookieString
import com.yenaly.han1meviewer.util.toLoginCookieList
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * 用於管理 Cookie。
 *
 * #issue-71: 我竟然栽倒在 Cookie 管理上好幾年了！你去看我以前的管理方式，
 * 是完全錯誤的，竟然還能維持應用正常運行，太離譜了！怪不得切換簡體繁體一直不起作用！
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/13 013 15:20
 */
class HCookieJar : CookieJar {

    companion object {
        // 使用 ConcurrentHashMap 保证 OkHttp 多线程并发请求时的线程安全
        @JvmStatic
        val cookieMap: MutableMap<String, MutableList<Cookie>> = ConcurrentHashMap()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = mutableListOf<Cookie>()
        // 读取时加锁，避免与 saveFromResponse 的写入产生 ConcurrentModificationException
        cookieMap[host]?.let { list ->
            synchronized(list) { cookies.addAll(list) }
        }

        cookies.addAll(Preferences.loginCookieStateFlow.value.toLoginCookieList(host))
        cookies.addAll(Preferences.cloudFlareCookieStateFlow.value.toLoginCookieList(host))

        Log.d("HCookieJar", "loadForRequest for $host: $cookies")

        return cookies
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        // 使用 putIfAbsent 保证只创建一次列表，避免两个线程同时判定 existing == null 后互相覆盖
        val existing = cookieMap.putIfAbsent(host, mutableListOf())
        if (existing == null) {
            // 首次写入：合并响应 cookie 与偏好/登录 cookie
            val merged = cookieMap[host] ?: mutableListOf()
            synchronized(merged) {
                merged.addAll(cookies)
                merged += Preferences.loginCookieStateFlow.value.toLoginCookieList(host)
            }
            return
        }
        // 已有 cookie：按 name 合并更新（RFC 6265 语义），避免覆盖未在本次响应中返回的 cookie
        synchronized(existing) {
            val byName = existing.associateBy { it.name }.toMutableMap()
            cookies.forEach { incoming -> byName[incoming.name] = incoming }
            val preferencesList = Preferences.loginCookieStateFlow.value.toLoginCookieList(host)
            preferencesList.forEach { pref -> byName[pref.name] = pref }
            existing.clear()
            existing.addAll(byName.values)
        }
    }
}