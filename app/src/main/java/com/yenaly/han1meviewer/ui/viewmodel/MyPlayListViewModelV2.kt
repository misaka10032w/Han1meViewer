package com.yenaly.han1meviewer.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.ModifiedPlaylistArgs
import com.yenaly.han1meviewer.logic.model.MyListItems
import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.screen.home.myplaylist.PlaylistUiState
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistSheetScrollState(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
)

class MyPlayListViewModelV2 : ViewModel() {

    private val _myPlaylistsFlow = MutableStateFlow<WebsiteState<Playlists>>(WebsiteState.Loading)
    val myPlaylistsFlow: StateFlow<WebsiteState<Playlists>> = _myPlaylistsFlow.asStateFlow()

    private val _cachedMyPlayList = MutableStateFlow<List<Playlists.Playlist>>(emptyList())

    private val _playlistStateFlow =
        MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>>(PageLoadingState.Loading)
    val playlistStateFlow = _playlistStateFlow.asStateFlow()
    private val _playlistDesc = MutableStateFlow<String?>(null)
    val playlistDesc = _playlistDesc.asStateFlow()

    private val _playlistFlow = MutableStateFlow(emptyList<HanimeInfo>())
    val playlistFlow = _playlistFlow.asStateFlow()
    private val _currentListInfo = MutableStateFlow<Pair<String, String>?>(null)
    val currentListInfo = _currentListInfo.asStateFlow()
    private val _playlistSheetScrollStates = MutableStateFlow<Map<String, PlaylistSheetScrollState>>(emptyMap())


    private val _refreshCompleted = MutableSharedFlow<Unit>()
    val refreshCompleted: SharedFlow<Unit> = _refreshCompleted

    private val _showSheet = MutableStateFlow(false)
    var currentPage = 1
    var isLoadingMore = false
        private set

    var playlistPage = 1
    private val _isLoadingMorePlaylists = MutableStateFlow(false)
    private val _noMorePlaylists = MutableStateFlow(false)

    /** 对外暴露的唯一主页面 UI 状态流。 */
    val mainUiState: StateFlow<PlaylistUiState> = combine(
        _cachedMyPlayList,
        _showSheet,
        _currentListInfo,
        _isLoadingMorePlaylists,
        _noMorePlaylists,
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        PlaylistUiState(
            playlists = array[0] as List<Playlists.Playlist>,
            showSheet = array[1] as Boolean,
            selectedListCode = (array[2] as Pair<String, String>?)?.first ?: "",
            selectedListTitle = (array[2] as Pair<String, String>?)?.second ?: "",
            isLoadingMore = array[3] as Boolean,
            noMorePlaylists = array[4] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistUiState())

    fun setShowSheet(value: Boolean) {
        _showSheet.value = value
    }
    fun setListInfo(code: String, title: String) {
        _currentListInfo.value = code to title
    }

    fun updatePlaylistSheetScrollState(
        listCode: String,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
    ) {
        if (listCode.isBlank()) return
        _playlistSheetScrollStates.update { prev ->
            prev + (
                listCode to PlaylistSheetScrollState(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                )
            )
        }
    }

    fun getPlaylistSheetScrollState(listCode: String): PlaylistSheetScrollState {
        return _playlistSheetScrollStates.value[listCode] ?: PlaylistSheetScrollState()
    }

    // 加载所有playlist
    fun loadMyPlayList(page: Int = 1, forceReload: Boolean = false) {
        Log.i("current_page",page.toString())
        if (page > 1 && (_isLoadingMorePlaylists.value || _noMorePlaylists.value)) return
        if (page == 1 || forceReload) {
            playlistPage = 1
            _noMorePlaylists.value = false
        }
        if (page > 1) {
            _isLoadingMorePlaylists.value = true
        }
        val userId = Preferences.savedUserId
        viewModelScope.launch {
            NetworkRepo.getPlaylists(page, userId).collect { state ->
                when (state) {
                    is WebsiteState.Loading -> {
                        if (page == 1 || forceReload) {
                            _myPlaylistsFlow.value = state
                        }
                    }
                    is WebsiteState.Error -> {
                        _myPlaylistsFlow.value = state
                        _isLoadingMorePlaylists.value = false
                    }
                    is WebsiteState.Success -> {
                        val newList = state.info.playlists
                        if (page == 1 || forceReload) {
                            _cachedMyPlayList.value = newList
                        } else {
                            _cachedMyPlayList.value = (_cachedMyPlayList.value + newList)
                                .distinctBy(Playlists.Playlist::listCode)
                            playlistPage = page
                        }
                        if (newList.isEmpty()) {
                            _noMorePlaylists.value = true
                        }
                        _myPlaylistsFlow.value = state
                        _isLoadingMorePlaylists.value = false
                        _refreshCompleted.emit(Unit)
                    }
                }
            }
        }
    }

    // 获取单个playlist内容
    fun getPlaylistItems(page: Int = 1, listCode: String, refresh: Boolean = false) {
        Log.i("getPlaylistItems","isLoadingMore:$isLoadingMore,listCode:$listCode,")
        if (isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            if (listCode.isBlank()) return@launch
            Log.i("getPlaylistItems","page:$page,refresh:$refresh")
            // 如果是第一页或刷新，重置状态
            if (page == 1 || refresh) {
                _playlistFlow.value = emptyList()
                _playlistDesc.value = null
                _playlistStateFlow.value = PageLoadingState.Loading
            } else {
                _playlistStateFlow.value = PageLoadingState.Loading
            }
            NetworkRepo.getMyPlayListItems(page, listCode).collect { state ->
                Log.i("getPlaylistItems","state:$state")
                when (state) {
                    is PageLoadingState.Success -> {
                        Log.i("getPlaylistItems","list size:${state.info.hanimeInfo.size}")
                        _playlistDesc.value = state.info.desc
                        val newList = state.info.hanimeInfo
                        if (newList.isEmpty()) {
                            _playlistStateFlow.value = PageLoadingState.NoMoreData
                        } else {
                            _playlistFlow.update { prevList ->
                                val baseList = if (page == 1 || refresh) emptyList() else prevList
                                (baseList + newList).distinctBy(HanimeInfo::videoCode)
                            }
                            _playlistStateFlow.value = PageLoadingState.Success(state.info)
                        }
                    }

                    is PageLoadingState.Error -> {
                        _playlistStateFlow.value = PageLoadingState.Error(state.throwable)
                    }

                    is PageLoadingState.Loading -> {
                        if (page == 1 || refresh) {
                            _playlistFlow.value = emptyList()
                        }
                    }

                    is PageLoadingState.NoMoreData -> {
                        _playlistStateFlow.value = PageLoadingState.NoMoreData
                    }
                }
            }
            isLoadingMore = false
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
    fun clearCurrentList(){
        _playlistFlow.value = emptyList()
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
