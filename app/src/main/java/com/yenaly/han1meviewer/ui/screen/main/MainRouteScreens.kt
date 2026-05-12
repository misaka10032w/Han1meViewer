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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
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
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.fragment.video.VideoFragment
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
import com.yenaly.han1meviewer.ui.screen.search.AdvancedSearchSheet
import com.yenaly.han1meviewer.ui.screen.search.SearchScreen
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
import com.yenaly.yenaly_libs.utils.copyTextToClipboard
import com.yenaly.yenaly_libs.utils.showLongToast
import com.yenaly.yenaly_libs.utils.showShortToast
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
    val confirmToExit = stringResource(R.string.confirm_to_exit)
    val finishedMasturbating = stringResource(R.string.finished_masturbating)
    val doMore = stringResource(R.string.do_more)
    val checkoutExit = stringResource(R.string.checkout_exit)
    val exit = stringResource(R.string.exit)
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
                    .setTitle(confirmToExit)
                    .setMessage(finishedMasturbating)
                    .setNegativeButton(doMore) { d, _ -> d.dismiss() }
                    .setNeutralButton(checkoutExit) { _, _ ->
                        checkInViewModel.addRecord(
                            LocalDate.now(),
                            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                            "自慰",
                            "",
                            "",
                        )
                        activity.finish()
                    }
                    .setPositiveButton(exit) { _, _ -> activity.finish() }
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
    val selectCustomDirectory = stringResource(R.string.select_custom_directory)
    val groupNameEmpty = stringResource(R.string.group_name_empty)
    val readSuccess = stringResource(R.string.read_success)
    val deleteSuccess = stringResource(R.string.delete_success)
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
                showLongToast(readSuccess)
            } else {
                showLongToast(selectCustomDirectory)
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
                showLongToast(groupNameEmpty)
            } else {
                viewModel.createNewGroup(name)
                showLongToast(context.getString(R.string.create_group_success, name))
            }
        },
        onDeleteGroup = { group ->
            viewModel.deleteGroup(group)
            showLongToast(deleteSuccess)
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
