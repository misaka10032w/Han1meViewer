package com.yenaly.han1meviewer.worker

import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.state.DownloadState
import com.yenaly.han1meviewer.util.applicationContext
import com.yenaly.han1meviewer.util.runSuspendCatching
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

/**
 * 优化后的下载管理器，利用 Channel 和 Semaphore 限制并发下载数，
 * 通过轮询 WorkManager 的任务状态等待完成并释放许可。
 */
object HanimeDownloadManagerV2 {

    private const val TAG = "HanimeDownloadManager"

    const val MAX_CONCURRENT_DOWNLOAD_DEF = 2

    /** 轮询 WorkManager 任务状态的间隔 */
    private const val WORK_COMPLETION_POLL_INTERVAL_MS = 5_000L

    /** 任务异常终止（DB 未终态化）后的自动重排次数上限，超过则标记失败，防止死循环 */
    private const val MAX_ANOMALY_RELAUNCH = 3

    // 注意：必须声明在 maxConcurrentDownloadCount 之前——
    // 其 setter 会 trySend，而 init 块按声明顺序执行
    private val downloadChannel = Channel<DownloadMsg>(capacity = Channel.UNLIMITED)

    var maxConcurrentDownloadCount = 0
        set(value) {
            field = if (value > 0) value else Int.MAX_VALUE
            // 如果更新并发数，重新创建 semaphore
            semaphore = Semaphore(field)
            // 并发数变化（尤其是调大）后立即尝试推进等待队列，
            // 否则要等到下一个活跃任务完成才会有任务被拉起
            downloadChannel.trySend(DownloadMsg.ProcessNext)
        }

    private val workManager = WorkManager.getInstance(applicationContext)

    // 信号量限制同时下载的任务数量
    private var semaphore: Semaphore = Semaphore(1)

    init {
        // 用 Preferences 里的值初始化，保证 0 会被转换成 Int.MAX_VALUE
        maxConcurrentDownloadCount = Preferences.downloadCountLimit
    }

    // Channel 内部状态：保存正在下载任务与等待队列
    private val activeDownloads = linkedMapOf<String, HanimeDownloadWorker.Args>()
    private val waitingQueue = ArrayDeque<HanimeDownloadWorker.Args>()

    // 任务异常终止后的重排次数（videoCode -> 次数），用于防止病态任务无限重排
    private val anomalyRelaunchCounts = mutableMapOf<String, Int>()

    // 协程 Scope，用于管理 channel 与任务协程
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Channel 消息类型
    private sealed class DownloadMsg {
        /**
         * 添加下载任务
         */
        data class Add(
            val args: HanimeDownloadWorker.Args,
            val redownload: Boolean = false,
            val waiting: Boolean = false,
            val state: DownloadState = DownloadState.Unknown
        ) : DownloadMsg()

        /**
         * 恢复下载任务（暂停 => 下载）
         */
        data class Resume(val args: HanimeDownloadWorker.Args) : DownloadMsg()

        /**
         * 停止下载任务
         */
        data class Stop(val args: HanimeDownloadWorker.Args) : DownloadMsg()

        /**
         * 删除下载任务
         */
        data class Delete(val args: HanimeDownloadWorker.Args) : DownloadMsg()

        /**
         * 处理下一个任务
         */
        data object ProcessNext : DownloadMsg()
    }

