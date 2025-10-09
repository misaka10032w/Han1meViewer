package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import android.os.Parcelable
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.HCacheManager
import com.yenaly.han1meviewer.HanimeResolution
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import com.yenaly.yenaly_libs.base.YenalyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/17 017 19:01
 */
class VideoViewModel(application: Application) : YenalyViewModel(application) {

    companion object {
        /**
         * 最小的 HKeyframe 保存間隔，暫定 5s
         */
        const val MIN_H_KEYFRAME_SAVE_INTERVAL = 5_000 // ms
    }
    val horizontalScrollPositions = mutableMapOf<String, Int>()
    var videoCode: String = EMPTY_STRING
        set(value) {
            field = value
            // 在這裏初始化所有需要videoCode的方法
            getHanimeVideo(value)
        }

    var fromDownload = false

    var hKeyframes: HKeyframeEntity? = null
    private val _videoList = MutableLiveData<List<HanimeInfo>>()
    val videoList: LiveData<List<HanimeInfo>> = _videoList
    private val _hanimeVideoStateFlow =
        MutableStateFlow<VideoLoadingState<HanimeVideo>>(VideoLoadingState.Loading)
    val hanimeVideoStateFlow = _hanimeVideoStateFlow.asStateFlow()

    private val _hanimeVideoFlow = MutableStateFlow<HanimeVideo?>(null)
    val hanimeVideoFlow = _hanimeVideoFlow.asStateFlow()

    // 保存当前 videoCode及请求数据以及对应的滚动位置
    val scrollPositionMap = mutableMapOf<String, Parcelable?>()
    val videoIntroDataMap = mutableMapOf<String, HanimeVideo?>()
    // 表示已经还原过的数据
    val videoIntroRestoredSet = mutableSetOf<String>()
    fun setVideoList(list: List<HanimeInfo>) {
        _videoList.value = list
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
            _hanimeVideoStateFlow.value = VideoLoadingState.Success(
                buildLocalPlayInfo(localUri)
            )
            _hanimeVideoFlow.value = buildLocalPlayInfo(localUri)
            return
        }
        if (videoIntroRestoredSet.contains(videoCode)) return
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
                _hanimeVideoStateFlow.value = state
                if (state is VideoLoadingState.Success) {
                    _hanimeVideoFlow.update { state.info }
                    csrfToken = state.info.csrfToken
                }
            }
        }
    }

    fun restoreFromCacheIfExists(code: String): Boolean {
        val cached = videoIntroDataMap[code] ?: return false
        videoIntroRestoredSet.add(code) //
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
                if (likeStatus) {
                    // 代表移除喜爱
                    _hanimeVideoFlow.update { it?.decFavTime() }
                } else {
                    // 代表添加喜爱
                    _hanimeVideoFlow.update { it?.incFavTime() }
                }
            }
        }
    }

    private val _modifyMyListFlow = MutableSharedFlow<WebsiteState<Int>>()
    val modifyMyListFlow = _modifyMyListFlow.asSharedFlow()

    fun modifyMyList(
        listCode: String,
        videoCode: String,
        isChecked: Boolean,
        position: Int,
    ) {
        viewModelScope.launch {
            NetworkRepo.addToMyList(listCode, videoCode, isChecked, position, csrfToken).collect {
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
                    _hanimeVideoFlow.update {
                        it?.copy(artist = it.artist?.copy(post = it.artist.post?.copy(isSubscribed = true)))
                    }
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
                    _hanimeVideoFlow.update {
                        it?.copy(artist = it.artist?.copy(post = it.artist.post?.copy(isSubscribed = false)))
                    }
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