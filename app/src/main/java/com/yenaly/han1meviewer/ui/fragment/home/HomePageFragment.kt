package com.yenaly.han1meviewer.ui.fragment.home

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.yenaly.han1meviewer.ADVANCED_SEARCH_MAP
import com.yenaly.han1meviewer.HAdvancedSearch
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.advancedSearchMapOf
import com.yenaly.han1meviewer.databinding.FragmentHomePageBinding
import com.yenaly.han1meviewer.logic.model.HomePage
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.StateLayoutMixin
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.adapter.AnnouncementCardAdapter
import com.yenaly.han1meviewer.ui.adapter.HanimeVideoRvAdapter
import com.yenaly.han1meviewer.ui.adapter.RvWrapper.Companion.wrappedWith
import com.yenaly.han1meviewer.ui.adapter.VideoColumnTitleAdapter
import com.yenaly.han1meviewer.ui.fragment.IToolbarFragment
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.fragment.funny.FunnyTouchListener
import com.yenaly.han1meviewer.ui.viewmodel.CheckInCalendarViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.han1meviewer.util.addUpdateListener
import com.yenaly.han1meviewer.util.checkBadGuy
import com.yenaly.han1meviewer.util.colorTransition
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.application
import com.yenaly.yenaly_libs.utils.getSpValue
import com.yenaly.yenaly_libs.utils.putSpValue
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/12 012 12:31
 */
