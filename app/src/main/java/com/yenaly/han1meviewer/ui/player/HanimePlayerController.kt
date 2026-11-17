package com.yenaly.han1meviewer.ui.player

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.Surface
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.ResolutionLinkMap
import com.yenaly.han1meviewer.ui.player.HanimePlayerController.Companion.RESUME_THRESHOLD_MS
import com.yenaly.han1meviewer.ui.player.kernel.KernelListener
import com.yenaly.han1meviewer.ui.player.kernel.KernelType
import com.yenaly.han1meviewer.ui.player.kernel.MediaRequest
import com.yenaly.han1meviewer.ui.player.kernel.PlayerKernel
import com.yenaly.han1meviewer.ui.player.kernel.PlayerKernelFactory
import com.yenaly.han1meviewer.util.HAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * 播放器状态中枢。
 *
 * 取代旧版 JZVD（`Jzvd` / `HJzvdStd`）的静态状态机与 UI View 混合职责：
 * - 持有一个 [PlayerKernel]，把它的事件翻译成 [PlayerUiState]（低频）与 [PlayerProgress]（约 100ms 高频）两条流；
 * - 对 UI 暴露命令方法（播放/暂停/seek/倍速/循环/清晰度切换/超分/手势锁…）；
 * - 收编音频焦点、流量确认、观看进度恢复、长按倍速等原散落在 `HJzvdStd` 的逻辑。
 *
 * 一个播放页面持有一个实例；随 [release] 释放全部资源。
 * 页面层（全屏/小窗/PiP）只消费状态流，不直接触碰内核。
 *
 * @param context 任意 Context，内部持有应用级
 * @param kernelType 播放内核类型
 */
