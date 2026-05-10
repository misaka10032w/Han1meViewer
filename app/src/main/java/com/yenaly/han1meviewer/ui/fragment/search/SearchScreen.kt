package com.yenaly.han1meviewer.ui.fragment.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.SearchHistoryEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.ui.component.EmptyView
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.han1meviewer.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// ─────────────────────────────────────────────
// 搜索 App Bar
// ─────────────────────────────────────────────

@Composable
fun SearchAppBar(
    query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit,
    onBack: () -> Unit, onOpenAdvancedSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit, focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val kb = LocalSoftwareKeyboardController.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .focusRequester(focusRequester)
            ) {
                BasicTextField(
                    value = query, onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .onFocusChanged { onFocusChanged(it.isFocused) }
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { kb?.hide(); onSearch() }),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text(
                                "搜索影片...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            inner()
                        }
                    }
                )
            }
            if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    Icons.Default.Close,
                    "清除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "筛选",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenAdvancedSearch() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// 搜索历史列表
// ─────────────────────────────────────────────

@Composable
fun SearchHistoryList(
    histories: List<SearchHistoryEntity>,
    onHistoryClick: (String) -> Unit,
    onDeleteHistory: (SearchHistoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = histories.isNotEmpty(),
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }) {
        Column(modifier = modifier) {
            histories.forEach { h ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHistoryClick(h.query) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        h.query,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = { onDeleteHistory(h) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "删除",
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 搜索结果网格
// ─────────────────────────────────────────────

@Composable
fun SearchResultsGrid(
    videos: List<HanimeInfo>, state: PageLoadingState<*>, onVideoClick: (String) -> Unit,
    onVideoLongClick: (String, String) -> Unit, onLoadMore: () -> Unit,
    canLoadMore: Boolean, gridState: LazyGridState, modifier: Modifier = Modifier
) {
    var isLoadingMore by remember { mutableStateOf(false) }
    LaunchedEffect(gridState, videos.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.map { it ?: 0 }
            .distinctUntilChanged().collect { last ->
            if (!isLoadingMore && last >= videos.size - 6 && canLoadMore) {
                isLoadingMore = true
                onLoadMore()
            }
        }
    }
    LaunchedEffect(state) { if (state !is PageLoadingState.Loading) isLoadingMore = false }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 164.dp),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(videos, key = { it.videoCode }) {
            VideoCardItem(
                it,
                true,
                onVideoClick,
                onVideoLongClick
            )
        }
        if (canLoadMore && state is PageLoadingState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 搜索状态 / 筛选标签
// ─────────────────────────────────────────────

@Composable
fun SearchStateIndicator(
    state: PageLoadingState<*>,
    resultCount: Int,
    modifier: Modifier = Modifier
) {
    when (state) {
        is PageLoadingState.Loading -> if (resultCount == 0) Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        is PageLoadingState.NoMoreData -> if (resultCount == 0) EmptyView(
            hint = "没有搜索到结果",
            picRes = R.drawable.h_chan_speechless
        )

        is PageLoadingState.Error -> EmptyView(
            hint = "加载失败: ${state.throwable.message}",
            picRes = R.drawable.h_chan_sad
        )
        is PageLoadingState.Success -> if (resultCount == 0) EmptyView(
            hint = "没有搜索到结果",
            picRes = R.drawable.h_chan_speechless
        )
    }
}

data class SearchFilter(
    val genre: String? = null,
    val sort: String? = null,
    val duration: String? = null
) {
    fun isNotEmpty() = genre != null || sort != null || duration != null
}

@Composable
fun ActiveFilterChips(
    filter: SearchFilter,
    onClearFilter: () -> Unit,
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    if (!filter.isNotEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOfNotNull(
            filter.genre?.let {
                viewModel.genres.find { g -> g.searchKey == it }?.name ?: it
            },
            filter.sort?.let { viewModel.sortOptions.find { s -> s.searchKey == it }?.name ?: it },
            filter.duration?.let {
                viewModel.durations.find { d -> d.searchKey == it }?.name ?: it
            }).forEach { label ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.clickable { onClearFilter() }) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 搜索主屏幕
// ─────────────────────────────────────────────

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    FlowPreview::class
)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel, onBack: () -> Unit, onOpenVideo: (String) -> Unit,
    onLongPressCopy: (String, String) -> Unit, onOpenAdvancedSearch: () -> Unit,
    initialQuery: String? = null, modifier: Modifier = Modifier
) {
    val searchState by viewModel.searchStateFlow.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchFlow.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf(initialQuery ?: "") }
    var histories by remember { mutableStateOf<List<SearchHistoryEntity>>(emptyList()) }
    var hasSearched by rememberSaveable { mutableStateOf(initialQuery != null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(SearchFilter()) }
    var isSearchFocused by remember { mutableStateOf(false) }

    val refreshState = rememberPullToRefreshState()
    val gridState = rememberLazyGridState()
    val focusReq = remember { FocusRequester() }
    val focusMgr = LocalFocusManager.current
    val kb = LocalSoftwareKeyboardController.current

    // 搜索执行
    fun executeSearch() {
        viewModel.getHanimeSearchResult(
            viewModel.page,
            viewModel.query,
            viewModel.genre,
            viewModel.sort,
            viewModel.broad,
            viewModel.getSearchDate(),
            viewModel.duration,
            tagFlatten(viewModel.tagMap),
            brandFlatten(viewModel.brandMap)
        )
    }

    fun doSearch() {
        viewModel.page = 1; viewModel.clearHanimeSearchResult(); executeSearch()
    }

    // 初始 query 自动搜索
    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank() && !hasSearched) {
            hasSearched = true; viewModel.query = initialQuery; viewModel.insertSearchHistory(
                SearchHistoryEntity(query = initialQuery)
            ); executeSearch()
        }
    }
    // 高级搜索参数（genre/sort 等）自动搜索
    LaunchedEffect(Unit) {
        if (!hasSearched && (viewModel.genre != null || viewModel.sort != null || viewModel.duration != null)) {
            hasSearched = true; doSearch()
        }
    }
    // refreshTriggerFlow
    LaunchedEffect(Unit) { viewModel.refreshTriggerFlow.collect { executeSearch() } }

    // 历史建议防抖
    @OptIn(FlowPreview::class)
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(300); viewModel.loadAllSearchHistories(searchQuery).collect { histories = it }
        } else {
            viewModel.loadAllSearchHistories().collect { histories = it.take(10) }
        }
    }

    LaunchedEffect(Unit) { focusReq.requestFocus() }
    LaunchedEffect(searchState) {
        if (searchState !is PageLoadingState.Loading) isRefreshing = false
    }

    // 返回键：有焦点时先关键盘
    BackHandler(enabled = isSearchFocused) { focusMgr.clearFocus(); kb?.hide() }

    Column(modifier = modifier.fillMaxSize()) {
        SearchAppBar(searchQuery, { searchQuery = it }, onSearch = {
            val q = searchQuery.trim()
            if (q.isNotBlank()) {
                hasSearched = true; viewModel.query =
                    q; focusMgr.clearFocus(); kb?.hide(); viewModel.insertSearchHistory(
                    SearchHistoryEntity(query = q)
                ); doSearch()
            }
        }, onBack, onOpenAdvancedSearch, { isSearchFocused = it }, focusReq)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ActiveFilterChips(
            filter,
            {
                filter = SearchFilter(); viewModel.genre = null; viewModel.sort =
                null; viewModel.duration = null; doSearch()
            },
            viewModel
        )

        Box(
            Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = { isRefreshing = true; doSearch() })
        ) {
            if (hasSearched) {
                // 已触发搜索，显示结果
                val showResults = searchResults.ifEmpty {
                    (searchState as? PageLoadingState.Success)?.info ?: emptyList()
                }
                Box(Modifier.fillMaxSize()) {
                    SearchStateIndicator(searchState, showResults.size)
                    if (showResults.isNotEmpty()) SearchResultsGrid(
                        showResults,
                        searchState,
                        onOpenVideo,
                        onLongPressCopy,
                        { viewModel.page++; executeSearch() },
                        searchState !is PageLoadingState.NoMoreData,
                        gridState
                    )
                }
            } else if (searchQuery.isBlank() && histories.isNotEmpty()) {
                // 未搜索 + 搜索框为空 → 显示历史
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "最近搜索",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    SearchHistoryList(
                        histories,
                        { query ->
                            searchQuery = query; hasSearched = true; viewModel.query =
                            query; focusMgr.clearFocus(); kb?.hide(); viewModel.insertSearchHistory(
                            SearchHistoryEntity(query = query)
                        ); doSearch()
                        },
                        { h ->
                            viewModel.deleteSearchHistory(h); histories =
                            histories.filter { it.id != h.id }
                        })
                }
            }
            if (isRefreshing) Box(Modifier.align(Alignment.TopCenter)) {
                PullToRefreshDefaults.Indicator(
                    refreshState,
                    isRefreshing,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 辅助
// ─────────────────────────────────────────────

private fun tagFlatten(map: android.util.SparseArray<Set<SearchOption>>): Set<String> {
    val r = mutableSetOf<String>(); for (i in 0 until map.size()) {
        map.valueAt(i).mapNotNullTo(r) { it.searchKey }
    }; return r
}

private fun brandFlatten(map: android.util.SparseArray<Set<SearchOption>>): Set<String> {
    val r = mutableSetOf<String>(); for (i in 0 until map.size()) {
        map.valueAt(i).mapNotNullTo(r) { it.searchKey }
    }; return r
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(showBackground = true, name = "搜索页顶栏")
@Composable
private fun SearchAppBarPreview() {
    MaterialTheme { SearchAppBar("", {}, {}, {}, {}, {}, remember { FocusRequester() }) }
}

@Preview(showBackground = true, name = "搜索结果网格")
@Composable
private fun SearchResultsGridPreview() {
    MaterialTheme {
        SearchResultsGrid(
            fakeHomePageVideos,
            PageLoadingState.Success(fakeHomePageVideos),
            {},
            { _, _ -> },
            {},
            true,
            rememberLazyGridState()
        )
    }
}
