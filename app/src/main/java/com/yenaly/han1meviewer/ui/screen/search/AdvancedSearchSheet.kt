package com.yenaly.han1meviewer.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.util.isNotEmpty
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SEARCH_YEAR_RANGE_END
import com.yenaly.han1meviewer.SEARCH_YEAR_RANGE_START
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.model.SearchOption.Companion.flatten
import com.yenaly.han1meviewer.logic.model.SearchOption.Companion.get
import com.yenaly.han1meviewer.ui.component.SelectableTag
import com.yenaly.han1meviewer.ui.component.SettingChoiceItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.model.AdvancedSearchDialogState
import com.yenaly.han1meviewer.ui.model.SearchScopeSection
import com.yenaly.han1meviewer.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSearchSheet(
    viewModel: SearchViewModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val histories by remember {
        DatabaseRepo.HanimeAdvancedSearchRepo.getSearchHistories()
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var dialogState by remember { mutableStateOf<AdvancedSearchDialogState?>(null) }
    var selectionVersion by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val typeLabel = stringResource(R.string.type)
    val sortLabel = stringResource(R.string.sort_option)
    val tagLabel = stringResource(R.string.tag)
    val releaseDateLabel = stringResource(R.string.release_date)
    val durationLabel = stringResource(R.string.duration)

    fun updateSelection(block: () -> Unit) {
        block()
        selectionVersion++
    }

    fun selectedOptionValue(options: List<SearchOption>, searchKey: String?): String? {
        return options.firstOrNull { it.searchKey == searchKey }?.value
    }

    val genreTitle = remember(selectionVersion, typeLabel) {
        selectedOptionValue(viewModel.genres, viewModel.genre)?.let { "$typeLabel: $it" } ?: typeLabel
    }
    val sortTitle = remember(selectionVersion, sortLabel) {
        selectedOptionValue(viewModel.sortOptions, viewModel.sort)?.let { "$sortLabel: $it" } ?: sortLabel
    }
    val selectedTagKeys = remember(selectionVersion) { viewModel.tagMap.flatten() }
    val selectedTagCount = remember(selectionVersion) { selectedTagKeys.size }
    val selectedTagOptions = remember(selectionVersion) {
        viewModel.tags.values
            .flatten()
            .filter { it.searchKey in selectedTagKeys }
            .toSet()
    }
    val tagTitle = remember(selectionVersion, tagLabel) {
        if (selectedTagCount > 0) "$tagLabel ($selectedTagCount)" else tagLabel
    }
    val releaseDateTitle = remember(selectionVersion, releaseDateLabel) {
        viewModel.getSearchDate()?.let { "$releaseDateLabel: $it" } ?: releaseDateLabel
    }
    val durationTitle = remember(selectionVersion, durationLabel) {
        selectedOptionValue(viewModel.durations, viewModel.duration)?.let { "$durationLabel: $it" }
            ?: durationLabel
    }

    dialogState?.let { state ->
        when (state) {
            is AdvancedSearchDialogState.SingleChoice -> {
                AlertDialog(
                    onDismissRequest = { dialogState = null },
                    title = { Text(stringResource(state.titleRes)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.options.forEachIndexed { index, option ->
                                SettingChoiceItem(
                                    title = option.value,
                                    selected = index == state.selectedIndex,
                                    onClick = {
                                        updateSelection { state.onSelect(option) }
                                        dialogState = null
                                    },
                                )
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                updateSelection { state.onReset() }
                                dialogState = null
                            }) {
                                Text(stringResource(R.string.reset))
                            }
                            TextButton(onClick = { dialogState = null }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    },
                )
            }

            is AdvancedSearchDialogState.MultiChoice -> {
                var selected by remember(state.key) { mutableStateOf(state.selected.toMutableSet()) }
                var broad by remember(state.key) { mutableStateOf(state.broad) }
                val pagerState = rememberPagerState(pageCount = { state.scopes.size })
                AlertDialog(
                    onDismissRequest = { dialogState = null },
                    title = { Text(stringResource(state.titleRes)) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.pair_widely),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = stringResource(R.string.pair_widely_alert),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Switch(
                                    checked = broad,
                                    onCheckedChange = { broad = it },
                                )
                            }
                            PrimaryScrollableTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                edgePadding = 16.dp,
                                divider = {}
                            ) {
                                state.scopes.forEachIndexed { index, scopeSection ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        text = {
                                            Text(
                                                text = stringResource(scopeSection.titleRes),
                                                softWrap = false
                                            )
                                        },
                                    )
                                }
                            }
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.height(320.dp),
                                verticalAlignment = Alignment.Top
                            ) { page ->
                                val scopeSection = state.scopes[page]
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(scopeSection.spanCount),
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(scopeSection.options, key = { it.searchKey.orEmpty() }) { option ->
                                        SelectableTag(
                                            text = option.value,
                                            selected = option in selected,
                                            onClick = {
                                                selected = selected.toMutableSet().also {
                                                    if (!it.add(option)) it.remove(option)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            updateSelection { state.onSave(selected, broad) }
                            dialogState = null
                        }) {
                            Text(stringResource(R.string.save))
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                updateSelection { state.onReset() }
                                dialogState = null
                            }) {
                                Text(stringResource(R.string.reset))
                            }
                            TextButton(onClick = { dialogState = null }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    },
                )
            }

            is AdvancedSearchDialogState.ReleaseDate -> {
                var selectedTab by remember(state.key) {
                    mutableIntStateOf(if (state.initialApproximate != null) 1 else 0)
                }
                var yearOnly by remember(state.key) {
                    mutableStateOf(state.initialMonth == null)
                }
                var selectedYear by remember(state.key) {
                    mutableIntStateOf(state.initialYear ?: SEARCH_YEAR_RANGE_END)
                }
                var selectedMonth by remember(state.key) {
                    mutableIntStateOf(state.initialMonth ?: 1)
                }
                var selectedApproximate by remember(state.key) {
                    mutableStateOf(state.initialApproximate)
                }
                AlertDialog(
                    onDismissRequest = { dialogState = null },
                    title = { Text(stringResource(R.string.release_date)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { selectedTab = 0 }) {
                                    Text(stringResource(R.string.specific_y_m))
                                }
                                FilledTonalButton(onClick = { selectedTab = 1 }) {
                                    Text(stringResource(R.string.approximate_range))
                                }
                            }
                            if (selectedTab == 0) {
                                TextButton(onClick = { yearOnly = !yearOnly }) {
                                    Text(
                                        stringResource(
                                            if (yearOnly) R.string.switch_to_year_month else R.string.switch_to_year
                                        )
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    WheelLikeColumn(
                                        title = "Year",
                                        values = (SEARCH_YEAR_RANGE_START..SEARCH_YEAR_RANGE_END).toList(),
                                        selectedValue = selectedYear,
                                        modifier = Modifier.weight(1f),
                                        label = { value -> value.toString() },
                                        onSelect = { selectedYear = it },
                                    )
                                    if (!yearOnly) {
                                        WheelLikeColumn(
                                            title = "Month",
                                            values = (1..12).toList(),
                                            selectedValue = selectedMonth,
                                            modifier = Modifier.weight(1f),
                                            label = { value -> value.toString() },
                                            onSelect = { selectedMonth = it },
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 300.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(state.options, key = { it.searchKey.orEmpty() }) { option ->
                                        SettingChoiceItem(
                                            title = option.value,
                                            selected = selectedApproximate == option.searchKey,
                                            onClick = { selectedApproximate = option.searchKey },
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (selectedTab == 0) {
                                updateSelection {
                                    state.onSaveSpecific(selectedYear, if (yearOnly) null else selectedMonth)
                                }
                            } else {
                                updateSelection { state.onSaveApproximate(selectedApproximate) }
                            }
                            dialogState = null
                        }) {
                            Text(stringResource(R.string.save))
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                updateSelection { state.onReset() }
                                dialogState = null
                            }) {
                                Text(stringResource(R.string.reset))
                            }
                            TextButton(onClick = { dialogState = null }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    },
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.advanced_search),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                if (histories.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.advanced_search_combination),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    item {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(histories, key = { it.id }) { history ->
                                AdvancedSearchHistoryCard(
                                    history = history,
                                    onDelete = {
                                        scope.launch {
                                            DatabaseRepo.HanimeAdvancedSearchRepo.deleteHistory(history.id)
                                        }
                                    },
                                    onClick = {
                                        viewModel.restoreSearchMap(history)
                                        viewModel.triggerNewSearch()
                                        onDismiss()
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AdvancedSearchChip(
                            title = genreTitle,
                            checked = viewModel.genre != null,
                            modifier = Modifier.weight(1f),
                            onLongClick = { updateSelection { viewModel.genre = null } },
                            onClick = {
                                dialogState = AdvancedSearchDialogState.SingleChoice(
                                    key = "genre",
                                    titleRes = R.string.type,
                                    options = viewModel.genres,
                                    selectedIndex = viewModel.genres.indexOfFirst { it.searchKey == viewModel.genre },
                                    onSelect = { option -> viewModel.genre = option.searchKey },
                                    onReset = { viewModel.genre = null },
                                )
                            },
                        )
                        AdvancedSearchChip(
                            title = sortTitle,
                            checked = viewModel.sort != null,
                            modifier = Modifier.weight(1f),
                            onLongClick = { updateSelection { viewModel.sort = null } },
                            onClick = {
                                dialogState = AdvancedSearchDialogState.SingleChoice(
                                    key = "sort",
                                    titleRes = R.string.sort_option,
                                    options = viewModel.sortOptions,
                                    selectedIndex = viewModel.sortOptions.indexOfFirst { it.searchKey == viewModel.sort },
                                    onSelect = { option -> viewModel.sort = option.searchKey },
                                    onReset = { viewModel.sort = null },
                                )
                            },
                        )
                        AdvancedSearchChip(
                            title = tagTitle,
                            checked = viewModel.tagMap.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            onLongClick = { updateSelection { viewModel.tagMap.clear() } },
                            onClick = {
                                dialogState = AdvancedSearchDialogState.MultiChoice(
                                    key = "tag",
                                    titleRes = R.string.tag,
                                    scopes = listOf(
                                        SearchScopeSection(R.string.video_attr, viewModel.tags[R.string.video_attr]),
                                        SearchScopeSection(R.string.relationship, viewModel.tags[R.string.relationship]),
                                        SearchScopeSection(R.string.characteristics, viewModel.tags[R.string.characteristics]),
                                        SearchScopeSection(R.string.appearance_and_figure, viewModel.tags[R.string.appearance_and_figure]),
                                        SearchScopeSection(R.string.story_plot, viewModel.tags[R.string.story_plot]),
                                        SearchScopeSection(R.string.story_location, viewModel.tags[R.string.story_location]),
                                        SearchScopeSection(R.string.sex_position, viewModel.tags[R.string.sex_position]),
                                    ),
                                    selected = selectedTagOptions,
                                    broad = viewModel.broad,
                                    onSave = { selected, broad ->
                                        viewModel.broad = broad
                                        val grouped = android.util.SparseArray<Set<SearchOption>>()
                                        selected.forEach { option ->
                                            listOf(
                                                R.string.video_attr,
                                                R.string.relationship,
                                                R.string.characteristics,
                                                R.string.appearance_and_figure,
                                                R.string.story_plot,
                                                R.string.story_location,
                                                R.string.sex_position,
                                            ).forEach { scope ->
                                                if (viewModel.tags[scope].contains(option)) {
                                                    val current = grouped[scope] ?: emptySet()
                                                    grouped.put(scope, current + option)
                                                }
                                            }
                                        }
                                        viewModel.tagMap = grouped
                                    },
                                    onReset = { viewModel.tagMap.clear() },
                                )
                            },
                        )
                        AdvancedSearchChip(
                            title = releaseDateTitle,
                            checked = viewModel.year != null || viewModel.month != null || viewModel.approxTime != null,
                            modifier = Modifier.weight(1f),
                            onLongClick = {
                                updateSelection {
                                    viewModel.year = null
                                    viewModel.month = null
                                    viewModel.approxTime = null
                                }
                            },
                            onClick = {
                                dialogState = AdvancedSearchDialogState.ReleaseDate(
                                    key = "date",
                                    options = viewModel.timeList,
                                    initialApproximate = viewModel.approxTime,
                                    initialYear = viewModel.year,
                                    initialMonth = viewModel.month,
                                    onSaveApproximate = { searchKey ->
                                        viewModel.approxTime = searchKey
                                        viewModel.year = null
                                        viewModel.month = null
                                    },
                                    onSaveSpecific = { year, month ->
                                        viewModel.year = year
                                        viewModel.month = month
                                        viewModel.approxTime = null
                                    },
                                    onReset = {
                                        viewModel.year = null
                                        viewModel.month = null
                                        viewModel.approxTime = null
                                    },
                                )
                            },
                        )
                        AdvancedSearchChip(
                            title = durationTitle,
                            checked = viewModel.duration != null,
                            modifier = Modifier.weight(1f),
                            onLongClick = { updateSelection { viewModel.duration = null } },
                            onClick = {
                                dialogState = AdvancedSearchDialogState.SingleChoice(
                                    key = "duration",
                                    titleRes = R.string.duration,
                                    options = viewModel.durations,
                                    selectedIndex = viewModel.durations.indexOfFirst { it.searchKey == viewModel.duration },
                                    onSelect = { option -> viewModel.duration = option.searchKey },
                                    onReset = { viewModel.duration = null },
                                )
                            },
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.search_options_tips),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        FilledIconButton(
                            onClick = {
                                viewModel.triggerNewSearch()
                                viewModel.insertAdvancedSearchHistory(
                                    viewModel.query,
                                    viewModel.genre,
                                    viewModel.sort,
                                    viewModel.broad,
                                    viewModel.getSearchDate(),
                                    viewModel.duration,
                                    viewModel.tagMap.flatten().map { SearchOption(searchKey = it) }.toSet(),
                                    viewModel.brandMap.flatten().map { SearchOption(searchKey = it) }.toSet(),
                                )
                                onDismiss()
                            },
                            modifier = Modifier.size(60.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_search_24),
                                contentDescription = stringResource(R.string.search),
                            )
                        }
                    }
                }
            }
        }
    }
}