    init {
        scope.launch {
            for (msg in downloadChannel) {
                when (msg) {
                    is DownloadMsg.Add -> {
                        if (msg.args.videoCode in activeDownloads) {
                            Log.d(TAG, "任务已存在：${msg.args.videoCode}")
                        } else if (waitingQueue.any { it.videoCode == msg.args.videoCode }) {
                            Log.d(TAG, "任务已在等待队列：${msg.args.videoCode}")
                        } else {
                            // Paused/Finished/Failed 的任务不参与自动调度
                            val schedulable = msg.state != DownloadState.Paused &&
                                    msg.state != DownloadState.Finished &&
                                    msg.state != DownloadState.Failed
                            if (activeDownloads.size < maxConcurrentDownloadCount && schedulable) {
                                Log.d(TAG, "添加任务：${msg.args.videoCode}")
                                activeDownloads[msg.args.videoCode] = msg.args
                                launchDownload(msg.args, msg.redownload, msg.waiting)
                            } else {
                                Log.d(TAG, "任务已满，加入等待队列：${msg.args.videoCode}")
                                when (msg.state) {
                                    DownloadState.Downloading -> {
                                        // 之前为 Downloading 的优先级更高
                                        waitingQueue.addFirst(msg.args)
                                        enqueueWaitingWork(msg.args, msg.redownload)
                                    }

                                    DownloadState.Queued, DownloadState.Unknown -> {
                                        waitingQueue.addLast(msg.args)
                                        enqueueWaitingWork(msg.args, msg.redownload)
                                    }

                                    else -> Unit
                                }
                            }
                        }
                    }

                    is DownloadMsg.Resume -> {
                        if (msg.args.videoCode in activeDownloads) {
                            Log.d(TAG, "任务已在下载中，无需恢复：${msg.args.videoCode}")
                        } else {
                            waitingQueue.removeIf { it.videoCode == msg.args.videoCode }
                            Log.d(TAG, "恢复任务：${msg.args.videoCode}")
                            // 如果 active 已满，则暂停一个任务，加入等待队列
                            while (activeDownloads.size >= maxConcurrentDownloadCount && activeDownloads.isNotEmpty()) {
                                val (videoCode, task) = activeDownloads.entries.first()
                                activeDownloads.remove(videoCode)
                                stopWork(task)
                                waitingQueue.addLast(task)
                                markQueued(task)
                                Log.d(TAG, "任务已满，暂停任务：$videoCode")
                            }
                            activeDownloads[msg.args.videoCode] = msg.args
                            launchDownload(msg.args, redownload = false, waiting = false)
                        }
                    }

                    is DownloadMsg.Stop -> {
                        if (activeDownloads.remove(msg.args.videoCode) != null) {
                            Log.d(TAG, "停止任务：${msg.args.videoCode}")
                            stopWork(msg.args)
                            processNext()
                        } else {
                            Log.e(TAG, "停止任务，不应该走到这里：${msg.args.videoCode}")
                            waitingQueue.removeIf { it.videoCode == msg.args.videoCode }
                            markPaused(msg.args)
                        }
                    }

                    is DownloadMsg.Delete -> {
                        if (activeDownloads.remove(msg.args.videoCode) != null) {
                            Log.d(TAG, "从正在下载列表中删除任务：${msg.args.videoCode}")
                        } else {
                            waitingQueue.removeIf { it.videoCode == msg.args.videoCode }
                            Log.d(TAG, "从等待队列中删除任务：${msg.args.videoCode}")
                        }
                        deleteWork(msg.args)
                        processNext()
                    }

                    DownloadMsg.ProcessNext -> processNext()
                }

                // 自愈：每处理完一条消息都尝试推进等待队列。
                // 否则一旦出现"槽位被误占/无人触发 ProcessNext"的情况（比如历史版本的
                // 观察者泄漏），等待队列里的任务会在有空位时也永远无人启动。
                if (waitingQueue.isNotEmpty()) processNext()
            }
        }
    }

    /**
     * 初始化，加载所有正在下载的任务
     */
    suspend fun init() {
        Log.d(TAG, "init")
        val allDownloading =
            DownloadDatabase.instance.hanimeDownloadDao.loadAllDownloadingHanimeOnce()
        allDownloading.forEach { entity ->
            val args = HanimeDownloadWorker.Args.fromEntity(entity)
            // addTask
            downloadChannel.send(DownloadMsg.Add(args, state = entity.state))
        }
    }

    /**
     * 添加下载任务
     */
    fun addTask(
        args: HanimeDownloadWorker.Args,
        redownload: Boolean = false, waiting: Boolean = false
    ) {
        scope.launch { downloadChannel.send(DownloadMsg.Add(args, redownload, waiting)) }
    }

    /**
     * 恢复下载任务
     */
    fun resumeTask(entity: HanimeDownloadEntity) {
        val args = HanimeDownloadWorker.Args.fromEntity(entity)
        scope.launch { downloadChannel.send(DownloadMsg.Resume(args)) }
    }

    /**
     * 停止下载任务
     */
    fun stopTask(entity: HanimeDownloadEntity) {
        val args = HanimeDownloadWorker.Args.fromEntity(entity)
        scope.launch { downloadChannel.send(DownloadMsg.Stop(args)) }
    }

    /**
     * 删除下载任务
     */
    fun deleteTask(entity: HanimeDownloadEntity) {
        val args = HanimeDownloadWorker.Args.fromEntity(entity)
        scope.launch { downloadChannel.send(DownloadMsg.Delete(args)) }
    }

    /**
     * 处理等待队列中的下一个任务
     */
    private fun processNext() {
        Log.d(TAG, "processNext")
        while (activeDownloads.size < maxConcurrentDownloadCount && waitingQueue.isNotEmpty()) {
            val next = waitingQueue.removeFirst()
            activeDownloads[next.videoCode] = next
            launchDownload(next, redownload = false, waiting = false)
        }
    }

