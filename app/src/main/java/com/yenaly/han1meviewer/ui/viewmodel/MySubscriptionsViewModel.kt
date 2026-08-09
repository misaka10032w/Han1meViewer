package com.yenaly.han1meviewer.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.MySubscriptions
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
import com.yenaly.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.CancellationException
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

    private val _refreshCompleted = MutableSharedFlow<Unit>()
    val refreshCompleted: SharedFlow<Unit> = _refreshCompleted

    fun reset() {
        currentPage = 1
        hasMore = true
        isLoadingMore = false
        cachedVideos.clear()
        cachedArtists.clear()
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
                    if (e is CancellationException) throw e
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
                        } else if (info.subscriptions.isNotEmpty()) {
                            // 后续页若返回新的订阅作者，按 artistName 去重后合并，避免缓存长期失效
                            val existingNames = cachedArtists.map { it.artistName }.toMutableSet()
                            info.subscriptions.forEach { item ->
                                if (existingNames.add(item.artistName)) {
                                    cachedArtists.add(item)
                                }
                            }
                        }
                        if (info.subscriptionsVideos.isNotEmpty()) {
                            // 按 videoCode 去重，避免分页返回重叠导致列表出现重复项
                            val existingCodes = cachedVideos.map { it.videoCode }.toMutableSet()
                            info.subscriptionsVideos.forEach { item ->
                                if (existingCodes.add(item.videoCode)) {
                                    cachedVideos.add(item)
                                }
                            }
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
}
