package com.yenaly.han1meviewer.ui.fragment.search

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yenaly.han1meviewer.ADVANCED_SEARCH_MAP
import com.yenaly.han1meviewer.AdvancedSearchMap
import com.yenaly.han1meviewer.HAdvancedSearch
import com.yenaly.han1meviewer.VideoCoverSize
import com.yenaly.han1meviewer.databinding.FragmentSearchBinding
import com.yenaly.han1meviewer.logic.entity.SearchHistoryEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.model.SearchOption.Companion.flatten
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.ui.StateLayoutMixin
import com.yenaly.han1meviewer.ui.adapter.FixedGridLayoutManager
import com.yenaly.han1meviewer.ui.adapter.HSubscriptionAdapter
import com.yenaly.han1meviewer.ui.adapter.HanimeSearchHistoryRvAdapter
import com.yenaly.han1meviewer.ui.adapter.HanimeVideoRvAdapter
import com.yenaly.han1meviewer.ui.viewmodel.MyListViewModel
import com.yenaly.han1meviewer.ui.viewmodel.SearchViewModel
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.dp
import com.yenaly.yenaly_libs.utils.unsafeLazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.Serializable


class SearchFragment : YenalyFragment<FragmentSearchBinding>(), StateLayoutMixin {


    val viewModel by viewModels<SearchViewModel>()
    private val myListViewModel by viewModels<MyListViewModel>()
    val adapter = HSubscriptionAdapter(this)
    private var hasAdapterLoaded = false
    val subscribeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                initSubscription()
            }
        }

    private val searchAdapter by unsafeLazy { HanimeVideoRvAdapter() }
    private val historyAdapter by unsafeLazy { HanimeSearchHistoryRvAdapter() }
    private var hasInitAdvancedSearch = false

    private val optionsPopupFragment by unsafeLazy { SearchOptionsPopupFragment() }

    private fun getAdvancedSearchMap(): Map<HAdvancedSearch, Any> {
        val raw = arguments?.get(ADVANCED_SEARCH_MAP) ?: return emptyMap()
        return when (raw) {
            is Map<*, *> -> {
                raw.mapNotNull { (k, v) ->
                    val key = when (k) {
                        is String -> runCatching { HAdvancedSearch.valueOf(k) }.getOrNull()
                        is HAdvancedSearch -> k
                        else -> null
                    }
                    if (key != null && v != null) key to v else null
                }.toMap()
            }
            is String -> mapOf(HAdvancedSearch.TAGS to raw) // 你默认使用 KEYWORD 搜索
            else -> emptyMap()
        }
    }



    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSearchBinding {
        return FragmentSearchBinding.inflate(inflater, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        Log.i("getAdvancedSearchMap",arguments.toString())
        Log.i("getAdvancedSearchMap",getAdvancedSearchMap().toString())
        if (!hasInitAdvancedSearch) {
            loadAdvancedSearch(getAdvancedSearchMap())
            hasInitAdvancedSearch = true
        }
        initSearchBar()
        initSubscription()
        binding.state.init()
        binding.searchRv.apply {
            layoutManager = if (viewModel.searchFlow.value.isNotEmpty())
                viewModel.searchFlow.value.buildFlexibleGridLayoutManager()
            else
                FixedGridLayoutManager(requireContext(), VideoCoverSize.Normal.videoInOneLine)
            if (adapter == null) {
                adapter = searchAdapter
            }
            clipToPadding = false
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                        binding.searchBar.hideHistory()
                    }
                }
            })
        }
        binding.searchSrl.apply {
            setOnLoadMoreListener { getHanimeSearchResult() }
            setOnRefreshListener { getNewHanimeSearchResult() }
            setDisableContentWhenRefresh(true)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.searchBar) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.searchRv) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = sysBars.bottom, top = sysBars.top + 68.dp)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.searchHeader) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = sysBars.top + 68.dp
            }
            WindowInsetsCompat.CONSUMED
        }
    }

