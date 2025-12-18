package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import android.os.Parcelable
import android.util.Log
import android.util.SparseArray
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.DatabaseRepo.HanimeAdvancedSearchRepo.toSearchOptionSet
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.entity.HanimeAdvancedSearchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.SearchHistoryEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.util.loadAssetAs
import com.yenaly.yenaly_libs.base.YenalyViewModel
import com.yenaly.yenaly_libs.utils.unsafeLazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/13 013 22:29
 */
class SearchViewModel(
    application: Application,
    private val state: SavedStateHandle
) : YenalyViewModel(application) {

    var page: Int = 1
    var query: String? = null

    // START: Use in [ChildCommentPopupFragment.kt]
    var genre: String?
        get() = state["genre"]
        set(value) { state["genre"] = value }

    var sort: String?
        get() = state["sort"]
        set(value) { state["sort"] = value }

    var year: Int?
        get() = state["year"]
        set(value) { state["year"] = value }

    var month: Int?
        get() = state["month"]
        set(value) { state["month"] = value }

    var approxTime: String?
        get() = state["approxTime"]
        set(value) { state["approxTime"] = value }

    var broad: Boolean = false
    var duration: String? = null

    var tagMap = SparseArray<Set<SearchOption>>()
    var brandMap = SparseArray<Set<SearchOption>>()

    // END: Use in [ChildCommentPopupFragment.kt]

    // START: Use in [SearchOptionsPopupFragment.kt]

    val genres by unsafeLazy {
        loadAssetAs<List<SearchOption>>(if (Preferences.baseUrl == HANIME_URL[3]) "search_options/genre_av.json" else "search_options/genre.json").orEmpty()
    }

    val tags by unsafeLazy {
        loadAssetAs<Map<String, List<SearchOption>>>("search_options/tags.json").orEmpty()
    }

    val brands by unsafeLazy {
        loadAssetAs<List<SearchOption>>("search_options/brands.json").orEmpty()
    }

    val sortOptions by unsafeLazy {
        loadAssetAs<List<SearchOption>>("search_options/sort_option.json").orEmpty()
    }

    val durations by unsafeLazy {
        loadAssetAs<List<SearchOption>>("search_options/duration.json").orEmpty()
    }
    val timeList by unsafeLazy {
        loadAssetAs<List<SearchOption>>("search_options/release_date.json").orEmpty()
    }

    // END: Use in [SearchOptionsPopupFragment.kt]

    private val _searchStateFlow =
        MutableStateFlow<PageLoadingState<List<HanimeInfo>>>(PageLoadingState.Loading)
    val searchStateFlow = _searchStateFlow.asStateFlow()

    private val _searchFlow = MutableStateFlow(emptyList<HanimeInfo>())
    val searchFlow = _searchFlow.asStateFlow()
    var recyclerViewState: Parcelable? = null

    fun clearHanimeSearchResult() = _searchStateFlow.update { PageLoadingState.Loading }

    fun getHanimeSearchResult(
        page: Int, query: String?, genre: String?,
        sort: String?, broad: Boolean, date: String?,
        duration: String?, tags: Set<String>, brands: Set<String>,
    ) {
        viewModelScope.launch {
            NetworkRepo.getHanimeSearchResult(
                page, query, genre,
                sort, broad, date ,
                duration, tags, brands
            ).collect { state ->
                val prev = _searchStateFlow.getAndUpdate { state }
                if (prev is PageLoadingState.Loading) _searchFlow.value = emptyList()
                _searchFlow.update { prevList ->
                    when (state) {
//                        is PageLoadingState.Success -> prevList + state.info
                        is PageLoadingState.Success -> {
                            val list = state.info
                            val codes = list.map { it.videoCode }
                            val watchedCodes= withContext(Dispatchers.IO) {
                                DatabaseRepo.WatchHistory.getWatched(codes).toSet()
                            }
                            val updatedList = list.map { item ->
                                item.copy(watched = watchedCodes.contains(item.videoCode))
                            }
                            prevList + updatedList
                        }
                        is PageLoadingState.Loading -> emptyList()
                        else -> prevList
                    }
                }
            }
        }
    }

    fun insertSearchHistory(history: SearchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.SearchHistory.insert(history)
            Log.d("insert_search_hty", "$history DONE!")
        }
    }

    fun getSearchDate(): String? {
        return when {
            approxTime != null -> approxTime
            year != null -> listOfNotNull(
                year?.let { "$it 年" },
                month?.let { "$it 月" }
            ).joinToString(" ")
            else -> null
        }
    }

    fun insertAdvancedSearchHistory(
        query: String?, genre: String?,
        sort: String?, broad: Boolean, date: String?,
        duration: String?, tags: Set<SearchOption>, brands: Set<SearchOption>,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val histories = DatabaseRepo.HanimeAdvancedSearchRepo.getSearchHistories(limit = 10)
                .first()

            val isDuplicate = histories.any { history ->
                history.query == query &&
                        history.genre == genre &&
                        history.sort == sort &&
                        history.broad == broad &&
                        history.date == date &&
                        history.duration == duration &&
                        history.tags?.toSearchOptionSet() == tags &&
                        history.brands?.toSearchOptionSet() == brands
            }

            if (!isDuplicate) {
                DatabaseRepo.HanimeAdvancedSearchRepo.saveSearch(
                    query = query,
                    genre = genre,
                    sort = sort,
                    broad = broad,
                    date = date,
                    duration = duration,
                    tags = tags,
                    brands = brands,
                )
                return@launch
            }
            Log.i("insertAdvancedSearchHistory","记录重复！")

        }
    }

    fun deleteSearchHistory(history: SearchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.SearchHistory.delete(history)
            Log.d("delete_search_hty", "$history DONE!")
        }
    }

    @JvmOverloads
    fun loadAllSearchHistories(keyword: String? = null) =
        DatabaseRepo.SearchHistory.loadAll(keyword).flowOn(Dispatchers.IO)

    fun deleteSearchHistoryByKeyword(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.SearchHistory.deleteByKeyword(query)
            Log.d("delete_search_hty", "$query DONE!")
        }
    }
    val refreshTriggerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    fun triggerNewSearch() {
        page = 1
        clearHanimeSearchResult()
        refreshTriggerFlow.tryEmit(Unit)
    }
    fun restoreSearchMap(history: HanimeAdvancedSearchHistoryEntity) {
        with(this) {
            page = 1
            query = history.query
            genre = history.genre
            sort = history.sort
            broad = history.broad == true
            duration = history.duration

            restoreDate(this, history.date)

            tagMap.clear()
            brandMap.clear()

            history.tags?.takeIf { it.isNotBlank() }?.let { tagsString ->
                val tagOptions = tagsString.toSearchOptionSet()
                tagMap.put(0, tagOptions)
            }

            history.brands?.takeIf { it.isNotBlank() }?.let { brandsString ->
                val brandOptions = brandsString.toSearchOptionSet()
                brandMap.put(0, brandOptions)
            }
        }
    }
    private fun restoreDate(viewModel: SearchViewModel, date: String?) {
        if (date.isNullOrBlank()) {
            viewModel.year = null
            viewModel.month = null
            viewModel.approxTime = null
            return
        }

        if (date.contains("過去")) {
            viewModel.approxTime = date
            viewModel.year = null
            viewModel.month = null
        } else {
            viewModel.approxTime = null
            val regex = """(\d+)\s*年(?:\s*(\d+)\s*月)?""".toRegex()
            val match = regex.find(date)
            if (match != null) {
                val (y, m) = match.destructured
                viewModel.year = y.toIntOrNull()
                viewModel.month = m.toIntOrNull()
            } else {
                viewModel.year = null
                viewModel.month = null
            }
        }
    }
}