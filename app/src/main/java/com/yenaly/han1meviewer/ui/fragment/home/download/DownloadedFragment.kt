package com.yenaly.han1meviewer.ui.fragment.home.download

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.FragmentListOnlyBinding
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.logic.model.DownloadItemNode
import com.yenaly.han1meviewer.logic.model.DownloadedNode
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.ui.StateLayoutMixin
import com.yenaly.han1meviewer.ui.adapter.HanimeDownloadedRvAdapter
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.activity
import com.yenaly.yenaly_libs.utils.dp
import com.yenaly.yenaly_libs.utils.showLongToast
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 已下载影片
 *
 * @project Han1meViewer
 *
 * @author Yenaly Liew - 创建 (2022/08/01)
 * 初始版本
 *
 * @author Misaka10032w - 更新 (2025/11/27)
 * 实现分组展示和展开/折叠功能
 * 实现分组移动、重命名等
 */
class DownloadedFragment : YenalyFragment<FragmentListOnlyBinding>(), StateLayoutMixin {

    val viewModel by viewModels<DownloadViewModel>()

    private lateinit var adapter: HanimeDownloadedRvAdapter
    private var headerNodes: List<DownloadHeaderNode> = emptyList()
    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentListOnlyBinding {
        return FragmentListOnlyBinding.inflate(inflater, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        binding.rvList.layoutManager = LinearLayoutManager(context)
        adapter = HanimeDownloadedRvAdapter(
            fragment = this,
            onHeaderClick = { header ->
                val updatedNodes = headerNodes.map {
                    if (it.groupKey == header.groupKey) {
                        it.copy(isExpanded = !it.isExpanded)
                    } else {
                        it
                    }
                }
                headerNodes = updatedNodes
                val flatList = headerNodes.toFlatNodeList()
                adapter.submitList(flatList)
            },
            onLongClickVideoItem = ::handleVideoGroupChange,
            onLongClickGroupHeader = ::handleGroupRename
        )
        binding.rvList.adapter = adapter
        ViewCompat.setOnApplyWindowInsetsListener(binding.rvList) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = navBar.bottom)
            WindowInsetsCompat.CONSUMED
        }
//        adapter.setStateViewLayout(R.layout.layout_empty_view)
        loadAllSortedDownloadedHanime()
    }

    private fun handleVideoGroupChange(video: VideoWithCategories) {
        viewLifecycleOwner.lifecycleScope.launch {
            val allGroups = try {
                viewModel.downloadedGroups.first()
            } catch (e: Exception) {
                e.printStackTrace()
                showLongToast("${requireContext().getString(R.string.unknown_error)}:${e.message}")
                return@launch
            }

            if (allGroups.isEmpty()) {
                showLongToast(requireContext().getString(R.string.video_group_empty))
                return@launch
            }
            showGroupSelectionDialog(video, allGroups)
        }
    }

    private fun handleGroupRename(headerNode: DownloadHeaderNode) {
        val context = requireContext()
        val currentGroupName = headerNode.groupKey
        val groupId = viewModel.downloadedGroups.value
            .find { it.name == currentGroupName }?.id
        if (groupId == null || groupId == DownloadGroupEntity.DEFAULT_GROUP_ID) {
            showLongToast(
                context.getString(
                    R.string.default_group_rename_not_allowed,
                    currentGroupName
                )
            )
            return
        }
        val input = EditText(context).apply {
            setText(currentGroupName)
            hint = context.getString(R.string.new_group_name)
        }
        val padding = 24.dp
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.rename_group))
            .setMessage(context.getString(R.string.current_group_name, currentGroupName))
            .setView(container)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isBlank() || newName == currentGroupName) {
                    val message = if (newName.isBlank()) {
                        context.getString(R.string.group_name_empty)
                    } else {
                        context.getString(R.string.group_name_unchanged)
                    }
                    showLongToast(message)
                } else {
                    viewModel.updateGroupName(groupId, newName)
                    showLongToast(context.getString(R.string.group_renamed, newName))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showGroupSelectionDialog(
        video: VideoWithCategories,
        allGroups: List<DownloadGroupEntity>
    ) {
        val context = requireContext()
        val groupNames = allGroups.map { it.name }.toTypedArray()
        val groupIds = allGroups.map { it.id }

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.modify_video_group, video.video.title))
            .setItems(groupNames) { dialog, which ->
                val selectedGroupId = groupIds[which]
                val selectedGroupName = groupNames[which]

                if (video.video.id != 0) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.updateVideoGroup(video.video.videoCode, selectedGroupId)

                        showLongToast(
                            context.getString(
                                R.string.group_modify_success,
                                selectedGroupName,
                                selectedGroupId.toString()
                            )
                        )
                        loadAllSortedDownloadedHanime()
                    }
                } else {
                    showLongToast(context.getString(R.string.unknown_group_id))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun bindDataObservers() {
        val ungroupedGroupName = requireContext().getString(R.string.ungrouped)
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                viewModel.downloaded,
                viewModel.downloadedGroups
            ) { videoList, allGroups ->
                Pair(videoList, allGroups)
            }
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { (videoList, allGroups) ->

                    val updatedGroupsName = allGroups.map { group ->
                        if (group.id == DownloadGroupEntity.DEFAULT_GROUP_ID) {
                            group.copy(name = ungroupedGroupName)
                        } else {
                            group
                        }
                    }
                    val groupIdToNameMap = updatedGroupsName.associate { it.id to it.name }

                    if (videoList.isEmpty()) {
                        //     adapter.setEmptyView(R.layout.layout_empty_view)
                        adapter.submitList(emptyList())
                        headerNodes = emptyList()
                        return@collect
                    }

                    if (headerNodes.isEmpty() || headerNodes.size != allGroups.size) {
                        headerNodes = videoList.toNodeList(groupIdToNameMap = groupIdToNameMap)
                    } else {

                        val newHeaderNodes =
                            videoList.toNodeList(groupIdToNameMap = groupIdToNameMap)

                        headerNodes = newHeaderNodes.map { newHeader ->
                            val oldHeader = headerNodes.find { it.groupKey == newHeader.groupKey }
                            newHeader.copy(isExpanded = oldHeader?.isExpanded ?: true)
                        }
                    }

                    adapter.submitList(headerNodes.toFlatNodeList())
                }
        }
    }

    /**
     * 将扁平列表 List<VideoWithCategories> 转换为 BaseNodeAdapter 所需的 List<DownloadHeaderNode> 结构。
     *
     * @param groupingKeySelector 定义分组依据
     * @return 包含所有分组头节点（Header Node）的列表。
     */
    fun List<VideoWithCategories>.toNodeList(
        groupingKeySelector: (VideoWithCategories) -> Int = { it.video.groupId },
        groupIdToNameMap: Map<Int, String>
    ): List<DownloadHeaderNode> {

        val groupedData = this.groupBy(groupingKeySelector).toSortedMap()
        val resultNodes = mutableListOf<DownloadHeaderNode>()

        for ((groupId, videos) in groupedData) {
            val groupName = groupIdToNameMap[groupId] ?: "ID: $groupId"
            val headerNode = DownloadHeaderNode(
                groupKey = groupName,
                originalVideos = videos,
                // 初始展开状态可以在这里设置
                isExpanded = true
            )
            resultNodes.add(headerNode)
        }

        return resultNodes
    }

    fun List<DownloadHeaderNode>.toFlatNodeList(): List<DownloadedNode> {
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

    fun onToolbarMenuSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.tb_search -> {
                showLongToast(requireContext().getString(R.string.coming_soon))
                true
            }

            R.id.tb_add_group -> {
                showCreateGroupDialog(requireContext())
                true
            }

            else -> false
        }
    }

    private fun showCreateGroupDialog(context: Context) {
        val input = EditText(context).apply {
            hint = context.getString(R.string.new_group_name)
        }
        val padding = 24.dp
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.create_new_group))
            .setView(container)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                val groupName = input.text.toString().trim()

                if (groupName.isBlank()) {
                    showLongToast(context.getString(R.string.group_name_empty))
                } else {
                    viewModel.createNewGroup(groupName)
                    showLongToast(context.getString(R.string.create_group_success,groupName))
                    dialog.dismiss()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        activity<AppCompatActivity>().supportActionBar?.setSubtitle(R.string.downloaded)
    }

    private fun loadAllSortedDownloadedHanime(): Boolean {
        viewModel.loadAllDownloadedHanime(
            sortedBy = HanimeDownloadEntity.SortedBy.ID,
            ascending = false
        )
        return true
    }
}