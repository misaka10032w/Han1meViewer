package com.yenaly.han1meviewer.ui.player.kernel

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Surface
import androidx.core.net.toUri
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.USER_AGENT
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.util.AnimeShaders
import com.yenaly.han1meviewer.util.AnimeShaders.getCert
import `is`.xyz.mpv.MPVLib

/**
 * 基于 libmpv 的播放内核。
 *
 * 自旧版 `MpvMediaKernel`（JZVD `JZMediaInterface` 实现）迁移而来，保留其全部能力：
 * - 完整的 mpv 选项配置（渲染器、硬解、插帧、缓存、丢帧、deband、TLS、代理等）；
 * - 用户自定义 mpv 参数（`;` 分隔、`#` 注释）；
 * - Anime4K 超分 shader 动态加载；
 * - 本地文件经 `fd://` 播放（content/file URI → ParcelFileDescriptor detach）；
 * - surface 尺寸同步（`android-surface-size`）。
 *
 * ### 线程模型
 * 所有方法在主线程调用；MPV 事件从 mpv 内部线程回调，经 [mainHandler] 切回主线程。
 */
class MpvKernel(
    private val context: Context,
) : PlayerKernel {

    companion object {
        private const val TAG = "MpvKernel"

        /** END_FILE 位置启发式的容差（秒）：距结尾不足该值视为自然播完。 */
        private const val END_OF_FILE_TOLERANCE_SEC = 2.0
    }

    override val type = KernelType.MpvPlayer

    override var listener: KernelListener? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    /** [prepare] 之前 setSurface 缓存的 surface，loadfile 后挂载。 */
    private var pendingSurface: Surface? = null

    /** 当前已 attach 的 surface。 */
    private var currentSurface: Surface? = null

    /** surface 未就绪时延迟加载的文件路径与请求。 */
    private var pendingPath: String? = null
    private var pendingRequest: MediaRequest? = null

    private val certFile = getCert(context)

    /** mpv 事件观察者回调里持续刷新的缓存值（mpv 事件线程写，主线程读）。 */
    @Volatile
    private var mpvTimePos = 0.0

    @Volatile
    private var mpvCacheDuration = 0.0

    @Volatile
    private var mpvDuration = 0.0

    /** 用户暂停属性（区别于缓存暂停），卡顿看门狗用。 */
    @Volatile
    private var pausedByUser = false

    // ---------------- 卡顿看门狗 ----------------
    //
    // mpv 配置了 cache-pause=no（不因缓存耗尽暂停），缓存耗尽/断网时
    // paused-for-cache 不会置位、time-pos 停走但没有任何事件可映射——
    // UI 会停留在「播放中」。看门狗在文件加载完成后周期检查：
    // 非用户暂停但 time-pos 连续 [STALL_THRESHOLD_MS] 未前进 → onBuffering，
    // 恢复前进 → onPlaying。

    /** time-pos 距上次前进的毫秒数超过该值视为卡顿。 */
    private val STALL_THRESHOLD_MS = 3000L

    /** 解码器缓存低于该秒数才与「位置不前进」共同判定为网络类卡顿。 */
    private val STALL_CACHE_EMPTY_SEC = 1.0

    @Volatile
    private var lastTimePosAtMs = 0L

    /**
     * 连续停滞判死窗口：窗口内播放位置净前进不足 [STALL_TO_ERROR_MIN_ADVANCE_SEC]
     * 且非用户暂停 → 判定播放已死。mpv 断网后无限重连（自定义 reconnect 参数等）
     * 不出 END_FILE 时，重连期间的 time-pos 微动会不断骗过「最近有前进」检测，
     * 这里用窗口净前进量兜底，保证 UI 不会无限停留在播放中/缓冲中。
     */
    private val STALL_TO_ERROR_WINDOW_MS = 20_000L
    private val STALL_TO_ERROR_MIN_ADVANCE_SEC = 0.5

    private var stallCheckpointAtMs = 0L

    @Volatile
    private var stallCheckpointPos = 0.0

    private var stallReported = false

    private val stallWatchdog = object : Runnable {
        override fun run() {
            if (!fileActive) return
            val now = android.os.SystemClock.elapsedRealtime()
            when {
                pausedByUser -> {
                    // 用户暂停下 time-pos 本就不动，不视为卡顿
                    stallReported = false
                    stallCheckpointAtMs = now
                    stallCheckpointPos = mpvTimePos
                }

                now - lastTimePosAtMs > STALL_THRESHOLD_MS && mpvCacheDuration < STALL_CACHE_EMPTY_SEC -> {
                    // 双条件：位置不前进 + 解码器缓存近空。time-pos 属性事件有
                    // 节流（慢解码时可能数秒不发），仅凭「无事件」会误报卡顿；
                    // 缓存近空才说明是网络/读取类停顿。
                    if (!stallReported) {
                        stallReported = true
                        Log.w(TAG, "playback stalled (no advance, cache empty), report BUFFERING")
                        listener?.onBuffering()
                    }
                    // 卡顿贯穿整个窗口且播放位置没有净前进 → 判死
                    if (now - stallCheckpointAtMs >= STALL_TO_ERROR_WINDOW_MS) {
                        val advanced = mpvTimePos - stallCheckpointPos
                        if (advanced < STALL_TO_ERROR_MIN_ADVANCE_SEC) {
                            fileActive = false
                            Log.e(
                                TAG,
                                "playback dead: only ${advanced}s advanced in " +
                                        "${STALL_TO_ERROR_WINDOW_MS / 1000}s, report ERROR",
                            )
                            stopStallWatchdog()
                            listener?.onError(IllegalStateException("playback stalled"))
                            return
                        }
                        stallCheckpointAtMs = now
                        stallCheckpointPos = mpvTimePos
                    }
                }

                stallReported -> {
                    stallReported = false
                    stallCheckpointAtMs = now
                    stallCheckpointPos = mpvTimePos
                    Log.i(TAG, "playback resumed after stall")
                    listener?.onPlaying()
                }
            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    private fun startStallWatchdog() {
        mainHandler.removeCallbacks(stallWatchdog)
        stallReported = false
        val now = android.os.SystemClock.elapsedRealtime()
        lastTimePosAtMs = now
        stallCheckpointAtMs = now
        stallCheckpointPos = mpvTimePos
        mainHandler.postDelayed(stallWatchdog, 1000)
    }

    private fun stopStallWatchdog() {
        mainHandler.removeCallbacks(stallWatchdog)
    }

    // ---------------- 准备超时 ----------------
    //
    // mpv 对打不开的流可能无限重连（用户自定义 reconnect 参数、较长
    // network-timeout 等），FILE_LOADED 永远不来，UI 停留在 PREPARING 转圈。
    // network-timeout + 15s 仍未加载完成即报错。

    private val prepareTimeout = Runnable {
        if (pendingLoad) {
            pendingLoad = false
            Log.e(TAG, "prepare timeout, report ERROR")
            listener?.onError(IllegalStateException("prepare timeout"))
        }
    }

    private fun schedulePrepareTimeout() {
        cancelPrepareTimeout()
        val timeoutMs = Preferences.mpvNetworkTimeout.coerceAtLeast(10) * 1000L + 15_000L
        mainHandler.postDelayed(prepareTimeout, timeoutMs)
    }

    private fun cancelPrepareTimeout() {
        mainHandler.removeCallbacks(prepareTimeout)
    }

    /** 本地文件播放用的 PFD（fd://）。 */
    private var currentPfd: ParcelFileDescriptor? = null
    private var detachFd: Int? = null
    private var pfdFilePath = false

    /** 上报过尺寸的缓存（video-params 观察回调用）。 */
    private var reportedVideoWidth = 0
    private var reportedVideoHeight = 0
    private var lastReportedSizeW = 0
    private var lastReportedSizeH = 0

    /**
     * END_FILE 归类用的加载状态（mpv 事件线程写，主线程读）：
     * - [pendingLoad]：START_FILE 后、FILE_LOADED 前——期间 END_FILE = 本次加载失败；
     * - [fileActive]：FILE_LOADED 后——END_FILE = 正常播完；
     * - 两者皆否：[release] 里 `loadfile ""` 产生的伪 END_FILE，忽略——
     *   mpv 事件异步，它可能在下一个内核 addObserver 之后才送达。
     */
    @Volatile
    private var pendingLoad = false

    @Volatile
    private var fileActive = false

    override val videoWidth: Int
        get() = MPVLib.getPropertyInt("video-params/w") ?: 0

    override val videoHeight: Int
        get() = MPVLib.getPropertyInt("video-params/h") ?: 0

    /** mpv 选项，随用户设置（[Preferences]）构建。 */
    private val mpvOptions: Map<String, String>
        get() = buildMap {
            // 视频输出驱动：GPU 渲染（支持 GLSL 滤镜/Anime4K/插帧）
            if (Preferences.enableGPUNextRenderer) {
                put("vo", "gpu-next")
            } else {
                put("vo", "gpu")
            }

            // 预设模式：fast（性能优先，画质略低；可改为 gpu-hq 追求高画质）
            put("profile", when (Preferences.mpvProfile) {
                "gpu-hq" -> "gpu-hq"
                "fast" -> "fast"
                else -> "default"
            })

            // 解码方式：选择合适的解码器（mediacodec/mediacodec-copy）
            put("hwdec", when (Preferences.mpvHwdec) {
                "Auto" -> "auto"
                "HW" -> "mediacodec-copy"
                "HW+" -> "mediacodec"
                "Vulkan" -> "vulkan-copy"
                "vulkan+" -> "vulkan"
                "SW" -> "no"
                else -> "auto"
            })

            // 日志等级：fatal → error → warn → info → status → verbose → debug → trace
            put("msg-level", "all=" + if (BuildConfig.DEBUG) "debug" else "warn")

            // 插帧设置
            if (Preferences.mpvInterpolation) {
                put("interpolation", "yes")       // 启用插帧
                put("tscale", "oversample")       // 时间插值算法
                put("video-sync", "display-resample") // 同步刷新率
            }

            // 缓存与性能
            put("cache", "yes")                   // 启用解复用缓存
            put("cache-secs", Preferences.mpvCacheSecs.toString()) // 预缓存秒数
            put("vd-lavc-threads", Runtime.getRuntime().availableProcessors().toString())
            if (Preferences.mpvFramedrop) {
                put("framedrop", "vo")            // GPU 繁忙时允许丢帧（保持音画同步，避免卡顿）
            } else {
                put("framedrop", "no")
            }

            put("deband", if (Preferences.mpvDeband) "yes" else "no") // 去色带
            put("cache-pause", "no")              // 缓存时是否暂停播放
            put("network-timeout", Preferences.mpvNetworkTimeout.toString()) // 请求超时

            put("tls-ca-file", certFile)          // 为播放器指定根证书文件，解决 tls-verify 播放失败问题
            put("tls-verify", if (Preferences.mpvTlsVerify) "no" else "yes")

            // 单独为 MPV 播放器配置代理，因为它不走 ProxySelector，也不支持 socks 代理
            val proxyIp = Preferences.proxyIp
            val proxyPort = Preferences.proxyPort
            if (proxyIp.isNotBlank() && proxyPort != -1) {
                val proxyUrl = "$proxyIp:$proxyPort"
                if (Preferences.proxyType == HProxySelector.TYPE_HTTP) {
                    put("http-proxy", "http://$proxyUrl")
                }
            }
            put("user-agent", USER_AGENT)
        }

    /** mpv 需要持续观察的属性。 */
    private val observedProperties = listOf(
        "time-pos" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,        // 当前播放时间（秒，带小数）
        "duration" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,        // 视频总时长（秒）
        "pause" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,             // 是否暂停
        "playback-active" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,   // 播放是否处于活动状态
        "video-params/w" to MPVLib.mpvFormat.MPV_FORMAT_INT64,   // 视频宽度（像素）
        "video-params/h" to MPVLib.mpvFormat.MPV_FORMAT_INT64,   // 视频高度（像素）
        "demuxer-cache-duration" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "paused-for-cache" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,  // 缓存耗尽暂停（→ BUFFERING）
    )

    /** 解析用户自定义 mpv 参数（`;` 分隔、`#` 注释、`key,value` 形式）。 */
    private fun parseCustomMpvParams(): LinkedHashMap<String, String> {
        val map = linkedMapOf<String, String>()
        Preferences.customMpvParams.split(";").forEach { entry ->
            val trimmedEntry = entry.trim()
            if (trimmedEntry.isEmpty() || trimmedEntry.startsWith("#")) return@forEach
            val parts = trimmedEntry.split(",", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    map[key] = value
                }
            }
        }
        return map
    }

    private fun init() {
        mpvOptions.forEach { (key, value) ->
            Log.i("MPV_config", "$key,$value")
            MPVLib.setOptionString(key, value)
        }
        try {
            parseCustomMpvParams().forEach { (key, value) ->
                Log.i("MPV_config", "custom: $key,$value")
                MPVLib.setOptionString(key, value)
            }
        } catch (e: Exception) {
            GlobalToasts.show(e.message.orEmpty(), level = GlobalToasts.ToastLevel.ERROR)
        }
        observedProperties.forEach { (name, type) ->
            MPVLib.observeProperty(name, type)
        }
        MPVLib.addObserver(mpvEventObserver)
    }

    /** 网络地址直接播放；本地 file/content URI 转 `fd://`。 */
    private fun prepareUri(uri: Uri): String? {
        return when (uri.scheme) {
            "http", "https" -> uri.toString()

            "file", "content" -> {
                try {
                    currentPfd = context.contentResolver.openFileDescriptor(uri, "r")
                    detachFd = currentPfd?.detachFd()
                    Log.d(TAG, "Detached FD = $detachFd")
                    pfdFilePath = true
                    if (detachFd != null) "fd://$detachFd" else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            else -> null
        }
    }

    /** 释放本地文件播放占用的 PFD。 */
    private fun releaseCurrentPfd(from: String? = null) {
        if (!pfdFilePath) return
        try {
            currentPfd?.let {
                Log.i(TAG, "Closing currentPfd: $it")
                it.close()
            }
            detachFd?.let { fd ->
                Log.i(TAG, "Closing detachFd: $fd")
                try {
                    ParcelFileDescriptor.adoptFd(fd).close()
                } catch (e: Exception) {
                    Log.w(TAG, "detachFd $fd already closed or invalid", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing PFD", e)
        } finally {
            currentPfd = null
            detachFd = null
            mainHandler.postDelayed({
                Log.i(TAG, "${from ?: "releaseCurrentPfd"} completed. PFD cleared.")
            }, 200)
        }
    }

    override fun prepare(request: MediaRequest) {
        init()
        if (request.url.isEmpty()) {
            Log.e(TAG, "视频链接为空")
            return
        }
        Log.i(TAG, "URL Link = ${request.url}")
        MPVLib.setOptionString("force-window", "yes")

        val path = prepareUri(request.url.toUri()) ?: return
        if (currentSurface != null) {
            loadFile(path, request)
        } else {
            // surface 未就绪：延迟到 setSurface 后再 loadfile，
            // 否则 vo 初始化会因 WinID 无效直接原生崩溃（原版 JZVD 靠
            // 「surface available 后才 prepare」的时序保证这一点）。
            pendingPath = path
            pendingRequest = request
        }
        listener?.onPreparing()
    }

    private fun loadFile(path: String, request: MediaRequest) {
        // 对齐 ExoPlayer 内核的 prepare 契约：加载即起播。
        // 上一次 release() 会把 pause 属性置 true，而 mpv 的属性跨 loadfile
        // 保留——新文件会以暂停态加载完成（画面冻结、进度不走，「暂停再播放」
        // 翻转两次 pause 才真正起播），因此这里必须显式恢复非暂停，
        // 并应用起播位置与倍速。
        MPVLib.setPropertyBoolean("pause", false)
        if (request.startPositionMs > 0) {
            // mpv 的 start 选项只作用于下一次文件加载
            MPVLib.setOptionString("start", (request.startPositionMs / 1000.0).toString())
        }
        MPVLib.setPropertyDouble("speed", request.speed.toDouble())
        schedulePrepareTimeout()
        MPVLib.command(arrayOf("loadfile", path))
        if (request.looping) setLooping(true)
    }

    override fun start() {
        MPVLib.setPropertyBoolean("pause", false)
    }

    override fun pause() {
        MPVLib.setPropertyBoolean("pause", true)
    }

    override fun seekTo(positionMs: Long) {
        Log.i(TAG, "seekTo: ${positionMs / 1000.0}")
        MPVLib.command(arrayOf("seek", (positionMs / 1000.0).toString(), "absolute", "exact"))
    }

    override fun setSpeed(speed: Float) {
        MPVLib.setPropertyDouble("speed", speed.toDouble())
    }

    override fun setLooping(looping: Boolean) {
        MPVLib.setPropertyString("loop-file", if (looping) "inf" else "no")
    }

    override fun setSurface(surface: Surface?) {
        currentSurface = surface
        if (surface != null) {
            MPVLib.attachSurface(surface)
            // surface 迟到：补上被延迟的 loadfile
            val path = pendingPath
            val request = pendingRequest
            if (path != null && request != null) {
                pendingPath = null
                pendingRequest = null
                loadFile(path, request)
            }
        } else {
            MPVLib.detachSurface()
        }
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        Log.d(TAG, "onSurfaceSizeChanged ${width}x$height")
        MPVLib.setPropertyString("android-surface-size", "${width}x$height")
    }

    override val isPlaying: Boolean
        // 属性读取失败(null)时保守判定为「未播放」：
        // setSpeedIndex 等调用方依赖 !isPlaying 补暂停，误报 true 会导致
        // 暂停中的视频切倍速后意外开播；误报 false 仅多一次无害的 pause()。
        get() = MPVLib.getPropertyBoolean("pause") == false

    override val currentPositionMs: Long
        get() = ((MPVLib.getPropertyDouble("time-pos") ?: 0.0).toLong()) * 1000

    override val durationMs: Long
        get() = ((MPVLib.getPropertyDouble("duration") ?: 0.0).toLong()) * 1000

    private fun clearSuperResolution() {
        MPVLib.command(arrayOf("change-list", "glsl-shaders", "clr", ""))
    }

    override fun setSuperResolution(index: Int) {
        if (index != 0) {
            val cmd = arrayOf(
                "change-list", "glsl-shaders", "set",
                AnimeShaders.getShader(context, index),
            )
            MPVLib.command(cmd)
        } else {
            clearSuperResolution()
        }
    }

    override fun release() {
        stopStallWatchdog()
        cancelPrepareTimeout()
        fileActive = false
        pendingLoad = false
        clearSuperResolution()
        MPVLib.setPropertyBoolean("pause", true)
        MPVLib.command(arrayOf("loadfile", "", "replace"))
        MPVLib.setPropertyString("vo", "null")
        MPVLib.setOptionString("force-window", "no")
        MPVLib.detachSurface()
        MPVLib.removeObserver(mpvEventObserver)
        mainHandler.postDelayed({ releaseCurrentPfd("mpv-release") }, 200)
        currentSurface = null
        pendingSurface = null
        pendingPath = null
        pendingRequest = null
        listener = null
    }

    private val mpvEventObserver = object : MPVLib.EventObserver {

        override fun eventProperty(property: String) = Unit

        override fun eventProperty(property: String, value: Long) {
            // video-params/w|h（INT64）：两值都就绪且变化时上报尺寸，
            // 供播放器高度自适应与 isPortraitVideo（全屏方向锁定）使用
            when (property) {
                "video-params/w" -> reportedVideoWidth = value.toInt()
                "video-params/h" -> reportedVideoHeight = value.toInt()
            }
            val w = reportedVideoWidth
            val h = reportedVideoHeight
            if (w > 0 && h > 0 && (w != lastReportedSizeW || h != lastReportedSizeH)) {
                lastReportedSizeW = w
                lastReportedSizeH = h
                mainHandler.post { listener?.onVideoSizeChanged(w, h) }
            }
        }

        override fun eventProperty(property: String, value: Boolean) {
            when (property) {
                // 用户暂停（区别于缓存暂停）：看门狗据此区分「暂停不动」与「卡顿不动」
                "pause" -> pausedByUser = value

                // 缓存耗尽暂停/恢复 → 对齐 ExoPlayer 的 BUFFERING/PLAYING 上报
                // （cache-pause=no 时不触发，卡顿由看门狗兜底）
                "paused-for-cache" -> mainHandler.post {
                    if (value) listener?.onBuffering() else listener?.onPlaying()
                }
            }
        }

        override fun eventProperty(property: String, value: String) = Unit

        // tag 版 MPVLib 接口新增的重载（本应用不观察 MPVNode 属性）
        override fun eventProperty(property: String, value: `is`.xyz.mpv.MPVNode) = Unit

        override fun eventProperty(property: String, value: Double) {
            when (property) {
                "time-pos" -> {
                    mpvTimePos = value
                    // 看门狗心跳：播放位置仍在前进
                    lastTimePosAtMs = android.os.SystemClock.elapsedRealtime()
                }
                "demuxer-cache-duration" -> mpvCacheDuration = value
                "duration" -> mpvDuration = value
            }
            if (mpvDuration > 0) {
                val bufferedTime = (mpvTimePos + mpvCacheDuration).coerceAtMost(mpvDuration)
                val bufferPercent = ((bufferedTime / mpvDuration) * 100).toInt()
                mainHandler.post { listener?.onBufferedUpdate(bufferPercent) }
            }
        }

        override fun event(eventId: Int) {
            mainHandler.post {
                when (eventId) {
                    MPVLib.mpvEventId.MPV_EVENT_START_FILE -> {
                        // 文件开始加载
                        mpvTimePos = 0.0
                        mpvCacheDuration = 0.0
                        mpvDuration = 0.0
                        pausedByUser = false
                        pendingLoad = true
                        fileActive = false
                        stopStallWatchdog()
                        listener?.onPreparing()
                    }

                    MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
                        // 文件加载成功（起播位置与倍速已在 loadFile 应用）
                        pendingLoad = false
                        fileActive = true
                        cancelPrepareTimeout()
                        startStallWatchdog()
                        listener?.onPrepared()
                    }

                    MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> {
                        // 播放（重新）开始
                        listener?.onPlaying()
                    }

                    MPVLib.mpvEventId.MPV_EVENT_END_FILE -> {
                        stopStallWatchdog()
                        cancelPrepareTimeout()
                        when {
                            // 本次加载未走完 FILE_LOADED：加载失败（网络错误等）
                            pendingLoad -> {
                                pendingLoad = false
                                releaseCurrentPfd("MPV_EVENT_END_FILE(error)")
                                listener?.onError(null)
                            }

                            fileActive -> {
                                fileActive = false
                                // 位置启发式区分「自然播完」与「中途断流」：
                                // mpv 的 END_FILE 不携带原因（观察者只给事件 ID），
                                // 断网/读流失败也会走这里——距结尾还很远即视为错误。
                                val reachedEnd = mpvDuration <= 0.0 ||
                                        mpvTimePos >= mpvDuration - END_OF_FILE_TOLERANCE_SEC
                                if (reachedEnd) {
                                    releaseCurrentPfd("MPV_EVENT_END_FILE")
                                    listener?.onCompleted()
                                } else {
                                    Log.e(
                                        TAG,
                                        "stream ended at ${mpvTimePos}s / ${mpvDuration}s, treat as error",
                                    )
                                    releaseCurrentPfd("MPV_EVENT_END_FILE(interrupted)")
                                    listener?.onError(null)
                                }
                            }

                            // 两者皆否：release() 里 loadfile "" 的伪事件
                            // （异步送达可能晚于下一个内核的 addObserver），忽略
                            else -> Log.i(TAG, "ignore stale END_FILE (no active load)")
                        }
                    }

                    MPVLib.mpvEventId.MPV_EVENT_SHUTDOWN -> {
                        Log.e(TAG, "event: $eventId")
                    }
                }
            }
        }
    }
}
