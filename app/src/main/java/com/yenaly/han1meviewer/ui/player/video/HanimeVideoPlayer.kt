package com.yenaly.han1meviewer.ui.player.video

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.ui.component.GlobalDialogs
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.animation.WalkingCatAnimation
import com.yenaly.han1meviewer.ui.player.HanimePlayerController
import com.yenaly.han1meviewer.ui.player.PlayerDefaults
import com.yenaly.han1meviewer.ui.player.PlayerEvent
import com.yenaly.han1meviewer.ui.player.PlayerState
import com.yenaly.han1meviewer.ui.player.PlayerUtils
import com.yenaly.han1meviewer.ui.player.kernel.KernelType
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

/**
 * keepScreenOn 的最后写入者：多个播放页共享 Activity 根 View，转场期间同时
 * 组合，只有当前持有者才能把标记清零。
 */
private val KeepScreenOnOwner = AtomicReference<Any?>(null)

/**
 * Compose 视频播放器主组件。
 *
 * 取代旧版 `HJzvdStd`（JzvdStd）：渲染 Surface + 手势层 + 控件层 + 弹层全部分层组合。
 * 页面负责把 [playerSurface]（可移动 Surface）与本组件一起放置到小窗或全屏层。
 *
 * ### 分层（自底向上）
 * 1. 视频渲染层（[playerSurface]，movableContent）
 * 2. 海报层（准备中显示封面）
 * 3. 手势层（[Modifier.playerGestures]）
 * 4. 控件层（顶栏/底栏/中键/锁/倒计时/迷你进度条）
 * 5. 浮层（手势指示、从头开始、重试）
 * 6. 侧边弹窗（倍速/清晰度/超分/关键帧）
 *
 * @param controller 播放器控制器（唯一状态来源）
 * @param playerSurface 可移动的视频 Surface（[com.yenaly.han1meviewer.ui.player.surface.rememberMovablePlayerSurface] 的返回值）
 * @param isFullscreen 是否全屏（顶栏内容、锁按钮、关键帧倒计时随其切换）
 * @param keyframeEntity 关键 H 帧实体（倒计时与列表弹窗），无则 null
 * @param onBack 顶栏返回
 * @param onGoHome 回到首页（普通窗口）
 * @param onToggleFullscreen 切换全屏
 * @param videoInfoLoading 页面视频信息仍在加载（此时无可播链接，中键显示
 *   loading 而非播放键，避免点击触发「跳转浏览器」兜底）
 * @param onNoVideoUrl 无可播放链接时点击中键
 * @param onAddHKeyframeAt 长按🥵在当前时刻添加关键帧（页面校验暂停状态）
 * @param onEditKeyframe 编辑关键帧
 * @param onDeleteKeyframe 删除关键帧
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HanimeVideoPlayer(
    controller: HanimePlayerController,
    playerSurface: @Composable (Modifier) -> Unit,
    isFullscreen: Boolean,
    keyframeEntity: HKeyframeEntity?,
    modifier: Modifier = Modifier,
    videoInfoLoading: Boolean = false,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onNoVideoUrl: () -> Unit,
    onAddHKeyframeAt: (positionMs: Long) -> Unit,
    onEditKeyframe: (HKeyframeEntity.Keyframe) -> Unit,
    onDeleteKeyframe: (HKeyframeEntity.Keyframe) -> Unit,
) {
    val view = LocalView.current
    val uiState by controller.uiState.collectAsStateWithLifecycle()
    val progress by controller.progress.collectAsStateWithLifecycle()

    val showHKeyframe = remember { Preferences.hKeyframesEnable }
    val sensitivity = remember { PlayerDefaults.toRealSensitivity(Preferences.slideSensitivity) }
    val remindAheadMs = remember { Preferences.whenCountdownRemind }
    val showCommentCountdown = remember { Preferences.showCommentWhenCountdown }
    val showBottomProgress = remember { Preferences.showBottomProgress }
    val superResLabels = remember {
        listOf(
            R.string.super_resolution_off,
            R.string.super_resolution_performance,
            R.string.super_resolution_quality,
        )
    }

    // ---------------- 本地 UI 状态 ----------------

    /** 控件是否显示。 */
    var showControls by remember { mutableStateOf(true) }

    /** 手势锁定时锁按钮单独的显隐（锁定后单击只切换锁可见性）。 */
    var lockVisible by remember { mutableStateOf(true) }

    /** 当前打开的侧边弹窗（仅关键帧列表使用右侧滑入面板）。 */
    var currentSheet by remember { mutableStateOf<PlayerSheetType?>(null) }

    /** 当前打开的气泡弹窗（倍速/清晰度/超分）。 */
    var bubble by remember { mutableStateOf<PlayerSheetType?>(null) }

    /** 气泡锚点（相对播放器左上，px）。 */
    var bubbleAnchor by remember {
        mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
    }

    /** 气泡方向：底栏按钮在锚点上方弹出。 */
    var bubblePreferAbove by remember { mutableStateOf(false) }

    /** 播放器根布局坐标（气泡锚点换算与宽度钳制用）。 */
    var playerRootCoords by remember {
        mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null)
    }

    /** 手势指示浮层数据。 */
    var gestureIndicator by remember { mutableStateOf<GestureIndicatorData?>(null) }

    /** 「从头开始播放」按钮可见。 */
    var resumeVisible by remember { mutableStateOf(false) }

    /** 长按倍速激活中（禁用拖动手势）。 */
    var speedBoostActive by remember { mutableStateOf(false) }

    /** 进度条拖动中（冻结控件自动隐藏）。 */
    var sliderDragging by remember { mutableStateOf(false) }

    /** 进度条拖动中的最新值（0..1），松手时 seek。 */
    var sliderDragValue by remember { mutableFloatStateOf(0f) }

    // 播放中（含准备/缓冲）保持屏幕常亮；view 为 Activity 共享根 View，
    // 用"最后写入者"所有权避免旧页 onDispose 覆盖新页的常亮标记
    DisposableEffect(controller, uiState.state) {
        val needKeepOn = uiState.state == PlayerState.PLAYING ||
                uiState.state == PlayerState.BUFFERING ||
                uiState.state == PlayerState.PREPARING
        if (needKeepOn) {
            KeepScreenOnOwner.set(controller)
            view.keepScreenOn = true
        } else if (KeepScreenOnOwner.get() === controller) {
            KeepScreenOnOwner.set(null)
            view.keepScreenOn = false
        }
        onDispose {
            if (KeepScreenOnOwner.compareAndSet(controller, null)) {
                view.keepScreenOn = false
            }
        }
    }

    // 控件自动隐藏：播放中且无弹窗/气泡/拖动时 3 秒后隐藏
    LaunchedEffect(
        showControls,
        uiState.state,
        currentSheet,
        bubble,
        sliderDragging,
        uiState.isGestureLocked,
    ) {
        if (showControls && uiState.state == PlayerState.PLAYING &&
            currentSheet == null && bubble == null && !sliderDragging && !uiState.isGestureLocked
        ) {
            delay(3_000.milliseconds)
            showControls = false
        }
    }

    // 控制器事件：流量确认 / 从头开始提示
    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                PlayerEvent.RequestWifiConfirmation -> {
                    GlobalDialogs.show(
                        GlobalDialogs.ConfirmRequest(
                            title = view.context.getString(R.string.warning),
                            message = view.context.getString(R.string.not_wifi_tips),
                            confirmText = view.context.getString(R.string.not_wifi_confirm),
                            dismissText = view.context.getString(R.string.cancel),
                            onConfirm = { controller.confirmWifiAndPlay() },
                        )
                    )
                }

                is PlayerEvent.ShowResumeButton -> {
                    resumeVisible = true
                }
            }
        }
    }

    // 「从头开始」5 秒后自动隐藏
    LaunchedEffect(resumeVisible) {
        if (resumeVisible) {
            delay(5_000.milliseconds)
            resumeVisible = false
        }
    }

    val controlsVisible = showControls && !uiState.isGestureLocked
    // 总时长：起播后走 ticker（progress），起播前回退到预加载回填的 uiState.durationMs
    val duration = progress.durationMs.takeIf { it > 0 } ?: uiState.durationMs
    val sliderValue = if (duration > 0) {
        progress.positionMs.toFloat() / duration
    } else {
        0f
    }
    val speedLabel = if (uiState.speedIndex == PlayerDefaults.DEF_SPEED_INDEX) {
        stringResource(R.string.speed)
    } else {
        PlayerDefaults.speedStringArray[uiState.speedIndex]
    }
    val superResLabel = if (uiState.superResolutionIndex == PlayerDefaults.DEF_SUPER_RESOLUTION_INDEX) {
        stringResource(R.string.anime_4k)
    } else {
        stringResource(superResLabels[uiState.superResolutionIndex])
    }

    /**
     * 把按钮锚点坐标换算为相对播放器左上的矩形。
     */
    fun anchorRectOf(
        coords: androidx.compose.ui.layout.LayoutCoordinates,
    ): androidx.compose.ui.geometry.Rect {
        val root = playerRootCoords ?: return androidx.compose.ui.geometry.Rect.Zero
        val chip = coords.boundsInRoot()
        val rootOrigin = root.boundsInRoot().topLeft
        return androidx.compose.ui.geometry.Rect(
            chip.left - rootOrigin.x,
            chip.top - rootOrigin.y,
            chip.right - rootOrigin.x,
            chip.bottom - rootOrigin.y,
        )
    }

    /** 打开气泡（控件保持显示，自动隐藏计时暂停）。 */
    fun openBubble(
        type: PlayerSheetType,
        coords: androidx.compose.ui.layout.LayoutCoordinates,
        preferAbove: Boolean,
    ) {
        bubble = type
        bubblePreferAbove = preferAbove
        bubbleAnchor = anchorRectOf(coords)
        showControls = true
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            // 控件滑入滑出等动画裁剪在播放器边界内，不越出到页面其它区域
            .clipToBounds()
            .onGloballyPositioned { playerRootCoords = it }
            .playerGestures(
                enabled = true,
                dragEnabled = !uiState.isGestureLocked && !speedBoostActive,
                seekEnabled = uiState.state != PlayerState.ERROR,
                sensitivity = sensitivity,
                durationMsProvider = { duration },
                positionMsProvider = { progress.positionMs },
                onTap = {
                    if (uiState.isGestureLocked) {
                        lockVisible = !lockVisible
                    } else {
                        showControls = !showControls
                    }
                },
                onDoubleTap = {
                    if (uiState.state == PlayerState.PLAYING ||
                        uiState.state == PlayerState.PAUSED
                    ) {
                        controller.togglePlayPause()
                    }
                },
                onLongPressStart = {
                    if (controller.isPlaying) {
                        speedBoostActive = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        controller.beginSpeedBoost()
                    }
                },
                onLongPressEnd = {
                    if (speedBoostActive) {
                        speedBoostActive = false
                        controller.endSpeedBoost()
                    }
                },
                onSeekTo = { controller.seekTo(it) },
                onIndicator = { gestureIndicator = it },
            ),
    ) {
        // 1. 视频渲染层：始终按视频宽高比居中（直连 surface 不会自动 letterbox）。
        //    全屏/小窗/退出全屏后任何容器尺寸下都保持比例，多余区域由黑底承担，
        //    杜绝全屏切换后的画面拉伸；小窗高度由页面按比例设置，黑边最少。
        val videoRatio = if (uiState.videoWidth > 0 && uiState.videoHeight > 0) {
            uiState.videoWidth.toFloat() / uiState.videoHeight
        } else {
            0f
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            playerSurface(
                if (videoRatio > 0f) {
                    Modifier.aspectRatio(videoRatio, matchHeightConstraintsFirst = videoRatio < 1f)
                } else {
                    Modifier.fillMaxSize()
                }
            )
        }

        // 2. 海报层（未开始播放前显示封面）
        if (uiState.state == PlayerState.IDLE ||
            uiState.state == PlayerState.PREPARING ||
            uiState.state == PlayerState.ERROR
        ) {
            AsyncImage(
                model = uiState.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // 3. 控件渐变衬底（跟随控件淡入淡出）
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                            )
                        )
                )
                Box(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                            )
                        )
                )
            }
        }

        // 4. 顶栏：从上方滑入滑出 + 淡入淡出
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            PlayerTopBar(
                isFullscreen = isFullscreen,
                title = uiState.title,
                showHKeyframeButton = showHKeyframe,
                showSuperResolution = controller.kernelType == KernelType.MpvPlayer,
                speedLabel = speedLabel,
                superResLabel = superResLabel,
                onBack = onBack,
                onGoHome = onGoHome,
                onSpeedClick = { coords ->
                    openBubble(PlayerSheetType.Speed, coords, preferAbove = false)
                },
                onKeyframeClick = {
                    showControls = false
                    currentSheet = PlayerSheetType.Keyframes
                },
                onKeyframeLongClick = { onAddHKeyframeAt(progress.positionMs) },
                onSuperResolutionClick = { coords ->
                    openBubble(PlayerSheetType.SuperResolution, coords, preferAbove = false)
                },
            )
        }

        // 5. 关键帧倒计时（全屏）
        if (isFullscreen) {
            KeyframeCountdownOverlay(
                positionMs = progress.positionMs,
                keyframes = keyframeEntity,
                remindAheadMs = remindAheadMs,
                showComment = showCommentCountdown,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 80.dp, start = 12.dp),
            )
        }

        // 6. 中部控件（loading / 播放暂停）
        PlayerCenterArea(
            state = if (videoInfoLoading) PlayerState.PREPARING else uiState.state,
            controlsVisible = controlsVisible,
            onPlayClick = {
                when {
                    // 视频信息尚未加载完成：无可播链接，且 loading 态下中键
                    // 不会出现——这里兜底忽略，绝不触发跳转浏览器
                    videoInfoLoading -> Unit
                    !uiState.hasMedia -> onNoVideoUrl()
                    uiState.state == PlayerState.IDLE -> controller.startVideo()
                    else -> controller.play()
                }
            },
            onPauseClick = { controller.pause() },
            onRetry = { controller.retry() },
            modifier = Modifier.fillMaxSize(),
        )

        // 7. 手势锁（全屏）
        AnimatedVisibility(
            visible = isFullscreen &&
                    (if (uiState.isGestureLocked) lockVisible else controlsVisible),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            FilledIconButton(
                onClick = {
                    val locked = !uiState.isGestureLocked
                    controller.setGestureLocked(locked)
                    if (locked) lockVisible = true
                },
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.45f)
                ),
            ) {
                Icon(
                    imageVector = if (uiState.isGestureLocked) {
                        Icons.Outlined.Lock
                    } else {
                        Icons.Outlined.LockOpen
                    },
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }

        // 8. 底栏：从下方滑入滑出 + 淡入淡出
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            PlayerBottomBar(
                currentTime = PlayerUtils.stringForTime(progress.positionMs),
                totalTime = PlayerUtils.stringForTime(duration),
                progress = sliderValue,
                buffered = progress.bufferedPercent / 100f,
                isLooping = uiState.isLooping,
                qualityLabel = uiState.qualities.getOrNull(uiState.currentQualityIndex)
                    ?: stringResource(R.string.clarity),
                isFullscreen = isFullscreen,
                onProgressChange = { fraction ->
                    sliderDragging = true
                    showControls = true
                    sliderDragValue = fraction
                },
                onProgressChangeFinished = {
                    sliderDragging = false
                    if (duration > 0) {
                        controller.seekTo((sliderDragValue * duration).toLong())
                    }
                },
                onToggleLooping = {
                    controller.setLooping(!uiState.isLooping)
                    GlobalToasts.show(
                        view.context.getString(
                            if (uiState.isLooping) R.string.loop_play_off else R.string.loop_play_on
                        ),
                        level = if (uiState.isLooping) {
                            GlobalToasts.ToastLevel.INFO
                        } else {
                            GlobalToasts.ToastLevel.SUCCESS
                        },
                    )
                },
                onQualityClick = { coords ->
                    openBubble(PlayerSheetType.Quality, coords, preferAbove = true)
                },
                onToggleFullscreen = onToggleFullscreen,
            )
        }

        // 9. 迷你底部进度条
        if (showBottomProgress && duration > 0 &&
            uiState.state != PlayerState.IDLE
        ) {
            LinearProgressIndicator(
                progress = { sliderValue.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp),
                trackColor = Color.White.copy(alpha = 0.15f),
            )
        }

        // 10. 「从头开始播放」
        AnimatedVisibility(
            visible = resumeVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp),
        ) {
            ElevatedButton(
                onClick = {
                    resumeVisible = false
                    controller.restartFromBeginning()
                },
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.start_from_beginning))
            }
        }

        // 11. 手势指示浮层
        GestureIndicatorOverlay(
            data = gestureIndicator,
            modifier = Modifier.fillMaxSize(),
        )

        // 12. 气泡弹窗（倍速/清晰度/超分）：指向触发按钮，弹出期间控件保持显示
        if (bubble != null) {
            // 点击气泡外区域关闭（吞掉点击，不透传给播放器手势）
            BubbleDismissScrim(
                onDismiss = { bubble = null },
                modifier = Modifier.fillMaxSize(),
            )
            val itemCount = when (bubble) {
                PlayerSheetType.Speed -> PlayerDefaults.speedArray.size
                PlayerSheetType.Quality -> uiState.qualities.size.coerceAtLeast(1)
                PlayerSheetType.SuperResolution -> 3
                else -> 1
            }
            PlayerBubblePopup(
                anchorX = bubbleAnchor.center.x,
                anchorEdgeY = if (bubblePreferAbove) bubbleAnchor.top else bubbleAnchor.bottom,
                preferAbove = bubblePreferAbove,
                playerWidthPx = playerRootCoords?.size?.width ?: 0,
                itemCount = itemCount,
                onDismiss = { bubble = null },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (bubble) {
                    PlayerSheetType.Speed -> SpeedBubble(
                        currentSpeedIndex = uiState.speedIndex,
                        onSelect = { index ->
                            controller.setSpeedIndex(index)
                            bubble = null
                        },
                    )

                    PlayerSheetType.Quality -> QualityBubble(
                        qualities = uiState.qualities,
                        currentIndex = uiState.currentQualityIndex,
                        onSelect = { index ->
                            controller.switchQuality(index)
                            bubble = null
                        },
                    )

                    PlayerSheetType.SuperResolution -> SuperResolutionBubble(
                        currentIndex = uiState.superResolutionIndex,
                        onSelect = { index ->
                            controller.setSuperResolution(index)
                            bubble = null
                        },
                    )

                    else -> Unit
                }
            }
        }

        // 13. 关键帧列表弹窗（内容长，保留右侧滑入面板）
        PlayerSideSheet(
            visible = currentSheet == PlayerSheetType.Keyframes,
            onDismiss = { currentSheet = null },
            modifier = Modifier.fillMaxSize(),
        ) {
            KeyframesSheet(
                keyframes = keyframeEntity,
                isLocalKeyframes = keyframeEntity?.author == null,
                onSeek = { keyframe ->
                    controller.seekTo(keyframe.position)
                    currentSheet = null
                },
                onEdit = { keyframe ->
                    currentSheet = null
                    onEditKeyframe(keyframe)
                },
                onDelete = { keyframe ->
                    currentSheet = null
                    onDeleteKeyframe(keyframe)
                },
            )
        }
    }
}

/**
 * 播放器中部区域：加载指示 / 播放暂停按钮 / 重试卡片。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerCenterArea(
    state: PlayerState,
    controlsVisible: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (state) {
            // 加载中
            PlayerState.PREPARING, PlayerState.BUFFERING -> {
                WalkingCatAnimation(modifier = Modifier.size(80.dp))
            }

            // 出错重试
            PlayerState.ERROR -> {
                RetryCard(onRetry = onRetry)
            }

            // 暂停/完成/未开始：大播放键
            PlayerState.PAUSED,
            PlayerState.COMPLETED,
            PlayerState.IDLE -> {
                FilledIconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(84.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                    )
                }
                if (state == PlayerState.COMPLETED) {
                    Text(
                        text = stringResource(R.string.player_replay),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                    )
                }
            }

            // 播放中：控件可见时显示小暂停键
            PlayerState.PLAYING -> {
                if (controlsVisible) {
                    IconButton(onClick = onPauseClick) {
                        Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 加载失败重试卡片。
 */
@Composable
private fun RetryCard(onRetry: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.player_video_load_failed),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.player_click_retry))
            }
        }
    }
}
