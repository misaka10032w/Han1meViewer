package com.yenaly.han1meviewer.util

import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.exception.CloudFlareBlockedException
import com.yenaly.han1meviewer.logic.exception.HanimeNotFoundException
import com.yenaly.han1meviewer.logic.exception.IPBlockedException
import com.yenaly.han1meviewer.logic.exception.LoginStateExpiredException
import com.yenaly.han1meviewer.logic.exception.ParseException
import com.yenaly.han1meviewer.logic.network.HProxySelector
import javax.net.ssl.SSLHandshakeException

/**
 * 统一埋点入口。
 *
 * 所有 Analytics 事件与错误上报都应经过本对象，便于统一处理：
 * - 参数值自动截断到 Firebase Analytics 的 100 字符上限；
 * - 错误除了记录 Analytics 事件外，判断是否为「真 bug 信号」（解析失败、SSL 握手失败等），
 *   是则同时上报 Crashlytics non-fatal，附带来源 breadcrumb；
 *   预期内的站点状态（IP 被封、Cloudflare 质询、影片不存在、登录过期）只记事件，不污染 Crashlytics；
 * - 崩溃安全：全部公开方法内部 catch [Throwable]（含依赖冲突等 Error），
 *   埋点失败只写 logcat，绝不向外抛异常、不影响应用运行；
 * - Crashlytics 上报受 [com.yenaly.han1meviewer.HanimeApplication] 中
 *   `isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG` 控制，debug 构建自动丢弃。
 */
object HAnalytics {

    private const val TAG = "HAnalytics"

    /**
     * Firebase Analytics 单个参数值的长度上限。
     */
    private const val MAX_PARAM_VALUE_LENGTH = 100

    // <editor-fold desc="基础事件">

    /**
     * 记录一条 Analytics 事件。
     *
     * 集合未开启（用户拒绝数据统计）时 Firebase SDK 会静默丢弃，无需额外判断。
     */
    fun logEvent(name: String, vararg params: Pair<String, Any?>) {
        try {
            val bundle = Bundle()
            for ((key, value) in params) {
                when (value) {
                    null -> Unit
                    is String -> bundle.putString(key, value.take(MAX_PARAM_VALUE_LENGTH))
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Float -> bundle.putFloat(key, value)
                    else -> bundle.putString(key, value.toString().take(MAX_PARAM_VALUE_LENGTH))
                }
            }
            Firebase.analytics.logEvent(name, bundle)
        } catch (t: Throwable) {
            // 埋点绝不允许弄崩应用：连 Error（依赖冲突 NoSuchMethodError、
            // Firebase 静态初始化失败等）也一并吞掉并记录日志
            logInternalFailure("logEvent($name)", t)
        }
    }

