package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.HCacheManager
import com.yenaly.han1meviewer.HanimeResolution
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.base.BaseViewModel
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import com.yenaly.han1meviewer.util.HAnalytics
import com.yenaly.han1meviewer.util.TagLocalizer
import com.yenaly.han1meviewer.util.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/17 017 19:01
 */
class VideoViewModel(application: Application) : BaseViewModel(application) {

    data class IntroScrollState(
        val firstVisibleItemIndex: Int = 0,
        val firstVisibleItemScrollOffset: Int = 0,
    )

    data class VideoHostUiState(
        val selectedTabIndex: Int = 0,
        val isAppBarExpanded: Boolean = true,
        val isInPipMode: Boolean = false,
        val playerHeightDp: Int = 250.dp,
    )

    private data class VideoIntroUiState(
        val playlistFirstVisibleIndex: Int? = null,
        val cachedVideo: HanimeVideo? = null,
        val introRestored: Boolean = false,
        val scrollState: IntroScrollState = IntroScrollState(),
        val selectedTabIndex: Int = 0,
        val isAppBarExpanded: Boolean = true,
    )

    companion object {
        /**
         * 最小的 HKeyframe 保存間隔，暫定 5s
         */
        const val MIN_H_KEYFRAME_SAVE_INTERVAL = 5_000 // ms
    }
    private val videoIntroUiStateMap = mutableMapOf<String, VideoIntroUiState>()
    var videoCode: String = EMPTY_STRING

    var fromDownload = false

    // 相关影片在别处展示时（独立标签页开关开启，或平板横屏分栏布局），简介底部不显示相关列表
    var hideRelatedInIntro by mutableStateOf(false)
    var hKeyframes: HKeyframeEntity? = null
    private val _videoList = MutableLiveData<List<HanimeInfo>>()
    val videoList: LiveData<List<HanimeInfo>> = _videoList
    private val _hanimeVideoStateFlow =
        MutableStateFlow<VideoLoadingState<HanimeVideo>>(VideoLoadingState.Loading)
    val hanimeVideoStateFlow = _hanimeVideoStateFlow.asStateFlow()

    private val _hanimeVideoFlow = MutableStateFlow<HanimeVideo?>(null)
    val hanimeVideoFlow = _hanimeVideoFlow.asStateFlow()
    private val _videoHostUiStateFlow = MutableStateFlow(VideoHostUiState())
    val videoHostUiStateFlow = _videoHostUiStateFlow.asStateFlow()

    /**
     * 评论数角标独立成流：评论数据在进场后异步到达，若放在
     * [VideoHostUiState] 里会让整个播放页宿主随之重组（播放器子树等），
     * 只有 tabs 需要消费它。
     */
    private val _commentBadgeCount = MutableStateFlow(0)
    val commentBadgeCount = _commentBadgeCount.asStateFlow()

    /**
     * 同系列影片已歸入的分組 id，作為下載時分組名的推薦項。
     *
     * 同系列任意一集在非預設分組中時推薦該分組，否則為 null。
     */
    val recommendedGroupId: StateFlow<Int?> =
        hanimeVideoFlow
            .map { video -> video?.playlist?.video?.map { it.videoCode }.orEmpty() }
            .distinctUntilChanged()
            .map { videoCodes ->
                if (videoCodes.isEmpty()) null
                else DatabaseRepo.HanimeDownload.findGroupIdOfSeries(videoCodes)
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> e.printStackTrace() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )

