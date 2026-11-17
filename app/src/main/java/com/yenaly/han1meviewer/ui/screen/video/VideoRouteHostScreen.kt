package com.yenaly.han1meviewer.ui.screen.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.graphics.drawable.Icon
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.PermissionRequester
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeVideoLink
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.exception.ParseException
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.bridge.VideoPageHost
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.GlobalDialogs
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.TextInputField
import com.yenaly.han1meviewer.ui.navigation.main.HomeRoute
import com.yenaly.han1meviewer.ui.navigation.main.VideoRoute
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.player.HanimePlayerController
import com.yenaly.han1meviewer.ui.player.PlayerState
import com.yenaly.han1meviewer.ui.player.kernel.KernelType
import com.yenaly.han1meviewer.ui.player.surface.rememberMovablePlayerSurface
import com.yenaly.han1meviewer.ui.player.video.HanimeVideoPlayer
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.OrientationManager
import com.yenaly.han1meviewer.util.OrientationManager.ScreenOrientation
import com.yenaly.han1meviewer.util.appScreenHeight
import com.yenaly.han1meviewer.util.appScreenWidth
import com.yenaly.han1meviewer.util.browse
import com.yenaly.han1meviewer.util.checkBadGuy
import com.yenaly.han1meviewer.util.copyToClipboard
import com.yenaly.han1meviewer.util.loadAssetAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.yenaly.han1meviewer.util.dp as dpToPx

/**
 * 播放页宿主（纯 Compose）。
 *
 * 取代旧版基于 View 体系的 `VideoRouteShell`（CoordinatorLayout + AppBarLayout +
 * CollapsingToolbarLayout）与 `HJzvdStd` 播放器：
 * - 播放器经 [rememberMovablePlayerSurface] 在「折叠头部小窗 / 平板分栏 / 全屏覆盖层」间移动；
 * - 全屏为 Compose 树内覆盖层（视频页无底部导航，页面级覆盖即可全屏），
 *   配合 [WindowInsetsControllerCompat] 隐藏系统栏与方向锁定；
 * - PiP、观看历史、自动连播、重力自动全屏等页面级逻辑自旧版等价迁移。
 */
