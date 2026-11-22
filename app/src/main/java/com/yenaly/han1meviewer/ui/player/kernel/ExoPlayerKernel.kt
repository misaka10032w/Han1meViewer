package com.yenaly.han1meviewer.ui.player.kernel

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 基于 Media3 ExoPlayer 的播放内核。
 *
 * 自旧版 `ExoMediaKernel`（JZVD `JZMediaInterface` 实现）迁移而来，保留其关键行为：
 * - 播放器实例构建与释放均在专用后台线程执行，避免阻塞主线程；
 * - 起播缓冲按设备 Java 堆分档限制（低内存设备防 OOM）；
 * - HLS（`.m3u8`）与普通媒体流自动选择对应 MediaSource；
 * - seek 超出已缓冲位置时回调 [KernelListener.onBuffering] 显示 loading。
 *
 * ### 线程模型
 * 所有公开方法在主线程调用；命令通过 [mediaHandler] 转发到播放器线程执行，
 * `isPlaying` / 位置等同步查询通过 [runOnPlayerThread] 以 300ms 超时等待结果。
 *
 * @param context 应用级 Context
 */
@OptIn(UnstableApi::class)
class ExoPlayerKernel(
    private val context: Context,
) : PlayerKernel {

    companion object {
        private const val TAG = "ExoPlayerKernel"

        private const val MIB = 1024 * 1024

        /** 起播只需要 1 秒缓冲，卡顿后重新起播需要 2 秒。 */
        private const val BUFFER_FOR_PLAYBACK_MS = 1_000
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_000

        /**
         * 按普通 Java 堆大小选择播放缓冲配置。
         *
         * Media3 默认会按 125 MB 视频 + 12.5 MB 音频准备缓冲，在 256 MB 堆的设备上，
         * 加上图片缓存后很容易直接 OOM。这里根据 `ActivityManager.memoryClass`
         * 分档限制缓冲，既照顾低内存设备，也尽量不牺牲大堆设备的播放体验。
         */
        private fun createLoadControl(context: Context): LoadControl {
            val memoryClassMb = context.getSystemService(ActivityManager::class.java)
                ?.memoryClass
                ?.takeIf { it > 0 }
                ?: 256

            // targetBufferBytes 是视频、音频、字幕等所有轨道的合计上限。
            val (targetBufferBytes, minBufferMs, maxBufferMs) = when {
                memoryClassMb <= 256 -> Triple(48 * MIB, 10_000, 20_000)
                memoryClassMb <= 384 -> Triple(64 * MIB, 15_000, 30_000)
                else -> Triple(96 * MIB, 20_000, 40_000)
            }
            Log.i(
                TAG,
                "memoryClass=${memoryClassMb}MB, " +
                        "targetBuffer=${targetBufferBytes / MIB}MB, " +
                        "bufferRange=${minBufferMs / 1_000}s-${maxBufferMs / 1_000}s",
            )

            return DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    minBufferMs,
                    maxBufferMs,
                    BUFFER_FOR_PLAYBACK_MS,
                    BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                )
                .setTargetBufferBytes(targetBufferBytes)
                // 优先按内存上限停止加载，避免低内存设备被缓冲撑爆。
                .setPrioritizeTimeOverSizeThresholds(false)
                .build()
        }
    }

    override val type = KernelType.ExoPlayer

    override var listener: KernelListener? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaHandler: Handler? = null
    private var mediaHandlerThread: HandlerThread? = null

    /** [prepare] 之前 setSurface 缓存的 surface，就绪后挂载。 */
    @Volatile
    private var pendingSurface: Surface? = null

    /** surface 未就绪时被延迟的播放请求（对齐 MPV 内核的 pendingPath 模式）。 */
    @Volatile
    private var pendingPrepare: MediaRequest? = null

    @Volatile
    private var _exoPlayer: ExoPlayer? = null

    /** release 后置位，防止已 post 未构建的播放器泄漏。 */
    @Volatile
    private var released = false

    private var prevSeek = 0L

    @Volatile
    override var videoWidth: Int = 0
        private set

    @Volatile
    override var videoHeight: Int = 0
        private set

    private val exoListener = object : Player.Listener {

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            val realWidth = videoSize.width * videoSize.pixelWidthHeightRatio
            val realHeight = videoSize.height
            videoWidth = realWidth.toInt()
            videoHeight = realHeight
            Log.i(TAG, "onVideoSizeChanged: realWidth=$realWidth, realHeight=$realHeight")
            mainHandler.post { listener?.onVideoSizeChanged(realWidth.toInt(), realHeight) }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            pushBufferedPercent()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.i(TAG, "onPlaybackStateChanged: $playbackState")
            pushBufferedPercent()
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    mainHandler.post { listener?.onBuffering() }
                }

                Player.STATE_READY -> {
                    Log.i(TAG, "STATE_READY reached")
                    runOnPlayerThread {
                        val pwr = _exoPlayer?.playWhenReady
                        Log.i(TAG, "STATE_READY playWhenReady=$pwr")
                        if (pwr == true) {
                            mainHandler.post {
                                Log.i(TAG, "notifying onPrepared+onPlaying")
                                listener?.onPrepared()
                                listener?.onPlaying()
                            }
                        } else {
                            // 暂停态就绪：补 onPaused，把暂停中远端 seek 触发的
                            // BUFFERING 恢复为 PAUSED（控制器带状态守卫，幂等）
                            mainHandler.post {
                                listener?.onPrepared()
                                listener?.onPaused()
                            }
                        }
                    }
                }

                Player.STATE_ENDED -> {
                    mainHandler.post { listener?.onCompleted() }
                }

                // STATE_IDLE：出错/停止后的空闲态，错误由 onPlayerError 上报，
                // 这里无需向控制器转发任何状态
                Player.STATE_IDLE -> Unit
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            Log.i(
                TAG,
                "onPlayWhenReadyChanged: $playWhenReady, reason=$reason, " +
                        "playbackState=${_exoPlayer?.playbackState}"
            )
            if (_exoPlayer?.playbackState == Player.STATE_READY) {
                if (playWhenReady) {
                    mainHandler.post { listener?.onPlaying() }
                } else {
                    mainHandler.post { listener?.onPaused() }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "onPlayerError: $error")
            mainHandler.post { listener?.onError(error) }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                runOnPlayerThread {
                    val playing = _exoPlayer?.playWhenReady == true
                    mainHandler.post {
                        listener?.onSeekComplete()
                        if (playing) {
                            listener?.onPlaying()
                        } else {
                            // 暂停中 seek：把可能的 BUFFERING 拉回暂停态，
                            // 否则 UI 会一直转圈
                            listener?.onPaused()
                        }
                    }
                }
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) = Unit
    }

    private fun pushBufferedPercent() {
        val per = _exoPlayer?.bufferedPercentage ?: return
        mainHandler.post { listener?.onBufferedUpdate(per) }
    }

    override fun prepare(request: MediaRequest) {
        // release() 会清空 pendingSurface 与 listener，而控制器在 prepare 之前已把
        // 当前 surface 经 setSurface 缓存进来、监听也在 init 时挂好（首次起播与
        // 换清晰度都是如此），这里必须保住两者：surface 丢了会「有声无画」，
        // listener 丢了会收不到 READY 回调（UI 永久 loading、进度不动）。
        val surfaceToKeep = pendingSurface
        val listenerToKeep = listener
        if (surfaceToKeep == null) {
            // surface 未就绪：延迟到 setSurface 后再构建，避免无 surface 的
            // 播放器以纯音频起播（对齐 MPV 的 pendingPath 模式）
            pendingPrepare = request
            released = false
            return
        }
        pendingPrepare = null
        release()
        pendingSurface = surfaceToKeep
        listener = listenerToKeep
        released = false
        Log.i(TAG, "prepare: ${request.url}")
        val thread = HandlerThread("ExoPlayerKernel").also { it.start() }
        val handler = Handler(thread.looper)
        mediaHandlerThread = thread
        mediaHandler = handler
        handler.post {
            if (released) {
                thread.quitSafely()
                return@post
            }
            val player = buildPlayer(request)
            if (released) {
                // 构建期间页面已释放：自行释放，避免孤儿播放器（纯音频、无人可停）
                runCatching { player.release() }
                thread.quitSafely()
                return@post
            }
            _exoPlayer = player
            pendingSurface?.let { player.setVideoSurface(it) }
            pendingSurface = null
        }
    }

    private fun buildPlayer(request: MediaRequest): ExoPlayer {
        val trackSelector = DefaultTrackSelector(
            context,
            AdaptiveTrackSelection.Factory(),
        )
        val renderersFactory = DefaultRenderersFactory(context)
        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(createLoadControl(context))
            .setBandwidthMeter(DefaultBandwidthMeter.Builder(context).build())
            .build()
        player.addListener(exoListener)

        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(request.headers),
        )
        val url = request.url
        Log.i(TAG, "URL Link = $url")
        val mediaSource = if (url.contains(".m3u8")) {
            HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
        }

        player.repeatMode =
            if (request.looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setMediaSource(mediaSource)
        if (request.startPositionMs > 0) {
            player.seekTo(request.startPositionMs)
        }
        if (request.speed != 1.0f) {
            player.playbackParameters = PlaybackParameters(request.speed, 1.0f)
        }
        player.prepare()
        // startPaused（预加载）：就绪后保持暂停，只预缓冲并回报时长
        player.playWhenReady = !request.startPaused
        Log.i(TAG, "buildPlayer done, pendingSurface=$pendingSurface")
        mainHandler.post { listener?.onPreparing() }
        return player
    }

    override fun start() {
        mediaHandler?.post {
            _exoPlayer?.playWhenReady = true
        }
    }

    override fun pause() {
        mediaHandler?.post {
            _exoPlayer?.playWhenReady = false
        }
    }

    override fun seekTo(positionMs: Long) {
        mediaHandler?.post {
            val player = _exoPlayer ?: return@post
            if (positionMs != prevSeek) {
                if (positionMs >= player.bufferedPosition) {
                    // seek 目标超出已缓冲范围，显示 loading
                    mainHandler.post { listener?.onBuffering() }
                }
                // 不强制 playWhenReady：暂停中 seek 保持暂停（对齐 MPV 内核）。
                // 暂停中的远端 seek 会先进入 BUFFERING，seek 完成后由
                // onPositionDiscontinuity 的 onPaused 恢复暂停态。
                player.seekTo(positionMs)
                prevSeek = positionMs
            }
        }
    }

    override fun setSpeed(speed: Float) {
        mediaHandler?.post {
            _exoPlayer?.playbackParameters = PlaybackParameters(speed, 1.0f)
        }
    }

    override fun setLooping(looping: Boolean) {
        mediaHandler?.post {
            _exoPlayer?.repeatMode =
                if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    override fun setSurface(surface: Surface?) {
        Log.i(TAG, "setSurface: $surface")
        pendingSurface = surface
        if (surface != null) {
            val deferred = pendingPrepare
            if (deferred != null) {
                // surface 迟到：补上被延迟的 prepare（pendingPrepare 非空即未 release）
                pendingPrepare = null
                prepare(deferred)
                return
            }
        }
        mediaHandler?.post {
            val player = _exoPlayer ?: return@post
            pendingSurface = null
            player.setVideoSurface(surface)
        }
    }

    override fun release() {
        released = true
        pendingSurface = null
        pendingPrepare = null
        listener = null
        val handler = mediaHandler
        val thread = mediaHandlerThread
        val player = _exoPlayer
        mediaHandler = null
        mediaHandlerThread = null
        _exoPlayer = null
        videoWidth = 0
        videoHeight = 0
        if (handler == null || thread == null) {
            player?.release()
            return
        }
        // release 不能放到主线程里，界面会卡顿
        handler.post {
            try {
                // 执行时兜底读字段：构建块可能刚写入新实例（捕获时为 null）
                player?.release() ?: _exoPlayer?.release()
            } catch (_: Exception) {
            }
            // media3 的 release 会向本 looper 投递收尾消息，延迟退出避免死线程警告
            handler.postDelayed({
                handler.removeCallbacksAndMessages(null)
                thread.quitSafely()
            }, 500)
        }
    }

    /**
     * 在播放器线程同步执行 [block]，超时（300ms）返回 null。
     *
     * 供 `isPlaying` / 位置 / 时长等同步查询使用。
     */
    private inline fun <T> runOnPlayerThread(crossinline block: () -> T?): T? {
        val handler = mediaHandler ?: return null
        if (Looper.myLooper() == handler.looper) {
            return block()
        }

        val result = AtomicReference<T?>()
        val latch = CountDownLatch(1)
        handler.post {
            try {
                result.set(block())
            } finally {
                latch.countDown()
            }
        }
        val finished = latch.await(300, TimeUnit.MILLISECONDS)
        return if (finished) result.get() else null
    }

    override val isPlaying: Boolean
        get() = runOnPlayerThread { _exoPlayer?.playWhenReady } == true

    override val currentPositionMs: Long
        get() {
            val pos = runOnPlayerThread { _exoPlayer?.currentPosition }
            if (pos == null) Log.w(TAG, "currentPositionMs: runOnPlayerThread timed out/null")
            return pos ?: 0L
        }

    override val durationMs: Long
        get() {
            val dur = runOnPlayerThread { _exoPlayer?.duration }
            if (dur == null) Log.w(TAG, "durationMs: runOnPlayerThread timed out/null")
            return dur ?: 0L
        }
}