    /**
     * 已下載分組列表，供下載時自動分組選擇（複用/挑選已有分組）使用。
     */
    val downloadGroups: StateFlow<List<DownloadGroupEntity>> =
        DatabaseRepo.HanimeDownload.getAllGroups()
            .flowOn(Dispatchers.IO)
            .catch { e -> e.printStackTrace() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    fun setVideoList(list: List<HanimeInfo>) {
        _videoList.value = list
    }

    fun getPlaylistFirstVisibleIndex(videoCode: String): Int? {
        return videoIntroUiStateMap[videoCode]?.playlistFirstVisibleIndex
    }

    fun setPlaylistFirstVisibleIndex(videoCode: String, index: Int) {
        updateVideoIntroUiState(videoCode) { copy(playlistFirstVisibleIndex = index) }
    }

    fun setVideoIntroCachedData(videoCode: String, video: HanimeVideo?) {
        updateVideoIntroUiState(videoCode) {
            copy(
                cachedVideo = video,
                introRestored = video != null,
            )
        }
    }

    fun clearVideoIntroRestoredFlag(videoCode: String) {
        updateVideoIntroUiState(videoCode) { copy(introRestored = false) }
    }

    fun getIntroScrollState(videoCode: String): IntroScrollState {
        return videoIntroUiStateMap[videoCode]?.scrollState ?: IntroScrollState()
    }

    fun getSelectedTabIndex(videoCode: String): Int {
        return _videoHostUiStateFlow.value.selectedTabIndex
    }

    fun setSelectedTabIndex(videoCode: String, selectedTabIndex: Int) {
        _videoHostUiStateFlow.update { it.copy(selectedTabIndex = selectedTabIndex) }
        updateVideoIntroUiState(videoCode) { copy(selectedTabIndex = selectedTabIndex) }
    }

    fun isAppBarExpanded(videoCode: String): Boolean {
        return _videoHostUiStateFlow.value.isAppBarExpanded
    }

    fun setAppBarExpanded(videoCode: String, isExpanded: Boolean) {
        _videoHostUiStateFlow.update { it.copy(isAppBarExpanded = isExpanded) }
        updateVideoIntroUiState(videoCode) { copy(isAppBarExpanded = isExpanded) }
    }

    fun setCommentBadgeCount(commentBadgeCount: Int) {
        _commentBadgeCount.value = commentBadgeCount
    }

    fun setPipMode(isInPipMode: Boolean) {
        _videoHostUiStateFlow.update { it.copy(isInPipMode = isInPipMode) }
        // 画中画使用埋点（只在进入时记录，避免退出时重复）
        if (isInPipMode) {
            HAnalytics.logEvent(
                FirebaseConstants.EVENT_VIDEO_PIP,
                FirebaseConstants.PARAM_VIDEO_CODE to videoCode,
            )
        }
    }

    fun setPlayerHeightDp(playerHeightDp: Int) {
        _videoHostUiStateFlow.update { it.copy(playerHeightDp = playerHeightDp) }
    }

    fun setIntroScrollState(
        videoCode: String,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
    ) {
        updateVideoIntroUiState(videoCode) {
            copy(
                scrollState = IntroScrollState(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                )
            )
        }
    }

    private inline fun updateVideoIntroUiState(
        videoCode: String,
        transform: VideoIntroUiState.() -> VideoIntroUiState,
    ) {
        val current = videoIntroUiStateMap[videoCode] ?: VideoIntroUiState()
        videoIntroUiStateMap[videoCode] = current.transform()
    }

    fun resolveTagSearchKey(tag: String): String = TagLocalizer.resolveSearchKey(tag)

    private fun HanimeVideo.withLocalizedLabels(): HanimeVideo {
        return copy(
            tags = TagLocalizer.localizeTags(tags),
            artist = artist?.copy(genre = TagLocalizer.localizeTag(artist.genre)),
        )
    }

    fun buildLocalPlayInfo(localPath: String? = null): HanimeVideo {
        val resolution = HanimeResolution()
        resolution.parseResolution(
            HanimeResolution.RES_1080P,
            resLink = localPath?:"",
            type = "video/mp4"
        )
        return HanimeVideo(
            title = "",
            coverUrl = "",
            chineseTitle = localPath?.toUri()?.lastPathSegment,
            introduction = "",
            uploadTime = null,
            views = "0",
            videoUrls = resolution.toResolutionLinkMap(),
            tags = emptyList(),
        )
    }
    fun getHanimeVideo(videoCode: String,localUri: String? = null) {
        if (videoCode == "-1"){
            val localPlayInfo = buildLocalPlayInfo(localUri)
            _hanimeVideoStateFlow.value = VideoLoadingState.Success(localPlayInfo)
            _hanimeVideoFlow.value = localPlayInfo
            return
        }
        if (videoIntroUiStateMap[videoCode]?.introRestored == true) return
        // 观看影片埋点：使用 GA4 标准 view_item 事件
        HAnalytics.logEvent(
            FirebaseAnalytics.Event.VIEW_ITEM,
            FirebaseAnalytics.Param.ITEM_ID to videoCode,
            FirebaseConstants.PARAM_VIDEO_CODE to videoCode,
            FirebaseConstants.PARAM_FROM_DOWNLOAD to fromDownload,
            FirebaseConstants.PARAM_IS_LOCAL to (localUri != null),
        )
        viewModelScope.launch {
            val flow = if (fromDownload) {
                HCacheManager.loadHanimeVideoInfo(application,videoCode).map { hv ->
                    if (hv == null) {
                        VideoLoadingState.NoContent
                    } else {
                        VideoLoadingState.Success(hv)
                    }
                }
            } else {
                NetworkRepo.getHanimeVideo(videoCode)
            }
            flow.collect { state ->
                val emitState = when {
                    localUri != null && state is VideoLoadingState.Success -> {
                        val resolution = HanimeResolution()
                        resolution.parseResolution(
                            HanimeResolution.RES_1080P,
                            resLink = localUri,
                            type = "video/mp4"
                        )
                        VideoLoadingState.Success(
                            state.info.copy(videoUrls = resolution.toResolutionLinkMap())
                                .withLocalizedLabels()
                        )
                    }

                    state is VideoLoadingState.Success -> {
                        VideoLoadingState.Success(state.info.withLocalizedLabels())
                    }

                    else -> state
                }
                _hanimeVideoStateFlow.value = emitState
                if (emitState is VideoLoadingState.Success) {
                    _hanimeVideoFlow.update { emitState.info }
                    csrfToken = emitState.info.csrfToken
                }
            }
        }
    }

    fun restoreFromCacheIfExists(code: String): Boolean {
        val cached = videoIntroUiStateMap[code]?.cachedVideo?.withLocalizedLabels() ?: return false
        updateVideoIntroUiState(code) { copy(introRestored = true) }
        _hanimeVideoFlow.value = cached
        _hanimeVideoStateFlow.value = VideoLoadingState.Success(cached)
        return true
    }



    private val _addToFavVideoFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    val addToFavVideoFlow = _addToFavVideoFlow.asSharedFlow()

    private val _loadDownloadedFlow = MutableSharedFlow<HanimeDownloadEntity?>()
    val loadDownloadedFlow = _loadDownloadedFlow.asSharedFlow()

    fun addToFavVideo(
        videoCode: String,
        currentUserId: String?,
    ) = modifyFavVideoInternal(videoCode, likeStatus = false, currentUserId)

    fun removeFromFavVideo(
        videoCode: String,
        currentUserId: String?,
    ) = modifyFavVideoInternal(videoCode, likeStatus = true, currentUserId)

    private fun modifyFavVideoInternal(
        videoCode: String,
        likeStatus: Boolean,
        currentUserId: String?,
    ) {
        viewModelScope.launch {
            NetworkRepo.addToMyFavVideo(
                videoCode, likeStatus, currentUserId, csrfToken
            ).collect { state ->
                _addToFavVideoFlow.emit(state)
                // 收藏/取消收藏埋点：GA4 标准 add_to_wishlist / remove_from_wishlist 事件
                when (state) {
                    is WebsiteState.Success -> HAnalytics.logEvent(
                        if (likeStatus) {
                            // SDK 无 REMOVE_FROM_WISHLIST 常量，使用 GA4 标准事件名
                            "remove_from_wishlist"
                        } else {
                            FirebaseAnalytics.Event.ADD_TO_WISHLIST
                        },
                        FirebaseAnalytics.Param.ITEM_ID to videoCode,
                        FirebaseConstants.PARAM_VIDEO_CODE to videoCode,
                        FirebaseConstants.PARAM_RESULT to "success",
                    )

                    is WebsiteState.Error -> HAnalytics.logEvent(
                        FirebaseAnalytics.Event.ADD_TO_WISHLIST,
                        FirebaseAnalytics.Param.ITEM_ID to videoCode,
                        FirebaseConstants.PARAM_VIDEO_CODE to videoCode,
                        FirebaseConstants.PARAM_RESULT to "failure",
                        FirebaseConstants.PARAM_REASON to state.throwable.message,
                    )

                    WebsiteState.Loading -> Unit
                }
                if (likeStatus) {
                    _hanimeVideoFlow.update { it?.rateVideo(isPositive = true) }
                } else {
                    _hanimeVideoFlow.update { it?.rateVideo(isPositive = true) }
                }
            }
        }
    }

    fun rateVideo(video: HanimeVideo, isPositive: Boolean) {
        viewModelScope.launch {
            NetworkRepo.rateVideo(
                videoCode = videoCode,
                isPositive = isPositive,
                likeStatus = video.isFav,
                unlikeStatus = video.isUnlike,
                likesCount = video.favTimes ?: 0,
                unlikesCount = video.unlikesCount ?: 0,
                currentUserId = video.currentUserId,
                token = csrfToken,
            ).collect { state ->
                _addToFavVideoFlow.emit(state)
                if (state is WebsiteState.Success) {
                    // 点赞/点踩埋点
                    HAnalytics.logEvent(
                        "rate_video",
                        FirebaseConstants.PARAM_VIDEO_CODE to videoCode,
                        FirebaseConstants.PARAM_METHOD to if (isPositive) "like" else "dislike",
                        FirebaseConstants.PARAM_RESULT to "success",
                    )
                    _hanimeVideoFlow.update { it?.rateVideo(isPositive) }
                } else if (state is WebsiteState.Error) {
                    HAnalytics.logEvent(
                        "rate_video",
                        FirebaseConstants.PARAM_VIDEO_CODE to videoCode,
                        FirebaseConstants.PARAM_METHOD to if (isPositive) "like" else "dislike",
                        FirebaseConstants.PARAM_RESULT to "failure",
                        FirebaseConstants.PARAM_REASON to state.throwable.message,
                    )
                }
            }
        }
    }

    private val _modifyMyListFlow = MutableSharedFlow<WebsiteState<Unit>>()
    val modifyMyListFlow = _modifyMyListFlow.asSharedFlow()

    fun modifyMyList(
        listCode: String,
        videoCode: String,
        isChecked: Boolean,
        position: Int,
    ) {
        viewModelScope.launch {
            NetworkRepo.addToMyList(listCode, videoCode, isChecked, csrfToken).collect {
                _modifyMyListFlow.emit(it)
                _hanimeVideoFlow.update { prev ->
                    val myList = prev?.myList?.myListInfo.orEmpty().toMutableList()
                    myList[position] = myList[position].copy(isSelected = isChecked)
                    prev?.copy(myList = prev.myList?.copy(myListInfo = myList))
                }
            }
        }
    }

    fun insertWatchHistory(history: WatchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.WatchHistory.insert(history)
            Log.d("insert_watch_hty", "$history DONE!")
        }
    }

