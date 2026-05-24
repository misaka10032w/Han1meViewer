package com.yenaly.han1meviewer.ui.navigation.main

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.screen.home.DownloadScreen
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadEvent
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.SafFileManager.checkSafPermissions
import com.yenaly.han1meviewer.util.SafFileManager.scanAndImportHanimeDownloads
import com.yenaly.han1meviewer.util.openDownloadedHanimeVideoLocally
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.yenaly_libs.utils.application
import com.yenaly.yenaly_libs.utils.showLongToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DownloadRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToLocalVideo: (String, String?) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: DownloadViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val dao = remember { DownloadDatabase.instance.hanimeDownloadDao }
    var showVideoNotExistConfirm by remember { mutableStateOf<VideoWithCategories?>(null) }
    var showDeleteVideoConfirm by remember { mutableStateOf<VideoWithCategories?>(null) }
    var showImportDownloadedConfirm by remember { mutableStateOf(false) }
    var isImportingDownloaded by remember { mutableStateOf(false) }

    val handleEvent: (DownloadEvent) -> Unit = { event ->
        when (event) {
            is DownloadEvent.OnPauseAll -> event.items.forEach { entity ->
                if (entity.isDownloading) HanimeDownloadManagerV2.stopTask(entity)
            }
            is DownloadEvent.OnResumeAll -> event.items.forEach { entity ->
                if (!entity.isDownloading) HanimeDownloadManagerV2.resumeTask(entity)
            }
            is DownloadEvent.OnPauseItem -> HanimeDownloadManagerV2.stopTask(event.item)
            is DownloadEvent.OnResumeItem -> HanimeDownloadManagerV2.resumeTask(event.item)
            is DownloadEvent.OnDeleteDownloadingItem -> HanimeDownloadManagerV2.deleteTask(event.item)

            is DownloadEvent.OnImportDownloaded -> {
                if (!Preferences.safDownloadPath.isNullOrBlank() &&
                    !Preferences.isUsePrivateStorage && !isImportingDownloaded
                ) {
                    showImportDownloadedConfirm = true
                } else {
                    showLongToast(application.getString(R.string.select_custom_directory))
                }
            }

            is DownloadEvent.OnOpenDownloadedVideo -> onNavigateToVideo(event.video.video.videoCode)
            is DownloadEvent.OnLocalPlayback -> onNavigateToLocalVideo(
                event.video.video.videoCode, event.video.video.videoUri
            )

            is DownloadEvent.OnExternalPlayback -> {
                context.openDownloadedHanimeVideoLocally(event.video.video.videoUri) {
                    showVideoNotExistConfirm = event.video
                }
            }

            is DownloadEvent.OnDeleteDownloadedVideo -> showDeleteVideoConfirm = event.video

            is DownloadEvent.OnMoveVideoGroup -> viewModel.updateVideoGroup(
                event.video.video.videoCode, event.groupId
            )

            is DownloadEvent.OnRenameGroup -> {
                viewModel.updateGroupName(event.groupId, event.newName)
                showLongToast(application.getString(R.string.group_renamed, event.newName))
            }

            is DownloadEvent.OnCreateGroup -> {
                if (event.name.isBlank()) {
                    showLongToast(application.getString(R.string.group_name_empty))
                } else {
                    viewModel.createNewGroup(event.name)
                    showLongToast(application.getString(R.string.create_group_success, event.name))
                }
            }

            is DownloadEvent.OnDeleteGroup -> {
                viewModel.deleteGroup(event.group)
                showLongToast(application.getString(R.string.delete_success))
            }

            is DownloadEvent.OnBatchDelete -> event.videos.forEach { video ->
                viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                SafFileManager.deleteDownloadVideoFolder(context, video.video.videoCode)
            }

            is DownloadEvent.OnBatchMoveGroup -> event.videos.forEach { video ->
                viewModel.updateVideoGroup(video.video.videoCode, event.groupId)
            }

            // 以下事件由 Screen 层自行处理，Route 不关心
            is DownloadEvent.OnToggleGroup,
            is DownloadEvent.OnCreateGroupDialogChange,
            is DownloadEvent.OnPageChange,
            is DownloadEvent.OnToggleMultiSelect,
            is DownloadEvent.OnToggleVideoSelection,
            is DownloadEvent.OnSelectAllCurrentGroup,
            is DownloadEvent.OnBatchMoveRequest -> Unit
        }
    }

    DownloadScreen(
        downloadingFlow = viewModel.loadAllDownloadingHanime(),
        downloadedFlow = viewModel.downloaded,
        downloadedGroupsFlow = viewModel.downloadedGroups,
        collapseDownloadedGroup = Preferences.collapseDownloadedGroup,
        onBack = onBack,
        onLoadDownloaded = {
            viewModel.loadAllDownloadedHanime(
                sortedBy = HanimeDownloadEntity.SortedBy.ID,
                ascending = false,
            )
        },
        onEvent = handleEvent,
    )

    ConfirmDialog(
        visible = showImportDownloadedConfirm,
        title = application.getString(R.string.confirm_import),
        message = application.getString(R.string.import_warning),
        confirmText = application.getString(R.string.ok),
        dismissText = application.getString(R.string.cancel),
        onConfirm = {
            showImportDownloadedConfirm = false
            isImportingDownloaded = true
            scope.launch {
                val importSucceeded = withContext(Dispatchers.IO) {
                    try {
                        if (!checkSafPermissions(context)) return@withContext false
                        scanAndImportHanimeDownloads(context, dao)
                        true
                    } catch (e: Exception) {
                        Log.e("ImportHanime", "Failed to import downloaded videos", e)
                        false
                    }
                }
                isImportingDownloaded = false
                if (importSucceeded) {
                    viewModel.loadAllDownloadedHanime(
                        sortedBy = HanimeDownloadEntity.SortedBy.ID,
                        ascending = false,
                    )
                    showLongToast(application.getString(R.string.read_success))
                } else {
                    showLongToast(application.getString(R.string.permission_error))
                }
            }
        },
        onDismiss = { showImportDownloadedConfirm = false },
    )

    showVideoNotExistConfirm?.let { video ->
        ConfirmDialog(
            visible = true,
            title = application.getString(R.string.video_not_exist),
            message = application.getString(R.string.video_deleted_sure_to_delete_item),
            confirmText = application.getString(R.string.delete),
            dismissText = application.getString(R.string.cancel),
            onConfirm = {
                viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                showVideoNotExistConfirm = null
            },
            onDismiss = { showVideoNotExistConfirm = null },
        )
    }

    showDeleteVideoConfirm?.let { video ->
        ConfirmDialog(
            visible = true,
            title = application.getString(R.string.sure_to_delete),
            message = application.getString(R.string.prepare_to_delete_s, video.video.title),
            confirmText = application.getString(R.string.confirm),
            dismissText = application.getString(R.string.cancel),
            onConfirm = {
                SafFileManager.deleteDownloadVideoFolder(context, video.video.videoCode)
                viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                showDeleteVideoConfirm = null
            },
            onDismiss = { showDeleteVideoConfirm = null },
        )
    }
}
