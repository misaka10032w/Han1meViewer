package com.yenaly.han1meviewer.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.dao.download.HanimeDownloadDao
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.han1meviewer.util.HImageMeower
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.SafFileManager.scanAndImportHanimeDownloads
import com.yenaly.han1meviewer.util.checkBadGuy
import com.yenaly.han1meviewer.util.openDownloadedHanimeVideoLocally
import com.yenaly.han1meviewer.util.openVideo
import com.yenaly.han1meviewer.util.requestPostNotificationPermission
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.han1meviewer.worker.HanimeDownloadWorker
import com.yenaly.yenaly_libs.utils.showLongToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class DownloadFragment : Fragment() {

    private val viewModel by viewModels<DownloadViewModel>()
    private val dao: HanimeDownloadDao
        get() = DownloadDatabase.instance.hanimeDownloadDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkBadGuy(requireContext(), R.raw.akarin)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HanimeTheme {
                    DownloadScreen(
                        downloadingFlow = viewModel.loadAllDownloadingHanime(),
                        downloadedFlow = viewModel.downloaded,
                        downloadedGroupsFlow = viewModel.downloadedGroups,
                        collapseDownloadedGroup = Preferences.collapseDownloadedGroup,
                        onBack = { findNavController().navigateUp() },
                        onPauseAll = { items ->
                            items.forEach { entity ->
                                if (entity.isDownloading) HanimeDownloadManagerV2.stopTask(entity)
                            }
                        },
                        onResumeAll = { items ->
                            items.forEach { entity ->
                                if (!entity.isDownloading) HanimeDownloadManagerV2.resumeTask(entity)
                            }
                        },
                        onPauseItem = HanimeDownloadManagerV2::stopTask,
                        onResumeItem = HanimeDownloadManagerV2::resumeTask,
                        onDeleteDownloadingItem = HanimeDownloadManagerV2::deleteTask,
                        onImportDownloaded = ::importDownloaded,
                        onLoadDownloaded = ::loadDownloaded,
                        onOpenDownloadedVideo = ::openDownloadedVideo,
                        onLocalPlayback = ::playDownloadedVideoLocally,
                        onExternalPlayback = ::playDownloadedVideoExternally,
                        onDeleteDownloadedVideo = ::deleteDownloadedVideo,
                        onMoveVideoGroup = { video, groupId ->
                            viewModel.updateVideoGroup(video.video.videoCode, groupId)
                        },
                        onRenameGroup = { groupId, newName ->
                            viewModel.updateGroupName(groupId, newName)
                            showLongToast(getString(R.string.group_renamed, newName))
                        },
                        onCreateGroup = { name ->
                            if (name.isBlank()) {
                                showLongToast(getString(R.string.group_name_empty))
                            } else {
                                viewModel.createNewGroup(name)
                                showLongToast(getString(R.string.create_group_success, name))
                            }
                        },
                        onDeleteGroup = { group ->
                            viewModel.deleteGroup(group)
                            showLongToast(getString(R.string.delete_success))
                        },
                    )
                }
            }
        }
    }

    private fun loadDownloaded() {
        viewModel.loadAllDownloadedHanime(
            sortedBy = com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity.SortedBy.ID,
            ascending = false,
        )
    }

    private fun importDownloaded() {
        if (!Preferences.safDownloadPath.isNullOrBlank() && !Preferences.isUsePrivateStorage) {
            viewLifecycleOwner.lifecycleScope.launch {
                scanAndImportHanimeDownloads(requireContext(), dao)
            }
            showLongToast(getString(R.string.read_success))
        } else {
            showLongToast(getString(R.string.select_custom_directory))
        }
    }

    private fun createTestTask() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            requireContext().requestPostNotificationPermission()
            val uuid = UUID.randomUUID()
            val args = HanimeDownloadWorker.Args(
                quality = "720P",
                downloadUrl = "https://ash-speed.hetzner.com/100MB.bin",
                videoType = "mp4",
                hanimeName = "Test-$uuid",
                videoCode = uuid.toString(),
                coverUrl = HImageMeower.placeholder(100, 200),
            )
            HanimeDownloadManagerV2.addTask(args, redownload = false)
        }
    }

    private fun openDownloadedVideo(video: VideoWithCategories) {
        findNavController().openVideo(video.video.videoCode)
    }

    private fun playDownloadedVideoLocally(video: VideoWithCategories) {
        findNavController().navigate(
            R.id.videoFragment,
            Bundle().apply {
                putString(com.yenaly.han1meviewer.VIDEO_CODE, video.video.videoCode)
                putBoolean(com.yenaly.han1meviewer.FROM_DOWNLOAD, true)
            }
        )
    }

    private fun playDownloadedVideoExternally(video: VideoWithCategories) {
        requireContext().openDownloadedHanimeVideoLocally(video.video.videoUri) {
            requireContext().showAlertDialog {
                setTitle(R.string.video_not_exist)
                setMessage(R.string.video_deleted_sure_to_delete_item)
                setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                }
                setNegativeButton(R.string.cancel, null)
            }
        }
    }

    private fun deleteDownloadedVideo(video: VideoWithCategories) {
        requireContext().showAlertDialog {
            setTitle(R.string.sure_to_delete)
            setMessage(getString(R.string.prepare_to_delete_s, video.video.title))
            setPositiveButton(R.string.confirm) { _, _ ->
                SafFileManager.deleteDownloadVideoFolder(requireContext(), video.video.videoCode)
                viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
            }
            setNegativeButton(R.string.cancel, null)
        }
    }
}
