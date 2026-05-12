package com.yenaly.han1meviewer.ui.screen.main

import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.util.isNotEmpty
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.load
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.yenaly.han1meviewer.PREVIEW_COMMENT_PREFIX
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.getHanimeSearchShareText
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.entity.HanimeAdvancedSearchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.model.SearchOption.Companion.flatten
import com.yenaly.han1meviewer.logic.model.SearchOption.Companion.get
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.search.SearchScreen
import com.yenaly.han1meviewer.ui.fragment.video.VideoFragment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.component.SettingChoiceItem
import com.yenaly.han1meviewer.ui.screen.home.DailyCheckInScreen
import com.yenaly.han1meviewer.ui.screen.home.HomePageScreen
import com.yenaly.han1meviewer.ui.screen.home.LocalSearchHistoryQuery
import com.yenaly.han1meviewer.ui.screen.home.MyFavVideoScreen
import com.yenaly.han1meviewer.ui.screen.home.MyWatchLaterScreen
import com.yenaly.han1meviewer.ui.screen.home.SubscriptionApp
import com.yenaly.han1meviewer.ui.screen.home.WatchHistoryScreen
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadScreen
import com.yenaly.han1meviewer.ui.screen.home.myplaylist.MyPlayListScreen
import com.yenaly.han1meviewer.ui.screen.home.preview.PreviewScreen
import com.yenaly.han1meviewer.ui.screen.video.CommentScreen
import com.yenaly.han1meviewer.ui.viewmodel.CheckInCalendarViewModel
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MyListViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import com.yenaly.han1meviewer.ui.viewmodel.MySubscriptionsViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.han1meviewer.ui.viewmodel.PreviewViewModel
import com.yenaly.han1meviewer.ui.viewmodel.SearchViewModel
import com.yenaly.han1meviewer.ui.widget.CheckInWidgetProvider
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.SafFileManager.scanAndImportHanimeDownloads
import com.yenaly.han1meviewer.util.openDownloadedHanimeVideoLocally
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.han1meviewer.SEARCH_YEAR_RANGE_END
import com.yenaly.han1meviewer.SEARCH_YEAR_RANGE_START
import com.yenaly.yenaly_libs.utils.copyTextToClipboard
import com.yenaly.yenaly_libs.utils.showLongToast
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeRouteScreen(
    activity: MainActivity,
    onNavigateToPreview: () -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToSearchAdvanced: (Map<String, String>) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val checkInViewModel: CheckInCalendarViewModel = viewModel()

    CompositionLocalProvider(
        LocalSearchHistoryQuery provides { keyword: String ->
            DatabaseRepo.SearchHistory.loadAll(keyword).first().map { it.query }
        }
    ) {
        HomePageScreen(
            viewModel = viewModel,
            onOpenDrawer = { activity.openMainDrawer() },
            onNavigateToPreview = onNavigateToPreview,
            onNavigateToSearch = { query -> onNavigateToSearch(query) },
            onOpenSearchPage = { onNavigateToSearch(null) },
            onNavigateToSearchAdvanced = onNavigateToSearchAdvanced,
            onOpenVideo = onNavigateToVideo,
            onLongPressVideoCopy = { code, title ->
                copyTextToClipboard(getHanimeShareText(title, code))
                showShortToast(R.string.copy_to_clipboard)
            },
            onShowExitDialog = {
                MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(R.string.confirm_to_exit))
                    .setMessage(context.getString(R.string.finished_masturbating))
                    .setNegativeButton(context.getString(R.string.do_more)) { d, _ -> d.dismiss() }
                    .setNeutralButton(context.getString(R.string.checkout_exit)) { _, _ ->
                        checkInViewModel.addRecord(
                            LocalDate.now(),
                            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                            "自慰",
                            "",
                            "",
                        )
                        activity.finish()
                    }
                    .setPositiveButton(context.getString(R.string.exit)) { _, _ -> activity.finish() }
                    .show()
            },
            onShowAnnouncementDialog = { title, content, imageUrl ->
                showAnnouncementDialog(context, title, SpannableString(content), imageUrl)
            },
        )
    }
}

@Composable
fun WatchHistoryRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MainViewModel = viewModel()
    WatchHistoryScreen(
        historiesFlow = viewModel.loadAllWatchHistories(),
        onBack = onBack,
        onOpenVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteHistory = viewModel::deleteWatchHistory,
        onDeleteAllHistories = viewModel::deleteAllWatchHistories,
    )
}

