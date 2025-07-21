package com.yenaly.han1meviewer.ui.fragment.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import cn.jzvd.JZMediaInterface
import cn.jzvd.Jzvd
import coil.load
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.yenaly.han1meviewer.COMMENT_TYPE
import com.yenaly.han1meviewer.FROM_DOWNLOAD
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.VIDEO_COMMENT_PREFIX
import com.yenaly.han1meviewer.databinding.FragmentVideoBinding
import com.yenaly.han1meviewer.getHanimeVideoLink
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.exception.ParseException
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.view.video.ExoMediaKernel
import com.yenaly.han1meviewer.ui.view.video.HMediaKernel
import com.yenaly.han1meviewer.ui.view.video.HanimeDataSource
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.getOrCreateBadgeOnTextViewAt
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.OrientationManager
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.dp
import com.yenaly.yenaly_libs.utils.makeBundle
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.startActivity
import com.yenaly.yenaly_libs.utils.view.attach
import com.yenaly.yenaly_libs.utils.view.setUpFragmentStateAdapter
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock


class VideoFragment : YenalyFragment<FragmentVideoBinding>(), OrientationManager.OrientationChangeListener {

    val viewModel by viewModels<VideoViewModel>()
    private val commentViewModel by activityViewModels<CommentViewModel>()
    private val kernel = HMediaKernel.Type.fromString(Preferences.switchPlayerKernel)

    private val fromDownload by lazy { requireArguments().getBoolean(FROM_DOWNLOAD, false) }
    private val videoCode by lazy { requireArguments().getString(VIDEO_CODE) ?: error("Missing video code") }
    private var videoTitle: String? = null
    private lateinit var orientationManager: OrientationManager
    private val tabNameArray = intArrayOf(R.string.introduction, R.string.comment)
    companion object {
        fun newInstance(videoCode: String): VideoFragment {
            return VideoFragment().apply {
                arguments = bundleOf(VIDEO_CODE to videoCode)
            }
        }
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentVideoBinding {
        return FragmentVideoBinding.inflate(inflater, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        viewModel.fromDownload = fromDownload
        viewModel.videoCode = videoCode
        commentViewModel.code = videoCode
        binding.videoPlayer.videoCode = videoCode

        ViewCompat.setOnApplyWindowInsetsListener(binding.videoPlayer) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = navBar.top
            }
            WindowInsetsCompat.CONSUMED
        }
        orientationManager = OrientationManager(requireActivity(), this)
        lifecycle.addObserver(orientationManager)
        binding.videoPlayer.orientationManager = orientationManager
        initViewPager()
        initHKeyframe()
        viewModel.getHanimeVideo(videoCode)
        Log.i("video_ui", "initData: $videoCode")

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (Jzvd.backPress()) {
                    // 由backPress()处理，无内部逻辑
                } else {
                    // 没处理，交由系统默认行为
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun bindDataObservers() {
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
                                binding.videoPlayer.startButton.setOnClickListener {
                                    showShortToast(R.string.fail_to_get_video_link)
                                    requireContext().browse(getHanimeVideoLink(videoCode))
                                }
                            } else {
                                binding.videoPlayer.setUp(
                                    HanimeDataSource(state.info.title, state.info.videoUrls),
                                    Jzvd.SCREEN_NORMAL, kernel
                                )
                            }
                            binding.videoPlayer.posterImageView.load(state.info.coverUrl) {
                                crossfade(true)
                            }
                            if (!fromDownload) {
                                val entity = WatchHistoryEntity(
                                    state.info.coverUrl,
                                    state.info.title,
                                    state.info.uploadTimeMillis,
                                    Clock.System.now().toEpochMilliseconds(),
                                    videoCode
                                )
                                viewModel.insertWatchHistoryWithCover(entity)
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
                    binding.videoPlayer.hKeyframe = it
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

    override fun onDestroyView() {
        super.onDestroyView()
        Jzvd.releaseAllVideos()
    }

    override fun onOrientationChanged(orientation: OrientationManager.ScreenOrientation) {
        if (Jzvd.CURRENT_JZVD != null
            && (binding.videoPlayer.state == Jzvd.STATE_PLAYING || binding.videoPlayer.state == Jzvd.STATE_PAUSE)
            && binding.videoPlayer.screen != Jzvd.SCREEN_TINY
            && Jzvd.FULLSCREEN_ORIENTATION != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ) {
            if (orientation.isLandscape && binding.videoPlayer.screen == Jzvd.SCREEN_NORMAL) {
                changeScreenFullLandscape(orientation)
            } else if (orientation === OrientationManager.ScreenOrientation.PORTRAIT
                && binding.videoPlayer.screen == Jzvd.SCREEN_FULLSCREEN
            ) {
                changeScreenNormal()
            }
        }
    }

    private fun changeScreenNormal() {
        if (binding.videoPlayer.screen == Jzvd.SCREEN_FULLSCREEN) {
            binding.videoPlayer.gotoNormalScreen()
        }
    }

    private fun changeScreenFullLandscape(orientation: OrientationManager.ScreenOrientation) {
        if (binding.videoPlayer.screen != Jzvd.SCREEN_FULLSCREEN) {
            if (System.currentTimeMillis() - Jzvd.lastAutoFullscreenTime > 2000) {
                binding.videoPlayer.autoFullscreen(orientation)
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
                addFragment { CommentFragment().makeBundle(COMMENT_TYPE to VIDEO_COMMENT_PREFIX) }
            }
        }

        binding.videoTl.attach(binding.videoVp) { tab, position ->
            tab.setText(tabNameArray[position])
        }
    }

    private fun initHKeyframe() {
        binding.videoPlayer.onGoHomeClickListener = {
            if (context is MainActivity && resources.getBoolean(R.bool.isTablet)) {
                findNavController().popBackStack()
            }
            requireContext().startActivity<MainActivity>()
        }
        binding.videoPlayer.onKeyframeClickListener = { v ->
            binding.videoPlayer.clickHKeyframe(v)
        }
        binding.videoPlayer.onKeyframeLongClickListener = {
            val mi: JZMediaInterface? = binding.videoPlayer.mediaInterface
            if (mi != null && !mi.isPlaying) {
                val currentPosition = binding.videoPlayer.currentPositionWhenPlaying
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val intent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                Intent(MainActivity.ACTION_TOGGLE_PLAY).setPackage(requireContext().packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val icon = Icon.createWithResource(requireContext(), R.drawable.ic_baseline_play_circle_outline_24)
            val action = RemoteAction(
                icon,
                getString(R.string.play_pause),
                getString(R.string.play_pause),
                intent
            )
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setActions(listOf(action))
                .build()
            requireActivity().enterPictureInPictureMode(params)
        }
    }

    fun shouldEnterPip(): Boolean {
        val isPlaying = (Jzvd.CURRENT_JZVD?.mediaInterface as? ExoMediaKernel)?.isPlaying ?: false
        Log.i("pipmode","enter pip mode?isPlaying:$isPlaying\n")
        return  !requireActivity().isInPictureInPictureMode
    }
    fun onPipModeChanged(isInPip: Boolean) {
        binding.videoTl.isVisible = !isInPip
        binding.videoVp.isUserInputEnabled = !isInPip
        binding.videoVp.isVisible = !isInPip
        binding.videoPlayer.setControlsVisible(!isInPip)
        binding.videoPlayer.centerSurfaceInPip(isInPip, binding.videoPlayer.screen == Jzvd.SCREEN_FULLSCREEN)
    }
}
