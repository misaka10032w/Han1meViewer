package com.yenaly.han1meviewer.logic.model

import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories

/**
 * 已下载分组的Model
 *
 * @project Han1meViewer
 *
 * @author Misaka10032w - 创建 (2025/11/27)
 * 初始版本
 * 实现分组展示和展开/折叠功能
 * 实现分组移动、重命名等
 */

sealed class DownloadedNode

data class DownloadHeaderNode(
    val groupKey: String,
    val originalVideos: List<VideoWithCategories>,
    var isExpanded: Boolean = true
) : DownloadedNode()

data class DownloadItemNode(
    val data: VideoWithCategories,
    val parentKey: String
) : DownloadedNode()