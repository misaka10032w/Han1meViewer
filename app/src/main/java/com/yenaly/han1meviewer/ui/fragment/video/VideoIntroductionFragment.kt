package com.yenaly.han1meviewer.ui.fragment.video

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yenaly.han1meviewer.ADVANCED_SEARCH_MAP
import com.yenaly.han1meviewer.HAdvancedSearch
import com.yenaly.han1meviewer.HCacheManager
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.Preferences.isAlreadyLogin
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.getHanimeVideoDownloadLink
import com.yenaly.han1meviewer.getHanimeVideoLink
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.fragment.PermissionRequester
import com.yenaly.han1meviewer.ui.screen.video.DownloadPromptState
import com.yenaly.han1meviewer.ui.screen.video.VideoIntroductionScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.loadAssetAs
import com.yenaly.han1meviewer.util.openVideo
import com.yenaly.han1meviewer.util.requestPostNotificationPermission
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.han1meviewer.worker.HanimeDownloadWorker
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.copyToClipboard
import com.yenaly.yenaly_libs.utils.findActivityOrNull
import com.yenaly.yenaly_libs.utils.shareText
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.unsafeLazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class VideoIntroductionFragment : Fragment() {

    private val viewModel: VideoViewModel by viewModels({ requireParentFragment() })
    private val genres by unsafeLazy {
        loadAssetAs<List<SearchOption>>(
            if (Preferences.baseUrl == HANIME_URL[3]) {
                "search_options/genre_av.json"
            } else {
                "search_options/genre.json"
            }
        ).orEmpty()
    }

    private var checkedQuality: String? = null
    private var pendingDownloadPrompt by mutableStateOf<DownloadPromptState?>(null)

    private val storagePermissionRequester: PermissionRequester?
        get() = activity as? PermissionRequester

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            val videoState = viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle().value
            val video = viewModel.hanimeVideoFlow.collectAsStateWithLifecycle().value
            val videoShareText = video?.title?.let { title ->
                getHanimeShareText(title, viewModel.videoCode)
            }.orEmpty()

            HanimeTheme {
                VideoIntroductionScreen(
                    video = video,
                    state = videoState,
                    fromDownload = viewModel.fromDownload,
                    hideRelatedInIntro = viewModel.hideRelatedInIntro,
                    shareText = videoShareText,
                    playlistInitialIndex = viewModel.horizontalScrollPositions[viewModel.videoCode] ?: 0,
                    downloadPrompt = pendingDownloadPrompt,
                    onRetry = { viewModel.getHanimeVideo(viewModel.videoCode) },
                    onOpenVideo = { item -> openVideo(item.videoCode) },
                    onOpenArtist = ::openArtistSearch,
                    onToggleSubscribe = ::toggleArtistSubscription,
                    onToggleFavorite = { video?.let(::toggleFavorite) },
                    onManageMyList = { _, selectedStates ->
                        updateMyListSelection(video?.myList, selectedStates)
                    },
                    onQuickCheckIn = ::saveQuickCheckIn,
                    onPrepareDownload = { quality ->
                        checkedQuality = quality
                        video?.let(::startDownloadFlow)
                    },
                    onDismissDownloadPrompt = {
                        pendingDownloadPrompt = null
                    },
                    onConfirmDownloadPrompt = {
                        video?.let(::confirmPendingDownload)
                    },
                    onRequestOpenOfficialDownloadPage = {
                        browse(getHanimeVideoDownloadLink(viewModel.videoCode))
                    },
                    onRequestOpenDownloadPermissionSettings = ::openDownloadPermissionSettings,
                    onShare = {
                        if (videoShareText.isNotBlank()) {
                            shareText(videoShareText, getString(R.string.long_press_share_to_copy))
                        }
                    },
                    onCopyShareText = {
                        if (videoShareText.isNotBlank()) {
                            videoShareText.copyToClipboard()
                            showShortToast(R.string.copy_to_clipboard)
                        }
                    },
                    onOpenWebPage = { browse(getHanimeVideoLink(viewModel.videoCode)) },
                    onOpenOriginalComic = video?.originalComic
                        ?.takeIf { it.isNotBlank() }
                        ?.let { comicLink -> { openOriginalComic(comicLink) } },
                    onCopyText = { text ->
                        text.copyToClipboard()
                        showShortToast(R.string.copy_to_clipboard)
                    },
                    onShowAllPlaylist = if (!viewModel.fromDownload && video?.playlist != null) {
                        {}
                    } else {
                        null
                    },
                    onPlaylistScrollChange = { index ->
                        viewModel.horizontalScrollPositions[viewModel.videoCode] = index
                    },
                    onIntroductionLinkClick = ::openIntroductionLink,
                )
            }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val code = viewModel.videoCode
        if (!viewModel.restoreFromCacheIfExists(code) && code != "-1") {
            viewModel.getHanimeVideo(code)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.CREATED) {
                launch {
                    viewModel.hanimeVideoFlow.collect { video ->
                        viewModel.videoIntroDataMap[code] = video
                        if (video != null) {
                            viewModel.videoIntroRestoredSet.add(code)
                        }
                        viewModel.setVideoList(video?.playlist?.video.orEmpty())
                        val savedIndex = viewModel.horizontalScrollPositions[code]
                        if (savedIndex == null) {
                            val playingIndex = video?.playlist?.video?.indexOfFirst { it.isPlaying } ?: -1
                            if (playingIndex >= 0) {
                                viewModel.horizontalScrollPositions[code] = playingIndex
                            }
                        }
                    }
                }

                launch {
                    viewModel.addToFavVideoFlow.collect { state ->
                        when (state) {
                            is WebsiteState.Error -> showShortToast(R.string.fav_failed)
                            WebsiteState.Loading -> Unit
                            is WebsiteState.Success -> {
                                if (state.info) {
                                    showShortToast(R.string.cancel_fav)
                                } else {
                                    showShortToast(R.string.add_to_fav)
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.loadDownloadedFlow.collect { entity ->
                        val quality = checkedQuality ?: return@collect
                        pendingDownloadPrompt = DownloadPromptState(
                            newQuality = quality,
                            oldQuality = entity?.quality,
                        )
                    }
                }

                launch {
                    viewModel.modifyMyListFlow.collect { state ->
                        when (state) {
                            is WebsiteState.Error -> showShortToast(R.string.modify_failed)
                            WebsiteState.Loading -> Unit
                            is WebsiteState.Success -> showShortToast(R.string.modify_success)
                        }
                    }
                }

                launch {
                    viewModel.subscribeArtistFlow.collect { state ->
                        when (state) {
                            is WebsiteState.Error -> showShortToast(R.string.subscribe_failed)
                            WebsiteState.Loading -> Unit
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
        }
    }

    override fun onDestroyView() {
        viewModel.videoIntroDataMap[viewModel.videoCode] = viewModel.hanimeVideoFlow.value
        viewModel.videoIntroRestoredSet.remove(viewModel.videoCode)
        super.onDestroyView()
    }

    fun refreshRelatedSection() = Unit

    private fun openArtistSearch(artist: HanimeVideo.Artist) {
        val searchKey = genres.firstOrNull { option ->
            option.lang?.let { lang ->
                artist.genre == lang.zhrCN ||
                    artist.genre == lang.zhrTW ||
                    artist.genre == lang.en
            } == true
        }?.searchKey ?: ""
        val map = buildMap<HAdvancedSearch, Serializable> {
            put(HAdvancedSearch.QUERY, artist.name)
            if (searchKey.isNotEmpty() && !Preferences.searchArtistIgnoreVideoType) {
                put(HAdvancedSearch.GENRE, searchKey)
            }
        }
        val bundleMap = HashMap<String, Serializable>().apply {
            map.forEach { (key, value) -> put(key.name, value) }
        }
        requireContext().findActivityOrNull<MainActivity>()?.let { activity ->
            activity.startActivity(
                Intent(activity, MainActivity::class.java).apply {
                    putExtra("startSearchFromMap", HashMap(bundleMap))
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
            return
        }
        context?.startActivity(
            Intent(context, MainActivity::class.java).apply {
                putExtra("startSearchFromMap", HashMap(bundleMap))
            }
        )
    }

    private fun toggleArtistSubscription(artist: HanimeVideo.Artist) {
        val post = artist.post ?: return
        if (!isAlreadyLogin) {
            showShortToast(R.string.login_first)
            return
        }
        if (artist.isSubscribed) {
            requireContext().showAlertDialog {
                setTitle(R.string.unsubscribe_artist)
                setMessage(R.string.sure_to_unsubscribe)
                setPositiveButton(R.string.sure) { _, _ ->
                    viewModel.unsubscribeArtist(post.userId, post.artistId)
                }
                setNegativeButton(R.string.no, null)
            }
        } else {
            viewModel.subscribeArtist(post.userId, post.artistId)
        }
    }

    private fun toggleFavorite(video: HanimeVideo) {
        if (!isAlreadyLogin) {
            showShortToast(R.string.login_first)
            return
        }
        if (video.isFav) {
            viewModel.removeFromFavVideo(viewModel.videoCode, video.currentUserId)
        } else {
            viewModel.addToFavVideo(viewModel.videoCode, video.currentUserId)
        }
    }

    private fun updateMyListSelection(
        myList: HanimeVideo.MyList?,
        selectedStates: List<Boolean>,
    ) {
        if (!isAlreadyLogin || myList == null || myList.myListInfo.isEmpty()) {
            showShortToast(R.string.login_first)
            return
        }
        myList.myListInfo.forEachIndexed { index, info ->
            val newChecked = selectedStates.getOrNull(index) ?: return@forEachIndexed
            if (info.isSelected != newChecked) {
                viewModel.modifyMyList(
                    listCode = info.code,
                    videoCode = viewModel.videoCode,
                    isChecked = newChecked,
                    position = index,
                )
            }
        }
    }

    private fun openOriginalComic(comicLink: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, comicLink.toUri()))
        } catch (_: Exception) {
            Toast.makeText(requireContext(), getString(R.string.fault_prompt), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openIntroductionLink(link: String) {
        try {
            requireContext().browse(link)
        } catch (_: Exception) {
            link.copyToClipboard()
            showShortToast(R.string.copy_to_clipboard)
        }
    }

    private fun saveQuickCheckIn(record: CheckInRecordEntity) {
        val context = requireContext()
        val normalizedRecord = if (record.sideDishes.contains("\u001E")) {
            record
        } else {
            record.copy(sideDishes = "${record.sideDishes}\u001E${viewModel.videoCode}")
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            CheckInRecordDatabase.getDatabase(context).checkInDao().insert(normalizedRecord)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.checkin, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startDownloadFlow(videoData: HanimeVideo) {
        if (videoData.videoUrls.isEmpty()) {
            showShortToast(R.string.no_video_links_found)
            return
        }
        storagePermissionRequester?.requestStoragePermission(
            onGranted = {
                viewModel.findDownloadedHanime(viewModel.videoCode)
            },
            onDenied = {
                Toast.makeText(
                    requireContext(),
                    "拒绝？拒绝就不好办了喵👿",
                    Toast.LENGTH_LONG,
                ).show()
                parentFragmentManager.popBackStack()
            },
            onPermanentlyDenied = { openDownloadPermissionSettings() },
        )
    }

    private fun confirmPendingDownload(videoData: HanimeVideo) {
        val redownload = pendingDownloadPrompt?.oldQuality != null
        pendingDownloadPrompt = null
        viewLifecycleOwner.lifecycleScope.launch {
            enqueueDownloadWork(videoData, redownload = redownload)
        }
    }

    private suspend fun enqueueDownloadWork(videoData: HanimeVideo, redownload: Boolean = false) {
        requireContext().requestPostNotificationPermission()
        val quality = requireNotNull(checkedQuality)
        context?.let { HCacheManager.saveHanimeVideoInfo(it, viewModel.videoCode, videoData) }
        HanimeDownloadManagerV2.addTask(
            HanimeDownloadWorker.Args(
                quality = quality,
                downloadUrl = videoData.videoUrls[quality]?.link,
                videoType = videoData.videoUrls[quality]?.suffix,
                hanimeName = videoData.title,
                videoCode = viewModel.videoCode,
                coverUrl = videoData.coverUrl,
            ),
            redownload = redownload,
        )
    }

    private fun openDownloadPermissionSettings() {
        AlertDialog.Builder(requireContext())
            .setTitle("权限被永久拒绝")
            .setMessage("请前往设置开启存储权限，以便保存下载内容。")
            .setPositiveButton("去设置") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${requireContext().packageName}".toUri()
                })
            }
            .setNegativeButton("取消") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .show()
    }
}