    fun insertWatchHistoryWithCover(history: WatchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.WatchHistory.insert(history)
        }
    }

    fun findDownloadedHanime(videoCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val info = DatabaseRepo.HanimeDownload.find(videoCode)
            _loadDownloadedFlow.emit(info)
        }
    }

    // true代表已关注成功，false代表取消关注成功
    private val _subscribeArtistFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    val subscribeArtistFlow = _subscribeArtistFlow.asSharedFlow()

    fun subscribeArtist(
        userId: String,
        artistId: String,
    ) {
        viewModelScope.launch {
            NetworkRepo.subscribeArtist(csrfToken, userId, artistId, true).collect { state ->
                _subscribeArtistFlow.emit(state)
                if (state is WebsiteState.Success) {
                    HAnalytics.logEvent(
                        FirebaseConstants.EVENT_SUBSCRIBE_ARTIST,
                        FirebaseConstants.PARAM_METHOD to artistId,
                        FirebaseConstants.PARAM_IS_CANCEL to false,
                        FirebaseConstants.PARAM_RESULT to "success",
                    )
                    _hanimeVideoFlow.update {
                        it?.copy(artist = it.artist?.copy(post = it.artist.post?.copy(isSubscribed = true)))
                    }
                } else if (state is WebsiteState.Error) {
                    HAnalytics.logEvent(
                        FirebaseConstants.EVENT_SUBSCRIBE_ARTIST,
                        FirebaseConstants.PARAM_METHOD to artistId,
                        FirebaseConstants.PARAM_IS_CANCEL to false,
                        FirebaseConstants.PARAM_RESULT to "failure",
                        FirebaseConstants.PARAM_REASON to state.throwable.message,
                    )
                }
            }
        }
    }

    fun unsubscribeArtist(
        userId: String,
        artistId: String,
    ) {
        viewModelScope.launch {
            NetworkRepo.subscribeArtist(csrfToken, userId, artistId, false).collect { state ->
                _subscribeArtistFlow.emit(state)
                if (state is WebsiteState.Success) {
                    HAnalytics.logEvent(
                        FirebaseConstants.EVENT_SUBSCRIBE_ARTIST,
                        FirebaseConstants.PARAM_METHOD to artistId,
                        FirebaseConstants.PARAM_IS_CANCEL to true,
                        FirebaseConstants.PARAM_RESULT to "success",
                    )
                    _hanimeVideoFlow.update {
                        it?.copy(artist = it.artist?.copy(post = it.artist.post?.copy(isSubscribed = false)))
                    }
                } else if (state is WebsiteState.Error) {
                    HAnalytics.logEvent(
                        FirebaseConstants.EVENT_SUBSCRIBE_ARTIST,
                        FirebaseConstants.PARAM_METHOD to artistId,
                        FirebaseConstants.PARAM_IS_CANCEL to true,
                        FirebaseConstants.PARAM_RESULT to "failure",
                        FirebaseConstants.PARAM_REASON to state.throwable.message,
                    )
                }
            }
        }
    }

    // boolean: 成功 or 失敗，String: 提示信息
    private val _modifyHKeyframeFlow = MutableSharedFlow<Pair<Boolean, String>>()
    val modifyHKeyframeFlow = _modifyHKeyframeFlow.asSharedFlow()
    private val _forceRefresh = MutableSharedFlow<Unit>(replay = 1)
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeKeyframe(videoCode: String): Flow<HKeyframeEntity?> {
        return _forceRefresh
            .onStart { emit(Unit) }
            .flatMapLatest {
                DatabaseRepo.HKeyframe.observe(videoCode).flowOn(Dispatchers.IO)
            }
    }
    fun appendHKeyframe(videoCode: String, title: String, hKeyframe: HKeyframeEntity.Keyframe) {
        viewModelScope.launch(Dispatchers.IO) {
            run {
                this@VideoViewModel.hKeyframes?.keyframes?.forEach { keyframeInDb ->
                    if (abs(keyframeInDb.position - hKeyframe.position) < MIN_H_KEYFRAME_SAVE_INTERVAL) {
                        Log.d("HKeyframe", "append_hkeyframe:time conflict: $keyframeInDb")
                        _modifyHKeyframeFlow.emit(
                            false to application.getString(
                                R.string.interval_must_greater_than_d,
                                MIN_H_KEYFRAME_SAVE_INTERVAL / 1_000L
                            )
                        )
                        return@run
                    }
                }
                DatabaseRepo.HKeyframe.appendKeyframe(videoCode, title, hKeyframe)
                Log.d("HKeyframe", "append_hkeyframe:$hKeyframe DONE!")
                _modifyHKeyframeFlow.emit(true to application.getString(R.string.add_success))
                _forceRefresh.emit(Unit)
            }
        }
    }
}