    /**
     * 启动下载任务，采用 semaphore 限制并发数，并等待任务完成后自动释放许可
     */
    private fun launchDownload(
        args: HanimeDownloadWorker.Args,
        redownload: Boolean,
        waiting: Boolean
    ) {
        scope.launch {
            // 如果当前处于等待状态，则直接启动任务。目的就是为了添加到列表，但不下载
            if (waiting) {
                Log.d(TAG, "launchDownload (waiting): ${args.videoCode}")
                markQueued(args)
            } else {
                try {
                    // 使用 semaphore.withPermit 来确保同时只有规定数量的任务在执行
                    semaphore.withPermit {
                        Log.d(TAG, "launchDownload (start): ${args.videoCode}")
                        // 启动 WorkManager 任务
                        val workId = startWork(args, redownload)
                        Log.d(TAG, "WorkManager 已入队：videoCode=${args.videoCode}, workId=$workId, redownload=$redownload")
                        // 轮询等待 WorkManager 任务完成
                        awaitWorkCompletion(args.videoCode, workId.toString())
                    }
                } catch (e: CancellationException) {
                    // 本协程被取消才真正向上抛（finally 仍会清理槽位）
                    currentCoroutineContext().ensureActive()
                    Log.e(TAG, "launchDownload 异常：${args.videoCode}", e)
                } catch (t: Throwable) {
                    // startWork / 等待过程中的意外异常不能让槽位永久泄漏
                    Log.e(TAG, "launchDownload 异常：${args.videoCode}", t)
                } finally {
                    // 下载完成或取消后，从 active 中移除，并尝试启动下一个任务
                    activeDownloads.remove(args.videoCode)
                    Log.d(TAG, "launchDownload (end): ${args.videoCode}")
                    // 对账：work 结束后 DB 仍是 Queued/Downloading，说明 worker 未能
                    // 正常终态化（典型：finally 写库被协程取消打断）。重新入队重试，
                    // 而不是把任务留在"卡在队列中"的孤儿状态。
                    withContext(NonCancellable) {
                        val entity = DatabaseRepo.HanimeDownload.find(
                            args.videoCode, args.quality.orEmpty()
                        )
                        if (entity != null && (
                                    entity.state == DownloadState.Queued ||
                                    entity.state == DownloadState.Downloading ||
                                    // Unknown: UI 占位入库的新任务，若 Worker 异常终止也可安全重排
                                    entity.state == DownloadState.Unknown
                                    )
                        ) {
                            val count = anomalyRelaunchCounts.merge(args.videoCode, 1, Int::plus) ?: 1
                            if (count <= MAX_ANOMALY_RELAUNCH) {
                                Log.w(
                                    TAG,
                                    "任务异常终止（state=${entity.state}），重新入队：${args.videoCode} ($count/$MAX_ANOMALY_RELAUNCH)"
                                )
                                downloadChannel.trySend(DownloadMsg.Add(args, state = entity.state))
                            } else {
                                Log.e(TAG, "任务反复异常终止，标记为失败：${args.videoCode}")
                                anomalyRelaunchCounts.remove(args.videoCode)
                                DatabaseRepo.HanimeDownload.update(
                                    entity.copy(state = DownloadState.Failed)
                                )
                            }
                        } else {
                            anomalyRelaunchCounts.remove(args.videoCode)
                        }
                    }
                    downloadChannel.trySend(DownloadMsg.ProcessNext)
                }
            }
        }
    }

    /**
     * 开启下载任务
     */
    private suspend fun startWork(
        args: HanimeDownloadWorker.Args,
        redownload: Boolean = false,
        waiting: Boolean = false,
        delete: Boolean = false
    ) = HanimeDownloadWorker.build(constraintsRequired = !delete) {
            setInputData(
                workDataOf(
                    HanimeDownloadWorker.QUALITY to args.quality,
                    HanimeDownloadWorker.DOWNLOAD_URL to args.downloadUrl,
                    HanimeDownloadWorker.VIDEO_TYPE to args.videoType,
                    HanimeDownloadWorker.HANIME_NAME to args.hanimeName,
                    HanimeDownloadWorker.VIDEO_CODE to args.videoCode,
                    HanimeDownloadWorker.COVER_URL to args.coverUrl,
                    HanimeDownloadWorker.GROUP_ID to (args.groupId ?: HanimeDownloadWorker.NO_GROUP_ID),
                    HanimeDownloadWorker.REDOWNLOAD to redownload,
                    HanimeDownloadWorker.IN_WAITING_QUEUE to waiting,
                    HanimeDownloadWorker.DELETE to delete
                )
            )
        }.apply {
            workManager.beginUniqueWork(
                args.videoCode, ExistingWorkPolicy.REPLACE, this
            ).enqueue().await()
        }.id

