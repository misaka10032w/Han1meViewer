package com.yenaly.han1meviewer.ui.fragment.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.JZMediaInterface
import cn.jzvd.Jzvd
import coil.load
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.yenaly.han1meviewer.FROM_DOWNLOAD
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.VIDEO_LAYOUT_MATCH_PARENT
import com.yenaly.han1meviewer.databinding.FragmentVideoBinding
import com.yenaly.han1meviewer.getHanimeVideoLink
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.exception.ParseException
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.adapter.HanimeVideoRvAdapter
import com.yenaly.han1meviewer.ui.screen.video.VideoScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.view.video.ExoMediaKernel
import com.yenaly.han1meviewer.ui.view.video.HJzvdStd
import com.yenaly.han1meviewer.ui.view.video.HMediaKernel
import com.yenaly.han1meviewer.ui.view.video.HanimeDataSource
import com.yenaly.han1meviewer.ui.view.video.VideoPlayerAppBarBehavior
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.checkBadGuy
import com.yenaly.han1meviewer.util.getOrCreateBadgeOnTextViewAt
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.utils.OrientationManager
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.dp
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.startActivity
import com.yenaly.yenaly_libs.utils.view.attach
import com.yenaly.yenaly_libs.utils.view.setUpFragmentStateAdapter
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

class VideoFragment : androidx.fragment.app.Fragment(), OrientationManager.OrientationChangeListener {

    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!
    private var videoPlayer: HJzvdStd? = null
    private val requireVideoPlayer get() = checkNotNull(videoPlayer)

    val viewModel by viewModels<VideoViewModel>()
    private val commentViewModel by viewModels<CommentViewModel>()
    private val kernel = HMediaKernel.Type.fromString(Preferences.switchPlayerKernel)

