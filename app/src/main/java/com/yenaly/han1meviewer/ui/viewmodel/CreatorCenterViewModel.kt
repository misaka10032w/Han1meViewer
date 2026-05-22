package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.CreatorSort
import com.yenaly.han1meviewer.logic.model.CreatorTab
import com.yenaly.han1meviewer.logic.model.CreatorUploadingItem
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.MyListItems
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.yenaly_libs.base.YenalyViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreatorCenterViewModel(application: Application) : YenalyViewModel(application) {

    private val _selectedTab = MutableStateFlow(CreatorTab.Uploaded)
    val selectedTab = _selectedTab.asStateFlow()

    private val _uploadedSort = MutableStateFlow(CreatorSort.Latest)
    val uploadedSort = _uploadedSort.asStateFlow()

    private val _uploadingSort = MutableStateFlow(CreatorSort.Latest)
    val uploadingSort = _uploadingSort.asStateFlow()

    private val _uploadedItems = MutableStateFlow(emptyList<HanimeInfo>())
    val uploadedItems = _uploadedItems.asStateFlow()

    private val _uploadingItems = MutableStateFlow(emptyList<CreatorUploadingItem>())
    val uploadingItems = _uploadingItems.asStateFlow()

    private val _uploadedState = MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>>(PageLoadingState.Loading)
    val uploadedState = _uploadedState.asStateFlow()

    private val _uploadingState = MutableStateFlow<PageLoadingState<MyListItems<CreatorUploadingItem>>>(PageLoadingState.Loading)
    val uploadingState = _uploadingState.asStateFlow()

    private val _uploadedPage = MutableStateFlow(0)
    val uploadedPage = _uploadedPage.asStateFlow()

    private val _uploadingPage = MutableStateFlow(0)
    val uploadingPage = _uploadingPage.asStateFlow()

    private val _uploadedLoadingMore = MutableStateFlow(false)
    val uploadedLoadingMore = _uploadedLoadingMore.asStateFlow()

    private val _uploadingLoadingMore = MutableStateFlow(false)
    val uploadingLoadingMore = _uploadingLoadingMore.asStateFlow()

    private var uploadedRefreshing = false
    private var uploadingRefreshing = false

    fun selectTab(tab: CreatorTab) {
        _selectedTab.value = tab
    }

    fun refreshUploaded(sort: CreatorSort = _uploadedSort.value) {
        _uploadedSort.value = sort
        uploadedRefreshing = true
        _uploadedPage.value = 0
        _uploadedItems.value = emptyList()
        _uploadedState.value = PageLoadingState.Loading
        loadUploadedPage(1)
    }

    fun loadMoreUploaded() {
        if (_uploadedLoadingMore.value || _uploadedState.value is PageLoadingState.Loading || _uploadedState.value is PageLoadingState.NoMoreData) return
        loadUploadedPage(_uploadedPage.value + 1)
    }

    fun refreshUploading(sort: CreatorSort = _uploadingSort.value) {
        _uploadingSort.value = sort
        uploadingRefreshing = true
        _uploadingPage.value = 0
        _uploadingItems.value = emptyList()
        _uploadingState.value = PageLoadingState.Loading
        loadUploadingPage(1)
    }

    fun loadMoreUploading() {
        if (_uploadingLoadingMore.value || _uploadingState.value is PageLoadingState.Loading || _uploadingState.value is PageLoadingState.NoMoreData) return
        loadUploadingPage(_uploadingPage.value + 1)
    }

    fun isLoggedIn(): Boolean = Preferences.isAlreadyLogin && Preferences.savedUserId.isNotBlank()

    private fun loadUploadedPage(page: Int) {
        val userId = Preferences.savedUserId
        if (userId.isBlank()) {
            _uploadedState.value = PageLoadingState.Error(IllegalStateException(application.getString(R.string.not_logged_in_currently)))
            return
        }
        _uploadedLoadingMore.value = !uploadedRefreshing && _uploadedItems.value.isNotEmpty()
        viewModelScope.launch {
            NetworkRepo.getUploadedVideos(userId, _uploadedSort.value, page).collect { state ->
                _uploadedState.value = state
                _uploadedItems.update { previous ->
                    when (state) {
                        is PageLoadingState.Success -> {
                            val incoming = state.info.hanimeInfo
                            if (incoming.isEmpty()) {
                                _uploadedState.value = PageLoadingState.NoMoreData
                            } else {
                                _uploadedPage.value = page
                            }
                            val base = if (uploadedRefreshing) emptyList() else previous
                            uploadedRefreshing = false
                            _uploadedLoadingMore.value = false
                            (base + incoming).distinctBy(HanimeInfo::videoCode)
                        }

                        is PageLoadingState.Loading -> previous
                        else -> {
                            uploadedRefreshing = false
                            _uploadedLoadingMore.value = false
                            previous
                        }
                    }
                }
            }
        }
    }

    private fun loadUploadingPage(page: Int) {
        val userId = Preferences.savedUserId
        if (userId.isBlank()) {
            _uploadingState.value = PageLoadingState.Error(IllegalStateException(application.getString(R.string.not_logged_in_currently)))
            return
        }
        _uploadingLoadingMore.value = !uploadingRefreshing && _uploadingItems.value.isNotEmpty()
        viewModelScope.launch {
            NetworkRepo.getUploadingVideos(userId, _uploadingSort.value, page).collect { state ->
                _uploadingState.value = state
                _uploadingItems.update { previous ->
                    when (state) {
                        is PageLoadingState.Success -> {
                            val incoming = state.info.hanimeInfo
                            if (incoming.isEmpty()) {
                                _uploadingState.value = PageLoadingState.NoMoreData
                            } else {
                                _uploadingPage.value = page
                            }
                            val base = if (uploadingRefreshing) emptyList() else previous
                            uploadingRefreshing = false
                            _uploadingLoadingMore.value = false
                            (base + incoming).distinctBy(CreatorUploadingItem::videoCode)
                        }

                        is PageLoadingState.Loading -> previous
                        else -> {
                            uploadingRefreshing = false
                            _uploadingLoadingMore.value = false
                            previous
                        }
                    }
                }
            }
        }
    }
}
