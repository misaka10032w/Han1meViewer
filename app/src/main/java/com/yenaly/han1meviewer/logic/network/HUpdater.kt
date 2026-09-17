package com.yenaly.han1meviewer.logic.network

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.exception.HUpdaterException
import com.yenaly.han1meviewer.logic.model.github.CommitComparison
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.util.checkNeedUpdate
import com.yenaly.han1meviewer.util.copyTo
import com.yenaly.han1meviewer.util.runSuspendCatching
import com.yenaly.yenaly_libs.utils.applicationContext
import okio.use
import java.io.File
import java.util.zip.ZipInputStream

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/21 021 08:28
 */
object HUpdater {

    const val TAG = "HUpdater"

    const val DEFAULT_BRANCH = "main"

    /**
     * 展示文案里附带细节（具体网络错误、HTTP 状态码）时的长度上限，
     * 避免把整段诊断信息塞进通知栏。
     */
    private const val MESSAGE_DETAIL_LIMIT = 120

    /**
     * Regex to match multiple line feeds to a single line feed
     */
    private val linefeedRegex = Regex("\\n{2,}")

    /**
     * 检查更新的数据来源。
     */
    enum class Channel(val logName: String) {
        /** GitHub Actions 的构建产物（CI 频道，比 Release 更脆弱） */
        CI("CI Artifact"),

        /** GitHub Release */
        RELEASE("Release"),
    }

    /**
     * Check for update
     *
     * 失败时抛出带分类的[HUpdaterException]，调用方通过[errorMessage(e)]拿到可直接展示的
     * 文案。
     *
     * 注意这里**不会**在失败时切换更新渠道：CI 频道的构建始终领先于 Release，一旦因为
     * CI 出错就回落到 Release，用户会被降级到更旧的版本，反而更容易混乱。渠道由用户设置
     * 唯一决定，出错就如实报错，由用户自己在设置页手动重试。
     *
     * @param forceCheck force check
     * @throws HUpdaterException.VersionCheck 无法从 GitHub 获取版本信息时
     */
    suspend fun checkForUpdate(forceCheck: Boolean = false): Latest? {
        if (!forceCheck && !Preferences.isUpdateDialogVisible) return null
        val channel = currentChannel()
        Log.d(TAG, "开始检查更新，渠道：${channel.logName}")
        return when (channel) {
            Channel.CI -> checkCiChannel()
            Channel.RELEASE -> checkReleaseChannel()
        }
    }

    /**
     * 用户当前选择的更新渠道。
     *
     * 渠道只由用户设置决定。CI 开关（Remote Config 的 `enable_ci_update`）只用于让作者
     * 在 CI 出故障时临时下线 CI 频道，默认开启；用户主动勾选 CI 频道时也始终以 CI 为准。
     */
    private fun currentChannel(): Channel {
        if (!Preferences.useCIUpdateChannel) return Channel.RELEASE
        // Remote Config 未初始化完成时读取会抛异常，这里不再吞掉并回落到 Release，
        // 而是作为一次明确的失败上报（界面提示具体原因，用户可手动重试）
        val ciEnabled = try {
            Firebase.remoteConfig.getBoolean(FirebaseConstants.ENABLE_CI_UPDATE)
        } catch (e: Exception) {
            Log.w(TAG, "读取 Remote Config 的 ${FirebaseConstants.ENABLE_CI_UPDATE} 失败：${e.message}")
            throw HUpdaterException.VersionCheck(
                message = "无法读取远端配置（${FirebaseConstants.ENABLE_CI_UPDATE}）：${e.message}",
                cause = e,
                reason = HUpdaterException.VersionCheck.Reason.NETWORK,
            )
        }
        if (!ciEnabled) {
            Log.w(TAG, "CI 频道已被 Remote Config（${FirebaseConstants.ENABLE_CI_UPDATE}）下线")
            throw HUpdaterException.VersionCheck(
                message = "CI 更新频道已被远端开关（${FirebaseConstants.ENABLE_CI_UPDATE}）下线",
                reason = HUpdaterException.VersionCheck.Reason.NO_RELEASE,
            )
        }
        return Channel.CI
    }