@OptIn(ExperimentalTime::class)
@Composable
fun VideoRouteHostScreen(
    activity: MainActivity,
    route: VideoRoute,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val viewModel: VideoViewModel = viewModel()
    val commentViewModel: CommentViewModel = viewModel()
    val hostUiState by viewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()
    val relatedItems =
        viewModel.hanimeVideoFlow.collectAsStateWithLifecycle().value?.relatedHanimes.orEmpty()

    val genres by produceState(
        initialValue = emptyList(),
        key1 = Preferences.baseUrl,
    ) {
        // 资产 JSON 读取 + 解析移到 IO：避免在组合期间占用主线程
        value = withContext(Dispatchers.IO) {
            loadAssetAs<List<com.yenaly.han1meviewer.logic.model.SearchOption>>(
                if (Preferences.baseUrl == com.yenaly.han1meviewer.HanimeConstants.HANIME_URL[3]) {
                    "search_options/genre_av.json"
                } else {
                    "search_options/genre.json"
                }
            ).orEmpty()
        }
    }
    val stringLongPressShare = remember(activity) {
        activity.getString(R.string.long_press_share_to_copy)
    }
    val useLargeVideoLayout = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    commentViewModel.code = route.videoCode
    viewModel.fromDownload = route.videoCode == "-1" || route.localUri != null

    var checkedQuality by remember(route.videoCode, route.localUri) {
        mutableStateOf<String?>(null)
    }
    var pendingDownloadPrompt by remember(route.videoCode, route.localUri) {
        mutableStateOf<DownloadPromptState?>(null)
    }
    var videoTitle by remember(route.videoCode, route.localUri) { mutableStateOf<String?>(null) }
    var showAddHKeyframeDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var unsubscribeArtist by remember { mutableStateOf<HanimeVideo.Artist?>(null) }
    var showDownloadPermissionDialog by remember { mutableStateOf(false) }

    // ---------------- 播放器核心 ----------------

    val kernelType = remember { KernelType.fromString(Preferences.switchPlayerKernel) }
    val controller = remember(route.videoCode, route.localUri) {
        HanimePlayerController(activity, kernelType)
    }
    val playerUiState by controller.uiState.collectAsStateWithLifecycle()

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    /** 关键帧实体（倒计时与列表弹窗数据源）。 */
    var keyframeEntity by remember(route.videoCode) { mutableStateOf<HKeyframeEntity?>(null) }

    /** 可移动的视频 Surface。 */
    val playerSurface = rememberMovablePlayerSurface(controller)

    /** 是否全屏。 */
    var isFullscreen by remember { mutableStateOf(false) }
    /** 是否处于 PiP。 */
    val isInPipMode = hostUiState.isInPipMode

    /** 播放器在窗口中的位置（PiP sourceRectHint 用）。 */
    var playerWindowBounds by remember {
        mutableStateOf(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset.Zero))
    }

    /** 进入全屏前的窗口亮度（退出时恢复）。 */
    var brightnessBackup by remember { mutableFloatStateOf(-1f) }

    /** 重力自动全屏防抖时间戳。 */
    var lastAutoFullscreenTime by remember { mutableLongStateOf(0L) }

    // ---------------- 全屏切换过渡 ----------------
    //
    // 覆盖层从小窗矩形（playerWindowBounds，与 PiP sourceRectHint 同源）
    // 扩展/收缩到全屏；动画期间小窗槽位保持黑色占位，surface 由 movableContent
    // 留在覆盖层，动画结束后才归还小窗，避免画面闪跳。进度只在 graphicsLayer
    //（draw 阶段）读取，不引发逐帧重组。

    /** 全屏过渡进度：0 = 小窗矩形，1 = 全屏。 */
    val fullscreenAnim = remember { Animatable(0f) }

    /** 覆盖层是否在组合中（含退出动画期间；结束后才移除）。 */
    var fullscreenOverlayVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isFullscreen, isInPipMode) {
        when {
            isInPipMode -> {
                fullscreenOverlayVisible = false
                fullscreenAnim.snapTo(0f)
            }

            isFullscreen -> {
                fullscreenOverlayVisible = true
                fullscreenAnim.snapTo(0f)
                fullscreenAnim.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            }

            else -> {
                // 只有覆盖层真展示过才播放退出动画
                if (fullscreenOverlayVisible) {
                    fullscreenAnim.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
                }
                fullscreenOverlayVisible = false
            }
        }
    }

    /**
     * 全屏覆盖层是否应顶替小窗槽位：进入/退出动画全程为 true——
     * 小窗槽位在覆盖层彻底消失前保持黑色占位。
     */
    val showFullscreenOverlay = fullscreenOverlayVisible && !isInPipMode

    fun enterFullscreen() {
        if (isFullscreen) return
        isFullscreen = true
        val window = activity.window
        brightnessBackup = window.attributes.screenBrightness
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        // 竖屏视频全屏时保持竖屏（对齐旧版 lockOrientation(PORTRAIT) 行为）
        activity.requestedOrientation = if (controller.isPortraitVideo) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    fun exitFullscreen() {
        if (!isFullscreen) return
        isFullscreen = false
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
        // 恢复进入全屏前的亮度（手势调节可能改动过）
        window.attributes = window.attributes.apply {
            screenBrightness = brightnessBackup.coerceAtLeast(-1f)
        }
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    BackHandler(enabled = isFullscreen) {
        exitFullscreen()
    }

    // ---------------- 重力自动全屏 ----------------

    val orientationManager = remember(activity) {
        OrientationManager(activity) { orientation ->
            val state = playerUiState.state
            if (!useLargeVideoLayout &&
                (state == PlayerState.PLAYING || state == PlayerState.PAUSED) &&
                !controller.isPortraitVideo
            ) {
                if (orientation.isLandscape && !isFullscreen &&
                    System.currentTimeMillis() - lastAutoFullscreenTime > 2_000
                ) {
                    lastAutoFullscreenTime = System.currentTimeMillis()
                    enterFullscreen()
                } else if (orientation == ScreenOrientation.PORTRAIT && isFullscreen) {
                    exitFullscreen()
                }
            }
        }
    }

    // ---------------- PiP ----------------

    /** PiP 是否允许（与 MainActivity.onUserLeaveHint 读取同一偏好）。
     *  每次重组直接读取（SharedPreferences 首次加载后在内存），设置页改动后不会残留旧值。 */
    val pipAllowed = Preferences.allowPipMode

    /** PiP 是否满足进入条件（仅播放中；暂停/停止/播完等非播放状态一律不进 PiP，issue #684）。 */
    val pipEligible = playerUiState.state == PlayerState.PLAYING

    /**
     * PiP 窗口宽高比：按视频实际比例（PiP 支持范围外时收敛到边界），
     * 未知时回落 16:9——窗口形状与视频一致，配合 sourceRectHint 才能无缝。
     */
    fun pipAspectRatio(): android.util.Rational {
        val w = playerUiState.videoWidth
        val h = playerUiState.videoHeight
        val ratio = if (w > 0 && h > 0) {
            (w.toFloat() / h).coerceIn(0.41841f, 2.39f)
        } else {
            16f / 9f
        }
        return android.util.Rational((ratio * 10_000).roundToInt(), 10_000)
    }

    /**
     * 统一构建 PiP 参数（进入/更新/autoEnter 共用）：
     * - 宽高比按视频实际比例，避免窗口形状跳变；
     * - [includeSourceHint] 携带 sourceRectHint（进入动画从小窗矩形起）；
     * - S+ 上按 [eligible] 设置 autoEnterEnabled——手势上滑回主页时
     *   系统直接把播放器动画进 PiP，全程无缝。
     */
    fun buildPipParams(includeSourceHint: Boolean, eligible: Boolean): PictureInPictureParams {
        val intent = PendingIntent.getBroadcast(
            activity,
            0,
            Intent(MainActivity.ACTION_TOGGLE_PLAY).setPackage(activity.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val icon = if (controller.isPlaying) {
            Icon.createWithResource(activity, R.drawable.ic_pip_pause_24)
        } else {
            Icon.createWithResource(activity, R.drawable.ic_pip_play_arrow_24)
        }
        val action = RemoteAction(
            icon,
            activity.getString(R.string.play_pause),
            activity.getString(R.string.play_pause),
            intent,
        )
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(pipAspectRatio())
            .setActions(listOf(action))
        if (includeSourceHint && playerWindowBounds.width > 0) {
            builder.setSourceRectHint(
                android.graphics.Rect(
                    playerWindowBounds.left.roundToInt(),
                    playerWindowBounds.top.roundToInt(),
                    playerWindowBounds.right.roundToInt(),
                    playerWindowBounds.bottom.roundToInt(),
                ),
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(eligible && pipAllowed)
        }
        return builder.build()
    }

    // 播放状态/视频尺寸/全屏态/PiP 模式变化时刷新 PiP 参数——autoEnter 依赖
    // 最近一次 setPictureInPictureParams。关键：isInPipMode 也作为键——
    // 从 PiP 展开回来时必须重设（PiP 期间 updatePipAction 刷新过按钮，
    // 否则 autoEnter=false 的旧参数会留驻系统，导致第二次回桌面不再进 PiP）
    LaunchedEffect(
        pipEligible,
        playerUiState.videoWidth,
        playerUiState.videoHeight,
        isFullscreen,
        pipAllowed,
        isInPipMode,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isInPipMode) {
            runCatching { activity.setPictureInPictureParams(buildPipParams(true, pipEligible)) }
        }
    }

    // 离开播放页时必须显式关闭 autoEnter（issue #684）：
    // setPictureInPictureParams 登记在 Activity 级、留驻系统，不会随本页销毁而失效。
    // 若不清除，只要本页播放/暂停过一次，之后从 App 任意页面回桌面，
    // 系统都会把已 release 的播放器塞进 PiP 小窗（黑屏死窗口）。
    DisposableEffect(activity) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    activity.setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setAutoEnterEnabled(false)
                            .build()
                    )
                }
            }
        }
    }

    fun updatePipAction() {
        if (!activity.isInPictureInPictureMode) return
        // 刷新播放/暂停按钮；保留 sourceRectHint 与 autoEnter（展开回 app 后
        // 系统仍用这套参数，重置 autoEnter 会让下一次回桌面进不了 PiP）
        runCatching { activity.setPictureInPictureParams(buildPipParams(true, pipEligible)) }
    }

    // PiP 小窗按钮图标随播放状态同步——播完/出错/音频焦点丢失等
    // 非按钮路径的状态变化也要刷新 RemoteAction 图标
    LaunchedEffect(isInPipMode, playerUiState.state) {
        if (isInPipMode) updatePipAction()
    }

    val pageHost = remember(activity, controller, viewModel) {
        object : VideoPageHost {
            override fun showCommentBadge(count: Int) {
                viewModel.setCommentBadgeCount(count)
            }

            override fun shouldEnterPip(): Boolean {
                return controller.isActiveForPip
            }

            override fun enterPipMode() {
                activity.enterPictureInPictureMode(buildPipParams(includeSourceHint = true, eligible = true))
            }

            override fun onPipModeChanged(isInPip: Boolean) {
                viewModel.setPipMode(isInPip)
                if (isInPip) {
                    updatePipAction()
                }
            }

            override fun togglePlayPause() {
                controller.togglePlayPause()
                updatePipAction()
            }
        }
    }

    DisposableEffect(activity, pageHost) {
        activity.registerCurrentVideoHost(pageHost)
        onDispose { activity.registerCurrentVideoHost(null) }
    }

    // ---------------- 媒体加载与进度恢复 ----------------

    val currentOnNoVideoUrl by rememberUpdatedState {
        GlobalToasts.show(
            activity.getString(R.string.fail_to_get_video_link),
            level = GlobalToasts.ToastLevel.ERROR,
        )
        activity.browse(getHanimeVideoLink(route.videoCode))
    }

    LaunchedEffect(route.videoCode, route.localUri, viewModel.fromDownload) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.hanimeVideoStateFlow.collect { state ->
                when (state) {
                    is VideoLoadingState.Error -> {
                        state.throwable.localizedMessage?.let {
                            GlobalToasts.show(it, level = GlobalToasts.ToastLevel.ERROR)
                        }
                        if (state.throwable is ParseException) {
                            activity.browse(getHanimeVideoLink(route.videoCode))
                        }
                    }

                    is VideoLoadingState.Loading -> Unit

                    is VideoLoadingState.Success -> {
                        videoTitle = state.info.title
                        val history = DatabaseRepo.WatchHistory.findBy(route.videoCode)
                        controller.setMedia(
                            title = state.info.title,
                            videoUrls = state.info.videoUrls,
                            savedProgressMs = history?.progress ?: 0L,
                            posterUrl = state.info.coverUrl,
                        )
                        if (!viewModel.fromDownload) {
                            viewModel.insertWatchHistoryWithCover(
                                WatchHistoryEntity(
                                    state.info.coverUrl,
                                    state.info.title,
                                    state.info.uploadTimeMillis,
                                    Clock.System.now().toEpochMilliseconds(),
                                    route.videoCode,
                                )
                            )
                        }
                        if (route.autoPlay) {
                            controller.startVideo()
                        }
                    }

                    is VideoLoadingState.NoContent -> {
                        GlobalToasts.show(
                            activity.getString(R.string.video_might_not_exist),
                            level = GlobalToasts.ToastLevel.ERROR,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(route.videoCode, controller) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.observeKeyframe(route.videoCode).collect {
                keyframeEntity = it
                viewModel.hKeyframes = it
            }
        }
    }

    // ---------------- 播放状态联动（滚动锁定 / 已看 / 自动连播） ----------------

    fun markWatchedIfReachedThreshold() {
        if (viewModel.fromDownload) return
        val duration = controller.durationMs
        val position = controller.currentPositionMs
        if (duration > 0 && position * 100 >= duration * Preferences.watchedProgressThreshold) {
            scope.launch {
                DatabaseRepo.WatchHistory.markWatched(route.videoCode)
            }
        }
    }

    fun autoPlayNextIfEnabled() {
        if (viewModel.fromDownload || !Preferences.autoPlayNext) return
        val videos = viewModel.hanimeVideoFlow.value?.playlist?.video.orEmpty()
        if (videos.isEmpty()) return
        val currentIndex = videos.indexOfFirst { it.videoCode == route.videoCode }
        if (currentIndex < 0) return
        val next = videos.getOrNull(currentIndex + 1) ?: return
        if (isFullscreen) {
            exitFullscreen()
        }
        val navController = activity.navController
        val currentId = navController.currentBackStackEntry?.destination?.id
        navController.navigateSafely(VideoRoute(next.videoCode, autoPlay = true)) {
            if (currentId != null) {
                popUpTo(currentId) { inclusive = true }
            }
        }
    }

    LaunchedEffect(controller) {
        controller.uiState.collect { state ->
            if (state.state == PlayerState.COMPLETED) {
                markWatchedIfReachedThreshold()
                autoPlayNextIfEnabled()
            }
        }
    }

    // ---------------- 生命周期（进度保存 / 后台暂停） ----------------

    DisposableEffect(lifecycleOwner, activity, controller, route.videoCode) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val progress = controller.currentPositionMs
                    scope.launch {
                        DatabaseRepo.WatchHistory.updateProgress(route.videoCode, progress)
                    }
                    markWatchedIfReachedThreshold()
                }

                Lifecycle.Event.ON_STOP -> {
                    // 息屏/切后台：只暂停播放，保持全屏状态——
                    // 解锁/回前台后仍处于全屏，不退回小窗
                    if (!activity.isInPictureInPictureMode) {
                        controller.pause()
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(orientationManager)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(orientationManager)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ---------------- 其它数据流 ----------------

    LaunchedEffect(route.videoCode, route.localUri) {
        checkedQuality = null
        pendingDownloadPrompt = null
        videoTitle = null
        // 签名校验涉及 PackageManager binder 调用与证书摘要计算，移出主线程
        withContext(Dispatchers.Default) {
            checkBadGuy(activity, R.raw.akarin)
        }
        viewModel.videoCode = route.videoCode
        viewModel.getHanimeVideo(route.videoCode, route.localUri)
    }

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.modifyHKeyframeFlow.collect { (success, reason) ->
                val level = if (success) GlobalToasts.ToastLevel.SUCCESS else GlobalToasts.ToastLevel.ERROR
                GlobalToasts.show(reason, level = level)
            }
        }
    }

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.modifyMyListFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> GlobalToasts.show(
                        activity.getString(R.string.add_failed),
                        level = GlobalToasts.ToastLevel.ERROR,
                    )

                    is WebsiteState.Success -> GlobalToasts.show(
                        activity.getString(R.string.add_success),
                        level = GlobalToasts.ToastLevel.SUCCESS,
                    )

                    WebsiteState.Loading -> Unit
                }
            }
        }
    }

    LaunchedEffect(viewModel, route.videoCode) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.loadDownloadedFlow.collect { entity ->
                val newQuality = checkedQuality ?: return@collect
                pendingDownloadPrompt = DownloadPromptState(
                    newQuality = newQuality,
                    oldQuality = entity?.quality,
                )
            }
        }
    }

    val actions = remember(activity, scope, viewModel, genres) {
        VideoRouteActions(
            context = activity,
            scope = scope,
            viewModel = viewModel,
            genres = genres,
            requestStoragePermission = { onGranted, onDenied, onPermanentlyDenied ->
                (activity as PermissionRequester).requestStoragePermission(
                    onGranted = onGranted,
                    onDenied = onDenied,
                    onPermanentlyDenied = onPermanentlyDenied,
                )
            },
            onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
            getCheckedQuality = { checkedQuality },
            setCheckedQuality = { checkedQuality = it },
            onStoragePermissionDenied = { activity.navController.popBackStack() },
            onRequestUnsubscribeConfirm = { artist -> unsubscribeArtist = artist },
            onRequestDownloadPermissionSettings = { showDownloadPermissionDialog = true },
        )
    }

    // ---------------- 播放器层 ----------------

    /** 页面视频信息是否仍在加载（加载期间中键显示 loading，不触发跳浏览器兜底）。 */
    val videoInfoLoading =
        viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle().value is VideoLoadingState.Loading

    /** 播放器槽位：surface + 控件整体，可被放置在折叠头部 / 分栏左栏 / 全屏覆盖层。 */
    @Composable
    fun PlayerSlot(modifier: Modifier, fullscreen: Boolean) {
        HanimeVideoPlayer(
            controller = controller,
            playerSurface = playerSurface,
            isFullscreen = fullscreen,
            keyframeEntity = keyframeEntity,
            modifier = modifier
                .onGloballyPositioned { playerWindowBounds = it.boundsInRoot() },
            videoInfoLoading = videoInfoLoading,
            onBack = {
                if (fullscreen) exitFullscreen()
                else activity.onBackPressedDispatcher.onBackPressed()
            },
            onGoHome = {
                if (activity.resources.getBoolean(R.bool.isTablet)) {
                    activity.navController.popBackStack(HomeRoute, false)
                }
                activity.startActivity(Intent(activity, MainActivity::class.java))
            },
            onToggleFullscreen = {
                if (fullscreen) exitFullscreen() else enterFullscreen()
            },
            onNoVideoUrl = { currentOnNoVideoUrl() },
            onAddHKeyframeAt = { positionMs ->
                if (!controller.isPlaying) {
                    showAddHKeyframeDialog = positionMs to (videoTitle ?: "Untitled")
                } else {
                    GlobalToasts.show(
                        activity.getString(R.string.pause_then_long_press),
                        level = GlobalToasts.ToastLevel.WARNING,
                    )
                }
            },
            onEditKeyframe = { keyframe ->
                GlobalDialogs.show(
                    GlobalDialogs.InputRequest(
                        title = activity.getString(R.string.modify_h_keyframe),
                        fields = listOf(
                            TextInputField(
                                label = activity.getString(R.string.position_ms),
                                initialValue = keyframe.position.toString(),
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            ),
                            TextInputField(
                                label = activity.getString(R.string.prompt),
                                initialValue = keyframe.prompt.orEmpty(),
                            ),
                        ),
                        confirmText = activity.getString(R.string.confirm),
                        dismissText = activity.getString(R.string.cancel),
                        onConfirm = { values ->
                            val pos = values.getOrElse(0) { keyframe.position.toString() }
                                .toLongOrNull() ?: keyframe.position
                            val prompt = values.getOrElse(1) { keyframe.prompt.orEmpty() }
                            activity.viewModel.modifyHKeyframe(
                                route.videoCode,
                                keyframe,
                                HKeyframeEntity.Keyframe(position = pos, prompt = prompt),
                            )
                            GlobalToasts.show(
                                activity.getString(R.string.modify_success),
                                level = GlobalToasts.ToastLevel.SUCCESS,
                            )
                        },
                    )
                )
            },
            onDeleteKeyframe = { keyframe ->
                GlobalDialogs.show(
                    GlobalDialogs.ConfirmRequest(
                        title = activity.getString(R.string.sure_to_delete),
                        message = com.yenaly.han1meviewer.ui.player.PlayerUtils
                            .stringForTime(keyframe.position),
                        confirmText = activity.getString(R.string.confirm),
                        dismissText = activity.getString(R.string.cancel),
                        onConfirm = {
                            activity.viewModel.removeHKeyframe(route.videoCode, keyframe)
                            GlobalToasts.show(
                                activity.getString(R.string.delete_success),
                                level = GlobalToasts.ToastLevel.SUCCESS,
                            )
                        },
                    )
                )
            },
        )
    }

    // ---------------- 布局 ----------------

    val configuration = LocalConfiguration.current
    val isTabletLandscape =
        useLargeVideoLayout && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showSplitLayout = isTabletLandscape && !isInPipMode
    val relatedAsTab = !viewModel.fromDownload && Preferences.relatedVideoAsTab

    var isSidePaneCollapsed by rememberSaveable { mutableStateOf(false) }

    // 简介底部是否内嵌相关影片
    LaunchedEffect(relatedAsTab, showSplitLayout) {
        viewModel.hideRelatedInIntro = relatedAsTab || showSplitLayout
    }

    // 播放器高度（px，沿用 VideoHostUiState.playerHeightDp 的像素语义）
    // 竖屏小窗高度上限：屏高的 60%——竖屏/方形视频按宽换算的高度会超过
    // 屏高（如 1080x1920 → 宽×1.78），点播放瞬间会撑满整个页面
    val maxSmallWindowHeightPx = (appScreenHeight * 0.6f).toInt()
    LaunchedEffect(isInPipMode, isSidePaneCollapsed, showSplitLayout) {
        if (isInPipMode) return@LaunchedEffect
        val height = if (showSplitLayout) {
            if (isSidePaneCollapsed) 500.dpToPx else 400.dpToPx
        } else {
            // 竖屏（折叠布局）的基础高度按屏宽 16:9、下限 250dp：
            // 平板竖屏不再显矮，手机维持原值；起播后由视频真实比例覆盖
            minOf(maxOf(250.dpToPx, appScreenWidth * 9 / 16), maxSmallWindowHeightPx)
        }
        if (hostUiState.playerHeightDp != height) {
            viewModel.setPlayerHeightDp(height)
        }
    }
    val playerHeightPx = hostUiState.playerHeightDp

    /** 视频宽高比（已加载后用于分栏播放器尺寸约束）。 */
    val videoRatio = if (playerUiState.videoWidth > 0 && playerUiState.videoHeight > 0) {
        playerUiState.videoWidth.toFloat() / playerUiState.videoHeight
    } else {
        0f
    }

    // 手机折叠布局：视频加载后按「宽满屏」的比例调整小窗高度（对齐旧版 JZVD resize 行为），
    // 同样受 60% 屏高上限约束
    LaunchedEffect(playerUiState.videoWidth, playerUiState.videoHeight, showSplitLayout, isInPipMode) {
        val w = playerUiState.videoWidth
        val h = playerUiState.videoHeight
        if (w > 0 && h > 0 && !showSplitLayout && !isFullscreen && !isInPipMode) {
            viewModel.setPlayerHeightDp(
                minOf(appScreenWidth * h / w, maxSmallWindowHeightPx)
            )
        }
    }

    /** 折叠展开状态持久化（跨越阈值时才上报）。 */
    var lastReportedExpanded by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // PiP：仅保留视频画面，无控件
            isInPipMode -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                ) {
                    playerSurface(Modifier.fillMaxSize())
                }
            }

            // 手机/竖屏：折叠头部 + tabs（让出状态栏高度，对齐旧版 shell 的 fitsSystemWindows）
            !showSplitLayout -> {
                VideoCollapsingHeader(
                    headerHeightPx = playerHeightPx,
                    scrollEnabled = playerUiState.state !in setOf(
                        PlayerState.PREPARING, PlayerState.BUFFERING, PlayerState.PLAYING,
                    ),
                    initialCollapsed = !viewModel.isAppBarExpanded(route.videoCode),
                    onCollapseRatioChange = { ratio ->
                        val expanded = ratio < 0.5f
                        if (expanded != lastReportedExpanded) {
                            lastReportedExpanded = expanded
                            viewModel.setAppBarExpanded(route.videoCode, expanded)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    headerContent = {
                        if (showFullscreenOverlay) {
                            // 播放器已移至全屏覆盖层（含切换动画期间），占位避免高度塌陷
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                        } else {
                            PlayerSlot(modifier = Modifier.fillMaxSize(), fullscreen = false)
                        }
                    },
                    content = {
                        VideoTabsHost(
                            activity = activity,
                            videoCode = route.videoCode,
                            localUri = route.localUri,
                            videoState = viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle().value,
                            viewModel = viewModel,
                            commentViewModel = commentViewModel,
                            fromDownload = viewModel.fromDownload,
                            pendingDownloadPrompt = pendingDownloadPrompt,
                            actions = actions,
                            stringLongPressShare = stringLongPressShare,
                            pageHost = pageHost,
                            onCheckedQualityChange = { checkedQuality = it },
                            onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
                            onDownloadPermissionDialogRequest = { showDownloadPermissionDialog = true },
                        )
                    },
                )
            }

            // 平板横屏分栏
            else -> {
                val indicatorWidth = 28.dp
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    val sideWidth by animateDpAsState(
                        targetValue = if (isSidePaneCollapsed) indicatorWidth else maxWidth * 0.38f,
                        animationSpec = tween(durationMillis = 300),
                        label = "sideTabsWidth",
                    )
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (relatedAsTab) {
                            // 左栏仅播放器，垂直居中
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (showFullscreenOverlay) {
                                    Box(modifier = Modifier.fillMaxWidth())
                                } else {
                                    PlayerSlot(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (videoRatio > 0f) {
                                                    Modifier.aspectRatio(
                                                        videoRatio,
                                                        matchHeightConstraintsFirst = videoRatio < 1f,
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        fullscreen = false,
                                    )
                                }
                            }
                        } else {
                            // 左栏：播放器（可折叠头部）+ 相关影片列表——暂停时与手机
                            // 折叠布局行为一致：上滑先折叠播放器，播放中锁定展开
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            ) {
                                // 展开态高度：宽按比例、不超过面板高的 65%；
                                // 比例未知时用 16:9 占位——否则未起播或竖屏视频
                                // 会把播放器撑满面板，相关影片被挤出屏幕无法滚动
                                val playerRatio = videoRatio.takeIf { it > 0f } ?: (16f / 9f)
                                val expandedPlayerHeightPx = with(LocalDensity.current) {
                                    minOf(maxWidth / playerRatio, maxHeight * 0.65f).roundToPx()
                                }
                                VideoCollapsingHeader(
                                    headerHeightPx = expandedPlayerHeightPx,
                                    scrollEnabled = playerUiState.state !in setOf(
                                        PlayerState.PREPARING,
                                        PlayerState.BUFFERING,
                                        PlayerState.PLAYING,
                                    ),
                                    initialCollapsed = !viewModel.isAppBarExpanded(route.videoCode),
                                    onCollapseRatioChange = { ratio ->
                                        val expanded = ratio < 0.5f
                                        if (expanded != lastReportedExpanded) {
                                            lastReportedExpanded = expanded
                                            viewModel.setAppBarExpanded(route.videoCode, expanded)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    headerContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black),
                                        ) {
                                            if (showFullscreenOverlay) {
                                                // 播放器已移至全屏覆盖层（含切换动画期间），占位避免高度塌陷
                                                Box(modifier = Modifier.fillMaxSize())
                                            } else {
                                                PlayerSlot(
                                                    modifier = Modifier.fillMaxSize(),
                                                    fullscreen = false,
                                                )
                                            }
                                        }
                                    },
                                    content = {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                        ) {
                                            if (relatedItems.isNotEmpty()) {
                                                RelatedVideosSection(
                                                    videos = relatedItems,
                                                    onOpenVideo = { item ->
                                                        activity.showVideoDetailFragment(item.videoCode)
                                                    },
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .width(sideWidth)
                                .fillMaxHeight(),
                        ) {
                            RelatedCollapseIndicator(
                                collapsed = isSidePaneCollapsed,
                                onClick = { isSidePaneCollapsed = !isSidePaneCollapsed },
                                modifier = Modifier
                                    .width(indicatorWidth)
                                    .fillMaxHeight(),
                            )
                            if (!isSidePaneCollapsed) {
                                VideoTabsHost(
                                    activity = activity,
                                    videoCode = route.videoCode,
                                    localUri = route.localUri,
                                    videoState = viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle().value,
                                    viewModel = viewModel,
                                    commentViewModel = commentViewModel,
                                    fromDownload = viewModel.fromDownload,
                                    pendingDownloadPrompt = pendingDownloadPrompt,
                                    actions = actions,
                                    stringLongPressShare = stringLongPressShare,
                                    pageHost = pageHost,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onCheckedQualityChange = { checkedQuality = it },
                                    onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
                                    onDownloadPermissionDialogRequest = { showDownloadPermissionDialog = true },
                                )
                            }
                        }
                    }
                }
            }
        }

        // 全屏覆盖层（后声明，天然覆盖其它内容）。
        // 过渡：从小窗矩形扩展到全屏（退出反向收缩），进度仅在 draw 阶段读取；
        // 小窗矩形来自 PiP 用途的 playerWindowBounds，若其不在当前屏内
        // （如全屏同时发生了旋转）则退化为纯淡入淡出。
        if (showFullscreenOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = fullscreenAnim.value
                        val b = playerWindowBounds
                        val rectValid = b.width > 0 && b.height > 0 &&
                                b.right <= size.width && b.bottom <= size.height
                        val sx0 = if (rectValid) b.width / size.width else 1f
                        val sy0 = if (rectValid) b.height / size.height else 1f
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = lerp(sx0, 1f, p)
                        scaleY = lerp(sy0, 1f, p)
                        translationX = if (rectValid) lerp(b.left, 0f, p) else 0f
                        translationY = if (rectValid) lerp(b.top, 0f, p) else 0f
                        // 快速淡入（前 1/4 达到不透明），退出时接近收完才淡出，
                        // 与黑色占位衔接避免闪烁
                        alpha = (p * 4f).coerceIn(0f, 1f)
                    }
                    .background(Color.Black),
            ) {
                PlayerSlot(modifier = Modifier.fillMaxSize(), fullscreen = true)
            }
        }
    }

    // ---------------- 对话框 ----------------

    unsubscribeArtist?.let { artist ->
        ConfirmDialog(
            visible = true,
            title = activity.getString(R.string.unsubscribe_artist),
            message = activity.getString(R.string.sure_to_unsubscribe),
            confirmText = activity.getString(R.string.sure),
            dismissText = activity.getString(R.string.no),
            onConfirm = {
                unsubscribeArtist = null
                actions.confirmUnsubscribe(artist)
            },
            onDismiss = { unsubscribeArtist = null },
        )
    }

    if (showDownloadPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadPermissionDialog = false },
            title = { Text(activity.getString(R.string.permission_permanently_denied_title)) },
            text = { Text(activity.getString(R.string.storage_permission_settings_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDownloadPermissionDialog = false
                    actions.goToDownloadPermissionSettings()
                }) {
                    Text(activity.getString(R.string.go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDownloadPermissionDialog = false
                    activity.navController.popBackStack()
                }) {
                    Text(activity.getString(R.string.cancel))
                }
            },
        )
    }

    showAddHKeyframeDialog?.let { (currentPosition, title) ->
        ConfirmDialog(
            visible = true,
            title = activity.getString(R.string.add_to_h_keyframe),
            message = buildString {
                appendLine(activity.getString(R.string.sure_to_add_to_h_keyframe))
                append(activity.getString(R.string.current_position_d_ms, currentPosition))
            },
            confirmText = activity.getString(R.string.confirm),
            dismissText = activity.getString(R.string.cancel),
            onConfirm = {
                viewModel.appendHKeyframe(
                    route.videoCode,
                    title,
                    HKeyframeEntity.Keyframe(position = currentPosition, prompt = null),
                )
                Firebase.analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
                    param(FirebaseAnalytics.Param.ITEM_ID, FirebaseConstants.H_KEYFRAMES)
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, FirebaseConstants.H_KEYFRAMES)
                }
                showAddHKeyframeDialog = null
            },
            onDismiss = { showAddHKeyframeDialog = null },
        )
    }
}