@Composable
fun MyFavVideoRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyListViewModel = viewModel()
    MyFavVideoScreen(
        favVideoFlow = viewModel.fav.favVideoFlow,
        favVideoStateFlow = viewModel.fav.favVideoStateFlow,
        deleteStateFlow = viewModel.fav.deleteMyFavVideoFlow,
        loadedPageCountFlow = viewModel.fav.loadedPageCount,
        isLoadingMoreFlow = viewModel.fav.isLoadingMore,
        onBack = onBack,
        onOpenVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteFavorite = { item ->
            val position = viewModel.fav.favVideoFlow.value.indexOfFirst { it.videoCode == item.videoCode }
            if (position >= 0) {
                viewModel.fav.deleteMyFavVideo(item.videoCode, position)
            }
        },
        onRefresh = {
            viewModel.fav.favVideoPage = 1
            viewModel.fav.clearMyListItems()
            viewModel.fav.getMyFavVideoItems(com.yenaly.han1meviewer.Preferences.savedUserId, 1)
            viewModel.fav.favVideoPage = 2
        },
        onLoadMore = {
            val page = viewModel.fav.favVideoPage
            viewModel.fav.getMyFavVideoItems(com.yenaly.han1meviewer.Preferences.savedUserId, page)
            viewModel.fav.favVideoPage = page + 1
        },
    )
}

@Composable
fun MyWatchLaterRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyListViewModel = viewModel()
    MyWatchLaterScreen(
        watchLaterFlow = viewModel.watchLater.watchLaterFlow,
        watchLaterStateFlow = viewModel.watchLater.watchLaterStateFlow,
        deleteStateFlow = viewModel.watchLater.deleteMyWatchLaterFlow,
        loadedPageCountFlow = viewModel.watchLater.loadedPageCount,
        isLoadingMoreFlow = viewModel.watchLater.isLoadingMore,
        onBack = onBack,
        onOpenVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteWatchLater = { item ->
            val position = viewModel.watchLater.watchLaterFlow.value.indexOfFirst { it.videoCode == item.videoCode }
            if (position >= 0) {
                viewModel.watchLater.deleteMyWatchLater(item.videoCode, position)
            }
        },
        onRefresh = {
            viewModel.watchLater.watchLaterPage = 1
            viewModel.watchLater.clearMyListItems()
            viewModel.watchLater.getMyWatchLaterItems(1)
            viewModel.watchLater.watchLaterPage = 2
        },
        onLoadMore = {
            val page = viewModel.watchLater.watchLaterPage
            viewModel.watchLater.getMyWatchLaterItems(page)
            viewModel.watchLater.watchLaterPage = page + 1
        },
    )
}

@Composable
fun SubscriptionRouteScreen(
    onBack: () -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MySubscriptionsViewModel = viewModel()
    SubscriptionApp(
        navigateBack = onBack,
        viewModel = viewModel,
        onClickArtist = { onNavigateToSearch(it) },
        onLongClickArtist = { artistName ->
            copyTextToClipboard(getHanimeSearchShareText(artistName))
            showShortToast(R.string.copy_to_clipboard)
        },
        onClickVideosItem = onNavigateToVideo,
        onLongClickVideosItem = { videoCode, title ->
            copyTextToClipboard(getHanimeShareText(title, videoCode))
            showShortToast(R.string.copy_to_clipboard)
        },
    )
}

@Composable
fun MyPlaylistRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyPlayListViewModelV2 = viewModel()
    MyPlayListScreen(
        viewModel = viewModel,
        navigateBack = onBack,
        onClickItem = onNavigateToVideo,
        onLongClickItem = { videoCode, title ->
            copyTextToClipboard(getHanimeShareText(title, videoCode))
            showShortToast(R.string.copy_to_clipboard)
        },
    )
}

@Composable
fun DailyCheckInRouteScreen(
    activity: MainActivity,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    DailyCheckInScreen(
        activity = activity,
        onBack = onBack,
        onAddWidget = {
            val mgr = AppWidgetManager.getInstance(activity)
            Toast.makeText(activity, "部分rom不支持引导式添加，请手动添加小部件", Toast.LENGTH_SHORT).show()
            if (mgr.isRequestPinAppWidgetSupported) {
                mgr.requestPinAppWidget(
                    ComponentName(activity, CheckInWidgetProvider::class.java),
                    null,
                    null,
                )
            } else {
                Toast.makeText(activity, R.string.widget_not_supported, Toast.LENGTH_SHORT).show()
            }
        },
        onNavigateToVideo = onNavigateToVideo,
    )
}

