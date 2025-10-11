package com.yenaly.han1meviewer.ui.fragment.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.yenaly.han1meviewer.ADVANCED_SEARCH_MAP
import com.yenaly.han1meviewer.AdvancedSearchMap
import com.yenaly.han1meviewer.HAdvancedSearch
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VideoCoverSize
import com.yenaly.han1meviewer.databinding.FragmentSearchBinding
import com.yenaly.han1meviewer.logic.entity.SearchHistoryEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.model.SearchOption.Companion.flatten
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.ui.StateLayoutMixin
import com.yenaly.han1meviewer.ui.adapter.FixedGridLayoutManager
import com.yenaly.han1meviewer.ui.adapter.HanimeSearchHistoryRvAdapter
import com.yenaly.han1meviewer.ui.adapter.HanimeVideoRvAdapter
import com.yenaly.han1meviewer.ui.viewmodel.SearchViewModel
import com.yenaly.han1meviewer.util.getSha
import com.yenaly.han1meviewer.util.isLegalBuild
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
    private var hasAdapterLoaded = false

    private val searchAdapter by unsafeLazy { HanimeVideoRvAdapter() }
    private val historyAdapter by unsafeLazy { HanimeSearchHistoryRvAdapter() }
    private var hasInitAdvancedSearch = false
    private var observersBound = false
    private val optionsPopupFragment by unsafeLazy { SearchOptionsPopupFragment() }
    @Suppress ("DEPRECATION")
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

                private var scrollDy  = 0
                private var lastDy = 0
                private var isVisible = true
                private val hideThreshold = 200    // 向上滚动多少隐藏
                private val showThreshold = 200    // 向下滚动多少显示

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if ((dy > 0 && lastDy <= 0) || (dy < 0 && lastDy >= 0)) {
                        scrollDy = 0
                    }
                    lastDy = dy
                    scrollDy += dy
                    // dy：向上滑动大于0，向下滑动小于0
                    if (dy > 0) {
                        if (isVisible && scrollDy > hideThreshold) {
                            binding.searchBar.animate()
                                .translationY(-binding.searchBar.height.toFloat())
                                .alpha(0f)
                                .setDuration(500)
                                .start()
                            isVisible = false
                            scrollDy = 0
                        }
                    } else if (dy < 0) {
                        if (!isVisible && scrollDy < -showThreshold) {
                            binding.searchBar.animate()
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(200)
                                .start()
                            isVisible = true
                            scrollDy = 0
                        }
                    }
                }
            })
        }
        binding.searchSrl.apply {
            setOnLoadMoreListener {
                viewModel.page++
                getHanimeSearchResult()
            }
            setOnRefreshListener { getNewHanimeSearchResult() }
            setDisableContentWhenRefresh(true)
        }
        binding.searchHeader.apply {
            val accentColor = MaterialColors
                .getColor(this,androidx.appcompat.R.attr.colorPrimary)
            val backgroundColor = MaterialColors
                .getColor(this, com.google.android.material.R.attr.colorOnPrimary)

            setColorSchemeColors(accentColor)
            setProgressBackgroundColorSchemeColor(backgroundColor)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.searchBar) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            WindowInsetsCompat.CONSUMED
        }

        binding.searchBar.post {
            val offset = binding.searchBar.height + 10.dp
            binding.searchRv.setPadding(
                binding.searchRv.paddingLeft,
                offset,
                binding.searchRv.paddingRight,
                binding.searchRv.paddingBottom
            )
            val headerLp = binding.searchHeader.layoutParams as ViewGroup.MarginLayoutParams
            headerLp.topMargin = offset
            binding.searchHeader.layoutParams = headerLp
        }
        if (!observersBound) {
            sfBindDataObservers()
            observersBound = true
        }
        binding.searchRv.post {
            // 手动修正因在[binding.searchBar]的offset插入之前恢复RV位置过早引起的位置错误，等到所有布局就绪再次恢复
            binding.searchRv.layoutManager?.onRestoreInstanceState(viewModel.recyclerViewState)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.recyclerViewState = binding.searchRv.layoutManager?.onSaveInstanceState()
    }

    @SuppressLint("SetTextI18n")
    private fun sfBindDataObservers() {
        Log.d("LifecycleDebug", "bindDataObservers called")
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchStateFlow.collect { state ->
                    Log.d("LifecycleDebug", "searchStateFlow collected: ${state.javaClass.simpleName}, page: ${viewModel.page}")
                    binding.searchRv.isGone = state is PageLoadingState.Error
                    when (state) {
                        is PageLoadingState.Loading -> {
                            if (viewModel.searchFlow.value.isEmpty())
                                binding.searchSrl.autoRefresh()
                        }

                        is PageLoadingState.Success -> {
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
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchFlow.collectLatest {
                    searchAdapter.submitList(it)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.refreshTriggerFlow.collect {
                    binding.searchSrl.autoRefresh()
                }
            }
        }

    }

    private fun getHanimeSearchResult() {
        Log.d("SearchActivity", buildString {
            appendLine("page: ${viewModel.page}, query: ${viewModel.query}, genre: ${viewModel.genre}, ")
            appendLine("sort: ${viewModel.sort}, broad: ${viewModel.broad}, year: ${viewModel.year}, ")
            appendLine("month: ${viewModel.month}, duration: ${viewModel.duration}, ")
            appendLine("tagMap: ${viewModel.tagMap}, brandMap: ${viewModel.brandMap}, approxTime:${viewModel.approxTime}")
        })
        val date = viewModel.getSearchDate()
        viewModel.getHanimeSearchResult(
            viewModel.page,
            viewModel.query, viewModel.genre, viewModel.sort, viewModel.broad,
            date, viewModel.duration,
            viewModel.tagMap.flatten(), viewModel.brandMap.flatten()
        )
        setSearchText(viewModel.query)
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
                viewModel.insertAdvancedSearchHistory(
                    viewModel.query,
                    viewModel.genre,
                    viewModel.sort,
                    viewModel.broad,
                    viewModel.getSearchDate(),
                    viewModel.duration,
                    viewModel.tagMap.flatten().map { SearchOption(searchKey = it) }.toSet(),
                    viewModel.brandMap.flatten().map { SearchOption(searchKey = it) }.toSet()
                )
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

    fun setSearchText(text: String?, canTextChange: Boolean = true) {
        if (!isLegalBuild(requireContext(), getSha(requireContext(),R.raw.akarin))){
            binding.searchBar.searchText = requireContext().getString(R.string.app_tampered)
            return
        }
        viewModel.query = text
        binding.searchBar.searchText = text
        binding.searchBar.canTextChange = canTextChange
    }
//    val searchText: String? get() = binding.searchBar.searchText

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
        Log.i("loadAdvancedSearch",any.toString())
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
                throw IllegalArgumentException("Expected Map or String for advanced search but got ${any.javaClass}")
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
                val tagMap = tags as Map<String, *>
                tagMap.forEach { (k, v) ->
                    val scope = viewModel.tags[k]
                    when (v) {
                        is String -> {
                            val option = scope?.find { it.searchKey == v }
                            val key = SearchOption.toScopeKey(k)
                            viewModel.tagMap[key] = option?.let(::setOf) ?: emptySet()
                        }

                        is Set<*> -> {
                            val keySet = scope?.filterTo(mutableSetOf()) { it.searchKey in v }
                            val key = SearchOption.toScopeKey(k)
                            viewModel.tagMap[key] = keySet
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
