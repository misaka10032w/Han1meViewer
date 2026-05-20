package com.yenaly.han1meviewer.ui.screen.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.util.Log
import android.util.Rational
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.jzvd.JZMediaInterface
import cn.jzvd.Jzvd
import coil.load
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeVideoLink
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.exception.ParseException
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.bridge.VideoPageHost
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.fragment.PermissionRequester
import com.yenaly.han1meviewer.ui.navigation.main.VideoRoute
import com.yenaly.han1meviewer.ui.view.video.ExoMediaKernel
import com.yenaly.han1meviewer.ui.view.video.HJzvdStd
import com.yenaly.han1meviewer.ui.view.video.HMediaKernel
import com.yenaly.han1meviewer.ui.view.video.HanimeDataSource
import com.yenaly.han1meviewer.ui.view.video.VideoPlayerAppBarBehavior
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.checkBadGuy
import com.yenaly.han1meviewer.util.loadAssetAs
import com.yenaly.yenaly_libs.utils.OrientationManager
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.copyToClipboard
import com.yenaly.yenaly_libs.utils.dp
import com.yenaly.yenaly_libs.utils.shareText
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.startActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

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
    val kernel = remember { HMediaKernel.Type.fromString(Preferences.switchPlayerKernel) }
    val genres = remember(Preferences.baseUrl) {
        loadAssetAs<List<SearchOption>>(
            if (Preferences.baseUrl == com.yenaly.han1meviewer.HanimeConstants.HANIME_URL[3]) {
                "search_options/genre_av.json"
            } else {
                "search_options/genre.json"
            }
        ).orEmpty()
    }
    val player = remember(route.videoCode, route.localUri) {
        createVideoPlayerView(activity)
    }
    val shell = remember(route.videoCode, route.localUri) {
        VideoRouteShell(activity, player)
    }
    val hostUiState by viewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()
    val relatedItems =
        viewModel.hanimeVideoFlow.collectAsStateWithLifecycle().value?.relatedHanimes.orEmpty()
    val stringLongPressShare = remember(activity) {
        activity.getString(R.string.long_press_share_to_copy)
    }

    commentViewModel.code = route.videoCode
    player.videoCode = route.videoCode
    viewModel.fromDownload = route.videoCode == "-1" || route.localUri != null

    var checkedQuality by remember(
        route.videoCode,
        route.localUri
    ) { mutableStateOf<String?>(null) }
    var pendingDownloadPrompt by remember(route.videoCode, route.localUri) {
        mutableStateOf<DownloadPromptState?>(null)
    }
    var videoTitle by remember(route.videoCode, route.localUri) { mutableStateOf<String?>(null) }
    var showAddHKeyframeDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }

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
            onDownloadPermissionDialogCancelled = { activity.navController.popBackStack() },
        )
    }

    val jzBackCallback = remember(activity) {
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (!Jzvd.backPress()) {
                    isEnabled = false
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    fun setPlayerHeight(height: Int) {
        shell.setPlayerHeight(height)
    }

    fun updatePipAction() {
        if (!activity.isInPictureInPictureMode) return
        val isPlaying = (Jzvd.CURRENT_JZVD?.mediaInterface as? ExoMediaKernel)?.isPlaying == true
        val icon = if (isPlaying) {
            Icon.createWithResource(activity, R.drawable.ic_pip_pause_24)
        } else {
            Icon.createWithResource(activity, R.drawable.ic_pip_play_arrow_24)
        }
        val title = if (isPlaying) "Pause Video" else "Play Video"
        val intent = PendingIntent.getBroadcast(
            activity,
            0,
            Intent(MainActivity.ACTION_TOGGLE_PLAY).setPackage(activity.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val action = RemoteAction(icon, title, activity.getString(R.string.play_pause), intent)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(action))
            .build()
        activity.setPictureInPictureParams(params)
    }

    fun changeScreenNormal() {
        if (player.screen == Jzvd.SCREEN_FULLSCREEN) {
            player.gotoNormalScreen()
        }
    }

    fun changeScreenFullLandscape(orientation: OrientationManager.ScreenOrientation) {
        if (player.screen != Jzvd.SCREEN_FULLSCREEN &&
            System.currentTimeMillis() - Jzvd.lastAutoFullscreenTime > 2000
        ) {
            player.autoFullscreen(orientation)
            Jzvd.lastAutoFullscreenTime = System.currentTimeMillis()
        }
    }

    val pageHost = remember(activity, player, shell, viewModel) {
        object : VideoPageHost {
            override fun showCommentBadge(count: Int) {
                viewModel.setCommentBadgeCount(count)
            }

            override fun shouldEnterPip(): Boolean {
                return player.state == Jzvd.STATE_PLAYING || player.state == Jzvd.STATE_PAUSE
            }

            override fun enterPipMode() {
                val intent = PendingIntent.getBroadcast(
                    activity,
                    0,
                    Intent(MainActivity.ACTION_TOGGLE_PLAY).setPackage(activity.packageName),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                val icon =
                    Icon.createWithResource(activity, R.drawable.ic_pip_pause_24)
                val action = RemoteAction(
                    icon,
                    activity.getString(R.string.play_pause),
                    activity.getString(R.string.play_pause),
                    intent,
                )
                val sourceRect = Rect()
                player.getGlobalVisibleRect(sourceRect)
                val params = PictureInPictureParams.Builder()
                    .setSourceRectHint(sourceRect)
                    .setAspectRatio(Rational(16, 9))
                    .setActions(listOf(action))
                    .build()
                activity.enterPictureInPictureMode(params)
            }

            override fun onPipModeChanged(isInPip: Boolean) {
                viewModel.setPipMode(isInPip)
                if (isInPip) {
                    viewModel.setPlayerHeightDp(MATCH_PARENT)
                } else if (Preferences.tabletMode &&
                    activity.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
                ) {
                    viewModel.setPlayerHeightDp(350.dp)
                } else {
                    viewModel.setPlayerHeightDp(250.dp)
                }
                setPlayerHeight(viewModel.videoHostUiStateFlow.value.playerHeightDp)
                shell.setTabsVisible(!isInPip)
                player.setControlsVisible(!isInPip)
                if (isInPip) {
                    updatePipAction()
                }
            }

            override fun togglePlayPause() {
                val mediaInterface = player.mediaInterface
                if (mediaInterface.isPlaying) {
                    mediaInterface.pause()
                } else {
                    mediaInterface.start()
                }
                updatePipAction()
            }
        }
    }

    DisposableEffect(
        activity,
        shell,
        player,
        pageHost,
        stringLongPressShare,
        route.videoCode,
        route.localUri
    ) {
        activity.registerCurrentVideoHost(pageHost)
        shell.setTabsHostContent {
            val videoState by viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle()
            VideoRouteContent(
                videoCode = route.videoCode,
                videoState = videoState,
                videoViewModel = viewModel,
                commentViewModel = commentViewModel,
                fromDownload = viewModel.fromDownload,
                pendingDownloadPrompt = pendingDownloadPrompt,
                onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
                onRetry = { viewModel.getHanimeVideo(route.videoCode, route.localUri) },
                onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
                onOpenArtist = actions::openArtistSearch,
                onNavigateToSearch = actions::openTagSearch,
                onToggleSubscribe = actions::toggleArtistSubscription,
                onToggleFavorite = actions::toggleFavorite,
                onManageMyList = actions::updateMyListSelection,
                onQuickCheckIn = { record ->
                    val normalizedRecord = if (record.sideDishes.contains("\u001E")) {
                        record
                    } else {
                        record.copy(sideDishes = "${record.sideDishes}\u001E${route.videoCode}")
                    }
                    scope.launch(Dispatchers.IO) {
                        CheckInRecordDatabase.getDatabase(activity).checkInDao()
                            .insert(normalizedRecord)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(activity, R.string.checkin, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onPrepareDownload = { quality, video ->
                    checkedQuality = quality
                    video?.let(actions::startDownloadFlow)
                },
                onConfirmDownloadPrompt = { video ->
                    video?.let { actions.confirmPendingDownload(it, pendingDownloadPrompt) }
                },
                onRequestOpenOfficialDownloadPage = actions::openOfficialDownloadPage,
                onRequestOpenDownloadPermissionSettings = actions::openDownloadPermissionSettings,
                onOpenWebPage = actions::openVideoWebPage,
                onOpenOriginalComic = actions::openOriginalComic,
                onOpenShare = { content, title -> shareText(content, title) },
                onCopyText = {
                    it.copyToClipboard()
                    showShortToast(R.string.copy_to_clipboard)
                },
                onIntroductionLinkClick = actions::openIntroductionLink,
                stringLongPressShare = stringLongPressShare,
                pageHost = pageHost,
            )
        }
        onDispose {
            activity.registerCurrentVideoHost(null)
            player.onVideoStateChanged = null
            player.fullscreenListener = null
            player.onGoHomeClickListener = null
            player.onKeyframeClickListener = null
            player.onKeyframeLongClickListener = null
            shell.clear()
            Jzvd.releaseAllVideos()
        }
    }

    DisposableEffect(lifecycleOwner, activity, player, shell, route.videoCode) {
        val orientationManager = OrientationManager(activity) { orientation ->
            if (!Preferences.tabletMode &&
                Jzvd.CURRENT_JZVD != null &&
                (player.state == Jzvd.STATE_PLAYING || player.state == Jzvd.STATE_PAUSE) &&
                player.screen != Jzvd.SCREEN_TINY &&
                Jzvd.FULLSCREEN_ORIENTATION != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ) {
                if (orientation.isLandscape && player.screen == Jzvd.SCREEN_NORMAL) {
                    changeScreenFullLandscape(orientation)
                } else if (orientation == OrientationManager.ScreenOrientation.PORTRAIT &&
                    player.screen == Jzvd.SCREEN_FULLSCREEN
                ) {
                    changeScreenNormal()
                }
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val progress = player.currentPositionWhenPlaying
                    scope.launch {
                        DatabaseRepo.WatchHistory.updateProgress(route.videoCode, progress)
                    }
                }

                Lifecycle.Event.ON_STOP -> Jzvd.goOnPlayOnPause()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(orientationManager)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        activity.onBackPressedDispatcher.addCallback(lifecycleOwner, jzBackCallback)
        player.orientationManager = orientationManager
        player.onGoHomeClickListener = {
            if (activity.resources.getBoolean(R.bool.isTablet)) {
                activity.navController.popBackStack()
            }
            activity.startActivity<MainActivity>()
        }
        player.onKeyframeClickListener = { view ->
            player.clickHKeyframe(view)
        }
        player.onKeyframeLongClickListener = {
            val mediaInterface: JZMediaInterface? = player.mediaInterface
            if (mediaInterface != null && !mediaInterface.isPlaying) {
                val currentPosition = player.currentPositionWhenPlaying
                showAddHKeyframeDialog = Pair(currentPosition, videoTitle ?: "Untitled")
            } else {
                showShortToast(R.string.pause_then_long_press)
            }
        }
        player.onVideoStateChanged = { state ->
            when (state) {
                Jzvd.STATE_PLAYING, Jzvd.STATE_PREPARING -> {
                    viewModel.setScrollDisabled(true)
                    shell.withBehavior { behavior ->
                        behavior.disableScroll = true
                    }
                    shell.setExpanded(expanded = true, animate = true)
                }

                Jzvd.STATE_PAUSE, Jzvd.STATE_AUTO_COMPLETE -> {
                    viewModel.setScrollDisabled(false)
                    shell.withBehavior { behavior ->
                        behavior.disableScroll = false
                    }
                }
            }
        }
        player.fullscreenListener = object : HJzvdStd.FullscreenListener {
            override fun onFullscreenChanged(isFullscreen: Boolean) {
                jzBackCallback.isEnabled = isFullscreen
                Log.i("JZVD screen state", isFullscreen.toString())
            }
        }
        shell.setOnOffsetChanged { totalScrollRange, verticalOffset ->
            viewModel.setAppBarBottomInsetPx(totalScrollRange + verticalOffset)
            viewModel.setAppBarExpanded(route.videoCode, verticalOffset == 0)
        }
        shell.setExpanded(expanded = viewModel.isAppBarExpanded(route.videoCode), animate = false)
        val initialHeight = if (Preferences.tabletMode) {
            350.dp
        } else {
            250.dp
        }
        viewModel.setPlayerHeightDp(initialHeight)
        setPlayerHeight(initialHeight)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(orientationManager)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    LaunchedEffect(route.videoCode, route.localUri) {
        checkedQuality = null
        pendingDownloadPrompt = null
        videoTitle = null
        checkBadGuy(activity, R.raw.akarin)
        viewModel.videoCode = route.videoCode
        viewModel.getHanimeVideo(route.videoCode, route.localUri)
    }

    LaunchedEffect(route.videoCode, route.localUri, player, kernel, viewModel.fromDownload) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.hanimeVideoStateFlow.collect { state ->
                when (state) {
                    is VideoLoadingState.Error -> {
                        state.throwable.localizedMessage?.let { showShortToast(it) }
                        if (state.throwable is ParseException) {
                            activity.browse(getHanimeVideoLink(route.videoCode))
                        }
                    }

                    is VideoLoadingState.Loading -> Unit

                    is VideoLoadingState.Success -> {
                        videoTitle = state.info.title
                        if (state.info.videoUrls.isEmpty()) {
                            player.startButton.setOnClickListener {
                                showShortToast(R.string.fail_to_get_video_link)
                                activity.browse(getHanimeVideoLink(route.videoCode))
                            }
                        } else {
                            player.setUp(
                                HanimeDataSource(state.info.title, state.info.videoUrls),
                                Jzvd.SCREEN_NORMAL,
                                kernel,
                            )
                        }
                        player.posterImageView.load(state.info.coverUrl) {
                            crossfade(true)
                        }
                        if (!viewModel.fromDownload) {
                            viewModel.insertWatchHistoryWithCover(
                                WatchHistoryEntity(
                                    state.info.coverUrl,
                                    state.info.title,
                                    state.info.uploadTimeMillis,
                                    kotlin.time.Clock.System.now().toEpochMilliseconds(),
                                    route.videoCode,
                                )
                            )
                        }
                        val history = DatabaseRepo.WatchHistory.findBy(route.videoCode)
                        player.savedProgress = history?.progress ?: 0L
                    }

                    is VideoLoadingState.NoContent -> {
                        showShortToast(R.string.video_might_not_exist)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    LaunchedEffect(route.videoCode, player) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.observeKeyframe(route.videoCode).collect {
                player.hKeyframe = it
                viewModel.hKeyframes = it
            }
        }
    }

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.modifyHKeyframeFlow.collect { (_, reason) ->
                showShortToast(reason)
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

    VideoShellContent(
        isTabletMode = Preferences.tabletMode,
        isInPipMode = hostUiState.isInPipMode,
        relatedItems = relatedItems,
        onHideRelatedInIntroChange = { viewModel.hideRelatedInIntro = it },
        onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
        mainHostFactory = {
            shell.mainHostView.also { view ->
                (view.parent as? ViewGroup)?.removeView(view)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )

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

private fun createVideoPlayerView(activity: MainActivity): HJzvdStd {
    return HJzvdStd(ContextThemeWrapper(activity, activity.theme)).apply {
        layoutParams = CollapsingToolbarLayout.LayoutParams(
            MATCH_PARENT,
            250.dp,
        ).apply {
            collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX
            parallaxMultiplier = 0.7f
        }
    }
}

private class VideoRouteShell(
    context: Context,
    private val playerView: HJzvdStd,
) {
    private val rootView = CoordinatorLayout(context).apply {
        fitsSystemWindows = true
        layoutParams = ViewGroup.LayoutParams(
            MATCH_PARENT,
            MATCH_PARENT,
        )
    }

    private val appBarLayout = AppBarLayout(context).apply {
        layoutParams = CoordinatorLayout.LayoutParams(
            MATCH_PARENT,
            WRAP_CONTENT,
        ).apply {
            behavior = VideoPlayerAppBarBehavior(context, null)
        }
    }

    private val collapsingToolbarLayout = CollapsingToolbarLayout(context).apply {
        layoutParams = AppBarLayout.LayoutParams(
            MATCH_PARENT,
            WRAP_CONTENT,
        ).apply {
            scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
        }
    }

    private val videoPlayerHost = FrameLayout(context).apply {
        layoutParams = CollapsingToolbarLayout.LayoutParams(
            MATCH_PARENT,
            WRAP_CONTENT,
        ).apply {
            collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX
            parallaxMultiplier = 1f
        }
    }

    private val videoTabsHost = ComposeView(context).apply {
        layoutParams = CoordinatorLayout.LayoutParams(
            MATCH_PARENT,
            MATCH_PARENT,
        ).apply {
            behavior = AppBarLayout.ScrollingViewBehavior()
        }
    }

    init {
        videoPlayerHost.addView(playerView, ViewGroup.LayoutParams(MATCH_PARENT, 250.dp))
        collapsingToolbarLayout.addView(videoPlayerHost)
        appBarLayout.addView(collapsingToolbarLayout)
        rootView.addView(videoTabsHost)
        rootView.addView(appBarLayout)
    }

    val mainHostView: View
        get() = rootView

    fun setTabsHostContent(content: @Composable () -> Unit) {
        videoTabsHost.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
        )
        videoTabsHost.setContent(content)
    }

    fun setTabsVisible(visible: Boolean) {
        videoTabsHost.isVisible = visible
    }

    fun setExpanded(expanded: Boolean, animate: Boolean) {
        appBarLayout.post {
            appBarLayout.setExpanded(expanded, animate)
        }
    }

    fun setOnOffsetChanged(onChanged: (totalScrollRange: Int, verticalOffset: Int) -> Unit) {
        appBarLayout.addOnOffsetChangedListener { appBar, verticalOffset ->
            onChanged(appBar.totalScrollRange, verticalOffset)
        }
    }

    fun withBehavior(block: (VideoPlayerAppBarBehavior) -> Unit) {
        val behavior = (appBarLayout.layoutParams as CoordinatorLayout.LayoutParams)
            .behavior as? VideoPlayerAppBarBehavior ?: return
        block(behavior)
    }

    fun setPlayerHeight(height: Int) {
        val lp = playerView.layoutParams
        lp.height = height
        playerView.layoutParams = lp
        playerView.requestLayout()
    }

    fun clear() {
        videoTabsHost.disposeComposition()
    }
}
