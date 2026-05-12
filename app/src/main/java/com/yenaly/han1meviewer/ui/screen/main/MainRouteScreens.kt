package com.yenaly.han1meviewer.ui.screen.main

import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeSearchShareText
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.home.DailyCheckInScreen
import com.yenaly.han1meviewer.ui.screen.home.HomePageScreen
import com.yenaly.han1meviewer.ui.screen.home.LocalSearchHistoryQuery
import com.yenaly.han1meviewer.ui.screen.home.MyFavVideoScreen
import com.yenaly.han1meviewer.ui.screen.home.MyWatchLaterScreen
import com.yenaly.han1meviewer.ui.screen.home.SubscriptionApp
import com.yenaly.han1meviewer.ui.screen.home.WatchHistoryScreen
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadScreen
import com.yenaly.han1meviewer.ui.screen.home.myplaylist.MyPlayListScreen
import com.yenaly.han1meviewer.ui.viewmodel.CheckInCalendarViewModel
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MyListViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import com.yenaly.han1meviewer.ui.viewmodel.MySubscriptionsViewModel
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

    DisposableEffect(Unit) {
        activity.hideToolbar()
        onDispose { }
    }

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
    activity: MainActivity,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MainViewModel = viewModel()
    DisposableEffect(Unit) {
        activity.hideToolbar()
        onDispose { activity.showToolbar() }
    }
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
    activity: MainActivity,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyListViewModel = viewModel()
    DisposableEffect(Unit) {
        activity.hideToolbar()
        onDispose { activity.showToolbar() }
    }
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
    activity: MainActivity,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyListViewModel = viewModel()
    DisposableEffect(Unit) {
        activity.hideToolbar()
        onDispose { activity.showToolbar() }
    }
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