    /**
     * 走 GitHub Actions 构建产物（CI 频道）。
     *
     * 相比 Release 频道，这里多了「工作流被清理 / Artifact 过期（90 天）」两种失败可能，
     * 因此按步骤分别做错误分类。
     */
    private suspend fun checkCiChannel(): Latest? {
        val curSha = BuildConfig.COMMIT_SHA
        // 特殊情况下才用注释部分，一般情况下 branch 都是固定的，要不然多一次
        // request 会对我的 API Token 造成负担。
        // val apiReq = request(HA1_GITHUB_API_URL)
        // val branch = apiReq.body?.string()?.let(::JSONObject)?.getString("default_branch")
        //     ?: return null

        val runs = requestWorkflowRuns()
        val workflowRun = runs.workflowRuns.firstOrNull()
        if (workflowRun == null) {
            // 没有任何成功的构建：可能是 CI 全挂了，但对用户来说就是「暂时没有新版本」
            Log.w(TAG, "CI 频道没有可用的工作流运行记录")
            return null
        }

        val shortSha = workflowRun.headSha.take(7)
        if (shortSha == curSha) {
            Log.d(TAG, "已是最新的 CI 构建：$shortSha")
            return null
        }

        val artifacts = requestArtifacts(workflowRun.artifactsUrl)
        val artifact = artifacts.artifacts.firstOrNull()
        val archiveUrl = artifact?.downloadLink
        val nodeId = artifact?.nodeId.orEmpty()
        if (archiveUrl.isNullOrBlank()) {
            // 有新的 commit 但产物被清理或构建失败，没有包可以下载
            Log.w(TAG, "工作流运行 ${workflowRun.headSha} 没有可下载的构建产物")
            throw HUpdaterException.VersionCheck(
                message = "CI 工作流（$shortSha）没有可下载的构建产物",
                cause = null,
                reason = HUpdaterException.VersionCheck.Reason.NO_RELEASE,
            )
        }

        // 变更日志属于「锦上添花」，拿不到就退回工作流标题，不影响更新本身
        val changelog = runSuspendCatching {
            requestCommitComparison(curSha, shortSha).commits.toChangelogPrettyString()
        }.getOrNull() ?: workflowRun.title
        return Latest("$shortSha (CI)", changelog, archiveUrl, nodeId)
    }

    /**
     * 走 GitHub Release。
     */
    private suspend fun checkReleaseChannel(): Latest? {
        val ver = requestLatestRelease()
        if (!checkNeedUpdate(ver.tagName)) return null
        val asset = ver.assets.firstOrNull()
        if (asset == null) {
            // Release 存在但没有上传安装包
            Log.w(TAG, "Release ${ver.tagName} 没有任何可下载的 asset")
            throw HUpdaterException.VersionCheck(
                message = "Release ${ver.tagName} 没有任何可下载的 asset",
                cause = null,
                reason = HUpdaterException.VersionCheck.Reason.NO_RELEASE,
            )
        }
        return Latest(ver.tagName, ver.body, asset.browserDownloadURL, asset.nodeID)
    }

    private suspend fun requestLatestRelease() = try {
        HanimeNetwork.githubService.getLatestVersion()
            .orThrowVersionCheck("GET releases/latest")
    } catch (e: HUpdaterException) {
        throw e
    } catch (e: Exception) {
        // 网络层异常（DNS、超时等）统一转成带分类的更新异常
        throw e.toVersionCheckException("GET releases/latest")
    }

    private suspend fun requestWorkflowRuns() = try {
        HanimeNetwork.githubService.getWorkflowRuns()
            .orThrowVersionCheck("GET actions/workflows/ci.yml/runs")
    } catch (e: HUpdaterException) {
        throw e
    } catch (e: Exception) {
        throw e.toVersionCheckException("GET actions/workflows/ci.yml/runs")
    }

    private suspend fun requestArtifacts(url: String) = try {
        HanimeNetwork.githubService.getArtifacts(url)
            .orThrowVersionCheck("GET workflow run artifacts")
    } catch (e: HUpdaterException) {
        throw e
    } catch (e: Exception) {
        throw e.toVersionCheckException("GET workflow run artifacts")
    }

    private suspend fun requestCommitComparison(curSha: String, latestSha: String) =
        HanimeNetwork.githubService.getCommitComparison(curSha, latestSha)
            .orThrowVersionCheck("GET compare/$curSha...$latestSha")

    /**
     * 把网络层异常（DNS、超时、连接重置等）包装成带分类的更新异常。
     */
    private fun Exception.toVersionCheckException(api: String): HUpdaterException.VersionCheck {
        return HUpdaterException.VersionCheck(
            message = "请求 $api 失败：${message ?: this::class.java.simpleName}",
            cause = this,
            reason = HUpdaterException.VersionCheck.Reason.NETWORK,
        )
    }

