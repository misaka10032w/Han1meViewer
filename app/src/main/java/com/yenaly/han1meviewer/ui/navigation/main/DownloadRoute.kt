package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadScreen
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.SafFileManager.scanAndImportHanimeDownloads
import com.yenaly.han1meviewer.util.openDownloadedHanimeVideoLocally
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.yenaly_libs.utils.application
import com.yenaly.yenaly_libs.utils.showLongToast
import kotlinx.coroutines.runBlocking

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
    var showVideoNotExistConfirm by remember { mutableStateOf<VideoWithCategories?>(null) }
    var showDeleteVideoConfirm by remember { mutableStateOf<VideoWithCategories?>(null) }
    DownloadScreen(
        downloadingFlow = viewModel.loadAllDownloadingHanime(),
        downloadedFlow = viewModel.downloaded,
        downloadedGroupsFlow = viewModel.downloadedGroups,
        collapseDownloadedGroup = Preferences.collapseDownloadedGroup,
        onBack = onBack,
        onPauseAll = { items ->
            items.forEach { entity ->
                if (entity.isDownloading) HanimeDownloadManagerV2.stopTask(
                    entity
                )
            }
        },
        onResumeAll = { items ->
            items.forEach { entity ->
                if (!entity.isDownloading) HanimeDownloadManagerV2.resumeTask(
                    entity
                )
            }
        },
        onPauseItem = HanimeDownloadManagerV2::stopTask,
        onResumeItem = HanimeDownloadManagerV2::resumeTask,
        onDeleteDownloadingItem = HanimeDownloadManagerV2::deleteTask,
        onImportDownloaded = {
            if (!Preferences.safDownloadPath.isNullOrBlank() && !Preferences.isUsePrivateStorage) {
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
        onLocalPlayback = { onNavigateToLocalVideo(it.video.videoCode, it.video.videoUri) },
        onExternalPlayback = { video ->
            context.openDownloadedHanimeVideoLocally(video.video.videoUri) {
                showVideoNotExistConfirm = video
            }
        },
        onDeleteDownloadedVideo = { video ->
            showDeleteVideoConfirm = video
        },
        onMoveVideoGroup = { video, groupId ->
            viewModel.updateVideoGroup(
                video.video.videoCode,
                groupId
            )
        },
        onRenameGroup = { groupId, newName ->
            viewModel.updateGroupName(groupId, newName)
            showLongToast(application.getString(R.string.group_renamed, newName))
        },
        onCreateGroup = { name ->
            if (name.isBlank()) {
                showLongToast(groupNameEmpty)
            } else {
                viewModel.createNewGroup(name)
                showLongToast(application.getString(R.string.create_group_success, name))
            }
        },
        onDeleteGroup = { group ->
            viewModel.deleteGroup(group)
            showLongToast(deleteSuccess)
        },
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
