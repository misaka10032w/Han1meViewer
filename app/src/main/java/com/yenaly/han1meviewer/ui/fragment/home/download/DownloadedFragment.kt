package com.yenaly.han1meviewer.ui.fragment.home.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.yenaly.han1meviewer.FROM_DOWNLOAD
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.logic.model.DownloadItemNode
import com.yenaly.han1meviewer.logic.model.DownloadedNode
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadedScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.openVideo
import com.yenaly.han1meviewer.util.openDownloadedHanimeVideoLocally
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.utils.showLongToast

class DownloadedFragment : Fragment() {

    val viewModel by viewModels<DownloadViewModel>()
    private var headerNodes by mutableStateOf<List<DownloadHeaderNode>>(emptyList())
    private var showCreateGroupDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAllSortedDownloadedHanime()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val videoList = viewModel.downloaded.collectAsStateWithLifecycle().value
                val groups = viewModel.downloadedGroups.collectAsStateWithLifecycle().value
                val displayGroups = groups.map { group ->
                    if (group.id == DownloadGroupEntity.DEFAULT_GROUP_ID) {
                        group.copy(name = getString(R.string.ungrouped))
                    } else {
                        group
                    }
                }
                val nodes = buildDownloadedNodes(videoList, groups)

                HanimeTheme {
                    DownloadedScreen(
                        nodes = nodes,
                        groups = displayGroups,
                        showCreateGroupDialog = showCreateGroupDialog,
                        onToggleGroup = ::toggleGroup,
                        onHeaderLongClick = ::handleHeaderLongClick,
                        onOpenVideo = ::openDownloadedVideo,
                        onLocalPlayback = ::playDownloadedVideoLocally,
                        onExternalPlayback = ::playDownloadedVideoExternally,
                        onDeleteVideo = ::deleteDownloadedVideo,
                        onMoveVideoGroup = ::moveVideoGroup,
                        onRenameGroup = ::renameGroup,
                        onCreateGroup = ::createGroup,
                        onDeleteGroup = ::deleteGroup,
                        onCreateGroupDialogChange = { showCreateGroupDialog = it },
                    )
                }
            }
        }
    }

    fun onToolbarMenuSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.tb_search -> {
                showLongToast(requireContext().getString(R.string.coming_soon))
                true
            }

            R.id.tb_add_group -> {
                showCreateGroupDialog = true
                true
            }

            else -> false
        }
    }

    private fun moveVideoGroup(video: VideoWithCategories, targetGroupId: Int) {
        viewModel.updateVideoGroup(video.video.videoCode, targetGroupId)
    }

    private fun renameGroup(groupId: Int, newName: String) {
        viewModel.updateGroupName(groupId, newName)
        showLongToast(getString(R.string.group_renamed, newName))
    }

    private fun createGroup(name: String) {
        if (name.isBlank()) {
            showLongToast(getString(R.string.group_name_empty))
            return
        }
        viewModel.createNewGroup(name)
        showLongToast(getString(R.string.create_group_success, name))
    }

    private fun deleteGroup(group: DownloadGroupEntity) {
        viewModel.deleteGroup(group)
        showLongToast(getString(R.string.delete_success))
    }

    private fun openDownloadedVideo(video: VideoWithCategories) {
        openVideo(video.video.videoCode)
    }

    private fun playDownloadedVideoLocally(video: VideoWithCategories) {
        findNavController().navigate(
            R.id.videoFragment,
            bundleOf(
                VIDEO_CODE to video.video.videoCode,
                FROM_DOWNLOAD to true,
            )
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

    private fun loadAllSortedDownloadedHanime(): Boolean {
        viewModel.loadAllDownloadedHanime(
            sortedBy = HanimeDownloadEntity.SortedBy.ID,
            ascending = false,
        )
        return true
    }

    private fun buildDownloadedNodes(
        videoList: List<VideoWithCategories>,
        allGroups: List<DownloadGroupEntity>,
    ): List<DownloadedNode> {
        val ungroupedGroupName = requireContext().getString(R.string.ungrouped)
        val updatedGroups = allGroups.map { group ->
            if (group.id == DownloadGroupEntity.DEFAULT_GROUP_ID) {
                group.copy(name = ungroupedGroupName)
            } else {
                group
            }
        }
        val groupIdToNameMap = updatedGroups.associate { it.id to it.name }

        if (videoList.isEmpty()) {
            headerNodes = emptyList()
            return emptyList()
        }

        val newHeaders = videoList.toNodeList(groupIdToNameMap)
        val oldExpandedByKey = headerNodes.associate { it.groupKey to it.isExpanded }
        headerNodes = newHeaders.map { newHeader ->
            newHeader.copy(
                isExpanded = oldExpandedByKey[newHeader.groupKey]
                    ?: !Preferences.collapseDownloadedGroup
            )
        }

        return headerNodes.toFlatNodeList()
    }

    private fun toggleGroup(header: DownloadHeaderNode) {
        headerNodes = headerNodes.map {
            if (it.groupKey == header.groupKey) it.copy(isExpanded = !it.isExpanded) else it
        }
    }

    private fun handleHeaderLongClick(header: DownloadHeaderNode) {
        if (header.groupKey == getString(R.string.ungrouped)) {
            showLongToast(getString(R.string.default_group_rename_not_allowed, header.groupKey))
            return
        }
    }

    private fun List<VideoWithCategories>.toNodeList(groupIdToNameMap: Map<Int, String>): List<DownloadHeaderNode> {
        val groupedData = this.groupBy { it.video.groupId }.toSortedMap()
        return buildList {
            for ((groupId, videos) in groupedData) {
                add(
                    DownloadHeaderNode(
                        groupKey = groupIdToNameMap[groupId] ?: "ID: $groupId",
                        originalVideos = videos,
                        isExpanded = !Preferences.collapseDownloadedGroup,
                    )
                )
            }
        }
    }

    private fun List<DownloadHeaderNode>.toFlatNodeList(): List<DownloadedNode> {
        val flatList = mutableListOf<DownloadedNode>()
        for (header in this) {
            flatList.add(header)
            if (header.isExpanded) {
                header.originalVideos.forEach { video ->
                    flatList.add(DownloadItemNode(video, header.groupKey))
                }
            }
        }
        return flatList
    }
}