    /**
     * Inject update to file
     *
     * @param url update url
     * @throws HUpdaterException.Download 下载或写入失败时
     */
    suspend fun File.injectUpdate(url: String, progress: (suspend (Int, Long, Long) -> Unit)? = null) {
        val isZip = url.endsWith("zip")
        Log.d(TAG, "开始下载更新包（${if (isZip) "CI zip" else "release"}）：$url")
        downloadTo(url, isZip, progress)
    }

    private suspend fun File.downloadTo(
        url: String,
        isZip: Boolean,
        progress: (suspend (Int, Long, Long) -> Unit)?,
    ) {
        val res = try {
            HanimeNetwork.githubService.request(url)
        } catch (e: Exception) {
            throw HUpdaterException.Download(
                message = "请求下载地址失败：${e.message ?: e::class.java.simpleName}",
                cause = e,
                reason = HUpdaterException.Download.Reason.NETWORK,
            )
        }
        // 先确认拿到的是安装包，再决定是否落盘
        val body = res.orThrowDownload(url)
        if (isZip) {
            Log.d(TAG, "Injecting update from zip ($url)")
            body.use {
                it.byteStream().use { stream ->
                    ZipInputStream(stream).use { zip ->
                        val entry = zip.nextEntry
                        if (entry == null) {
                            throw HUpdaterException.Download(
                                message = "下载到的 CI 压缩包内容为空（$url）",
                                cause = null,
                                reason = HUpdaterException.Download.Reason.INVALID_PAYLOAD,
                            )
                        }
                        outputStream().use { out ->
                            Log.i(TAG, "content length: ${it.contentLength()}")
                            // 估摸着压缩率为0.56左右，稍微估算解压后大小，防止进度卡在100%时间过长
                            zip.copyTo(out, (it.contentLength() * 1.79).toLong(), progress = progress)
                        }
                    }
                }
            }
        } else {
            Log.d(TAG, "Injecting update from release ($url)")
            outputStream().use { out ->
                body.use {
                    Log.i(TAG, "content length: ${it.contentLength()}")
                    it.byteStream().copyTo(out, it.contentLength(), progress = progress)
                }
            }
        }
    }

    /**
     * 更新失败时可直接展示给用户的文案。
     *
     * 与[checkForUpdate] / [injectUpdate]抛出的异常配套使用：把「限额耗尽 / 密钥失效 /
     * 网络异常」这类具体原因呈现给用户，而不是笼统的「更新失败」。
     *
     * @return 可直接放进 UI / 通知的文案；异常不是[HUpdaterException]时返回 null，
     * 由调用方决定自己的兜底文案
     */
    fun errorMessage(e: Throwable?): CharSequence? {
        val reason = e as? HUpdaterException ?: return null
        val template = applicationContext.getString(reason.errorMessageRes)
        if (!template.contains("%s")) return template

        // 模板带 %s 时补上简短细节：有状态码就用 `HTTP xxx`，
        // 否则退回底层网络错误（如 “Connection refused”）
        val detail = reason.statusCode?.let { "HTTP $it" }
            ?: e.cause?.message?.takeIf { it.isNotBlank() }
            ?: e.message?.takeIf { it.isNotBlank() }
            ?: return template

        return runCatching {
            applicationContext.getString(reason.errorMessageRes, detail.take(MESSAGE_DETAIL_LIMIT))
        }.getOrDefault(template)
    }

    /**
     * 更新失败的详细诊断信息，写日志 / Crashlytics 用（含状态码与响应体片段）。
     */
    fun errorDetail(e: Throwable?): String? {
        return e?.message?.takeIf { it.isNotBlank() }
    }

    /**
     * This function is used to filter out commits that are not authored by the user.
     */
    private val CommitComparison.Commit.CommitDetail.CommitAuthor.isAuthorShouldIgnore: Boolean
        get() = name.contains("dependabot")

    private fun List<CommitComparison.Commit>.toChangelogPrettyString(): String {
        return filterNot { commit ->
            commit.commit.author.isAuthorShouldIgnore
        }.distinct().reversed().joinToString("\n\n") { commit ->
            val message = commit.commit.message.replace(linefeedRegex, "\n")
            "↓ (@${commit.commit.author.name})\n$message"
        }
    }
}
