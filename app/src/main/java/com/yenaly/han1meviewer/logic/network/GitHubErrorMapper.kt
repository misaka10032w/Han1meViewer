package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.logic.exception.HUpdaterException
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * GitHub 接口的响应 → 更新异常的映射层。
 *
 * 关键点：GitHub 的所有 REST 接口都通过 **状态码 + 响应体提示** 来表达失败：
 *
 * - `401` Bad credentials：Token 失效/未配置（限额、密钥失效都属于这一类）
 * - `403` 两种截然不同的含义：请求频率超限（响应体含 rate limit 字样）
 *   与权限不足（Actions 权限被关、仓库私有、Artifact 过期）
 * - `429` 二级频率限制
 *
 * 以前的代码用的是「直接返回实体类型」的 Retrofit 方法，非 2xx 会直接抛
 * `HttpException`，**读不到响应体与状态码**，所以无法区分这些情况。这里统一改成
 * `Response<T>` + [orThrowVersionCheck] / [orThrowDownload]，把信息完整地保留进异常里。
 *
 * @project Han1meViewer
 * @author Misaka10032w
 */

private val RateLimitHints = listOf(
    "rate limit",
    "secondary rate",
    "too many requests",
    "api rate limit exceeded",
)

/** 诊断信息里保留的响应体长度上限 */
private const val DETAIL_LIMIT = 300

/**
 * 把 Retrofit 响应转换为版本信息，失败时抛出带分类的[HUpdaterException.VersionCheck]。
 *
 * @param api 出错的接口地址，仅用于生成可读的诊断信息
 */
internal fun <T> Response<T>.orThrowVersionCheck(api: String): T {
    if (isSuccessful) {
        return body() ?: throw HUpdaterException.VersionCheck(
            message = "GitHub 接口 $api 返回了空响应体",
            reason = HUpdaterException.VersionCheck.Reason.SERVER,
        )
    }
    throw toVersionCheckException(api)
}

private fun Response<*>.toVersionCheckException(
    api: String,
): HUpdaterException.VersionCheck {
    val code = code()
    val body = errorBodyText()
    val hints = "$body ${message()}".lowercase()
    val reason = when {
        code == 429 || hints.containsAny(RateLimitHints) -> {
            HUpdaterException.VersionCheck.Reason.RATE_LIMITED
        }

        code == 401 -> HUpdaterException.VersionCheck.Reason.BAD_CREDENTIALS

        // 403 有两种含义，靠响应体里的限额提示区分
        code == 403 -> HUpdaterException.VersionCheck.Reason.FORBIDDEN

        // Artifact 过期后会返回 410 Gone，工作流被清理时可能返回 404
        code == 404 || code == 410 -> HUpdaterException.VersionCheck.Reason.NO_RELEASE

        else -> HUpdaterException.VersionCheck.Reason.SERVER
    }
    return HUpdaterException.VersionCheck(
        message = diagnostic("GitHub 请求失败（$api）", code, body),
        reason = reason,
        statusCode = code,
    )
}

/**
 * 下载专用的响应校验。
 *
 * 下载请求以前完全不检查[Response.isSuccessful]，于是 `403 insufficient_scope` 的
 * 错误 JSON 会被原样写进 `update.apk`，用户看到的是「安装失败」——真正的失败原因
 * 在第一步就被丢掉了。
 */
internal fun Response<ResponseBody>.orThrowDownload(api: String): ResponseBody {
    if (isSuccessful) {
        return body() ?: throw HUpdaterException.Download(
            message = "下载接口 $api 返回了空响应体",
            reason = HUpdaterException.Download.Reason.SERVER,
        )
    }
    throw toDownloadException(api)
}

private fun Response<*>.toDownloadException(api: String): HUpdaterException.Download {
    val code = code()
    val body = errorBodyText()
    val hints = "$body ${message()}".lowercase()
    val reason = when {
        code == 429 || hints.containsAny(RateLimitHints) -> {
            HUpdaterException.Download.Reason.RATE_LIMITED
        }

        code == 401 -> HUpdaterException.Download.Reason.BAD_CREDENTIALS

        // Artifact 下载走 302 到对象存储，403 基本都是过期或权限被回收
        code == 403 || code == 404 || code == 410 -> {
            HUpdaterException.Download.Reason.ARTIFACT_EXPIRED
        }

        code >= 500 -> HUpdaterException.Download.Reason.SERVER

        else -> HUpdaterException.Download.Reason.CLIENT
    }
    return HUpdaterException.Download(
        message = diagnostic("更新包下载失败（$api）", code, body),
        reason = reason,
        statusCode = code,
    )
}

/**
 * 读取错误响应体原始内容（用于判断 403 的真实含义与排查问题）。
 */
private fun Response<*>.errorBodyText(): String {
    return runCatching { errorBody()?.string() }.getOrNull().orEmpty()
}

private fun diagnostic(prefix: String, code: Int, body: String): String {
    return buildString {
        append("$prefix：HTTP $code")
        if (body.isNotBlank()) append("，响应：${body.take(DETAIL_LIMIT)}")
    }
}

private fun String.containsAny(keywords: List<String>): Boolean {
    return keywords.any { it in this }
}