class HanimePlayerController(
    context: Context,
    /** 内核类型（UI 判断 MPV 专属功能显隐用）。 */
    val kernelType: KernelType,
) : KernelListener {

    companion object {
        private const val TAG = "HanimePlayerController"

        /** 恢复播放按钮出现的最小历史进度（ms）。 */
        private const val RESUME_THRESHOLD_MS = 5_000L

        /** 进度刷新周期（ms），保持关键帧倒计时 0.1s 精度。 */
        private const val PROGRESS_INTERVAL_MS = 100L
    }

    private val appContext = context.applicationContext

    /** 进度 ticker 等内部协程作用域，[release] 时取消。 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** 当前内核；换源/重试时释放重建。 */
    private var kernel: PlayerKernel = PlayerKernelFactory.create(kernelType, appContext)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _progress = MutableStateFlow(PlayerProgress())
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    // ---------------- 媒体元数据 ----------------

    /** 清晰度 → 链接，顺序即 [PlayerUiState.qualities] 顺序。 */
    private var qualityLinks: List<Pair<String, String>> = emptyList()

    /** 观看历史进度（起播后据此恢复）。 */
    private var savedProgressMs = 0L

    /** 本次媒体是否已恢复过历史进度。 */
    private var hasRestoredProgress = false

    /** 会话内流量确认框是否已展示过（原 JZVD `WIFI_TIP_DIALOG_SHOWED`）。 */
    private var wifiTipShowed = false

    /** 长按倍速前的基准速度。 */
    private var speedBoostBase = PlayerDefaults.DEF_SPEED

    // ---------------- 音频焦点 ----------------

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Volatile
    private var pausedByAudioFocusLoss = false

    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                -> {
                if (_uiState.value.state == PlayerState.PLAYING) {
                    pausedByAudioFocusLoss = true
                    pause()
                }
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                if (pausedByAudioFocusLoss) {
                    pausedByAudioFocusLoss = false
                    play()
                }
            }
        }
    }

    init {
        kernel.listener = this
        // 状态机转移日志：UI 状态回归观察用（仅在状态真正变化时输出）
        scope.launch {
            var lastState: PlayerState? = null
            _uiState.collect { ui ->
                if (ui.state != lastState) {
                    Log.i(TAG, "UI state: $lastState -> ${ui.state}")
                    lastState = ui.state
                }
            }
        }
    }

    // ---------------- 生命周期 ----------------

    /**
     * 设置媒体源。
     *
     * 应用默认清晰度设置（`default_video_quality`），随后由页面调用 [startVideo] 起播。
     *
     * @param title 视频标题
     * @param videoUrls 清晰度 → 链接映射（[ResolutionLinkMap]）
     * @param savedProgressMs 观看历史进度，超过 [RESUME_THRESHOLD_MS] 且用户允许时起播后恢复
     * @param posterUrl 封面 URL
     */
    fun setMedia(
        title: String,
        videoUrls: ResolutionLinkMap,
        savedProgressMs: Long = 0L,
        posterUrl: String? = null,
    ) {
        this.savedProgressMs = savedProgressMs
        hasRestoredProgress = false
        if (Preferences.disableMobileDataWarning) wifiTipShowed = true

        val links = videoUrls.entries.map { it.key to it.value.link }
        qualityLinks = links

        val preferredQuality = Preferences.videoQuality
        val index = links.indexOfFirst { it.first == preferredQuality }
            .takeIf { it >= 0 } ?: 0

        // 会话倍速从用户偏好初始化（此前仅 MPV 内核在 FILE_LOADED 里生效，
        // 换清晰度时还会被重置回偏好值）；随后由 MediaRequest.speed 随每次
        // prepare 应用到内核，两个内核行为一致。
        val preferredSpeedIndex = PlayerDefaults.speedArray
            .indexOfFirst { it == Preferences.playerSpeed }
            .takeIf { it >= 0 } ?: PlayerDefaults.DEF_SPEED_INDEX

        _uiState.value = PlayerUiState(
            title = title,
            qualities = links.map { it.first },
            currentQualityIndex = index,
            speedIndex = preferredSpeedIndex,
            posterUrl = posterUrl,
            hasMedia = links.isNotEmpty(),
        )
        _progress.value = PlayerProgress()
        Log.i(
            TAG,
            "setMedia: qualities=${links.map { it.first }}, " +
                    "preferred=$preferredQuality, index=$index, savedProgress=$savedProgressMs"
        )
    }

    /**
     * 开始播放（等价旧版 `startVideo`）。
     *
     * 非 WiFi 且会话内未确认过流量时，先发射 [PlayerEvent.RequestWifiConfirmation]，
     * 由页面弹框确认后再调用 [confirmWifiAndPlay]。
     */
    fun startVideo() {
        if (!_uiState.value.hasMedia) return
        if (!wifiTipShowed && !isWifiConnected()) {
            _events.tryEmit(PlayerEvent.RequestWifiConfirmation)
            return
        }
        startPlayback()
    }

    /** 流量确认后继续播放。 */
    fun confirmWifiAndPlay() {
        wifiTipShowed = true
        startPlayback()
    }

    private fun startPlayback() {
        val quality = qualityLinks.getOrNull(_uiState.value.currentQualityIndex) ?: return
        Log.i(TAG, "startPlayback: ${quality.first} -> ${quality.second}")
        // 起播埋点：记录清晰度与内核类型，用于分析用户清晰度偏好与播放环境
        HAnalytics.logEvent(
            FirebaseConstants.EVENT_PLAYBACK_START,
            FirebaseConstants.PARAM_QUALITY to quality.first,
            FirebaseConstants.PARAM_KERNEL to kernelType.name,
            FirebaseConstants.PARAM_METHOD to "start",
        )
        _uiState.update { it.copy(state = PlayerState.PREPARING) }
        reprepareKernel(positionMs = 0L)
    }

    /**
     * 重试（出错后），从当前位置重新加载。
     */
    fun retry() {
        if (!_uiState.value.hasMedia) return
        // 重试埋点：重试次数可反映播放失败的真实规模
        HAnalytics.logEvent(
            FirebaseConstants.EVENT_PLAYBACK_START,
            FirebaseConstants.PARAM_QUALITY to (qualityLinks.getOrNull(_uiState.value.currentQualityIndex)?.first),
            FirebaseConstants.PARAM_KERNEL to kernelType.name,
            FirebaseConstants.PARAM_METHOD to "retry",
        )
        _uiState.update { it.copy(state = PlayerState.PREPARING) }
        reprepareKernel(positionMs = 0L)
    }

    /**
     * 切换清晰度（保持当前播放进度，等价旧版 `changeUrl`）。
     */
    fun switchQuality(index: Int) {
        if (!_uiState.value.hasMedia) return
        if (index == _uiState.value.currentQualityIndex) return
        val position = kernel.currentPositionMs
        // 切清晰度埋点
        HAnalytics.logEvent(
            FirebaseConstants.EVENT_VIDEO_QUALITY_SWITCH,
            FirebaseConstants.PARAM_FROM_QUALITY to (qualityLinks.getOrNull(_uiState.value.currentQualityIndex)?.first),
            FirebaseConstants.PARAM_QUALITY to (qualityLinks.getOrNull(index)?.first),
            FirebaseConstants.PARAM_KERNEL to kernelType.name,
        )
        _uiState.update { it.copy(currentQualityIndex = index, state = PlayerState.PREPARING) }
        Log.i(TAG, "switchQuality: ${_uiState.value.qualities.getOrNull(index)} @$position")
        reprepareKernel(positionMs = position)
    }

    /**
     * 释放并按当前状态重建内核，随后以给定位置准备媒体。
     *
     * 重建后先把当前 surface 挂回新内核（MPV 必须在 loadfile 前 attach surface，
     * 否则 vo 初始化会原生崩溃）。
     */
    private fun reprepareKernel(positionMs: Long) {
        kernel.release()
        kernel = PlayerKernelFactory.create(kernel.type, appContext)
        kernel.listener = this
        currentSurface?.let { kernel.setSurface(it) }
        val state = _uiState.value
        val quality = qualityLinks.getOrNull(state.currentQualityIndex) ?: return
        kernel.prepare(
            MediaRequest(
                url = quality.second,
                title = state.title,
                looping = state.isLooping,
                startPositionMs = positionMs,
                speed = PlayerDefaults.speedArray[state.speedIndex],
            )
        )
    }

    /**
     * 释放全部资源（内核、协程、音频焦点），页面销毁时调用，幂等。
     */
    fun release() {
        stopTicker()
        abandonAudioFocus()
        kernel.release()
        currentSurface?.release()
        currentSurface = null
        scope.cancel()
    }

    // ---------------- 播放控制 ----------------

    /** 播放；[PlayerState.COMPLETED] 时从头播放。 */
    fun play() {
        requestAudioFocus()
        if (_uiState.value.state == PlayerState.COMPLETED) {
            kernel.seekTo(0L)
        }
        kernel.start()
        _uiState.update { it.copy(state = PlayerState.PLAYING) }
    }

    /** 暂停。 */
    fun pause() {
        kernel.pause()
        _uiState.update { it.copy(state = PlayerState.PAUSED) }
    }

    /** 播放/暂停切换。 */
    fun togglePlayPause() {
        if (_uiState.value.state == PlayerState.PLAYING) pause() else play()
    }

    /** seek 到指定位置（ms）。 */
    fun seekTo(positionMs: Long) {
        kernel.seekTo(positionMs.coerceAtLeast(0L))
    }

    /** 从头开始播放（「从头开始播放」按钮）。 */
    fun restartFromBeginning() {
        hasRestoredProgress = true
        play()
        seekTo(0L)
    }

    /**
     * 设置倍速档位（下标对应 [PlayerDefaults.speedArray]）。
     *
     * 暂停状态下设置倍速后保持暂停（旧版为规避系统内核副作用，保留该行为）。
     */
    fun setSpeedIndex(index: Int) {
        val speed = PlayerDefaults.speedArray.getOrNull(index) ?: return
        _uiState.update { it.copy(speedIndex = index) }
        val playing = kernel.isPlaying
        kernel.setSpeed(speed)
        if (!playing) kernel.pause()
    }

    /**
     * 开始长按倍速（临时按 [Preferences.longPressSpeedTime] 倍加速）。
     */
    fun beginSpeedBoost() {
        val state = _uiState.value
        speedBoostBase = PlayerDefaults.speedArray[state.speedIndex]
        kernel.setSpeed(speedBoostBase * Preferences.longPressSpeedTime)
    }

    /**
     * 结束长按倍速，恢复基准速度。
     */
    fun endSpeedBoost() {
        kernel.setSpeed(speedBoostBase)
    }

    /** 设置单视频循环。 */
    fun setLooping(looping: Boolean) {
        _uiState.update { it.copy(isLooping = looping) }
        kernel.setLooping(looping)
    }

    /** 设置 Anime4K 超分档位（仅 MPV 内核生效）。 */
    fun setSuperResolution(index: Int) {
        _uiState.update { it.copy(superResolutionIndex = index) }
        kernel.setSuperResolution(index)
    }

    /** 锁定/解锁手势。 */
    fun setGestureLocked(locked: Boolean) {
        _uiState.update { it.copy(isGestureLocked = locked) }
    }

    // ---------------- Surface ----------------

    /** 当前持有的 Surface 包装，跨布局移动时复用。 */
    private var currentSurface: Surface? = null

    /**
     * 视频输出 SurfaceTexture 就绪（或复用旧纹理）时挂载到内核。
     */
    fun attachSurfaceTexture(surfaceTexture: SurfaceTexture) {
        currentSurface?.release()
        currentSurface = Surface(surfaceTexture)
        kernel.setSurface(currentSurface)
    }

    /**
     * Surface 尺寸变化（MPV 内核需要同步 `android-surface-size`）。
     */
    fun onSurfaceSizeChanged(width: Int, height: Int) {
        kernel.onSurfaceSizeChanged(width, height)
    }

    // ---------------- 内核事件 ----------------

    override fun onPreparing() = Unit

    override fun onPrepared() = Unit

    override fun onBuffering() {
        _uiState.update { state ->
            if (state.state in BUFFERING_STATES) state.copy(state = PlayerState.BUFFERING) else state
        }
    }

    override fun onPlaying() {
        maybeRestoreWatchProgress()
        _uiState.update { it.copy(state = PlayerState.PLAYING) }
        startTicker()
    }

    override fun onPaused() {
        _uiState.update { state ->
            if (state.state == PlayerState.PLAYING || state.state == PlayerState.BUFFERING) {
                state.copy(state = PlayerState.PAUSED)
            } else {
                state
            }
        }
    }

    override fun onCompleted() {
        stopTicker()
        abandonAudioFocus()
        _uiState.update { it.copy(state = PlayerState.COMPLETED) }
    }

    override fun onSeekComplete() = Unit

    override fun onBufferedUpdate(percent: Int) {
        _progress.update { it.copy(bufferedPercent = percent) }
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
        _uiState.update { it.copy(videoWidth = width, videoHeight = height) }
    }

    override fun onError(error: Throwable?) {
        stopTicker()
        abandonAudioFocus()
        Log.e(TAG, "onError: $error")
        // 播放器错误埋点：事件 + Crashlytics non-fatal（含内核与清晰度上下文）
        HAnalytics.logError(
            eventName = FirebaseConstants.EVENT_PLAYER_ERROR,
            source = "player",
            throwable = error,
            FirebaseConstants.PARAM_KERNEL to kernelType.name,
            FirebaseConstants.PARAM_QUALITY to (qualityLinks.getOrNull(_uiState.value.currentQualityIndex)?.first),
        )
        _uiState.update { it.copy(state = PlayerState.ERROR) }
    }

    /**
     * 首次播放时恢复观看历史进度，并提示「从头开始播放」。
     */
    private fun maybeRestoreWatchProgress() {
        if (hasRestoredProgress || !_uiState.value.hasMedia) return
        if (savedProgressMs <= RESUME_THRESHOLD_MS) return
        if (!Preferences.allowResumePlayback) return
        hasRestoredProgress = true
        kernel.seekTo(savedProgressMs)
        _events.tryEmit(PlayerEvent.ShowResumeButton(savedProgressMs))
    }

    // ---------------- 进度 ticker ----------------

    private var tickerJob: Job? = null

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        Log.i(TAG, "startTicker")
        tickerJob = scope.launch {
            while (isActive) {
                _progress.value = PlayerProgress(
                    positionMs = kernel.currentPositionMs,
                    durationMs = kernel.durationMs,
                    bufferedPercent = _progress.value.bufferedPercent,
                )
                delay(PROGRESS_INTERVAL_MS.milliseconds)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    // ---------------- 音频焦点 ----------------

    private fun requestAudioFocus() {
        if (audioFocusRequest != null) return
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
            .also { audioManager.requestAudioFocus(it) }
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
        pausedByAudioFocusLoss = false
    }

    // ---------------- 便捷查询 ----------------

    /** 当前是否真正在播放（PiP 远程按钮图标等使用）。 */
    val isPlaying: Boolean
        get() = kernel.isPlaying

    /** 是否处于可进 PiP 的状态（仅播放中；暂停/停止等非播放状态不进 PiP，issue #684）。 */
    val isActiveForPip: Boolean
        get() = _uiState.value.state == PlayerState.PLAYING

    /** 当前播放位置（ms），页面保存观看历史时直接读取。 */
    val currentPositionMs: Long
        get() = kernel.currentPositionMs

    /** 总时长（ms）。 */
    val durationMs: Long
        get() = kernel.durationMs

    /** 视频是否为竖屏/方形视频（决定全屏方向锁定：此类视频全屏保持竖屏，提高空间利用率）。 */
    val isPortraitVideo: Boolean
        get() = _uiState.value.videoWidth > 0 &&
                _uiState.value.videoHeight > 0 &&
                _uiState.value.videoWidth <= _uiState.value.videoHeight

    private fun isWifiConnected(): Boolean {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}

/** 允许转入缓冲态的来源状态。 */
private val BUFFERING_STATES = setOf(
    PlayerState.PREPARING,
    PlayerState.BUFFERING,
    PlayerState.PLAYING,
    PlayerState.PAUSED,
)