/**
 * tabs 内容宿主：封装 [VideoRouteContent] 在分栏与折叠布局间的公共调用。
 */
@Composable
private fun VideoTabsHost(
    activity: MainActivity,
    videoCode: String,
    localUri: String?,
    videoState: VideoLoadingState<*>,
    viewModel: VideoViewModel,
    commentViewModel: CommentViewModel,
    fromDownload: Boolean,
    pendingDownloadPrompt: DownloadPromptState?,
    actions: VideoRouteActions,
    stringLongPressShare: String,
    pageHost: VideoPageHost,
    modifier: Modifier = Modifier,
    onCheckedQualityChange: (String?) -> Unit,
    onPendingDownloadPromptChange: (DownloadPromptState?) -> Unit,
    onDownloadPermissionDialogRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        VideoRouteContent(
        videoCode = videoCode,
        videoState = videoState,
        videoViewModel = viewModel,
        commentViewModel = commentViewModel,
        fromDownload = fromDownload,
        pendingDownloadPrompt = pendingDownloadPrompt,
        onPendingDownloadPromptChange = onPendingDownloadPromptChange,
        onRetry = { viewModel.getHanimeVideo(videoCode, localUri) },
        onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
        onOpenArtist = actions::openArtistSearch,
        onNavigateToSearch = actions::openTagSearch,
        onToggleSubscribe = actions::toggleArtistSubscription,
        onToggleFavorite = actions::toggleFavorite,
        onRateVideo = actions::rateVideo,
        onManageMyList = actions::updateMyListSelection,
        onQuickCheckIn = { record ->
            val normalizedRecord = if (record.sideDishes.contains("\u001E")) {
                record
            } else {
                record.copy(sideDishes = "${record.sideDishes}\u001E${videoCode}")
            }
            scope.launch(Dispatchers.IO) {
                CheckInRecordDatabase.getDatabase(activity).checkInDao()
                    .insert(normalizedRecord)
                withContext(Dispatchers.Main) {
                    GlobalToasts.show(
                        activity.getString(R.string.checkin),
                        level = GlobalToasts.ToastLevel.SUCCESS,
                    )
                }
            }
        },
        onPrepareDownload = { quality, video ->
            onCheckedQualityChange(quality)
            video?.let(actions::startDownloadFlow)
        },
        onConfirmDownloadPrompt = { video, autoGroupName ->
            video?.let {
                actions.confirmPendingDownload(it, pendingDownloadPrompt, autoGroupName)
            }
        },
        onRequestOpenOfficialDownloadPage = actions::openOfficialDownloadPage,
        onRequestOpenDownloadPermissionSettings = onDownloadPermissionDialogRequest,
        onOpenWebPage = actions::openVideoWebPage,
        onOpenOriginalComic = actions::openOriginalComic,
        onOpenShare = { content, title ->
            ShareCompat.IntentBuilder(activity)
                .setType("text/plain")
                .setText(content)
                .setChooserTitle(title)
                .startChooser()
        },
        onCopyText = {
            it.copyToClipboard()
            GlobalToasts.show(
                activity.getString(R.string.copy_to_clipboard),
                level = GlobalToasts.ToastLevel.INFO,
            )
        },
        onIntroductionLinkClick = actions::openIntroductionLink,
        stringLongPressShare = stringLongPressShare,
        pageHost = pageHost,
        )
    }
}

/**
 * 分栏右栏折叠指示条。
 */
@Composable
private fun RelatedCollapseIndicator(
    collapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (collapsed) {
                Icons.AutoMirrored.Filled.KeyboardArrowLeft
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        )
    }
}