@Composable
fun DownloadRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToLocalVideo: (String, String?) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: DownloadViewModel = viewModel()
    val dao = remember { DownloadDatabase.instance.hanimeDownloadDao }
    DownloadScreen(
        downloadingFlow = viewModel.loadAllDownloadingHanime(),
        downloadedFlow = viewModel.downloaded,
        downloadedGroupsFlow = viewModel.downloadedGroups,
        collapseDownloadedGroup = com.yenaly.han1meviewer.Preferences.collapseDownloadedGroup,
        onBack = onBack,
        onPauseAll = { items ->
            items.forEach { entity -> if (entity.isDownloading) HanimeDownloadManagerV2.stopTask(entity) }
        },
        onResumeAll = { items ->
            items.forEach { entity -> if (!entity.isDownloading) HanimeDownloadManagerV2.resumeTask(entity) }
        },
        onPauseItem = HanimeDownloadManagerV2::stopTask,
        onResumeItem = HanimeDownloadManagerV2::resumeTask,
        onDeleteDownloadingItem = HanimeDownloadManagerV2::deleteTask,
        onImportDownloaded = {
            if (!com.yenaly.han1meviewer.Preferences.safDownloadPath.isNullOrBlank() && !com.yenaly.han1meviewer.Preferences.isUsePrivateStorage) {
                runBlocking {
                    scanAndImportHanimeDownloads(context, dao)
                }
                showLongToast(context.getString(R.string.read_success))
            } else {
                showLongToast(context.getString(R.string.select_custom_directory))
            }
        },
        onLoadDownloaded = {
            viewModel.loadAllDownloadedHanime(
                sortedBy = HanimeDownloadEntity.SortedBy.ID,
                ascending = false,
            )
        },
        onOpenDownloadedVideo = { onNavigateToVideo(it.video.videoCode) },
        onLocalPlayback = { onNavigateToLocalVideo(it.video.videoCode, null) },
        onExternalPlayback = { video ->
            context.openDownloadedHanimeVideoLocally(video.video.videoUri) {
                context.showAlertDialog {
                    setTitle(R.string.video_not_exist)
                    setMessage(R.string.video_deleted_sure_to_delete_item)
                    setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                    }
                    setNegativeButton(R.string.cancel, null)
                }
            }
        },
        onDeleteDownloadedVideo = { video ->
            context.showAlertDialog {
                setTitle(R.string.sure_to_delete)
                setMessage(context.getString(R.string.prepare_to_delete_s, video.video.title))
                setPositiveButton(R.string.confirm) { _, _ ->
                    SafFileManager.deleteDownloadVideoFolder(context, video.video.videoCode)
                    viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                }
                setNegativeButton(R.string.cancel, null)
            }
        },
        onMoveVideoGroup = { video, groupId -> viewModel.updateVideoGroup(video.video.videoCode, groupId) },
        onRenameGroup = { groupId, newName ->
            viewModel.updateGroupName(groupId, newName)
            showLongToast(context.getString(R.string.group_renamed, newName))
        },
        onCreateGroup = { name ->
            if (name.isBlank()) {
                showLongToast(context.getString(R.string.group_name_empty))
            } else {
                viewModel.createNewGroup(name)
                showLongToast(context.getString(R.string.create_group_success, name))
            }
        },
        onDeleteGroup = { group ->
            viewModel.deleteGroup(group)
            showLongToast(context.getString(R.string.delete_success))
        },
    )
}

