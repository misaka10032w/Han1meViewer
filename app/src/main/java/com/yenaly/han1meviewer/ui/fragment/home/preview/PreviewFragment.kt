package com.yenaly.han1meviewer.ui.fragment.home.preview

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.color.MaterialColors
import com.yenaly.han1meviewer.DATE_CODE
import com.yenaly.han1meviewer.PREVIEW_COMMENT_PREFIX
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.FragmentPreviewBinding
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.pienization
import com.yenaly.han1meviewer.ui.adapter.HanimePreviewNewsRvAdapter
import com.yenaly.han1meviewer.ui.adapter.HanimePreviewTourRvAdapter
import com.yenaly.han1meviewer.ui.view.CenterLinearLayoutManager
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.han1meviewer.ui.viewmodel.PreviewViewModel
import com.yenaly.han1meviewer.util.addUpdateListener
import com.yenaly.han1meviewer.util.colorTransition
import com.yenaly.han1meviewer.util.toColorStateList
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.appScreenWidth
import com.yenaly.yenaly_libs.utils.unsafeLazy
import com.yenaly.yenaly_libs.utils.view.AppBarLayoutStateChangeListener
import com.yenaly.yenaly_libs.utils.view.innerRecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PreviewFragment : YenalyFragment<FragmentPreviewBinding>() {

    companion object {
        private const val ANIM_DURATION = 300L
        private val animInterpolator = FastOutSlowInInterpolator()
    }

    val viewModel by viewModels<PreviewViewModel>()

    private val dateUtils = DateUtils()
    private val badgeDrawable by unsafeLazy { BadgeDrawable.create(requireContext()) }

    private var shouldTriggerScroll = false

    private val tourSimplifiedAdapter = HanimePreviewTourRvAdapter()
    private val colorPrimary by lazy {
        MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorPrimary,
            Color.BLACK
        )
    }
    private val colorOnPrimary by lazy {
        MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorOnPrimary,
            Color.WHITE
        )
    }
    private val newsAdapter = HanimePreviewNewsRvAdapter()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPreviewBinding =
        FragmentPreviewBinding.inflate(inflater, container, false)


    @androidx.annotation.OptIn(ExperimentalBadgeUtils::class)
    override fun bindDataObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.previewFlow.collect { state ->
                    binding.nsvPreview.isGone = state !is WebsiteState.Success
                    binding.appBar.setExpanded(state is WebsiteState.Success, true)
                    when (state) {
                        is WebsiteState.Error -> {
                            (requireActivity() as AppCompatActivity).supportActionBar?.title =
                                state.throwable.pienization
                        }
                        is WebsiteState.Loading -> {
                            binding.fabPrevious.isEnabled = false
                            binding.fabNext.isEnabled = false
                        }
                        is WebsiteState.Success -> {
                            binding.vpNews.setCurrentItem(0, false)
                            (requireActivity() as AppCompatActivity).supportActionBar?.title =
                                getString(
                                    R.string.latest_hanime_list_monthly,
                                    dateUtils.current.format(DateUtils.NORMAL_FORMAT))

                            binding.fabPrevious.apply {
                                isVisible = state.info.hasPrevious
                                isEnabled = state.info.hasPrevious
                                text = dateUtils.prevDate.format(DateUtils.NORMAL_FORMAT)
                            }
                            binding.fabNext.apply {
                                isVisible = state.info.hasNext
                                isEnabled = state.info.hasNext
                                text = dateUtils.nextDate.format(DateUtils.NORMAL_FORMAT)
                            }
                            binding.cover.load(state.info.headerPicUrl) {
                                crossfade(true)
                                allowHardware(false)
                                target(
                                    onStart = binding.cover::setImageDrawable,
                                    onError = binding.cover::setImageDrawable,
                                    onSuccess = {
                                        binding.cover.setImageDrawable(it)
                                        it.toBitmapOrNull()?.let(Palette::Builder)?.generate { p ->
                                            p?.let(::handleHeaderPalette)
                                        }
                                    }
                                )
                            }
                            tourSimplifiedAdapter.submitList(state.info.latestHanime)
                            delay(100)
                            handleToolbarColor(0)
                            newsAdapter.submitList(state.info.previewInfo)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            PreviewCommentPrefetcher.Companion.here().commentFlow.collectLatest { comments ->
                badgeDrawable.apply {
                    isVisible = true
                    number = comments.size
                    badgeGravity = BadgeDrawable.TOP_START
                }
                BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.toolbar, R.id.tb_comment)
            }
        }
    }

    override fun initData(savedInstanceState: Bundle?) {

        // Toolbar
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_preview_toolbar, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.tb_comment -> {
                        val args = Bundle().apply {
                            putString("date", dateUtils.current.format(DateUtils.NORMAL_FORMAT))
                            putString(DATE_CODE, dateUtils.current.format(DateUtils.FORMATTED_FORMAT))
                        }
                        findNavController().navigate(R.id.action_nv_preview_to_nv_preview_comment, args)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.toolbar.setTitleTextColor(colorOnPrimary)
        (requireActivity() as AppCompatActivity).supportActionBar?.let {
            it.title = null
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeActionContentDescription(R.string.back)
        }

        PreviewCommentPrefetcher.Companion.here().tag(PreviewCommentPrefetcher.Scope.PREVIEW_ACTIVITY)

        dateUtils.current.format(DateUtils.FORMATTED_FORMAT).let { format ->
            viewModel.getHanimePreview(format)
            loadComments(format)
        }

        // 绑定按钮
        binding.fabPrevious.setOnClickListener {
            dateUtils.toPrevDate().format(DateUtils.FORMATTED_FORMAT).let { format ->
                viewModel.getHanimePreview(format)
                loadComments(format)
            }
        }
        binding.fabNext.setOnClickListener {
            dateUtils.toNextDate().format(DateUtils.FORMATTED_FORMAT).let { format ->
                viewModel.getHanimePreview(format)
                loadComments(format)
            }
        }

        // ViewPager + RecyclerView 配置
        binding.vpNews.adapter = newsAdapter
        binding.rvTourSimplified.apply {
            // 每次新建 LayoutManager，避免被复用
            val layoutManager = object : CenterLinearLayoutManager(requireContext()) {
                init {
                    orientation = HORIZONTAL
                    reverseLayout = false
                }

                override fun scrollVerticallyBy(
                    dy: Int,
                    recycler: RecyclerView.Recycler?,
                    state: RecyclerView.State?,
                ): Int {
                    if (!binding.vpNews.isInTouchMode) {
                        if (dy > 0) {
                            binding.appBar.setExpanded(false, true)
                        } else {
                            binding.appBar.setExpanded(true, true)
                        }
                    }
                    return super.scrollVerticallyBy(dy, recycler, state)
                }
            }

            this.layoutManager = layoutManager
            adapter = tourSimplifiedAdapter
            clipToPadding = false

            // 处理左右 padding
            ViewCompat.setOnApplyWindowInsetsListener(this) { v, _ ->
                val elementWidth = v.resources.getDimension(R.dimen.video_cover_simplified_width_small)
                val padding = appScreenWidth / 2f - elementWidth / 2f
                v.updatePadding(left = padding.toInt(), right = padding.toInt())
                WindowInsetsCompat.CONSUMED
            }

            // 每次新建并 attach SnapHelper
            val snapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (shouldTriggerScroll) {
                            val view = snapHelper.findSnapView(layoutManager)
                            val position = view?.let(::getChildAdapterPosition)
                                ?: RecyclerView.NO_POSITION
                            binding.vpNews.setCurrentItem(position, false)
                        }
                        shouldTriggerScroll = true
                    }
                }
            })
        }


        tourSimplifiedAdapter.setOnItemClickListener { _, _, position ->
            binding.vpNews.setCurrentItem(position, false)
            binding.appBar.setExpanded(false, true)
        }

        binding.vpNews.innerRecyclerView?.apply {
            isNestedScrollingEnabled = false
            clipToPadding = false
            ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                v.updatePadding(bottom = systemBars.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }
        binding.vpNews.offscreenPageLimit = 1
        binding.vpNews.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                shouldTriggerScroll = false
                binding.rvTourSimplified.smoothScrollToPosition(position)
                handleToolbarColor(position)
            }
        })

        initAnimation()

        bindDataObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PreviewCommentPrefetcher.Companion.bye(PreviewCommentPrefetcher.Scope.PREVIEW_ACTIVITY)
    }

    private fun loadComments(currentFormat: String) {
        PreviewCommentPrefetcher.Companion.here()
            .fetch(PREVIEW_COMMENT_PREFIX, currentFormat)
    }

    private fun handleToolbarColor(index: Int) {
        val data = tourSimplifiedAdapter.getItem(index)?.coverUrl
        val request = ImageRequest.Builder(requireContext())
            .data(data)
            .allowHardware(false)
            .target(
                onError = {

                },
                onSuccess = {
                    it.toBitmapOrNull()?.let(Palette::Builder)?.generate { p ->
                        p?.let(::handleToolbarPalette)
                    }
                }
            )
            .build()
        requireContext().imageLoader.enqueue(request)
    }

    private fun handleToolbarPalette(p: Palette) {
        val darkMuted =
            p.darkMutedSwatch?.rgb ?: p.darkVibrantSwatch?.rgb ?: p.lightVibrantSwatch?.rgb
            ?: p.lightMutedSwatch?.rgb ?: Color.BLACK
        colorTransition(
            fromColor = (binding.collapsingToolbar.contentScrim as ColorDrawable).color,
            toColor = ColorUtils.blendARGB(darkMuted, Color.BLACK, 0.3f)
        ) {
            duration = ANIM_DURATION
            interpolator = animInterpolator
            addUpdateListener(lifecycle) {
                val value = it.animatedValue as Int
                binding.collapsingToolbar.setContentScrimColor(value)
            }
        }
        colorTransition(
            fromColor = (binding.llTour.background as? ColorDrawable)?.color ?: Color.TRANSPARENT,
            toColor = darkMuted
        ) {
            duration = ANIM_DURATION
            interpolator = animInterpolator
            addUpdateListener(lifecycle) {
                val value = it.animatedValue as Int
                binding.llTour.setBackgroundColor(value)
            }
        }
    }

    private fun handleHeaderPalette(p: Palette) {
        val lightVibrant = p.getLightVibrantColor(colorPrimary)
        val per70lightVibrantStateList =
            ColorUtils.setAlphaComponent(lightVibrant, 0xB3).toColorStateList()
        binding.fabPrevious.backgroundTintList = per70lightVibrantStateList
        binding.fabNext.backgroundTintList = per70lightVibrantStateList
        val titleTextColorStateList =
            (p.lightVibrantSwatch?.titleTextColor ?: Color.BLACK).toColorStateList()
        binding.fabPrevious.iconTint = titleTextColorStateList
        binding.fabNext.iconTint = titleTextColorStateList
        binding.fabPrevious.setTextColor(titleTextColorStateList)
        binding.fabNext.setTextColor(titleTextColorStateList)
    }

    private fun initAnimation() {
        binding.appBar.addOnOffsetChangedListener(object : AppBarLayoutStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
                when (state) {
                    State.EXPANDED -> {
                        binding.fabPrevious.animate().translationX(0F).setDuration(ANIM_DURATION)
                            .setInterpolator(animInterpolator).start()
                        binding.fabNext.animate().translationX(0F).setDuration(ANIM_DURATION)
                            .setInterpolator(animInterpolator).start()
                    }

                    State.INTERMEDIATE -> {
                        binding.fabPrevious.animate().translationX(-500F).setDuration(
                            ANIM_DURATION
                        )
                            .setInterpolator(animInterpolator).start()
                        binding.fabNext.animate().translationX(500F).setDuration(ANIM_DURATION)
                            .setInterpolator(animInterpolator).start()
                    }

                    State.COLLAPSED -> {

                    }
                }
            }
        })
    }

    /**
     * 单纯给这个用的DateUtils
     */
    private class DateUtils {

        companion object {
            /**
             * 2022/2
             */
            val NORMAL_FORMAT = LocalDateTime.Companion.Format {
                year(); char('/'); monthNumber(Padding.NONE)
            }

            /**
             * 202202
             */
            val FORMATTED_FORMAT = LocalDateTime.Companion.Format {
                year(); monthNumber()
            }
        }

        // 當前顯示的日期
        var current: LocalDateTime = currentDate
            private set

        @OptIn(ExperimentalTime::class)
        val currentDate: LocalDateTime
            get() = Clock.System.now().toLocalDateTime(TimeZone.Companion.currentSystemDefault())

        @OptIn(ExperimentalTime::class)
        val prevDate: LocalDateTime
            get() {
                val instant = current.toInstant(TimeZone.Companion.currentSystemDefault())
                return instant.minus(1, DateTimeUnit.Companion.MONTH, TimeZone.Companion.currentSystemDefault())
                    .toLocalDateTime(TimeZone.Companion.currentSystemDefault())
            }

        @OptIn(ExperimentalTime::class)
        val nextDate: LocalDateTime
            get() {
                val instant = current.toInstant(TimeZone.Companion.currentSystemDefault())
                return instant.plus(1, DateTimeUnit.Companion.MONTH, TimeZone.Companion.currentSystemDefault())
                    .toLocalDateTime(TimeZone.Companion.currentSystemDefault())
            }

        fun toPrevDate(): LocalDateTime {
            current = prevDate
            return current
        }

        fun toNextDate(): LocalDateTime {
            current = nextDate
            return current
        }
    }
}