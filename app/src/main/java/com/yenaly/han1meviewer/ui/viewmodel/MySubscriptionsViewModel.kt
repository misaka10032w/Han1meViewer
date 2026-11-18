package com.yenaly.han1meviewer.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.MySubscriptions
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
import com.yenaly.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class MySubscriptionsViewModel : ViewModel() {

    private val _subscriptionsState = MutableStateFlow<WebsiteState<MySubscriptions>>(WebsiteState.Loading)
    val subscriptionsState: StateFlow<WebsiteState<MySubscriptions>> = _subscriptionsState.asStateFlow()

    private var currentPage = 1
    private var hasMore = true
    private var isLoadingMore = false
    private val cachedVideos = mutableListOf<SubscriptionVideosItem>()
    private val cachedArtists = mutableListOf<SubscriptionItem>()

    private val _loadedPage = MutableStateFlow(1)
    val loadedPage = _loadedPage.asStateFlow()

    private val _refreshCompleted = MutableSharedFlow<Unit>()
    val refreshCompleted: SharedFlow<Unit> = _refreshCompleted

    private var hasLoaded = false
    fun reset() {
        hasLoaded = false
        _subscriptionsState.value = WebsiteState.Loading
    }

    fun loadMySubscriptions(forceReload: Boolean = false) {
        if (isLoadingMore) return
        if (forceReload) {
            currentPage = 1
            hasMore = true
            cachedVideos.clear()
            cachedArtists.clear()
        }
        isLoadingMore = true

        viewModelScope.launch {
            NetworkRepo.getMySubscriptions(page = currentPage)
                .onStart {
                    if (currentPage == 1) {
                        _subscriptionsState.value = WebsiteState.Loading
                    }
                }
                .catch { e ->
                    _subscriptionsState.value = WebsiteState.Error(e)
                    _refreshCompleted.emit(Unit)
                    isLoadingMore = false
                }
                .collect { state ->
                    if (state is WebsiteState.Success) {
                        _refreshCompleted.emit(Unit)
                        val info = state.info
                        if (currentPage == 1) {
                            cachedArtists.clear()
                            cachedArtists.addAll(info.subscriptions)
                        }
                        if (info.subscriptionsVideos.isNotEmpty()) {
                            _loadedPage.value = currentPage
                            // 翻页边界可能重叠：加载下一页期间若有新投稿，按时间排序的
                            // 两页会在边界返回重复条目；订阅列表以 videoCode 为 Lazy key，
                            // 重复会抛 "Key was already used" 崩溃，拼接时跳过已有条目。
                            val newVideos = info.subscriptionsVideos.filter { incoming ->
                                cachedVideos.none { it.videoCode == incoming.videoCode }
                            }
                            cachedVideos.addAll(newVideos)
                            currentPage++
                            Log.i("getMySubscriptions","currentPage:$currentPage")
                        } else {
                            hasMore = false
                        }
                        _subscriptionsState.value = WebsiteState.Success(
                            MySubscriptions(
                                subscriptions = cachedArtists.toList(),
                                subscriptionsVideos = cachedVideos.toList(),
                                maxPage = info.maxPage
                                )
                        )
                    } else if (state is WebsiteState.Error){
                        _subscriptionsState.value = WebsiteState.Error(state.throwable)
                    }
                    isLoadingMore = false
                }
        }
    }

    fun canLoadMore() = hasMore && !isLoadingMore

    fun goToPage(page: Int) {
        if (isLoadingMore) return
        currentPage = page
        hasMore = true
        cachedVideos.clear()
        cachedArtists.clear()
        isLoadingMore = false
        _subscriptionsState.value = WebsiteState.Loading
        loadMySubscriptions(forceReload = false)
    }
}
