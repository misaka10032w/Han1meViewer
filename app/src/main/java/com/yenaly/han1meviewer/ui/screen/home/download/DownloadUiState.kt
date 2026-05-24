package com.yenaly.han1meviewer.ui.screen.home.download

import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.model.DownloadedNode

/**
 * 下载页面 UI 状态。
 *
 * @param downloadingItems 正在下载/等待中的任务列表
 * @param downloadedNodes 已下载视频的扁平节点列表
 * @param displayGroups 用于 UI 展示的分组列表（默认分组名已替换）
 * @param currentPage 当前选中的 Tab 索引 (0=下载中, 1=已下载)
 * @param showCreateGroupDialog 是否显示新建分组对话框
 */
data class DownloadUiState(
    val downloadingItems: List<HanimeDownloadEntity> = emptyList(),
    val downloadedNodes: List<DownloadedNode> = emptyList(),
    val displayGroups: List<DownloadGroupEntity> = emptyList(),
    val currentPage: Int = 0,
    val showCreateGroupDialog: Boolean = false,
)

/**
 * 下载页面的用户交互事件。
 */
sealed interface DownloadEvent {
    /** 暂停全部下载任务 */
    data class OnPauseAll(val items: List<HanimeDownloadEntity>) : DownloadEvent

    /** 恢复全部暂停任务 */
    data class OnResumeAll(val items: List<HanimeDownloadEntity>) : DownloadEvent

    /** 暂停单个下载任务 */
    data class OnPauseItem(val item: HanimeDownloadEntity) : DownloadEvent

    /** 恢复单个下载任务 */
    data class OnResumeItem(val item: HanimeDownloadEntity) : DownloadEvent

    /** 删除下载中任务 */
    data class OnDeleteDownloadingItem(val item: HanimeDownloadEntity) : DownloadEvent

    /** 导入已下载视频 */
    data object OnImportDownloaded : DownloadEvent

    /** 打开已下载视频详情 */
    data class OnOpenDownloadedVideo(val video: VideoWithCategories) : DownloadEvent

    /** 本地播放已下载视频 */
    data class OnLocalPlayback(val video: VideoWithCategories) : DownloadEvent

    /** 外部播放器播放 */
    data class OnExternalPlayback(val video: VideoWithCategories) : DownloadEvent

    /** 删除已下载视频 */
    data class OnDeleteDownloadedVideo(val video: VideoWithCategories) : DownloadEvent

    /** 移动视频到指定分组 */
    data class OnMoveVideoGroup(val video: VideoWithCategories, val groupId: Int) : DownloadEvent

    /** 重命名分组 */
    data class OnRenameGroup(val groupId: Int, val newName: String) : DownloadEvent

    /** 创建新分组 */
    data class OnCreateGroup(val name: String) : DownloadEvent

    /** 删除分组 */
    data class OnDeleteGroup(val group: DownloadGroupEntity) : DownloadEvent

    /** 切换分组展开/折叠 */
    data class OnToggleGroup(val groupKey: String) : DownloadEvent

    /** 新建分组对话框状态变化 */
    data class OnCreateGroupDialogChange(val visible: Boolean) : DownloadEvent

    /** 切换 Tab 页 */
    data class OnPageChange(val page: Int) : DownloadEvent
}
