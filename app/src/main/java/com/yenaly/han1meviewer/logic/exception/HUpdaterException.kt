package com.yenaly.han1meviewer.logic.exception

import com.yenaly.han1meviewer.R
import java.io.IOException

/**
 * 更新模块专用的异常体系。
 *
 * 更新功能完全依赖 GitHub，会遇到限额耗尽、Token 失效、仓库无权限、Artifact 过期、
 * 代理/直连不通等各种问题。以前这些情况一律被当成「检查更新失败」，用户与开发者都拿不到
 * 任何线索；这里把每种失败映射为一个独立类型，并用 [errorMessageRes] 给出可直接展示的
 * 文案，设置页/通知栏据此提示对应原因，用户可自行点击重试。
 *
 * @project Han1meViewer
 * @author Misaka10032w
 */
sealed class HUpdaterException(
    message: String,
    cause: Throwable? = null,
    /**
     * 可直接展示给用户的文案资源 ID。
     */
    val errorMessageRes: Int,
    /**
     * 出错的 HTTP 状态码，没有走到 HTTP 层（纯网络故障）时为 null。
     * 用于把 `HTTP 503` 这类简短细节补进展示文案。
     */
    val statusCode: Int? = null,
) : IOException(message, cause) {

    /**
     * 检查更新（版本信息接口）失败。
     */
    class VersionCheck(
        message: String,
        cause: Throwable? = null,
        reason: Reason,
        statusCode: Int? = null,
    ) : HUpdaterException(message, cause, reason.errorMessageRes, statusCode) {

        /**
         * 细分原因，便于日志与埋点区分失败类型。
         */
        val reason: Reason = reason

        /**
         * 检查更新失败的细分原因。
         */
        enum class Reason(val errorMessageRes: Int) {
            /** 请求频率超限（403 + rate limit 提示，或 429） */
            RATE_LIMITED(R.string.update_error_rate_limited),

            /** Token 缺失或已失效（401） */
            BAD_CREDENTIALS(R.string.update_error_bad_credentials),

            /** 仓库/Artifact 无访问权限，或 Actions 权限被回收（403） */
            FORBIDDEN(R.string.update_error_forbidden),

            /** 渠道里根本没有可用的发布版本或构建产物 */
            NO_RELEASE(R.string.update_error_no_release),

            /** GitHub 侧 5xx 或返回了无法解析的内容 */
            SERVER(R.string.update_error_server),

            /** 网络层问题：DNS、超时、连接被重置等 */
            NETWORK(R.string.update_error_network),
        }
    }

    /**
     * 下载更新包失败。
     */
    class Download(
        message: String,
        cause: Throwable? = null,
        reason: Reason,
        statusCode: Int? = null,
    ) : HUpdaterException(message, cause, reason.errorMessageRes, statusCode) {

        /**
         * 细分原因，便于日志与埋点区分失败类型。
         */
        val reason: Reason = reason

        /**
         * 下载更新包失败的细分原因。
         */
        enum class Reason(val errorMessageRes: Int) {
            /** 请求频率超限，通常需要等限额重置 */
            RATE_LIMITED(R.string.update_error_download_rate_limited),

            /** Token 缺失或已失效 */
            BAD_CREDENTIALS(R.string.update_error_download_bad_credentials),

            /** CI Artifact 已过期（90 天）或无权访问 */
            ARTIFACT_EXPIRED(R.string.update_error_download_artifact_expired),

            /** 其他 4xx */
            CLIENT(R.string.update_error_download_client),

            /** GitHub 侧 5xx */
            SERVER(R.string.update_error_download_server),

            /** 网络层问题：DNS、超时、连接被重置等 */
            NETWORK(R.string.update_error_download_network),

            /** 响应内容不是预期的安装包（例如被认证页/错误页掉包） */
            INVALID_PAYLOAD(R.string.update_error_download_invalid_payload),
        }
    }
}