    private val fromDownload by lazy { requireArguments().getBoolean(FROM_DOWNLOAD, false) }
    private val videoCode by lazy {
        requireArguments().getString(VIDEO_CODE) ?: error("Missing video code")
    }
    private val videoUri by lazy { requireArguments().getString("LOCAL_URI") }
    private var videoTitle: String? = null
    private lateinit var orientationManager: OrientationManager
    private val tabNameArray by lazy {
        checkBadGuy(requireContext(), R.raw.akarin)
    }
    private val jzBackCallback = object : OnBackPressedCallback(false) {

        override fun handleOnBackPressed() {
            if (!Jzvd.backPress()) {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private val relatedVideoAdapter by lazy {
        HanimeVideoRvAdapter(VIDEO_LAYOUT_MATCH_PARENT) { item ->
            (activity as? MainActivity)?.showVideoDetailFragment(item.videoCode)
        }
    }

    private val isTabletMode get() = Preferences.tabletMode

    private var tabletLandscapeLayout: LinearLayout? = null
    private var tabletRightPanel: LinearLayout? = null
    private var tabletRightRecyclerView: RecyclerView? = null
    private var lastAppliedTabletLandscape: Boolean? = null

    // 以系统当前配置为准，避免设备自然方向和传感器角度定义差异
    private val isCurrentlyLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val videoBinding = FragmentVideoBinding.inflate(inflater, container, false)
        _binding = videoBinding
        if (videoPlayer == null) {
            videoPlayer = createVideoPlayerView()
        }
        if (videoPlayer?.parent !== binding.collapsingToolbar) {
            (videoPlayer?.parent as? ViewGroup)?.removeView(videoPlayer)
            binding.collapsingToolbar.addView(videoPlayer, 0)
        }
        val contentRoot = videoBinding.root
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val videoState by viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle()
                HanimeTheme {
                    VideoScreen(
                        state = videoState,
                        onRetry = { viewModel.getHanimeVideo(videoCode, videoUri) },
                    ) {
                        AndroidView(
                            factory = { contentRoot },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    private fun createVideoPlayerView(): HJzvdStd {
        return HJzvdStd(ContextThemeWrapper(requireContext(), requireContext().theme)).apply {
            layoutParams = com.google.android.material.appbar.CollapsingToolbarLayout.LayoutParams(
                MATCH_PARENT,
                250.dp,
            ).apply {
                collapseMode = com.google.android.material.appbar.CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX
                parallaxMultiplier = 0.7f
            }
        }
    }

    private fun initData() {
        if (videoCode == "-1") {
            viewModel.fromDownload = true
        } else {
            viewModel.fromDownload = fromDownload
        }
        viewModel.videoCode = videoCode
        commentViewModel.code = videoCode
        requireVideoPlayer.videoCode = videoCode
        checkBadGuy(requireContext(), R.raw.akarin)
        orientationManager = OrientationManager(requireActivity(), this)
        lifecycle.addObserver(orientationManager)
        requireVideoPlayer.orientationManager = orientationManager
        initViewPager()
        initHKeyframe()
        viewModel.getHanimeVideo(videoCode, videoUri)
        Log.i("video_ui", "initData: $videoCode")

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            jzBackCallback
        )

        // #217 修复加入折叠播放器功能后导致的pager高度测量问题引起子fragment下端溢出250dp（播放器高度）导致的回复
        // 评论按钮和至少一条底端评论不可见的问题
        binding.appbar.addOnOffsetChangedListener { appBar, verticalOffset ->
            val totalScrollRange = appBar.totalScrollRange
            val offset = totalScrollRange + verticalOffset
            binding.videoVp.setPadding(0, 0, 0, offset)
        }

        val behavior = (binding.appbar.layoutParams as CoordinatorLayout.LayoutParams)
            .behavior as VideoPlayerAppBarBehavior
        requireVideoPlayer.onVideoStateChanged = { state ->
            when (state) {
                Jzvd.STATE_PLAYING, Jzvd.STATE_PREPARING -> {
                    behavior.disableScroll = true
                    binding.appbar.post {
                        binding.appbar.setExpanded(true, true)
                    }
                }
                Jzvd.STATE_PAUSE, Jzvd.STATE_AUTO_COMPLETE -> {
                    behavior.disableScroll = false
                }
            }
        }
        setupTabletLayoutListener()
        if (isTabletMode) {
            syncTabletUi(force = true)
        }
    }

    /**
     * 平板模式下监听容器真实尺寸变化，等布局更新完成后再同步单/双栏
     */
    private fun setupTabletLayoutListener() {
        binding.videoRootContainer.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (!isTabletMode) return@addOnLayoutChangeListener
            val widthChanged = right - left != oldRight - oldLeft
            val heightChanged = bottom - top != oldBottom - oldTop
            if (widthChanged || heightChanged) {
                syncTabletUi()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        bindDataObservers()
        requireVideoPlayer.fullscreenListener = object : HJzvdStd.FullscreenListener {
            override fun onFullscreenChanged(isFullscreen: Boolean) {
                jzBackCallback.isEnabled = isFullscreen
                Log.i("JZVD screen state", isFullscreen.toString())
                // 退出全屏后恢复平板布局
                if (!isFullscreen && isTabletMode) {
                    requireVideoPlayer.post { syncTabletUi(force = true) }
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun bindDataObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.hanimeVideoStateFlow.collect { state ->
                    when (state) {
                        is VideoLoadingState.Error -> {
                            state.throwable.localizedMessage?.let { showShortToast(it) }
                            if (state.throwable is ParseException) {
                                requireContext().browse(getHanimeVideoLink(videoCode))
                            }
                        }

                        is VideoLoadingState.Loading -> {}

                        is VideoLoadingState.Success -> {
                            Log.i("video_ui", "video loaded: ${state.info.title}")
                            videoTitle = state.info.title

                            if (state.info.videoUrls.isEmpty()) {
                                requireVideoPlayer.startButton.setOnClickListener {
                                    showShortToast(R.string.fail_to_get_video_link)
                                    requireContext().browse(getHanimeVideoLink(videoCode))
                                }
                            } else {
                                requireVideoPlayer.setUp(
                                    HanimeDataSource(state.info.title, state.info.videoUrls),
                                    Jzvd.SCREEN_NORMAL, kernel
                                )
                            }
                            requireVideoPlayer.posterImageView.load(state.info.coverUrl) {
                                crossfade(true)
                            }
                            if (!fromDownload) {
                                val entity = WatchHistoryEntity(
                                    state.info.coverUrl,
                                    state.info.title,
                                    state.info.uploadTimeMillis,
                                    kotlin.time.Clock.System.now().toEpochMilliseconds(),
                                    videoCode
                                )
                                viewModel.insertWatchHistoryWithCover(entity)
                            }
                            val history = DatabaseRepo.WatchHistory.findBy(videoCode)
                            val progress = history?.progress ?: 0L
                            requireVideoPlayer.savedProgress = progress
                            // 平板模式：加载完成后同步相关视频区域状态
                            if (isTabletMode) {
                                syncTabletUi(force = true)
                            }
                        }

                        is VideoLoadingState.NoContent -> {
                            showShortToast(R.string.video_might_not_exist)
                        }

//                        is VideoLoadingState.Error -> TODO()
//                        VideoLoadingState.Loading -> TODO()
//                        VideoLoadingState.NoContent -> TODO()
//                        is VideoLoadingState.Success<*> -> TODO()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.observeKeyframe(videoCode)
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    Log.i("HKeyframe", "KeyframeFlow:collected entity: $it")
                    requireVideoPlayer.hKeyframe = it
                    viewModel.hKeyframes = it
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.modifyHKeyframeFlow.collect { (_, reason) ->
                showShortToast(reason)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Jzvd.goOnPlayOnPause()
    }

    override fun onPause() {
        super.onPause()
        val progress = requireVideoPlayer.currentPositionWhenPlaying
        val videoCode = videoCode
        lifecycleScope.launch {
            DatabaseRepo.WatchHistory.updateProgress(videoCode, progress)
        }
    }

    override fun onDestroyView() {
        detachTabletLayoutViews()
        tabletLandscapeLayout = null
        tabletRightPanel = null
        tabletRightRecyclerView = null
        lastAppliedTabletLandscape = null
        super.onDestroyView()
        videoPlayer = null
        _binding?.unbind()
        _binding = null
        Jzvd.releaseAllVideos()
    }

    // 非平板模式下：处理自动旋转全屏逻辑
    override fun onOrientationChanged(orientation: OrientationManager.ScreenOrientation) {
        if (!isTabletMode
            && Jzvd.CURRENT_JZVD != null
            && (requireVideoPlayer.state == Jzvd.STATE_PLAYING || requireVideoPlayer.state == Jzvd.STATE_PAUSE)
            && requireVideoPlayer.screen != Jzvd.SCREEN_TINY
            && Jzvd.FULLSCREEN_ORIENTATION != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ) {
            if (orientation.isLandscape && requireVideoPlayer.screen == Jzvd.SCREEN_NORMAL) {
                changeScreenFullLandscape(orientation)
            } else if (orientation === OrientationManager.ScreenOrientation.PORTRAIT
                && requireVideoPlayer.screen == Jzvd.SCREEN_FULLSCREEN
            ) {
                changeScreenNormal()
            }
        }
    }

    /**
     * 根据当前方向应用平板布局
     * 横屏：左右两栏（左 62% = 播放器+简介评论，右 38% = 相关视频）
     * 竖屏：上下单栏（播放器增高至 350dp）
     */
    private fun applyTabletLayout() {
        if (isCurrentlyLandscape) {
            setupTabletLandscape()
        } else {
            setupTabletPortrait()
        }
    }

    private fun syncTabletUi(force: Boolean = false) {
        if (!isTabletMode) return
        val container = binding.videoRootContainer
        if (!container.isLaidOut || container.width <= 0 || container.height <= 0) {
            if (force) {
                container.post {
                    if (isAdded && view != null) {
                        syncTabletUi(force = true)
                    }
                }
            }
            return
        }
        val isLandscape = isCurrentlyLandscape
        val layoutChanged = lastAppliedTabletLandscape != isLandscape
        if (!force && !layoutChanged) return

        lastAppliedTabletLandscape = isLandscape
        viewModel.hideRelatedInIntro = isLandscape
        applyTabletLayout()

        val relatedItems = viewModel.hanimeVideoFlow.value?.relatedHanimes.orEmpty()
        if (isLandscape) {
            relatedVideoAdapter.submitList(relatedItems)
            scheduleRightPanelGridSpanUpdate(relatedItems)
        }

        childFragmentManager.fragments
            .filterIsInstance<VideoIntroductionFragment>()
            .forEach { it.refreshRelatedSection() }
    }

    // 竖屏 / 手机模式：CoordinatorLayout 直接填满容器
    private fun setupTabletPortrait() {
        val container = binding.videoRootContainer
        val main = binding.videoMain
        if (tabletLandscapeLayout != null && tabletLandscapeLayout!!.parent != null) {
            tabletLandscapeLayout!!.removeView(main)
            container.removeView(tabletLandscapeLayout)
        }
        if (main.parent !== container) {
            (main.parent as? ViewGroup)?.removeView(main)
            main.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            container.addView(main)
        }
        safeSetPlayerHeight(350.dp)
    }

    // 横屏模式：创建 / 复用左右两栏布局
    private fun setupTabletLandscape() {
        val container = binding.videoRootContainer
        val main = binding.videoMain
        if (tabletLandscapeLayout != null && main.parent === tabletLandscapeLayout
            && tabletLandscapeLayout!!.parent === container
        ) {
            safeSetPlayerHeight(350.dp)
            scheduleRightPanelGridSpanUpdate(viewModel.hanimeVideoFlow.value?.relatedHanimes.orEmpty())
            return
        }
        (main.parent as? ViewGroup)?.removeView(main)
        (tabletLandscapeLayout?.parent as? ViewGroup)?.removeView(tabletLandscapeLayout)
        if (tabletLandscapeLayout == null) {
            tabletLandscapeLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            }
            val rightPanel = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 0.38f)
            }
            tabletRightPanel = rightPanel
            val titleTv = TextView(requireContext()).apply {
                text = getString(R.string.related_video)
                setTextAppearance(android.R.style.TextAppearance_Medium)
                setPadding(24.dp, 20.dp, 24.dp, 8.dp)
            }
            rightPanel.addView(titleTv)
            tabletRightRecyclerView = RecyclerView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
                setPadding(8.dp, 4.dp, 8.dp, 8.dp)
                clipToPadding = false
                layoutManager = GridLayoutManager(requireContext(), 1)
                adapter = relatedVideoAdapter
                addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
                    if (right - left != oldRight - oldLeft) {
                        scheduleRightPanelGridSpanUpdate(viewModel.hanimeVideoFlow.value?.relatedHanimes.orEmpty())
                    }
                }
            }
            rightPanel.addView(tabletRightRecyclerView)
            tabletLandscapeLayout!!.addView(rightPanel)
        }
        tabletLandscapeLayout!!.removeView(main)
        main.layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 0.62f)
        tabletLandscapeLayout!!.addView(main, 0)
        container.removeAllViews()
        container.addView(tabletLandscapeLayout)
        safeSetPlayerHeight(350.dp)
    }

    private fun detachTabletLayoutViews() {
        val main = binding.videoMain
        (main.parent as? ViewGroup)?.removeView(main)
        tabletRightRecyclerView?.adapter = null
        (tabletRightRecyclerView?.parent as? ViewGroup)?.removeView(tabletRightRecyclerView)
        (tabletRightPanel?.parent as? ViewGroup)?.removeView(tabletRightPanel)
        (tabletLandscapeLayout?.parent as? ViewGroup)?.removeView(tabletLandscapeLayout)
    }

    private fun scheduleRightPanelGridSpanUpdate(items: List<com.yenaly.han1meviewer.logic.model.HanimeInfo>) {
        val rv = tabletRightRecyclerView ?: return
        if (items.isEmpty()) return
        if (rv.isLaidOut) {
            updateRightPanelGridSpan(items)
        }
        rv.post {
            rv.doOnNextLayout {
                updateRightPanelGridSpan(items)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isTabletMode) return
        binding.videoRootContainer.post { syncTabletUi(force = true) }
    }

    // 根据右侧面板宽度动态计算相关视频网格列数
    private fun updateRightPanelGridSpan(items: List<com.yenaly.han1meviewer.logic.model.HanimeInfo>) {
        val rv = tabletRightRecyclerView ?: return
        if (items.isEmpty()) return
        val width = rv.width.takeIf { it > 0 } ?: rv.measuredWidth
        if (width <= 0) return
        val availableWidth = (width - rv.paddingStart - rv.paddingEnd).coerceAtLeast(0)
        val itemWidth = if (items.first().itemType == com.yenaly.han1meviewer.logic.model.HanimeInfo.NORMAL) {
            resources.getDimension(R.dimen.video_cover_width)
        } else {
            resources.getDimension(R.dimen.video_cover_simplified_width)
        }
        val spanCount = (availableWidth / itemWidth).toInt().coerceAtLeast(1)
        val lm = rv.layoutManager as? GridLayoutManager ?: return
        if (lm.spanCount != spanCount) {
            lm.spanCount = spanCount
            rv.recycledViewPool.clear()
            rv.adapter?.notifyItemRangeChanged(0, rv.adapter?.itemCount ?: 0)
            rv.requestLayout()
        }
    }

    // 安全设置播放器高度（绕过 CollapsingToolbarLayout 的类型强转崩溃）
    private fun safeSetPlayerHeight(height: Int) {
        val lp = requireVideoPlayer.layoutParams
        if (lp is com.google.android.material.appbar.CollapsingToolbarLayout.LayoutParams) {
            lp.height = height
            requireVideoPlayer.layoutParams = lp
        }
    }

    private fun changeScreenNormal() {
        if (requireVideoPlayer.screen == Jzvd.SCREEN_FULLSCREEN) {
            requireVideoPlayer.gotoNormalScreen()
        }
    }

    private fun changeScreenFullLandscape(orientation: OrientationManager.ScreenOrientation) {
        if (requireVideoPlayer.screen != Jzvd.SCREEN_FULLSCREEN) {
            if (System.currentTimeMillis() - Jzvd.lastAutoFullscreenTime > 2000) {
                requireVideoPlayer.autoFullscreen(orientation)
                Jzvd.lastAutoFullscreenTime = System.currentTimeMillis()
            }
        }
    }

    private fun initViewPager() {
        binding.videoVp.offscreenPageLimit = 1
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val disableComments = prefs.getBoolean("disable_comments", false)

        binding.videoVp.setUpFragmentStateAdapter(this) {
            addFragment { VideoIntroductionFragment() }
            if (!fromDownload && !disableComments) {
                addFragment { CommentFragment() }
            }
        }

        binding.videoTl.attach(binding.videoVp) { tab, position ->
            tab.setText(tabNameArray[position])
        }
    }

    private fun initHKeyframe() {
        requireVideoPlayer.onGoHomeClickListener = {
            if (context is MainActivity && resources.getBoolean(R.bool.isTablet)) {
                findNavController().popBackStack()
            }
            requireContext().startActivity<MainActivity>()
        }
        requireVideoPlayer.onKeyframeClickListener = { v ->
            requireVideoPlayer.clickHKeyframe(v)
        }
        requireVideoPlayer.onKeyframeLongClickListener = {
            val mi: JZMediaInterface? = requireVideoPlayer.mediaInterface
            if (mi != null && !mi.isPlaying) {
                val currentPosition = requireVideoPlayer.currentPositionWhenPlaying
                it.context.showAlertDialog {
                    setTitle(R.string.add_to_h_keyframe)
                    setMessage(buildString {
                        appendLine(getString(R.string.sure_to_add_to_h_keyframe))
                        append(getString(R.string.current_position_d_ms, currentPosition))
                    })
                    setPositiveButton(R.string.confirm) { _, _ ->
                        viewModel.appendHKeyframe(
                            videoCode,
                            videoTitle ?: "Untitled",
                            HKeyframeEntity.Keyframe(
                                position = currentPosition,
                                prompt = null
                            )
                        )
                        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
                            param(FirebaseAnalytics.Param.ITEM_ID, FirebaseConstants.H_KEYFRAMES)
                            param(FirebaseAnalytics.Param.CONTENT_TYPE, FirebaseConstants.H_KEYFRAMES)
                        }
                    }
                    setNegativeButton(R.string.cancel, null)
                }
            } else {
                showShortToast(R.string.pause_then_long_press)
            }
        }
    }

    fun showRedDotCount(count: Int) {
        binding.videoTl.getOrCreateBadgeOnTextViewAt(
            tabNameArray.indexOf(R.string.comment),
            null, Gravity.END, 4.dp
        ) {
            isVisible = count > 0
            number = count
        }
    }

    fun enterPipMode() {
        val aspectRatio = Rational(16, 9)
        val intent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            Intent(MainActivity.ACTION_TOGGLE_PLAY).setPackage(requireContext().packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val icon = Icon.createWithResource(requireContext(), R.drawable.ic_baseline_pause_24_tintwhite)
        val action = RemoteAction(icon, getString(R.string.play_pause), getString(R.string.play_pause), intent)
        val sourceRect = Rect()
        requireVideoPlayer.getGlobalVisibleRect(sourceRect)
        val params = PictureInPictureParams.Builder()
            .setSourceRectHint(sourceRect)
            .setAspectRatio(aspectRatio)
            .setActions(listOf(action))
            .build()
        requireActivity().enterPictureInPictureMode(params)
    }

    fun updatePipAction() {
        if (requireActivity().isInPictureInPictureMode) {
            val isPlaying = (Jzvd.CURRENT_JZVD?.mediaInterface as? ExoMediaKernel)?.isPlaying == true
            val icon = if (isPlaying) {
                Icon.createWithResource(requireContext(), R.drawable.ic_baseline_pause_24_tintwhite)
            } else {
                Icon.createWithResource(requireContext(), R.drawable.ic_baseline_play_arrow_24_tintwhite)
            }
            val title = if (isPlaying) "Pause Video" else "Play Video"
            val intent = PendingIntent.getBroadcast(
                requireContext(), 0,
                Intent(MainActivity.ACTION_TOGGLE_PLAY).setPackage(requireContext().packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val action = RemoteAction(icon, title, getString(R.string.play_pause), intent)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(listOf(action))
                .build()
            requireActivity().setPictureInPictureParams(params)
        }
    }


    fun shouldEnterPip(): Boolean {
        return requireVideoPlayer.state == Jzvd.STATE_PLAYING || requireVideoPlayer.state == Jzvd.STATE_PAUSE
    }
    fun onPipModeChanged(isInPip: Boolean) {
        if (isInPip) {
            safeSetPlayerHeight(MATCH_PARENT)
        } else if (isTabletMode && !isCurrentlyLandscape) {
            safeSetPlayerHeight(350.dp)
        } else {
            safeSetPlayerHeight(250.dp)
        }
        binding.videoTl.isVisible = !isInPip
        binding.videoVp.isUserInputEnabled = !isInPip
        binding.videoVp.isVisible = !isInPip
        requireVideoPlayer.setControlsVisible(!isInPip)
        if (isInPip) updatePipAction()
    }
    fun togglePlayPause() {
        val player = requireVideoPlayer
        if (player.mediaInterface.isPlaying) {
            player.mediaInterface.pause()
        } else {
            player.mediaInterface.start()
        }
        updatePipAction()
    }
}
