package com.yenaly.han1meviewer.ui.screen.home.download

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.logic.model.DownloadItemNode
import com.yenaly.han1meviewer.logic.model.DownloadedNode
import com.yenaly.han1meviewer.logic.state.DownloadState

/**
 * 将已下载视频列表按分组 ID 转换为 [DownloadHeaderNode] 列表。
 *
 * @param groupIdToNameMap 分组 ID -> 名称映射
 * @param collapseDownloadedGroup 默认是否折叠分组
 * @return 按分组聚合后的 Header 节点列表
 */
fun List<VideoWithCategories>.toNodeList(
    groupIdToNameMap: Map<Int, String>,
    collapseDownloadedGroup: Boolean,
): List<DownloadHeaderNode> {
    val groupedData = this.groupBy { it.video.groupId }.toSortedMap()
    return buildList {
        for ((groupId, videos) in groupedData) {
            add(
                DownloadHeaderNode(
                    groupKey = groupIdToNameMap[groupId] ?: "ID: $groupId",
                    originalVideos = videos,
                    isExpanded = !collapseDownloadedGroup,
                )
            )
        }
    }
}

/**
 * 将 Header 列表展开为扁平节点列表（Header + 展开状态下的子项）。
 *
 * @return 扁平化的 [DownloadedNode] 列表
 */
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

/**
 * 将未分组的分组名称替换为"未分组"字符串资源。
 *
 * @param List<DownloadGroupEntity> 分组列表
 * @return 替换后的分组列表
 */
@Composable
fun List<DownloadGroupEntity>.toDisplayGroups(): List<DownloadGroupEntity> = map { group ->
    if (group.id == DownloadGroupEntity.DEFAULT_GROUP_ID) {
        group.copy(name = stringResource(R.string.ungrouped))
    } else {
        group
    }
}

/**
 * 下载状态对应的显示文本。
 *
 * @param state 下载状态
 * @param progress 下载进度 (0-100)
 * @return 本地化文本
 */
@Composable
fun downloadStateText(state: DownloadState, progress: Int): String = when (state) {
    DownloadState.Queued -> stringResource(R.string.already_in_queue)
    DownloadState.Downloading -> stringResource(R.string.download_progress_percent, progress)
    DownloadState.Paused -> stringResource(R.string.paused)
    DownloadState.Failed -> stringResource(R.string.download_failed_tap_retry)
    DownloadState.Finished -> stringResource(R.string.download_complete)
    DownloadState.Unknown -> stringResource(R.string.loading)
}

/**
 * 下载状态对应的图标资源 ID。
 *
 * @param state 下载状态
 * @return 图标 drawable 资源 ID
 */
fun downloadStateIcon(state: DownloadState): Int = when (state) {
    DownloadState.Queued -> R.drawable.ic_baseline_play_arrow_24
    DownloadState.Downloading -> R.drawable.ic_baseline_pause_24
    DownloadState.Paused -> R.drawable.ic_baseline_play_arrow_24
    DownloadState.Failed -> R.drawable.baseline_error_outline_24
    DownloadState.Finished -> R.drawable.ic_baseline_check_circle_24
    DownloadState.Unknown -> R.drawable.ic_baseline_download_24
}
