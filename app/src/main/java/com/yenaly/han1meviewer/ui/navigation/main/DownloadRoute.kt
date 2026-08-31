package com.yenaly.han1meviewer.ui.navigation.main

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.screen.home.DownloadScreen
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadEvent
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.SafFileManager.ImportFailureReason
import com.yenaly.han1meviewer.util.SafFileManager.ImportResult
import com.yenaly.han1meviewer.util.SafFileManager.checkSafPermissions
import com.yenaly.han1meviewer.util.SafFileManager.scanAndImportHanimeDownloads
import com.yenaly.han1meviewer.util.openDownloadedHanimeVideoLocally
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.yenaly_libs.utils.application
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
    var showImportProgress by remember { mutableStateOf(false) }
    var importProgress by remember { mutableIntStateOf(0) }
    var importTotal by remember { mutableIntStateOf(0) }
    var importCurrentName by remember { mutableStateOf<String?>(null) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var showImportResult by remember { mutableStateOf(false) }

    val selectCustomDirectory = stringResource(R.string.select_custom_directory)
    val groupNameEmpty = stringResource(R.string.group_name_empty)
    val deleteSuccess = stringResource(R.string.delete_success)
    val permissionError = stringResource(R.string.permission_error)

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
                    GlobalToasts.show(selectCustomDirectory, level = GlobalToasts.ToastLevel.WARNING)
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
                GlobalToasts.show(application.getString(R.string.group_renamed, event.newName), level = GlobalToasts.ToastLevel.INFO)
            }

            is DownloadEvent.OnCreateGroup -> {
                if (event.name.isBlank()) {
                    GlobalToasts.show(groupNameEmpty, level = GlobalToasts.ToastLevel.WARNING)
                } else {
                    viewModel.createNewGroup(event.name)
                    GlobalToasts.show(application.getString(R.string.create_group_success, event.name), level = GlobalToasts.ToastLevel.SUCCESS)
                }
            }

            is DownloadEvent.OnDeleteGroup -> {
                viewModel.deleteGroup(event.group)
                GlobalToasts.show(deleteSuccess, level = GlobalToasts.ToastLevel.SUCCESS)
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
        title = application.getString(R.string.read_download_dir_title),
        message = application.getString(R.string.read_download_dir_message),
        confirmText = application.getString(R.string.ok),
        dismissText = application.getString(R.string.cancel),
        onConfirm = {
            showImportDownloadedConfirm = false
            isImportingDownloaded = true
            showImportProgress = true
            importProgress = 0
            importTotal = 0
            importCurrentName = null
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        if (!checkSafPermissions(context)) return@withContext null
                        scanAndImportHanimeDownloads(context, dao) { imported, total, currentName ->
                            importProgress = imported
                            importTotal = total
                            importCurrentName = currentName
                        }
                    } catch (e: Exception) {
                        Log.e("ImportHanime", "Failed to import downloaded videos", e)
                        null
                    }
                }
                isImportingDownloaded = false
                showImportProgress = false
                if (result == null) {
                    GlobalToasts.show(permissionError, level = GlobalToasts.ToastLevel.ERROR)
                } else {
                    viewModel.loadAllDownloadedHanime(
                        sortedBy = HanimeDownloadEntity.SortedBy.ID,
                        ascending = false,
                    )
                    if (result.failureCount == 0) {
                        GlobalToasts.show(
                            application.getString(R.string.import_success_count, result.successCount),
                            level = GlobalToasts.ToastLevel.SUCCESS,
                        )
                    } else {
                        importResult = result
                        showImportResult = true
                    }
                }
            }
        },
        onDismiss = { showImportDownloadedConfirm = false },
    )

    if (showImportProgress) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.read_download_dir_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.reading_download_dir))
                    importCurrentName?.let { name ->
                        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    LinearWavyProgressIndicator(
                        progress = {
                            if (importTotal > 0) importProgress.toFloat() / importTotal else 0f
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val percent = if (importTotal > 0) importProgress * 100 / importTotal else 0
                    Text(
                        stringResource(R.string.import_progress_format)
                            .format(importProgress, importTotal, percent)
                    )
                }
            },
            confirmButton = {},
        )
    }

    if (showImportResult) {
        val result = importResult
        AlertDialog(
            onDismissRequest = { showImportResult = false },
            title = { Text(stringResource(R.string.import_result_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(stringResource(R.string.import_success_count, result?.successCount ?: 0))
                    if (result != null && result.failureCount > 0) {
                        Text(stringResource(R.string.import_fail_count, result.failureCount))
                        for (failure in result.failures) {
                            val reasonText = when (failure.reason) {
                                ImportFailureReason.INVALID_FOLDER_NAME ->
                                    stringResource(R.string.import_fail_reason_invalid_name)
                                ImportFailureReason.MISSING_INFO_JSON ->
                                    stringResource(R.string.import_fail_reason_missing_info)
                                ImportFailureReason.IMPORT_EXCEPTION ->
                                    stringResource(R.string.import_fail_reason_exception)
                            }
                            Text("• ${failure.name}（$reasonText）")
                        }
                        Text(stringResource(R.string.import_fail_suggestion))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportResult = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

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
