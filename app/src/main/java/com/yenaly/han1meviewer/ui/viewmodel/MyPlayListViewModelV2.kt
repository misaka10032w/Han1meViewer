package com.yenaly.han1meviewer.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.ModifiedPlaylistArgs
import com.yenaly.han1meviewer.logic.model.MyListItems
import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPlayListViewModelV2 : ViewModel() {

    private val _myPlaylistsFlow = MutableStateFlow<WebsiteState<Playlists>>(WebsiteState.Loading)
    val myPlaylistsFlow: StateFlow<WebsiteState<Playlists>> = _myPlaylistsFlow.asStateFlow()

    private val _cachedMyPlayList = MutableStateFlow<List<Playlists.Playlist>>(emptyList())
    val cachedMyPlayList: StateFlow<List<Playlists.Playlist>> = _cachedMyPlayList.asStateFlow()

    private val _playlistStateFlow =
        MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>>(PageLoadingState.Loading)
    val playlistStateFlow = _playlistStateFlow.asStateFlow()
    private val _playlistDesc = MutableStateFlow<String?>(null)
    val playlistDesc = _playlistDesc.asStateFlow()

    private val _playlistFlow = MutableStateFlow(emptyList<HanimeInfo>())
    val playlistFlow = _playlistFlow.asStateFlow()


    private val _refreshCompleted = MutableSharedFlow<Unit>()
    val refreshCompleted: SharedFlow<Unit> = _refreshCompleted

    private val _showSheet = MutableStateFlow(false)
    val showSheet: StateFlow<Boolean> = _showSheet.asStateFlow()

    fun setShowSheet(value: Boolean) {
        _showSheet.value = value
    }

    // 加载所有playlist
    fun loadMyPlayList(forceReload: Boolean = false) {
        viewModelScope.launch {
            NetworkRepo.getPlaylists().collect { state ->
                _myPlaylistsFlow.value = state
                if (state is WebsiteState.Success) {
                    _cachedMyPlayList.value = state.info.playlists
                    _refreshCompleted.emit(Unit)
                }
            }
        }
    }

    // 获取单个playlist内容
    fun getPlaylistItems(page: Int = 1, listCode: String) {
        viewModelScope.launch {
            if (listCode.isBlank()) return@launch
            if (page == 1) {
                _playlistFlow.value = emptyList()
                _playlistDesc.value = null
                _playlistStateFlow.value = PageLoadingState.Loading
            }
            NetworkRepo.getMyListItems(page, listCode).collect { state ->
                val prev = _playlistStateFlow.getAndUpdate { state }
                if (prev is PageLoadingState.Loading) {
                    _playlistFlow.value = emptyList()
                    _playlistDesc.value = null
                }
                if (state is PageLoadingState.Success) {
                    _playlistDesc.value = state.info.desc
                }
                _playlistFlow.update { prevList ->
                    when (state) {
                        is PageLoadingState.Success -> prevList + state.info.hanimeInfo
                        is PageLoadingState.Loading -> emptyList()
                        else -> prevList
                    }
                }
            }
        }
    }

    private val _deleteFromPlaylistFlow = MutableSharedFlow<WebsiteState<Int>>()
    val deleteFromPlaylistFlow = _deleteFromPlaylistFlow.asSharedFlow()
    // 从详情页删除某视频
    fun deleteFromPlaylist(listCode: String, videoCode: String, position: Int) {
        viewModelScope.launch {
            NetworkRepo.deleteMyListItems(listCode, videoCode, position, csrfToken).collect {
                _deleteFromPlaylistFlow.emit(it)
                _playlistFlow.update { prevList ->
                    if (it is WebsiteState.Success) {
                        prevList.toMutableList().apply { removeAt(position) }
                    } else prevList
                }
            }
        }
    }

    private val _modifyPlaylistFlow = MutableSharedFlow<WebsiteState<ModifiedPlaylistArgs>>()
    val modifyPlaylistFlow = _modifyPlaylistFlow.asSharedFlow()
    // 编辑Playlist
    fun modifyPlaylist(listCode: String, title: String, desc: String, delete: Boolean) {
        Log.i("modify_playlist","${listCode},${title},${desc}")
        viewModelScope.launch {
            NetworkRepo.modifyPlaylist(listCode, title, desc, delete, csrfToken).collect {
                _modifyPlaylistFlow.emit(it)
                if (delete) {
                    clearMyListItems()
                }
            }
        }
    }
    fun clearMyListItems() {
        _playlistStateFlow.value = PageLoadingState.Loading
    }
    private val _createPlaylistFlow = MutableSharedFlow<WebsiteState<Unit>>()
    val createPlaylistFlow = _createPlaylistFlow.asSharedFlow()
    //创建Playlist
    fun createPlaylist(title: String, description: String) {
        viewModelScope.launch {
            NetworkRepo.createPlaylist(EMPTY_STRING, title, description, csrfToken).collect {
                _createPlaylistFlow.emit(it)
            }
        }
    }
}
