package com.yenaly.han1meviewer.ui.viewmodel.mylist

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.MyListItems
import com.yenaly.han1meviewer.logic.model.MyListType
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.yenaly_libs.base.YenalyViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavSubViewModel(application: Application) : YenalyViewModel(application) {

    var favVideoPage = 1
    private var csrfToken: String? = null

    private val _favVideoStateFlow: MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>> =
        MutableStateFlow(PageLoadingState.Loading)
    val favVideoStateFlow = _favVideoStateFlow.asStateFlow()

    private val _favVideoFlow = MutableStateFlow(emptyList<HanimeInfo>())
    val favVideoFlow = _favVideoFlow.asStateFlow()

    private val _loadedPageCount = MutableStateFlow(0)
    val loadedPageCount = _loadedPageCount.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private var isRefreshing = true

    fun getMyFavVideoItems(userId: String, page: Int) {
        _isLoadingMore.value = !isRefreshing && _favVideoFlow.value.isNotEmpty()
        viewModelScope.launch {
            NetworkRepo.getMyListItems(userId, MyListType.FAV_VIDEO, page).collect { state ->
                _favVideoStateFlow.value = state
                _favVideoFlow.update { prevList ->
                    when (state) {
                        is PageLoadingState.Success -> {
                            csrfToken = state.info.csrfToken
                            _loadedPageCount.value = page
                            if (state.info.hanimeInfo.isEmpty()){
                                _favVideoStateFlow.update { PageLoadingState.NoMoreData }
                            }
                            val baseList = if (isRefreshing) emptyList() else prevList
                            isRefreshing = false
                            _isLoadingMore.value = false
                            (baseList + state.info.hanimeInfo).distinctBy(HanimeInfo::videoCode)
                        }

                        is PageLoadingState.Loading -> prevList
                        else -> {
                            _isLoadingMore.value = false
                            prevList
                        }
                    }
                }
            }
        }
    }

    private val _deleteMyFavVideoFlow =
        MutableSharedFlow<WebsiteState<Boolean>>()
    val deleteMyFavVideoFlow = _deleteMyFavVideoFlow.asSharedFlow()

    fun deleteMyFavVideo(videoCode: String, position: Int) {
        viewModelScope.launch {
            NetworkRepo.addToMyFavVideo(
                videoCode = videoCode,
                likeStatus = true,
                currentUserId = Preferences.savedUserId,
                token = csrfToken,
            ).collect {
                _deleteMyFavVideoFlow.emit(it)
                _favVideoFlow.update { list ->
                    if (it is WebsiteState.Success) {
                        list.toMutableList().apply { removeAt(position) }
                    } else list
                }
            }
        }
    }

    fun clearMyListItems() {
        isRefreshing = true
        _isLoadingMore.value = false
        _loadedPageCount.value = 0
        _favVideoFlow.value = emptyList()
        _favVideoStateFlow.value = PageLoadingState.Loading
    }
}