class HomePageFragment : YenalyFragment<FragmentHomePageBinding>(),
    IToolbarFragment<MainActivity>, StateLayoutMixin {

    companion object {
        private val animInterpolator = FastOutSlowInInterpolator()
        private const val ANIM_DURATION = 300L
    }

    val viewModel by activityViewModels<MainViewModel>()
    val checkInViewModel by activityViewModels<CheckInCalendarViewModel>()
    private val latestHanimeAdapter = HanimeVideoRvAdapter()
    private val latestReleaseAdapter = HanimeVideoRvAdapter()
    private val latestUploadAdapter = HanimeVideoRvAdapter()
    private val chineseSubtitleAdapter = HanimeVideoRvAdapter()
    private val hanimeTheyWatchedAdapter = HanimeVideoRvAdapter()
    private val hanimeCurrentAdapter = HanimeVideoRvAdapter()
    private val hotHanimeMonthlyAdapter = HanimeVideoRvAdapter()
    private val animeShortAdapter = HanimeVideoRvAdapter()
    private val motionAnimeAdapter = HanimeVideoRvAdapter()
    private val thereDWorkAdapter = HanimeVideoRvAdapter()
    private val douJinWorkAdapter = HanimeVideoRvAdapter()
    private val cosplayAdapter = HanimeVideoRvAdapter()
    private val newAnimeTrailerAdapter = HanimeVideoRvAdapter()
    private val someFunnyTouchListener = FunnyTouchListener(application) {
        showShortToast("WTF?")
    }
    private var announcementCardAdapter: AnnouncementCardAdapter? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val concatAdapter by lazy {
        ConcatAdapter(
            VideoColumnTitleAdapter(requireContext(),R.string.latest_hanime).apply {
                onMoreHanimeListener = {
                    showSearchFragment(advancedSearchMapOf(HAdvancedSearch.GENRE to "裏番"))
                }
            },
            latestHanimeAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "latestHanime"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] = lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.latest_release).apply {
                onMoreHanimeListener = {
                    showSearchFragment(advancedSearchMapOf(HAdvancedSearch.SORT to "最新上市"))
                }
            },
            latestReleaseAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "latestRelease"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] = lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.latest_upload).apply {
                onMoreHanimeListener = {
                    showSearchFragment(advancedSearchMapOf(HAdvancedSearch.SORT to "最新上傳"))
                }
            },
            latestUploadAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "latestUpload"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] = lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.chinese_subtitle).apply {
                onMoreHanimeListener = {
                    showSearchFragment(advancedSearchMapOf(HAdvancedSearch.TAGS to hashMapOf("video_attributes" to "中文字幕")))
                }
            },
            chineseSubtitleAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "chineseSubtitle"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] = lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.they_watched).apply {
                onMoreHanimeListener = {
                    showSearchFragment(advancedSearchMapOf(HAdvancedSearch.SORT to "他們在看"))
                }
            },
            hanimeTheyWatchedAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "hanimeTheyWatched"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] = lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.ranking_today).apply {
                onMoreHanimeListener = {
                    showSearchFragment(advancedSearchMapOf(HAdvancedSearch.SORT to "本日排行"))
                }
            },
            hanimeCurrentAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "hanimeCurrent"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] = lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.ranking_this_month).apply {
                onMoreHanimeListener = {
                    showSearchFragment(advancedSearchMapOf(HAdvancedSearch.SORT to "本月排行"))
                }
            },
            hotHanimeMonthlyAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)}
                .apply {
                    doOnWrap {
                        val key = "hotHanimeMonthly"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] = lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.new_anime_trailers).apply {
                onMoreHanimeListener = {
                    findNavController().navigate(R.id.action_nv_home_page_to_nv_preview)
                }
            },
            newAnimeTrailerAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "newAnimeTrailer"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] =
                                    lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(), R.string.category_instant_noodle).apply {
                onMoreHanimeListener = {
                    showSearchFragment(
                        advancedSearchMapOf(
                            HAdvancedSearch.GENRE to "泡麵番",
                            HAdvancedSearch.SORT to "最新上傳"
                        )
                    )
                }
            },
            animeShortAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "animeShort"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] =
                                    lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.category_motion_anime).apply {
                onMoreHanimeListener = {
                    showSearchFragment(
                        advancedSearchMapOf(
                            HAdvancedSearch.GENRE to "Motion Anime",
                            HAdvancedSearch.SORT to "最新上傳"
                        )
                    )
                }
            },
            motionAnimeAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "motionAnime"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] =
                                    lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.category_3d_animation).apply {
                onMoreHanimeListener = {
                    showSearchFragment(
                        advancedSearchMapOf(
                            HAdvancedSearch.GENRE to "3DCG",
                            HAdvancedSearch.SORT to "最新上傳"
                        )
                    )
                }
            },
            thereDWorkAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "thereDWork"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] =
                                    lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.animation_2d).apply {
                onMoreHanimeListener = {
                    showSearchFragment(
                        advancedSearchMapOf(
                            HAdvancedSearch.GENRE to "2D動畫",
                            HAdvancedSearch.SORT to "最新上傳"
                        )
                    )
                }
            },
            douJinWorkAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "animation2d"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] =
                                    lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
            VideoColumnTitleAdapter(requireContext(),R.string.category_cosplay).apply {
                onMoreHanimeListener = {
                    showSearchFragment(
                        advancedSearchMapOf(
                            HAdvancedSearch.GENRE to "Cosplay",
                            HAdvancedSearch.SORT to "最新上傳"
                        )
                    )
                }
            },
            cosplayAdapter
                .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
                .apply {
                    doOnWrap {
                        val key = "cosplay"
                        val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                        val pos = viewModel.horizontalScrollPositions[key] ?: 0
                        post { lm.scrollToPositionWithOffset(pos, 0) }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                viewModel.horizontalScrollPositions[key] =
                                    lm.findFirstVisibleItemPosition()
                            }
                        })
                    }
                },
        )
    }

    /**
     * 用於判斷是否需要 setExpanded，防止重複喚出 AppBar
     */
    private var isAfterRefreshing = false

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomePageBinding {
        return FragmentHomePageBinding.inflate(inflater, container, false)
    }

    /**
     * 初始化数据
     */
    override fun initData(savedInstanceState: Bundle?) {
        (activity as MainActivity).setupToolbar()
        binding.state.init()
        checkBadGuy(requireContext(),R.raw.akarin)
        view?.setOnTouchListener(someFunnyTouchListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            easterEgg()
        }

        binding.rv.layoutManager = LinearLayoutManager(context)
        binding.rv.adapter = concatAdapter
        binding.rv.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(binding.rv) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        binding.homePageSrl.apply {
            setOnRefreshListener {
                isAfterRefreshing = false
                // will enter here firstly. cuz the flow's def value is Loading.
                viewModel.getHomePage()
                viewModel.loadAnnouncements(true)
            }
            setEnableLoadMore(false)
        }
        binding.header.apply {
            val accentColor = MaterialColors
                .getColor(this,androidx.appcompat.R.attr.colorPrimary)
            val backgroundColor = MaterialColors
                .getColor(this, com.google.android.material.R.attr.colorOnPrimary)

            setColorSchemeColors(accentColor)
            setProgressBackgroundColorSchemeColor(backgroundColor)
        }
        onBackPressedCallback = object : OnBackPressedCallback(false) { // 初始禁用
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onResume() {
        super.onResume()
        // 只有当 HomePageFragment 在最前台时，才启用自己的返回回调
        if (isAdded) {
            onBackPressedCallback.isEnabled = true
        }
        (activity as? ToolbarHost)?.hideToolbar()
    }

    override fun onPause() {
        super.onPause()
        // 只要 HomePageFragment 被覆盖或离开前台，立刻禁用回调
        onBackPressedCallback.isEnabled = false
    }

    @SuppressLint("SetTextI18n")
    override fun bindDataObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.homePageFlow.collect { state ->
                    binding.rv.isGone = state !is WebsiteState.Success
                    binding.banner.isVisible =
                        state is WebsiteState.Success || binding.banner.isVisible // 只有在刚开始的时候是不可见的
                    if (!isAfterRefreshing) {
                        binding.appBar.setExpanded(state is WebsiteState.Success, true)
                    }
                    when (state) {
                        is WebsiteState.Loading -> {
                            binding.homePageSrl.autoRefresh()
                            binding.rv.isGone = latestHanimeAdapter.items.isEmpty()
                        }

                        is WebsiteState.Success -> {
                            isAfterRefreshing = true
                            binding.homePageSrl.finishRefresh()
                            initBanner(state.info)
                            latestHanimeAdapter.submitList(state.info.latestHanime)
                            latestUploadAdapter.submitList(state.info.latestUpload)
                            hotHanimeMonthlyAdapter.submitList(state.info.hotHanimeMonthly)
                            hanimeCurrentAdapter.submitList(state.info.hanimeCurrent)
                            hanimeTheyWatchedAdapter.submitList(state.info.hanimeTheyWatched)
                            latestReleaseAdapter.submitList(state.info.latestRelease)
                            chineseSubtitleAdapter.submitList(state.info.chineseSubtitle)
                            newAnimeTrailerAdapter.submitList(state.info.newAnimeTrailer)
                            animeShortAdapter.submitList(state.info.animeShort)
                            motionAnimeAdapter.submitList(state.info.motionAnime)
                            thereDWorkAdapter.submitList(state.info.thereDWork)
                            douJinWorkAdapter.submitList(state.info.douJinWork)
                            cosplayAdapter.submitList(state.info.cosplay)
                            binding.state.showContent()
                            initAnnouncements()
                        }

                        is WebsiteState.Error -> {
                            binding.homePageSrl.finishRefresh()
                            binding.state.showError(state.throwable)
                            val priorityZero = viewModel.announcements.value
                                ?.filter { it.priority == 0 && (it.title.isNotBlank() || it.content.isNotBlank()) }
                                .orEmpty()
                            if (priorityZero.isNotEmpty()) {
                                val message = priorityZero.joinToString("\n\n") { it.content }
                                val title = priorityZero.firstOrNull()?.title ?: "公告"

                                AlertDialog.Builder(requireContext())
                                    .setTitle(title)
                                    .setMessage(message)
                                    .setPositiveButton("确定", null)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.rv.adapter = null
        binding.rv.layoutManager = null
        super.onDestroyView()
    }

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//    }

    private fun initBanner(info: HomePage) {
        info.banner?.let { banner ->
            binding.tvBannerTitle.text = banner.title
            binding.tvBannerDesc.text = banner.description
            binding.cover.load(banner.picUrl) {
                crossfade(true)
                allowHardware(false)
                target(
                    onStart = binding.cover::setImageDrawable,
                    onError = binding.cover::setImageDrawable,
                    onSuccess = {
                        binding.cover.setImageDrawable(it)
                        it.toBitmapOrNull()?.let(Palette::Builder)?.generate { p ->
                            p?.let(::handlePalette)
                        }
                    }
                )
            }
            binding.btnBanner.isEnabled = banner.videoCode != null
            binding.btnBanner.setOnClickListener {
                banner.videoCode?.let { videoCode ->
                    (requireActivity() as? MainActivity)?.showVideoDetailFragment(videoCode)
                }
            }
        }
    }

    // #issue-160: 修复字段销毁后调用引发的错误
    private fun handlePalette(p: Palette) {
        bindingOrNull?.let { binding ->
            val lightVibrant = p.getLightVibrantColor(Color.RED)

            val buttonBgColor =
                p.darkVibrantSwatch?.rgb ?: p.darkMutedSwatch?.rgb ?: Color.TRANSPARENT
            // 动态actionbar颜色会让浅色很丑
//            val darkVibrantForContentScrim =
//                p.darkVibrantSwatch?.rgb ?: p.darkMutedSwatch?.rgb ?: p.lightVibrantSwatch?.rgb
//                ?: p.lightMutedSwatch?.rgb ?: Color.BLACK
//            binding.collapsingToolbar.setContentScrimColor(darkVibrantForContentScrim)
            binding.btnBanner.background = GradientDrawable().apply {
                colors = intArrayOf(Color.TRANSPARENT, buttonBgColor)
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
            }
            binding.ivBanner.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            colorTransition(
                fromColor = (binding.aColor.background as ColorDrawable).color,
                toColor = lightVibrant
            ) {
                interpolator = animInterpolator
                duration = ANIM_DURATION
                addUpdateListener(this@HomePageFragment) {
                    val color = it.animatedValue as Int
                    binding.aColor.setBackgroundColor(color)
                }
            }
        }
    }

//    private fun showSearchFragment(advancedSearchMap: AdvancedSearchMap) {
//        startActivity<SearchActivity>(ADVANCED_SEARCH_MAP to advancedSearchMap)
//    }

    private fun showSearchFragment(advancedSearchMap: Map<HAdvancedSearch, Any>) {
        val bundleMap = HashMap<String, Serializable>().apply {
            advancedSearchMap.forEach { (key, value) ->
                if (value is Serializable) {
                    this[key.name] = value
                } else {
                    throw IllegalArgumentException("Value for ${key.name} is not Serializable.")
                }
            }
        }
        Log.i("advancedSearchMap",bundleMap.toString())

        findNavController().navigate(
            R.id.searchFragment,
            bundleOf(ADVANCED_SEARCH_MAP to bundleMap)
        )
    }




    @RequiresApi(Build.VERSION_CODES.S)
    private var easterEggCount = 1f

    @RequiresApi(Build.VERSION_CODES.S)
    private fun easterEgg() {
        binding.cover.setOnClickListener {
            binding.cover.setRenderEffect(
                RenderEffect.createBlurEffect(
                    easterEggCount,
                    easterEggCount,
                    Shader.TileMode.CLAMP
                )
            )
            easterEggCount++
        }
        binding.cover.setOnLongClickListener {
            binding.cover.setRenderEffect(null)
            easterEggCount = 1f
            true
        }
    }

    override fun MainActivity.setupToolbar() {
        val toolbar = this@HomePageFragment.binding.toolbar
        setSupportActionBar(toolbar)
        this@HomePageFragment.addMenu(R.menu.menu_main_toolbar, viewLifecycleOwner) { item ->
            when (item.itemId) {
                R.id.tb_search -> {
                    findNavController().navigate(R.id.action_home_to_search)
                    return@addMenu true
                }

                R.id.tb_previews -> {
                    findNavController().navigate(R.id.nv_preview)
                    return@addMenu true
                }
            }
            return@addMenu item.onNavDestinationSelected(navController)
        }

        toolbar.setupWithMainNavController()
    }
    fun Fragment.showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_to_exit))
            .setMessage(getString(R.string.finished_masturbating))
            .setNegativeButton(getString(R.string.do_more)) { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton(getString(R.string.checkout_exit)) { dialog, _ ->
                checkInViewModel.incrementCheckIn(java.time.LocalDate.now())
                requireActivity().finish()
            }
            .setPositiveButton(getString(R.string.exit)) { _, _ ->
                requireActivity().finish()
            }
            .show()
    }

    private fun initAnnouncements() {
        val lastDismissTime = getSpValue("last_dismiss_time",0L,"setting_pref")
        val shouldShowAnno = System.currentTimeMillis() - lastDismissTime > 24*60*60*1000L
        if (!shouldShowAnno) return
        if (view == null) return
        viewModel.announcements.observe(viewLifecycleOwner) { sortedList ->
            if (sortedList.isNotEmpty()) {
                announcementCardAdapter?.let {
                    concatAdapter.removeAdapter(it)
                }
                announcementCardAdapter = AnnouncementCardAdapter(
                    sortedList,
                    onClick = { item ->
                        showAnnouncementDialog(
                            requireContext(),
                            title = item.title,
                            content = item.getHighlightedContent(),
                            imageUrl = item.imageUrl,
                            positiveText = item.positiveText,
                            positiveAction = {  },
                            negativeText = item.negativeText,
                            negativeAction = {  },
                            )
                    },
                    onClose = {
                        putSpValue("last_dismiss_time", System.currentTimeMillis(),"setting_pref")
                        announcementCardAdapter?.let {
                            concatAdapter.removeAdapter(it)
                            announcementCardAdapter = null
                        }
                    }
                )
                concatAdapter.addAdapter(0, announcementCardAdapter!!)
            }else{
                announcementCardAdapter?.let {
                    concatAdapter.removeAdapter(it)
                    announcementCardAdapter = null
                }
            }
        }
 //       viewModel.loadAnnouncements()
    }
    fun showAnnouncementDialog(
        context: Context,
        title: String,
        content: Spanned,
        imageUrl: String? = null,
        positiveText: String? = null,
        positiveAction: (() -> Unit)? = null,
        negativeText: String? = null,
        negativeAction: (() -> Unit)? = null
    ) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_announcement, null, false)

        val tvTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val tvContent = dialogView.findViewById<TextView>(R.id.dialogContent)
        val ivImage = dialogView.findViewById<ShapeableImageView>(R.id.dialogImage)
        val positiveText = positiveText ?: context.getString(R.string.i_understand)

        tvTitle.text = title
        tvTitle.visibility = View.VISIBLE

        tvContent.text = content
        tvContent.movementMethod = LinkMovementMethod.getInstance()
        tvContent.highlightColor = Color.TRANSPARENT

        if (!imageUrl.isNullOrBlank()) {
            ivImage.visibility = View.VISIBLE
            ivImage.load(imageUrl) {
                placeholder(R.drawable.akarin)
                error(R.drawable.baseline_error_outline_24)
            }
            ivImage.setOnClickListener {
                val fullScreenDialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                val fullImageView = ImageView(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    load(imageUrl)
                }

                fullScreenDialog.setContentView(fullImageView)

                fullImageView.scaleX = 0.8f
                fullImageView.scaleY = 0.8f
                fullImageView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()

                fullImageView.setOnClickListener {
                    fullImageView.animate()
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(200)
                        .withEndAction { fullScreenDialog.dismiss() }
                        .start()
                }

                fullImageView.setOnLongClickListener {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(getString(R.string.save_image_confirm))
                        .setPositiveButton(getString(R.string.sure)) { _, _ ->
                            saveImageToGallery(imageUrl)
                        }
                        .setNegativeButton(getString(R.string.cancel)){ dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                    true
                }

                fullScreenDialog.show()
            }
        }

        val builder = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
        builder.setPositiveButton(positiveText) { _, _ ->
            positiveAction?.invoke()
        }

        if (!negativeText.isNullOrBlank()) {
            builder.setNegativeButton(negativeText) { _, _ ->
                negativeAction?.invoke()
            }
        }

        builder.show()
    }
    private fun saveImageToGallery(imageUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val loader = ImageLoader(requireContext())
            val request = ImageRequest.Builder(requireContext())
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
            result?.let { bitmap ->
                val filename = "IMG_${System.currentTimeMillis()}.jpg"
                val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val uri = requireContext().contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    uri?.let { requireContext().contentResolver.openOutputStream(it) }
                } else {
                    val file = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        filename
                    )
                    FileOutputStream(file)
                }
                fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                withContext(Dispatchers.Main) {
                    showShortToast(getString(R.string.saved))
                }
            }
        }
    }
}