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
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import com.yenaly.yenaly_libs.base.YenalyViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WatchLaterSubViewModel(application: Application) : YenalyViewModel(application) {

    var watchLaterPage = 1

    private val _watchLaterStateFlow: MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>> =
        MutableStateFlow(PageLoadingState.Loading)
    val watchLaterStateFlow = _watchLaterStateFlow.asStateFlow()

    private val _watchLaterFlow = MutableStateFlow(emptyList<HanimeInfo>())
    val watchLaterFlow = _watchLaterFlow.asStateFlow()

    private val _loadedPageCount = MutableStateFlow(0)
    val loadedPageCount = _loadedPageCount.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private var isRefreshing = true

    fun getMyWatchLaterItems(page: Int) {
        val userId = Preferences.savedUserId
        _isLoadingMore.value = !isRefreshing && _watchLaterFlow.value.isNotEmpty()
        viewModelScope.launch {
            NetworkRepo.getMyListItems(userId, MyListType.WATCH_LATER, page).collect { state ->
                _watchLaterStateFlow.value = state
                _watchLaterFlow.update { prevList ->
                    when (state) {
                        is PageLoadingState.Success -> {
                            _loadedPageCount.value = page
                            if (state.info.hanimeInfo.isEmpty()) {
                                _watchLaterStateFlow.update { PageLoadingState.NoMoreData }
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

    private val _deleteMyWatchLaterFlow =
        MutableSharedFlow<WebsiteState<Boolean>>()
    val deleteMyWatchLaterFlow = _deleteMyWatchLaterFlow.asSharedFlow()

    fun deleteMyWatchLater(videoCode: String, position: Int) {
        viewModelScope.launch {
            NetworkRepo.addToMyList(
                listCode = "save",
                videoCode = videoCode,
                isChecked = false,
                position = position,
                csrfToken = csrfToken,
            ).collect { state ->
                _deleteMyWatchLaterFlow.emit(
                    when (state) {
                        is WebsiteState.Error -> WebsiteState.Error(state.throwable)
                        WebsiteState.Loading -> WebsiteState.Loading
                        is WebsiteState.Success -> WebsiteState.Success(true)
                    }
                )
                _watchLaterFlow.update { list ->
                    if (state is WebsiteState.Success) {
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
        _watchLaterFlow.value = emptyList()
        _watchLaterStateFlow.value = PageLoadingState.Loading
    }
}