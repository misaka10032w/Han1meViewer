package com.yenaly.han1meviewer.ui.fragment.video

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.viewpager2.widget.ViewPager2
import coil.load
import coil.transform.CircleCropTransformation
import com.chad.library.adapter4.viewholder.DataBindingHolder
import com.ctetin.expandabletextviewlibrary.ExpandableTextView
import com.ctetin.expandabletextviewlibrary.app.LinkType
import com.itxca.spannablex.spannable
import com.lxj.xpopup.XPopup
import com.yenaly.han1meviewer.ADVANCED_SEARCH_MAP
import com.yenaly.han1meviewer.HAdvancedSearch
import com.yenaly.han1meviewer.HCacheManager
import com.yenaly.han1meviewer.HanimeResolution
import com.yenaly.han1meviewer.LOCAL_DATE_FORMAT
import com.yenaly.han1meviewer.Preferences.isAlreadyLogin
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_LAYOUT_MATCH_PARENT
import com.yenaly.han1meviewer.VIDEO_LAYOUT_WRAP_CONTENT
import com.yenaly.han1meviewer.VideoCoverSize
import com.yenaly.han1meviewer.databinding.FragmentVideoIntroductionBinding
import com.yenaly.han1meviewer.databinding.ItemVideoIntroductionBinding
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.getHanimeVideoDownloadLink
import com.yenaly.han1meviewer.getHanimeVideoLink
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.adapter.AdapterLikeDataBindingPage
import com.yenaly.han1meviewer.ui.adapter.BaseSingleDifferAdapter
import com.yenaly.han1meviewer.ui.adapter.HanimeVideoRvAdapter
import com.yenaly.han1meviewer.ui.adapter.RvWrapper.Companion.wrappedWith
import com.yenaly.han1meviewer.ui.adapter.VideoColumnTitleAdapter
import com.yenaly.han1meviewer.ui.fragment.PermissionRequester
import com.yenaly.han1meviewer.ui.fragment.PlaylistBottomSheetFragment
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.requestPostNotificationPermission
import com.yenaly.han1meviewer.util.setDrawableTop
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.han1meviewer.worker.HanimeDownloadWorker
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.copyToClipboard
import com.yenaly.yenaly_libs.utils.shareText
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.view.clickTrigger
import com.yenaly.yenaly_libs.utils.view.clickWithCondition
import com.yenaly.yenaly_libs.utils.view.findParent
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.format
import java.io.Serializable

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/18 018 21:09
 */
class VideoIntroductionFragment : YenalyFragment<FragmentVideoIntroductionBinding>() {

    companion object {
        private const val FAV = 1
        private const val PLAYLIST = 1 shl 1
        private const val SUBSCRIBE = 1 shl 2

        val COMPARATOR = object : DiffUtil.ItemCallback<HanimeVideo>() {
            override fun areItemsTheSame(oldItem: HanimeVideo, newItem: HanimeVideo): Boolean {
                return true
            }

            override fun areContentsTheSame(oldItem: HanimeVideo, newItem: HanimeVideo): Boolean {
                return false
            }

            override fun getChangePayload(oldItem: HanimeVideo, newItem: HanimeVideo): Any {
                var bitset = 0
                if (oldItem.isFav != newItem.isFav)
                    bitset = bitset or FAV
                if (!(oldItem.myList?.isSelectedArray contentEquals newItem.myList?.isSelectedArray))
                    bitset = bitset or PLAYLIST
                if (oldItem.artist?.isSubscribed != newItem.artist?.isSubscribed)
                    bitset = bitset or SUBSCRIBE
                return bitset
            }
        }
    }

    val viewModel: VideoViewModel by viewModels({ requireParentFragment() })


    private var checkedQuality: String? = null