    /**
     * 取消正在执行的 WorkManager 任务
     */
    private suspend fun stopWork(args: HanimeDownloadWorker.Args) {
        runSuspendCatching {
            workManager.cancelUniqueWork(args.videoCode).await()
            markPaused(args)
            Log.d(TAG, "stopWork (cancelUniqueWork): ${args.videoCode}")
        }.onFailure { t -> // 上述方法可能无法取消任务
            t.printStackTrace()
            markPaused(args)
        }
    }

    private suspend fun markQueued(args: HanimeDownloadWorker.Args) {
        DatabaseRepo.HanimeDownload.find(args.videoCode, args.quality.orEmpty())?.let { entity ->
            DatabaseRepo.HanimeDownload.update(entity.copy(state = DownloadState.Queued))
        }
    }

    private suspend fun enqueueWaitingWork(
        args: HanimeDownloadWorker.Args,
        redownload: Boolean = false
    ) {
        val entity = DatabaseRepo.HanimeDownload.find(args.videoCode, args.quality.orEmpty())
        if (entity == null) {
            Log.d(TAG, "入库占位 work 已入队（waiting=true）：videoCode=${args.videoCode}")
            startWork(args, redownload = redownload, waiting = true)
        } else {
            Log.d(TAG, "占位 work 无需创建（DB 已有记录），仅标记 Queued：videoCode=${args.videoCode}")
            DatabaseRepo.HanimeDownload.update(entity.copy(state = DownloadState.Queued))
        }
    }

    private suspend fun markPaused(args: HanimeDownloadWorker.Args) {
        DatabaseRepo.HanimeDownload.find(args.videoCode, args.quality.orEmpty())?.let { entity ->
            if (entity.state != DownloadState.Finished) {
                DatabaseRepo.HanimeDownload.update(entity.copy(state = DownloadState.Paused))
            }
        }
    }

    /**
     * 删除下载任务
     *
     * 删除操作交给 WorkManager 处理
     */
    private suspend fun deleteWork(args: HanimeDownloadWorker.Args) = startWork(args, delete = true)

    /**
     * 等待指定 work 结束。
     *
     * 旧实现用 LiveData observeForever 按 workId 过滤 unique work 列表，存在两个问题：
     * 1. work 被 REPLACE（同名重新入队）或被清理后，旧的 workId 从 unique name
     *    列表里永远消失，观察者永远匹配不到，协程永久挂起，信号量许可和
     *    activeDownloads 槽位被永久占用（僵尸槽位），等待队列再也无法推进；
     * 2. 观察依赖 Room InvalidationTracker 的推送，一旦漏推同样挂死。
     *
     * 改为轮询 WorkManager 真值：任务结束（含 CANCELLED）或 workId 消失
     * （被替换/删除，原 work 不可能继续下载）都视为结束，不会永久挂起。
     */
    private suspend fun awaitWorkCompletion(videoCode: String, workId: String) {
        while (true) {
            val infos = try {
                workManager.getWorkInfosForUniqueWork(videoCode).awaitFuture()
            } catch (e: CancellationException) {
                // 只有"本协程"被取消才向上抛；future 自身被取消（少见）
                // 在 JVM 上同样是 CancellationException，需区分，否则任务会脱离管理
                currentCoroutineContext().ensureActive()
                Log.w(TAG, "awaitWorkCompletion future 被取消：$videoCode")
                null
            } catch (t: Throwable) {
                // 查询失败（瞬时 binder 异常等）不视为结束，下轮重试
                Log.w(TAG, "awaitWorkCompletion 查询失败：$videoCode, ${t.message}")
                null
            }
            if (infos != null) {
                val info = infos.firstOrNull { it.id.toString() == workId }
                if (info == null || info.state.isFinished) {
                    Log.d(TAG, "任务结束：$videoCode (state=${info?.state})")
                    return
                }
            }
            delay(WORK_COMPLETION_POLL_INTERVAL_MS.milliseconds)
        }
    }

    /**
     * 挂起等待 ListenableFuture 完成。
     *
     * 用 Futures.addCallback 而非 addListener+get()：
     * 回调只在 future 完成后触发、直接把值/异常传给协程，
     * 全程无任何潜在阻塞调用（IDE 也不会再报
     * "Possibly blocking call in non-blocking context"）。
     * 直接执行器下，回调运行在完成 future 的线程上。
     */
    private suspend fun <T> ListenableFuture<T>.awaitFuture(): T =
        suspendCancellableCoroutine { cont ->
            Futures.addCallback(
                this,
                object : FutureCallback<T> {
                    override fun onSuccess(result: T) {
                        cont.resume(result)
                    }

                    override fun onFailure(t: Throwable) {
                        cont.resumeWithException(t)
                    }
                },
            ) { it.run() }
            cont.invokeOnCancellation { cancel(false) }
        }
}