//    override fun onDestroyView() {
//        binding.searchRv.apply {
//            adapter = null
//            layoutManager = null
//            clearOnScrollListeners()
//        }
//        super.onDestroyView()
//        _binding?.unbind()
//    }

    @SuppressLint("SetTextI18n")
    override fun bindDataObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.searchStateFlow.collect { state ->
                    binding.searchRv.isGone = state is PageLoadingState.Error
                    when (state) {
                        is PageLoadingState.Loading -> {
                            if (viewModel.searchFlow.value.isEmpty())
                                binding.searchSrl.autoRefresh()
                        }

                        is PageLoadingState.Success -> {
                            viewModel.page++
                            binding.searchSrl.finishRefresh()
                            binding.searchSrl.finishLoadMore(true)
                            if (!hasAdapterLoaded) {
                                binding.searchRv.layoutManager =
                                    state.info.buildFlexibleGridLayoutManager()
                                hasAdapterLoaded = true
                            }
                            binding.state.showContent()
                        }

                        is PageLoadingState.NoMoreData -> {
                            binding.searchSrl.finishLoadMoreWithNoMoreData()
                            if (viewModel.searchFlow.value.isEmpty()) {
                                binding.state.showEmpty()
                                binding.searchRv.isGone = true
                            }
                        }

                        is PageLoadingState.Error -> {
                            binding.searchSrl.finishRefresh()
                            binding.searchSrl.finishLoadMore(false)
                            binding.state.showError(state.throwable)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.searchFlow.collectLatest {
                    searchAdapter.submitList(it)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshTriggerFlow.collect {
                    binding.searchSrl.autoRefresh()
                    getNewHanimeSearchResult()
                }
            }
        }

    }


    private fun getHanimeSearchResult() {
        Log.d("SearchActivity", buildString {
            appendLine("page: ${viewModel.page}, query: ${viewModel.query}, genre: ${viewModel.genre}, ")
            appendLine("sort: ${viewModel.sort}, broad: ${viewModel.broad}, year: ${viewModel.year}, ")
            appendLine("month: ${viewModel.month}, duration: ${viewModel.duration}, ")
            appendLine("tagMap: ${viewModel.tagMap}, brandMap: ${viewModel.brandMap}")
        })
        viewModel.getHanimeSearchResult(
            viewModel.page,
            viewModel.query, viewModel.genre, viewModel.sort, viewModel.broad,
            viewModel.year, viewModel.month, viewModel.duration,
            viewModel.tagMap.flatten(), viewModel.brandMap.flatten()
        )
    }

    /**
     * 獲取最新結果，清除之前保存的所有數據
     */
    fun getNewHanimeSearchResult() {
        viewModel.page = 1
        hasAdapterLoaded = false
        viewModel.clearHanimeSearchResult()
        getHanimeSearchResult()
        Log.i("SearchDebug", "New search triggered with: ${viewModel.tagMap}, ${viewModel.year}")

    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun initSearchBar() {
//        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
//            if (binding.searchBar.hideHistory()) return@addCallback
//            requireActivity().finish()
//        }
        historyAdapter.listener = object : HanimeSearchHistoryRvAdapter.OnItemViewClickListener {
            override fun onItemClickListener(v: View, history: SearchHistoryEntity?) {
                binding.searchBar.searchText = history?.query
            }

            override fun onItemRemoveListener(v: View, history: SearchHistoryEntity?) {
                history?.let(viewModel::deleteSearchHistory)
            }
        }

        binding.searchBar.apply hsb@{
            historyAdapter = this@SearchFragment.historyAdapter
            onTagClickListener = { optionsPopupFragment.showIn(this@SearchFragment) }
            onBackClickListener = { findNavController().popBackStack() }
            onSearchClickListener = { _, text ->
                viewModel.query = text
                if (text.isNotBlank()) {
                    viewModel.insertSearchHistory(SearchHistoryEntity(text))
                }
                binding.searchSrl.autoRefresh()
            }

            textChangeFlow()
                .debounce(300)
                .flatMapLatest { viewModel.loadAllSearchHistories(it) }
                .flowOn(Dispatchers.IO)
                .onEach { (historyAdapter as HanimeSearchHistoryRvAdapter).submitList(it) }
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .launchIn(lifecycleScope)
        }
    }

    private fun initSubscription() {
        myListViewModel.subscription.getSubscriptionsWithSinglePage()
    }

    fun setSearchText(text: String?, canTextChange: Boolean = true) {
        viewModel.query = text
        binding.searchBar.searchText = text
        binding.searchBar.canTextChange = canTextChange
    }
    val searchText: String? get() = binding.searchBar.searchText

    private fun List<HanimeInfo>.buildFlexibleGridLayoutManager(): GridLayoutManager {
        val counts = if (any { it.itemType == HanimeInfo.NORMAL })
            VideoCoverSize.Normal.videoInOneLine else VideoCoverSize.Simplified.videoInOneLine
        return FixedGridLayoutManager(requireContext(), counts)
    }
    @Suppress("UNCHECKED_CAST")
    private fun loadAdvancedSearch(any: Any) {
//        if (any is String) {
//            setSearchText(any)
//            return
//        }
       // val map = any as AdvancedSearchMap
        Log.i("getAdvancedSearchMap",any.toString())
        val map: AdvancedSearchMap = when (any) {
            is Map<*, *> -> {
                val pairs = any.mapNotNull { (k, v) ->
                    val key = when (k) {
                        is HAdvancedSearch -> k
                        is String -> runCatching { HAdvancedSearch.valueOf(k) }.getOrNull()
                        else -> null
                    }
                    val value = v as? Serializable
                    if (key != null && value != null) key to value else null
                }
                val safeMap = pairs.toMap()
                Log.i("loadAdvancedSearch", safeMap.toString())
                HashMap(safeMap)
            }
            is String -> {
                hashMapOf(HAdvancedSearch.TAGS to any)
            }
            else -> {
                throw IllegalArgumentException("Expected Map or String for advanced search but got ${any?.javaClass}")
            }
        }

        (map[HAdvancedSearch.QUERY] as? String)?.let {
            setSearchText(it)
        }
        viewModel.genre = map[HAdvancedSearch.GENRE] as? String
        viewModel.sort = map[HAdvancedSearch.SORT] as? String
        viewModel.year = map[HAdvancedSearch.YEAR] as? Int
        viewModel.month = map[HAdvancedSearch.MONTH] as? Int
        viewModel.duration = map[HAdvancedSearch.DURATION] as? String
        Log.i("loadAdvancedSearch",map[HAdvancedSearch.TAGS].toString())
        when (val tags = map[HAdvancedSearch.TAGS]) {
            is Map<*, *> -> {
                val tagMap = tags as Map<Int, *>
                tagMap.forEach { (k, v) ->
                    val key = k.toString()
                    val scope = viewModel.tags[key]
                    when (v) {
                        is String -> {
                            val option = scope?.find { it.searchKey == v }
                            viewModel.tagMap[k] = option?.let(::setOf) ?: emptySet()
                        }

                        is Set<*> -> {
                            val keySet = scope?.filterTo(mutableSetOf()) { it.searchKey in v }
                            viewModel.tagMap[k] = keySet
                        }
                    }
                }
            }

            // 不推荐使用
            is String -> {
                setSearchText(tags)
                Log.i("loadAdvancedSearch2",map[HAdvancedSearch.TAGS].toString())
                kotlin.run t@{
                    viewModel.tags.forEach { (k, v) ->
                        v.find { it.searchKey == tags }?.let { so ->
                            val scopeKey = SearchOption.toScopeKey(k)
                            viewModel.tagMap[scopeKey] = setOf(so)
                            return@t
                        }
                    }
                }
            }

            // 不推荐使用
            is Set<*> -> {
                viewModel.tags.forEach { (k, v) ->
                    val keySet = v.filterTo(mutableSetOf()) { it.searchKey in tags }
                    viewModel.tagMap[SearchOption.toScopeKey(k)] = keySet
                }
            }
        }
        when (val brands = map[HAdvancedSearch.BRANDS]) {
            is String -> {
                val brand = viewModel.brands.find { it.searchKey == brands }
                viewModel.brandMap[HMultiChoicesDialog.UNKNOWN_ADAPTER] =
                    brand?.let(::setOf) ?: emptySet()
            }

            is Set<*> -> {
                val keySet = viewModel.brands.filterTo(mutableSetOf()) { it.searchKey in brands }
                viewModel.brandMap[HMultiChoicesDialog.UNKNOWN_ADAPTER] = keySet
            }
        }
    }
}