@Composable
fun SearchRouteScreen(
    activity: MainActivity,
    route: SearchRoute,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: SearchViewModel = viewModel(viewModelStoreOwner = activity)
    var showAdvancedSearchSheet by remember { mutableStateOf(false) }

    LaunchedEffect(route.advancedSearchJson) {
        route.advancedSearchJson?.let { json ->
            runCatching { Json.decodeFromString<Map<String, String>>(json) }
                .onSuccess { params ->
                    params.forEach { (key, value) ->
                        when (key.uppercase()) {
                            "QUERY" -> viewModel.query = value
                            "GENRE" -> viewModel.genre = value
                            "SORT" -> viewModel.sort = value
                            "YEAR" -> viewModel.year = value.toIntOrNull()
                            "MONTH" -> viewModel.month = value.toIntOrNull()
                            "DURATION" -> viewModel.duration = value
                        }
                    }
                }
        }
    }

    if (showAdvancedSearchSheet) {
        AdvancedSearchSheet(
            viewModel = viewModel,
            onDismiss = { showAdvancedSearchSheet = false },
        )
    }

    SearchScreen(
        viewModel = viewModel,
        initialQuery = route.query,
        onBack = onBack,
        onOpenVideo = onNavigateToVideo,
        onLongPressCopy = { videoCode, title ->
            copyTextToClipboard(getHanimeShareText(title, videoCode))
            showShortToast(R.string.copy_to_clipboard)
        },
        onOpenAdvancedSearch = { showAdvancedSearchSheet = true },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSearchSheet(
    viewModel: SearchViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val histories by remember {
        DatabaseRepo.HanimeAdvancedSearchRepo.getSearchHistories()
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var dialogState by remember { mutableStateOf<AdvancedSearchDialogState?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                                        state.onSelect(option)
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
                                state.onReset()
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
                            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                                state.scopes.forEachIndexed { index, scopeSection ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        text = { Text(stringResource(scopeSection.titleRes)) },
                                    )
                                }
                            }
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.heightIn(max = 420.dp),
                            ) { page ->
                                val scopeSection = state.scopes[page]
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(scopeSection.spanCount),
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(scopeSection.options, key = { it.searchKey.orEmpty() }) { option ->
                                        SettingChoiceItem(
                                            title = option.value,
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
                            state.onSave(selected, broad)
                            dialogState = null
                        }) {
                            Text(stringResource(R.string.save))
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                state.onReset()
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
                    mutableStateOf(if (state.initialApproximate != null) 1 else 0)
                }
                var yearOnly by remember(state.key) {
                    mutableStateOf(state.initialMonth == null)
                }
                var selectedYear by remember(state.key) {
                    mutableStateOf(state.initialYear ?: SEARCH_YEAR_RANGE_END)
                }
                var selectedMonth by remember(state.key) {
                    mutableStateOf(state.initialMonth ?: 1)
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
                                state.onSaveSpecific(selectedYear, if (yearOnly) null else selectedMonth)
                            } else {
                                state.onSaveApproximate(selectedApproximate)
                            }
                            dialogState = null
                        }) {
                            Text(stringResource(R.string.save))
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                state.onReset()
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
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            title = stringResource(R.string.type),
                            checked = viewModel.genre != null,
                            modifier = Modifier.weight(1f),
                            onLongClick = { viewModel.genre = null },
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
                            title = stringResource(R.string.sort_option),
                            checked = viewModel.sort != null,
                            modifier = Modifier.weight(1f),
                            onLongClick = { viewModel.sort = null },
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
                            title = stringResource(R.string.tag),
                            checked = viewModel.tagMap.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            onLongClick = { viewModel.tagMap.clear() },
                            onClick = {
                                dialogState = AdvancedSearchDialogState.MultiChoice(
                                    key = "tag",
                                    titleRes = R.string.tag,
                                    scopes = listOf(
                                        SearchScopeSection(R.string.video_attr, viewModel.tags[R.string.video_attr], spanCount = 1),
                                        SearchScopeSection(R.string.relationship, viewModel.tags[R.string.relationship], spanCount = 2),
                                        SearchScopeSection(R.string.characteristics, viewModel.tags[R.string.characteristics]),
                                        SearchScopeSection(R.string.appearance_and_figure, viewModel.tags[R.string.appearance_and_figure]),
                                        SearchScopeSection(R.string.story_plot, viewModel.tags[R.string.story_plot]),
                                        SearchScopeSection(R.string.story_location, viewModel.tags[R.string.story_location]),
                                        SearchScopeSection(R.string.sex_position, viewModel.tags[R.string.sex_position]),
                                    ),
                                    selected = viewModel.tagMap.flatten().map { SearchOption(searchKey = it) }.toSet(),
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
                            title = stringResource(R.string.release_date),
                            checked = viewModel.year != null || viewModel.month != null || viewModel.approxTime != null,
                            modifier = Modifier.weight(1f),
                            onLongClick = {
                                viewModel.year = null
                                viewModel.month = null
                                viewModel.approxTime = null
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
                            title = stringResource(R.string.duration),
                            checked = viewModel.duration != null,
                            modifier = Modifier.weight(1f),
                            onLongClick = { viewModel.duration = null },
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
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
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

private sealed interface AdvancedSearchDialogState {
    val key: String
    val titleRes: Int

    data class SingleChoice(
        override val key: String,
        override val titleRes: Int,
        val options: List<SearchOption>,
        val selectedIndex: Int,
        val onSelect: (SearchOption) -> Unit,
        val onReset: () -> Unit,
    ) : AdvancedSearchDialogState

    data class MultiChoice(
        override val key: String,
        override val titleRes: Int,
        val scopes: List<SearchScopeSection>,
        val selected: Set<SearchOption>,
        val broad: Boolean,
        val onSave: (Set<SearchOption>, Boolean) -> Unit,
        val onReset: () -> Unit,
    ) : AdvancedSearchDialogState

    data class ReleaseDate(
        override val key: String,
        val options: List<SearchOption>,
        val initialApproximate: String?,
        val initialYear: Int?,
        val initialMonth: Int?,
        val onSaveApproximate: (String?) -> Unit,
        val onSaveSpecific: (Int, Int?) -> Unit,
        val onReset: () -> Unit,
    ) : AdvancedSearchDialogState {
        override val titleRes: Int = R.string.release_date
    }
}

private data class SearchScopeSection(
    val titleRes: Int,
    val options: List<SearchOption>,
    val spanCount: Int = 3,
)

@Composable
private fun AdvancedSearchChip(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val containerColor = if (checked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
    }
    val contentColor = if (checked) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .heightIn(min = 52.dp),
        color = containerColor,
        tonalElevation = if (checked) 2.dp else 0.dp,
        shadowElevation = if (checked) 2.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun AdvancedSearchHistoryCard(
    history: HanimeAdvancedSearchHistoryEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val conditions = remember(history) {
        buildList {
            history.genre?.takeIf { it.isNotBlank() }?.let { add("${context.getString(R.string.type)}: $it") }
            history.sort?.takeIf { it.isNotBlank() }?.let { add("${context.getString(R.string.sort_option)}: $it") }
            if (history.broad == true) add(context.getString(R.string.pair_widely))
            history.date?.takeIf { it.isNotBlank() }?.let { add("${context.getString(R.string.release_date)}: $it") }
            history.duration?.takeIf { it.isNotBlank() }?.let { add("${context.getString(R.string.duration)}: $it") }
            if (!history.tags.isNullOrBlank()) add("${context.getString(R.string.tag)}: ${history.tags}")
            if (!history.brands.isNullOrBlank()) add("${context.getString(R.string.brand)}: ${history.brands}")
        }.joinToString(" || ")
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                history.query?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (conditions.isNotBlank()) {
                    Text(
                        text = conditions,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_delete_24),
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
    }
}

@Composable
private fun <T> WheelLikeColumn(
    title: String,
    values: List<T>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        LazyColumn(
            modifier = Modifier.heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(values, key = { label(it) }) { value ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (value == selectedValue) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.96f)
                    },
                    tonalElevation = if (value == selectedValue) 2.dp else 0.dp,
                    onClick = { onSelect(value) },
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Text(
                            text = label(value),
                            color = if (value == selectedValue) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewRouteScreen(
    activity: MainActivity,
    onBack: () -> Unit,
    onNavigateToPreviewComment: (String, String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: PreviewViewModel = viewModel()
    val commentViewModel: CommentViewModel = viewModel(viewModelStoreOwner = activity)
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    val previewState = viewModel.previewFlow.collectAsStateWithLifecycle().value
    val commentCount = PreviewCommentPrefetcher.here(commentViewModel)
        .commentFlow
        .collectAsStateWithLifecycle()
        .value
        .size

    fun preloadImages(preview: HanimePreview?) {
        if (preview == null) return
        buildList {
            preview.headerPicUrl?.let(::add)
            addAll(preview.latestHanime.map { it.coverUrl })
            addAll(preview.previewInfo.mapNotNull { it.coverUrl })
        }.distinct().forEach { url ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build()
            )
        }
    }

    LaunchedEffect(previewState) {
        when (val state = previewState) {
            is WebsiteState.Success -> preloadImages(state.info)
            else -> Unit
        }
    }

    DisposableEffect(Unit) {
        PreviewCommentPrefetcher.here(commentViewModel)
            .tag(PreviewCommentPrefetcher.Scope.PREVIEW_ACTIVITY)
        onDispose {
            PreviewCommentPrefetcher.bye(PreviewCommentPrefetcher.Scope.PREVIEW_ACTIVITY)
            commentViewModel.clearCommentData()
        }
    }

    PreviewScreen(
        previewState = previewState,
        getCachedPreviewState = viewModel::getCachedPreview,
        commentCount = commentCount,
        onBack = onBack,
        onLoadDate = { code ->
            viewModel.getHanimePreview(code)
            viewModel.preloadPreview(shiftMonthCodeForPreview(code, -1))
            viewModel.preloadPreview(shiftMonthCodeForPreview(code, 1))
            PreviewCommentPrefetcher.here(commentViewModel).fetch(PREVIEW_COMMENT_PREFIX, code)
        },
        onOpenComment = onNavigateToPreviewComment,
        onOpenVideo = onNavigateToVideo,
    )
}

@Composable
fun PreviewCommentRouteScreen(
    activity: MainActivity,
    route: PreviewCommentRoute,
    onBack: () -> Unit,
) {
    val viewModel: CommentViewModel = viewModel(viewModelStoreOwner = activity)
    val comments = viewModel.videoCommentFlow
    val commentState = viewModel.videoCommentStateFlow

    LaunchedEffect(route.dateCode) {
        viewModel.code = route.dateCode
        val prefetched = PreviewCommentPrefetcher.here(viewModel).commentFlow.value
        if (prefetched.isNotEmpty()) {
            viewModel.updateComments(prefetched)
        } else {
            viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode)
        }
    }

    DisposableEffect(Unit) {
        PreviewCommentPrefetcher.here(viewModel)
            .tag(PreviewCommentPrefetcher.Scope.PREVIEW_COMMENT_ACTIVITY)
        onDispose {
            PreviewCommentPrefetcher.bye(PreviewCommentPrefetcher.Scope.PREVIEW_COMMENT_ACTIVITY)
        }
    }

    CommentScreen(
        commentsFlow = comments,
        commentStateFlow = commentState,
        reportMessageFlow = kotlinx.coroutines.flow.emptyFlow(),
        currentSortType = viewModel.currentSortType,
        reportReasons = viewModel.reportReason,
        isPreviewCommentPrefetched = true,
        isAlreadyLogin = com.yenaly.han1meviewer.Preferences.isAlreadyLogin,
        onRefresh = { viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode) },
        onReply = { _, _: String -> },
        onReport = { _, _ -> },
        onThumbUp = { _ -> },
        onThumbDown = { _ -> },
        onViewMoreReplies = { _ -> },
        onSortChange = viewModel::setSortType,
        onComposeComment = { _: String -> },
    )
}

@Composable
fun VideoRouteScreen(
    activity: MainActivity,
    route: VideoRoute,
) {
    val containerId = remember(route.videoCode, route.localUri) { View.generateViewId() }

    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                id = containerId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = {
            val tag = videoBridgeTag(route.videoCode, route.localUri)
            val existing = activity.supportFragmentManager.findFragmentByTag(tag)
            if (existing == null) {
                activity.supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(
                        containerId,
                        VideoFragment().apply {
                            arguments = Bundle().apply {
                                putString(VIDEO_CODE, route.videoCode)
                                putString("LOCAL_URI", route.localUri)
                            }
                        },
                        tag,
                    )
                }
            }
        },
    )
}

private fun shiftMonthCodeForPreview(code: String, delta: Int): String {
    var year = code.substring(0, 4).toInt()
    var month = code.substring(4, 6).toInt() + delta
    while (month < 1) {
        month += 12
        year -= 1
    }
    while (month > 12) {
        month -= 12
        year += 1
    }
    return "%04d%02d".format(year, month)
}

private fun showAnnouncementDialog(
    context: Context,
    title: String,
    content: Spanned,
    imageUrl: String?,
) {
    val view = LayoutInflater.from(context).inflate(R.layout.dialog_announcement, null, false)
    view.findViewById<TextView>(R.id.dialogTitle).apply {
        text = title
        visibility = View.VISIBLE
    }
    view.findViewById<TextView>(R.id.dialogContent).apply {
        text = content
        movementMethod = LinkMovementMethod.getInstance()
        highlightColor = Color.TRANSPARENT
    }
    if (!imageUrl.isNullOrBlank()) {
        view.findViewById<ShapeableImageView>(R.id.dialogImage).apply {
            visibility = View.VISIBLE
            load(imageUrl) {
                placeholder(R.drawable.akarin)
                error(R.drawable.baseline_error_outline_24)
            }
            setOnClickListener {
                Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
                    setContentView(ImageView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        load(imageUrl)
                        setOnClickListener { dismiss() }
                    })
                    show()
                }
            }
        }
    }
    MaterialAlertDialogBuilder(context)
        .setView(view)
        .setPositiveButton(context.getString(R.string.i_understand), null)
        .show()
}