    /**
     * 记录 screen_view 事件。
     *
     * @param screenName 页面名称，如 Home、Video
     * @param screenClassName 页面类名/路由名
     */
    fun logScreenView(screenName: String, screenClassName: String) {
        try {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName.take(MAX_PARAM_VALUE_LENGTH))
                param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClassName.take(MAX_PARAM_VALUE_LENGTH))
            }
        } catch (t: Throwable) {
            logInternalFailure("logScreenView($screenName)", t)
        }
    }

    // </editor-fold>

    // <editor-fold desc="错误上报">

    /**
     * 上报错误。
     *
     * 1. 记录一条 [eventName] Analytics 事件（默认附带 source、exception_class、exception_msg、http_code）；
     * 2. 若异常属于「真 bug 信号」（见 [isBugSignal]），先写 Crashlytics breadcrumb
     *    再 recordException，使其可在 Crashlytics 后台看到堆栈与趋势。
     *
     * @param eventName 事件名，如 network_error、player_error
     * @param source 错误来源，如 home_page、search、video
     * @param throwable 异常，可为 null（纯逻辑错误）
     * @param params 追加参数
     */
    fun logError(
        eventName: String,
        source: String,
        throwable: Throwable?,
        vararg params: Pair<String, Any?>,
    ) {
        try {
            val extra = mutableListOf<Pair<String, Any?>>(
                FirebaseConstants.PARAM_SOURCE to source,
                FirebaseConstants.PARAM_EXCEPTION_CLASS to throwable?.javaClass?.simpleName,
            )
            throwable?.message?.takeIf { it.isNotBlank() }?.let {
                extra.add(FirebaseConstants.PARAM_EXCEPTION_MSG to it)
            }
            extractHttpCode(throwable)?.let {
                extra.add(FirebaseConstants.PARAM_HTTP_CODE to it)
            }
            extra.addAll(params)
            logEvent(eventName, *extra.toTypedArray())

            if (throwable != null && isBugSignal(throwable)) {
                recordNonFatal(source, throwable)
            }
        } catch (t: Throwable) {
            // 参数构造本身是纯 Kotlin 操作，理论上不会失败；兜底保证 HAnalytics 永不向外抛异常
            logInternalFailure("logError($source)", t)
        }
    }

    /**
     * 网络请求失败的统一上报（NetworkRepo 三个 flow 构建器的 catch 中调用）。
     */
    fun logNetworkError(source: String, throwable: Throwable) {
        logError(FirebaseConstants.EVENT_NETWORK_ERROR, source, throwable)
    }

    /**
     * 记录 non-fatal 异常到 Crashlytics，并在日志中留下来源 breadcrumb。
     */
    private fun recordNonFatal(source: String, throwable: Throwable) {
        try {
            Firebase.crashlytics.log("HAnalytics: non-fatal at source=$source")
            Firebase.crashlytics.recordException(throwable)
        } catch (t: Throwable) {
            logInternalFailure("recordNonFatal($source)", t)
        }
    }

    /**
     * 判断异常是否为「真 bug 信号」：
     * - [ParseException]：站点结构变更导致解析失败，是最需要感知的回归；
     * - [SSLHandshakeException]：证书/网络环境异常；
     * - 其它未知异常（如从 HTTP 状态码包装出的 IllegalStateException）：可能是站点行为变化。
     *
     * 以下属于预期内的站点状态，只记 Analytics 事件即可：
     * [IPBlockedException]、[CloudFlareBlockedException]、[HanimeNotFoundException]、
     * [LoginStateExpiredException]。
     */
    private fun isBugSignal(throwable: Throwable): Boolean {
        if (BuildConfig.DEBUG) return false // debug 构建本身不上报 Crashlytics
        return throwable !is IPBlockedException &&
                throwable !is CloudFlareBlockedException &&
                throwable !is HanimeNotFoundException &&
                throwable !is LoginStateExpiredException
    }

    /**
     * NetworkRepo 会把 HTTP 状态码包进异常 message 首部（如 "403 Forbidden"），
     * 这里尝试还原成数字参数，便于按状态码聚合。
     */
    private fun extractHttpCode(throwable: Throwable?): Int? {
        val message = throwable?.message ?: return null
        val match = Regex("^\\s*(\\d{3})\\b").find(message) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    // </editor-fold>

    // <editor-fold desc="Crashlytics 上下文键">

    /**
     * 把当前网络环境配置写入 Crashlytics 自定义键，
     * 崩溃时可直接还原用户当时的站点/代理/DoH 配置。
     *
     * 启动时与网络设置变化后调用。
     */
    fun updateNetworkContextKeys() {
        try {
            Firebase.crashlytics.setCustomKeys {
                key(FirebaseConstants.CURRENT_SITE, Preferences.baseUrl.take(MAX_PARAM_VALUE_LENGTH))
                key(FirebaseConstants.USE_CUSTOM_MIRROR, Preferences.useCustomMirrorSite)
                key(FirebaseConstants.PROXY_TYPE, proxyTypeName(Preferences.proxyType))
                key(FirebaseConstants.DOH_ENABLED, Preferences.useDoH)
                key(FirebaseConstants.DOH_PRESET, Preferences.dohPreset)
                key(FirebaseConstants.USE_BUILTIN_HOSTS, Preferences.useBuiltInHosts)
                key(FirebaseConstants.USE_BACKUP_CDN, Preferences.useBackupMediaCdn)
            }
        } catch (t: Throwable) {
            logInternalFailure("updateNetworkContextKeys", t)
        }
    }

    /**
     * HAnalytics 内部自身的失败记录：只写 logcat，绝不向上抛。
     *
     * 埋点属于旁路逻辑，任何失败（包括 Firebase 未初始化、依赖版本冲突的
     * NoSuchMethodError/LinkageError 等 Error）都不能影响应用正常运行。
     */
    private fun logInternalFailure(where: String, t: Throwable) {
        try {
            Log.w(TAG, "$where failed.", t)
        } catch (_: Throwable) {
            // 连 Log 都失败（极罕见）时无能为力，保持静默
        }
    }

    private fun proxyTypeName(type: Int): String = when (type) {
        HProxySelector.TYPE_DIRECT -> "direct"
        HProxySelector.TYPE_SYSTEM -> "system"
        HProxySelector.TYPE_HTTP -> "http"
        HProxySelector.TYPE_SOCKS -> "socks"
        else -> "unknown"
    }

    // </editor-fold>
}
