package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.MyListItems
import com.yenaly.han1meviewer.logic.model.OnlineWatchHistorySort
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import com.yenaly.yenaly_libs.base.YenalyViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnlineWatchHistoryViewModel(application: Application) : YenalyViewModel(application) {

    private val _state = MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>>(PageLoadingState.Loading)
    val state = _state.asStateFlow()

    private val _items = MutableStateFlow(emptyList<HanimeInfo>())
    val items = _items.asStateFlow()

    private val _selectedSort = MutableStateFlow(OnlineWatchHistorySort.Latest)
    val selectedSort = _selectedSort.asStateFlow()

    private val _loadedPageCount = MutableStateFlow(0)
    val loadedPageCount = _loadedPageCount.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private val _deleteFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    val deleteFlow = _deleteFlow.asSharedFlow()

    private var isRefreshing = true
    private var isManualRefreshing = false

    fun refresh(sort: OnlineWatchHistorySort = _selectedSort.value) {
        val sortChanged = _selectedSort.value != sort
        _selectedSort.value = sort
        isRefreshing = true
        isManualRefreshing = true
        _isLoadingMore.value = false
        _loadedPageCount.value = 0
        if (sortChanged) {
            _items.value = emptyList()
        }
        _state.value = PageLoadingState.Loading
        loadPage(1)
    }

    fun loadNextPage() {
        if (_state.value is PageLoadingState.Loading || _state.value is PageLoadingState.NoMoreData || _isLoadingMore.value) {
            return
        }
        loadPage(_loadedPageCount.value + 1)
    }

    private fun loadPage(page: Int) {
        val userId = Preferences.savedUserId
        if (userId.isBlank()) {
            _state.value = PageLoadingState.Error(IllegalStateException(application.getString(com.yenaly.han1meviewer.R.string.not_logged_in_currently)))
            _isLoadingMore.value = false
            return
        }
        _isLoadingMore.value = !isRefreshing && _items.value.isNotEmpty()
        viewModelScope.launch {
            NetworkRepo.getOnlineWatchHistories(userId, _selectedSort.value, page).collect { pageState ->
                _state.value = pageState
                _items.update { previousItems ->
                    when (pageState) {
                        is PageLoadingState.Success -> {
                            val incoming = pageState.info.hanimeInfo
                            if (incoming.isEmpty()) {
                                _state.value = PageLoadingState.NoMoreData
                            } else {
                                _loadedPageCount.value = page
                            }
                            val baseItems = if (isRefreshing) emptyList() else previousItems
                            isRefreshing = false
                            isManualRefreshing = false
                            _isLoadingMore.value = false
                            (baseItems + incoming).distinctBy(HanimeInfo::videoCode)
                        }

                        is PageLoadingState.Loading -> previousItems

                        else -> {
                            isManualRefreshing = false
                            _isLoadingMore.value = false
                            previousItems
                        }
                    }
                }
            }
        }
    }

    fun isRefreshing(): Boolean = isManualRefreshing

    fun deleteItem(item: HanimeInfo) {
        val position = _items.value.indexOfFirst { it.videoCode == item.videoCode }
        if (position < 0) return
        viewModelScope.launch {
            NetworkRepo.deleteOnlineWatchHistory(
                videoCode = item.videoCode,
                position = position,
                csrfToken = csrfToken,
            ).collect { state ->
                when (state) {
                    is WebsiteState.Success -> {
                        _items.update { list ->
                            list.toMutableList().apply { removeAt(state.info) }
                        }
                        _deleteFlow.emit(WebsiteState.Success(true))
                    }

                    is WebsiteState.Error -> _deleteFlow.emit(WebsiteState.Error(state.throwable))
                    WebsiteState.Loading -> _deleteFlow.emit(WebsiteState.Loading)
                }
            }
        }
    }
}