    private val videoIntroAdapter = VideoIntroductionAdapter()
    private val playlistTitleAdapter by lazy {
        VideoColumnTitleAdapter(requireContext(),title = R.string.series_video, notifyWhenSet = true)
    }
    private val playlistAdapter = HanimeVideoRvAdapter(VIDEO_LAYOUT_WRAP_CONTENT)
    private val playlistWrapper = playlistAdapter
        .wrappedWith { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
        .apply {
            doOnWrap {
                val key = viewModel.videoCode
                val lm = layoutManager as? LinearLayoutManager ?: return@doOnWrap
                val pos = viewModel.horizontalScrollPositions[key] ?: 0
                post { lm.scrollToPositionWithOffset(pos, 0) }
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                        viewModel.horizontalScrollPositions[key] = lm.findFirstVisibleItemPosition()
                    }
                })
            }
        }

    private val relatedTitleAdapter by lazy {
        VideoColumnTitleAdapter(requireContext(),title = R.string.related_video)
    }
    private val relatedAdapter = HanimeVideoRvAdapter(VIDEO_LAYOUT_MATCH_PARENT)

    private var multi = ConcatAdapter()

//    private val layoutManager by unsafeLazy {
//        GridLayoutManager(context, 1).apply {
//            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
//                override fun getSpanSize(position: Int): Int {
//                    if (multi.getWrappedAdapterAndPosition(position).first === relatedAdapter) {
//                        return 1
//                    }
//                    return spanCount
//                }
//            }
//        }
//    }


    /**
     * 保证 submitList 不同时调用
     */
    private val mutex = Mutex()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentVideoIntroductionBinding {
        return FragmentVideoIntroductionBinding.inflate(inflater, container, false)
    }
    private lateinit var layoutManager: GridLayoutManager
    override fun initData(savedInstanceState: Bundle?) {
        layoutManager = createLayoutManager(1)
        binding.rvVideoIntro.adapter = multi
        binding.rvVideoIntro.addOnItemTouchListener(VideoIntroTouchListener())
        binding.rvVideoIntro.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(binding.rvVideoIntro) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = navBar.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val code = viewModel.videoCode
        viewModel.scrollPositionMap[code] = binding.rvVideoIntro.layoutManager?.onSaveInstanceState()
        viewModel.videoIntroDataMap[code] = viewModel.hanimeVideoFlow.value
        viewModel.videoIntroRestoredSet.remove(code)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val code = viewModel.videoCode
        if (!viewModel.restoreFromCacheIfExists(code)) {
            if (code != "-1"){
                viewModel.getHanimeVideo(code)
            }
        }
    }


    private fun createLayoutManager(spanCount: Int): GridLayoutManager {
        return GridLayoutManager(requireContext(), spanCount).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    if (multi.getWrappedAdapterAndPosition(position).first === relatedAdapter) {
                        return 1
                    }
                    return spanCount
                }
            }
        }
    }
    override fun bindDataObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.hanimeVideoStateFlow.collect { state ->
                    Log.i("video_ui", "bindDataObservers: $state")
                    binding.rvVideoIntro.isVisible = state is VideoLoadingState.Success
                    when (state) {
                        is VideoLoadingState.Error -> Unit

                        is VideoLoadingState.Loading -> Unit

                        is VideoLoadingState.Success -> {
                            val video = state.info
                            val code = viewModel.videoCode

                            if (viewModel.videoIntroRestoredSet.contains(code)) return@collect
                            val savedState = viewModel.scrollPositionMap[code]
                            multi = ConcatAdapter()
                            binding.rvVideoIntro.adapter = multi
                            layoutManager = createLayoutManager(video.relatedHanimes.eachGridCounts)
                            binding.rvVideoIntro.layoutManager = layoutManager

                            mutex.withLock {
                                videoIntroAdapter.submit(video)
                            }

                            multi.addAdapter(videoIntroAdapter)
                            val cached = viewModel.videoIntroDataMap[code]

                            if (video.playlist != null && !viewModel.fromDownload) {
                                val bottomSheet = PlaylistBottomSheetFragment()
                                playlistTitleAdapter.subtitle = video.playlist.playlistName
                                multi.addAdapter(playlistTitleAdapter)
                                multi.addAdapter(playlistWrapper)
                                if (cached?.playlist?.video != video.playlist.video) {
                                    playlistAdapter.submitList(video.playlist.video)
                                }
                                viewModel.setVideoList(video.playlist.video)

                                playlistTitleAdapter.apply {
                                    onMoreHanimeListener = {
                                        bottomSheet.show(parentFragmentManager,
                                            PlaylistBottomSheetFragment.TAG)
                                    }
                                }
                            }

                            if (!viewModel.fromDownload) {
                                multi.addAdapter(relatedTitleAdapter)
                                multi.addAdapter(relatedAdapter)
                                if (cached?.relatedHanimes != video.relatedHanimes) {
                                    relatedAdapter.submitList(video.relatedHanimes)
                                }
                            }

                            viewModel.videoIntroDataMap[code] = video
                            viewModel.videoIntroRestoredSet.add(code)
                            savedState?.let { state ->
                                binding.rvVideoIntro.post {
                                    binding.rvVideoIntro.layoutManager?.onRestoreInstanceState(state)
                                }
                            }
                        }
                        is VideoLoadingState.NoContent -> Unit
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.hanimeVideoFlow.collect { video ->
                    mutex.withLock {
                        videoIntroAdapter.submit(video)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.addToFavVideoFlow.collect { state ->
                videoIntroAdapter.binding?.btnAddToFav?.setTag(
                    R.id.click_condition, state != WebsiteState.Loading
                )
                when (state) {
                    is WebsiteState.Error -> {
                        showShortToast(R.string.fav_failed)
                    }

                    is WebsiteState.Loading -> Unit
                    is WebsiteState.Success -> {
                        val isFav = state.info
                        if (isFav) {
                            showShortToast(R.string.cancel_fav)
                        } else {
                            showShortToast(R.string.add_to_fav)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadDownloadedFlow.collect { entity ->
                if (entity == null) { // 没下
                    viewModel.hanimeVideoFlow.value?.let {
                        val checkedQuality = requireNotNull(checkedQuality)
                        notifyDownload(it, oldQuality = null, newQuality = checkedQuality) {
                            launch {
                                enqueueDownloadWork(it)
                            }
                        }
                    }
                } else {
                    viewModel.hanimeVideoFlow.value?.let {
                        // #issue-194: 重复下载提示&重新下载
                        val checkedQuality = requireNotNull(checkedQuality)
                        notifyDownload(
                            it, oldQuality = entity.quality, newQuality = checkedQuality
                        ) {
                            launch {
                                enqueueDownloadWork(it, redownload = true)
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.modifyMyListFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> {
                        showShortToast(R.string.modify_failed)
                    }

                    is WebsiteState.Loading -> Unit
                    is WebsiteState.Success -> {
                        showShortToast(R.string.modify_success)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.subscribeArtistFlow.collect { state ->
                videoIntroAdapter.binding?.btnSubscribe?.setTag(
                    R.id.click_condition, state != WebsiteState.Loading
                )
                when (state) {
                    is WebsiteState.Error -> {
                        showShortToast(R.string.subscribe_failed)
                    }

                    is WebsiteState.Loading -> Unit
                    is WebsiteState.Success -> {
                        if (state.info) {
                            showShortToast(R.string.subscribe_success)
                        } else {
                            showShortToast(R.string.unsubscribe_success)
                        }
                        activity?.setResult(Activity.RESULT_OK)
                    }
                }
            }
        }
    }

    private fun notifyDownload(
        info: HanimeVideo, oldQuality: String?, newQuality: String,
        action: () -> Unit
    ) {
        val notifyMsg = spannable {
            getString(R.string.download_video_detail_below).text()
            newline(2)
            if (oldQuality != null) {
                getString(R.string.check_video_exists_in_download, oldQuality).text()
                newline(2)
            }
            getString(R.string.name_with_colon).text()
            newline()
            info.title.span {
                style(Typeface.BOLD)
            }
            newline()
            getString(R.string.quality_with_colon).text()
            newline()
            if (oldQuality != null && oldQuality != newQuality) {
                "$oldQuality → ".text()
                newQuality.span {
                    style(Typeface.BOLD)
                }
            } else {
                newQuality.span {
                    style(Typeface.BOLD)
                }
            }
            newline(2)
            getString(R.string.after_download_tips).text()
        }
        requireContext().showAlertDialog {
            setTitle(if (oldQuality != null) R.string.sure_to_redownload else R.string.sure_to_download)
            setMessage(notifyMsg)
            setPositiveButton(R.string.sure) { _, _ ->
                action.invoke()
            }
            setNegativeButton(R.string.no, null)
            setNeutralButton(R.string.go_to_official) { _, _ ->
                browse(getHanimeVideoDownloadLink(viewModel.videoCode))
            }
        }
    }

    private suspend fun enqueueDownloadWork(videoData: HanimeVideo, redownload: Boolean = false) {
        requireContext().requestPostNotificationPermission()
        val checkedQuality = requireNotNull(checkedQuality)
        context?.let { HCacheManager.saveHanimeVideoInfo(it, viewModel.videoCode, videoData) }
        // HanimeDownloadManager.addTask(
        HanimeDownloadManagerV2.addTask(
            HanimeDownloadWorker.Args(
                quality = checkedQuality,
                downloadUrl = videoData.videoUrls[checkedQuality]?.link,
                videoType = videoData.videoUrls[checkedQuality]?.suffix,
                hanimeName = videoData.title,
                videoCode = viewModel.videoCode,
                coverUrl = videoData.coverUrl,
            ),
            redownload = redownload
        )
    }

    private val List<HanimeInfo>.eachGridCounts
        get() = if (isNotEmpty() && this.first().itemType == HanimeInfo.NORMAL) {
            VideoCoverSize.Normal.videoInOneLine
        } else {
            VideoCoverSize.Simplified.videoInOneLine
        }

    private inner class VideoIntroTouchListener : OnItemTouchListener {

        private var startX = 0
        private var vp2: ViewPager2? = null
        private var isNotHorizontalWrapper = false

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.x.toInt()
                    val childView = rv.findChildViewUnder(e.x, e.y)
                    val position = childView?.let(rv::getChildAdapterPosition) ?: return false
                    val adapter = multi.getWrappedAdapterAndPosition(position).first
                    isNotHorizontalWrapper = adapter !== playlistWrapper
                    val vp2 = vp2 ?: rv.findParent<ViewPager2>().also { vp2 = it }
                    if (vp2.isUserInputEnabled != isNotHorizontalWrapper) {
                        vp2.isUserInputEnabled = isNotHorizontalWrapper
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isNotHorizontalWrapper) return false
                    val endX = e.x.toInt()
                    val direction = startX - endX
                    val canScrollHorizontally =
                        playlistWrapper.wrapper?.canScrollHorizontally(1)?.not()?.let { csh ->
                            if (!csh) false else direction > 0
                        } ?: true
                    val vp2 = vp2 ?: rv.findParent<ViewPager2>().also { vp2 = it }
                    if (vp2.isUserInputEnabled != canScrollHorizontally) {
                        vp2.isUserInputEnabled = canScrollHorizontally
                    }
                }
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) = Unit

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit

    }

    private inner class VideoIntroductionAdapter :
        BaseSingleDifferAdapter<HanimeVideo, DataBindingHolder<ItemVideoIntroductionBinding>>(
            COMPARATOR
        ), AdapterLikeDataBindingPage<ItemVideoIntroductionBinding> {

        override var binding: ItemVideoIntroductionBinding? = null

        private val onLinkClickListener =
            ExpandableTextView.OnLinkClickListener { type, content, _ ->
                when (type) {
                    LinkType.LINK_TYPE -> {
                        // #issue-crashlytics-8a65dcf527b961e98d9991352e36a425
                        try {
                            content?.let(context::browse)
                        } catch (_: Exception) {
                            content?.copyToClipboard()
                            showShortToast(R.string.copy_to_clipboard)
                        }
                    }

                    else -> Unit
                }
            }

        override fun onBindViewHolder(
            holder: DataBindingHolder<ItemVideoIntroductionBinding>,
            item: HanimeVideo?,
        ) {
            item ?: return
            holder.binding.apply {
                this@VideoIntroductionAdapter.binding = this
                uploadTime.text = item.uploadTime?.format(LOCAL_DATE_FORMAT)
                views.text = if (viewModel.fromDownload) {
                    getString(R.string.s_view_times, "0721")
                } else {
                    getString(R.string.s_view_times, item.views.toString())
                }
                tvIntroduction.linkClickListener = onLinkClickListener
                tvIntroduction.setContent(item.introduction)
                tags.tags = item.tags
                tags.lifecycle = viewLifecycleOwner.lifecycle
                btnOriginalComic.setOnClickListener {
                    item.originalComic?.takeIf { it.isNotBlank() }?.let { comicLink ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, comicLink.toUri())
                            startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context,
                                getString(R.string.fault_prompt), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                btnOriginalComic.visibility = if (!item.originalComic.isNullOrBlank()) View.VISIBLE else View.GONE

                initTitle(item)
                initArtist(item.artist)
                initDownloadButton(item)
                initFunctionBar(item)
            }
        }

        override fun onBindViewHolder(
            holder: DataBindingHolder<ItemVideoIntroductionBinding>,
            item: HanimeVideo?,
            payloads: List<Any>,
        ) {
            if (payloads.isEmpty() || payloads.first() == 0)
                return super.onBindViewHolder(holder, item, payloads)
            item ?: return
            val bitset = payloads.first() as Int
            if (bitset and FAV != 0) {
                holder.binding.initFavButton(item)
            }
            // #issue-202: 加入清单之后不会正常刷新
            if (bitset and PLAYLIST != 0) {
                holder.binding.initMyList(item.myList)
            }
            if (bitset and SUBSCRIBE != 0) {
                holder.binding.initArtist(item.artist)
            }
        }

        override fun onCreateViewHolder(
            context: Context,
            parent: ViewGroup,
            viewType: Int,
        ): DataBindingHolder<ItemVideoIntroductionBinding> {
            return DataBindingHolder(
                ItemVideoIntroductionBinding.inflate(
                    LayoutInflater.from(context), parent, false
                )
            )
        }

        private fun ItemVideoIntroductionBinding.initTitle(info: HanimeVideo) {
            title.text = info.chineseTitle
            chineseTitle.text = info.title.also { initShareButton(it) }
            // #issue-80: 长按复制功能请求
            title.setOnLongClickListener {
                title.text.copyToClipboard()
                showShortToast(R.string.copy_to_clipboard)
                return@setOnLongClickListener true
            }
            chineseTitle.setOnLongClickListener {
                chineseTitle.text.copyToClipboard()
                showShortToast(R.string.copy_to_clipboard)
                return@setOnLongClickListener true
            }
        }

        private fun ItemVideoIntroductionBinding.initFavButton(info: HanimeVideo) {
            if (info.isFav) {
                btnAddToFav.setDrawableTop(R.drawable.ic_baseline_favorite_24)
                btnAddToFav.setText(R.string.liked)
            } else {
                btnAddToFav.setDrawableTop(R.drawable.ic_baseline_favorite_border_24)
                btnAddToFav.setText(R.string.add_to_fav)
            }
            // #issue-204: 收藏可能会导致重复
            // reason: 1. 在收藏时，可能会多次点击，导致多次请求
            //         2. payload 后没有重新绑定新 videoData，点击事件未更新
            btnAddToFav.clickWithCondition(viewLifecycleOwner.lifecycle, R.id.click_condition) {
                if (isAlreadyLogin) {
                    it.setTag(R.id.click_condition, false)
                    if (info.isFav) {
                        viewModel.removeFromFavVideo(
                            viewModel.videoCode,
                            info.currentUserId,
                        )
                    } else {
                        viewModel.addToFavVideo(
                            viewModel.videoCode,
                            info.currentUserId,
                        )
                    }
                } else {
                    showShortToast(R.string.login_first)
                }
            }
        }

        private fun ItemVideoIntroductionBinding.initArtist(artist: HanimeVideo.Artist?) {
            if (artist == null) {
                vgArtist.isGone = true
            } else {
                vgArtist.isGone = false
                vgArtist.setOnClickListener {
                    val map = hashMapOf<HAdvancedSearch, Serializable>(
                        HAdvancedSearch.QUERY to artist.name,
                        HAdvancedSearch.GENRE to artist.genre
                    )
                    val bundleMap = HashMap<String, Serializable>().apply {
                        map.forEach { (k, v) -> put(k.name, v) }
                    }
                    try{
                        findNavController().navigate(
                            R.id.searchFragment,
                            bundleOf(ADVANCED_SEARCH_MAP to bundleMap),
                        )
                    }catch (_:IllegalStateException){
                        context.startActivity(
                            Intent(context, MainActivity::class.java).apply {
                                putExtra("startSearchFromMap", HashMap(bundleMap)) // 必须是 Serializable
                            }
                        )
                    }
                }
                tvArtist.text = artist.name
                tvGenre.text = artist.genre
                ivArtist.load(artist.avatarUrl) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
                btnSubscribe.isVisible = artist.post != null
                if (btnSubscribe.isVisible && artist.post != null) {
                    btnSubscribe.text = if (artist.isSubscribed) {
                        getString(R.string.subscribed)
                    } else {
                        getString(R.string.subscribe)
                    }
                    btnSubscribe.clickWithCondition(
                        viewLifecycleOwner.lifecycle, R.id.click_condition
                    ) {
                        if (isAlreadyLogin) {
                            if (artist.isSubscribed) {
                                context.showAlertDialog {
                                    setTitle(R.string.unsubscribe_artist)
                                    setMessage(R.string.sure_to_unsubscribe)
                                    setPositiveButton(R.string.sure) { _, _ ->
                                        it.setTag(R.id.click_condition, false)
                                        viewModel.unsubscribeArtist(
                                            artist.post.userId,
                                            artist.post.artistId
                                        )
                                    }
                                    setNegativeButton(R.string.no, null)
                                }
                            } else {
                                it.setTag(R.id.click_condition, false)
                                viewModel.subscribeArtist(
                                    artist.post.userId,
                                    artist.post.artistId
                                )
                            }
                        } else {
                            showShortToast(R.string.login_first)
                        }
                    }
                }
            }
        }

        private fun ItemVideoIntroductionBinding.initFunctionBar(videoData: HanimeVideo) {
            if (viewModel.fromDownload) {
                nsvButtons.isGone = true
            } else {
                nsvButtons.isVisible = true
                initFavButton(videoData)
                initMyList(videoData.myList)
                btnToWebpage.clickTrigger(viewLifecycleOwner.lifecycle) {
                    browse(getHanimeVideoLink(viewModel.videoCode))
                }
            }
        }

        private fun ItemVideoIntroductionBinding.initMyList(myList: HanimeVideo.MyList?) {
            btnMyList.setOnClickListener {
                if (isAlreadyLogin && myList != null && myList.myListInfo.isNotEmpty()) {
                    requireContext().showAlertDialog {
                        setTitle(R.string.add_to_playlist)
                        setMultiChoiceItems(
                            myList.titleArray,
                            myList.isSelectedArray,
                        ) { _, index, isChecked ->
                            viewModel.modifyMyList(
                                myList.myListInfo[index].code,
                                viewModel.videoCode, isChecked, index
                            )
                        }
                        setNeutralButton(R.string.back, null)
                    }
                } else {
                    showShortToast(R.string.login_first)
                }
            }
        }

        private fun ItemVideoIntroductionBinding.initShareButton(title: String) {
            val shareText = getHanimeShareText(title, viewModel.videoCode)
            btnShare.setOnClickListener {
                shareText(shareText, getString(R.string.long_press_share_to_copy))
            }
            btnShare.setOnLongClickListener {
                shareText.copyToClipboard()
                showShortToast(R.string.copy_to_clipboard)
                return@setOnLongClickListener true
            }
        }

        private val storagePermissionRequester: PermissionRequester?
            get() = activity as? PermissionRequester

        private fun ItemVideoIntroductionBinding.initDownloadButton(videoData: HanimeVideo) {

            if (videoData.videoUrls.isEmpty()) {
                showShortToast(R.string.no_video_links_found)
            } else btnDownload.clickTrigger(viewLifecycleOwner.lifecycle) {
                storagePermissionRequester?.requestStoragePermission(
                    onGranted = {
                        XPopup.Builder(context)
                            .atView(it)
                            .asAttachList(videoData.videoUrls.keys.toTypedArray(), null) { _, key ->
                                if (key == HanimeResolution.RES_UNKNOWN) {
                                    showShortToast(R.string.cannot_download_here)
                                    browse(getHanimeVideoDownloadLink(viewModel.videoCode))
                                } else {
                                    checkedQuality = key
                                    viewModel.findDownloadedHanime(viewModel.videoCode)
                                }
                            }.show()
                    },
                    onDenied = {
                        Toast.makeText(
                            requireContext(),
                            "拒绝？拒绝就不好办了喵👿",
                            Toast.LENGTH_LONG
                        ).show()
                        parentFragmentManager.popBackStack()
                    },
                    onPermanentlyDenied = {
                        showGoToSettingsDialog()
                    }
                )

            }
        }
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("权限被永久拒绝")
            .setMessage("请前往设置开启存储权限，以便保存下载内容。")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${requireContext().packageName}".toUri()
                }
                startActivity(intent)
            }
            .setNegativeButton("取消") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .show()
    }
}