package com.yenaly.han1meviewer.ui.screen.home.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.logic.model.DownloadItemNode
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeDownloadedGroups
import com.yenaly.han1meviewer.ui.preview.fakeDownloadedNodes

/**
 * 已下载 Tab 页面（Content 层）。
 *
 * 接收 [DownloadUiState] + [DownloadEvent] 回调，不持有 ViewModel。
 *
 * @param uiState 页面 UI 状态
 * @param onEvent 用户交互事件回调
 */
@Composable
fun DownloadedScreen(
    uiState: DownloadUiState,
    onEvent: (DownloadEvent) -> Unit,
) {
    var pendingRename by remember { mutableStateOf<DownloadHeaderNode?>(null) }
    var pendingMoveVideo by remember { mutableStateOf<VideoWithCategories?>(null) }

    CreateGroupDialog(
        visible = uiState.showCreateGroupDialog,
        groups = uiState.displayGroups,
        onDismiss = { onEvent(DownloadEvent.OnCreateGroupDialogChange(false)) },
        onConfirm = {
            onEvent(DownloadEvent.OnCreateGroup(it))
            onEvent(DownloadEvent.OnCreateGroupDialogChange(false))
        },
        onDeleteGroup = { onEvent(DownloadEvent.OnDeleteGroup(it)) },
    )

    GroupRenameDialog(
        header = pendingRename,
        groups = uiState.displayGroups,
        onDismiss = { pendingRename = null },
        onConfirm = { header, newName ->
            uiState.displayGroups.find { it.name == header.groupKey }?.let { group ->
                onEvent(DownloadEvent.OnRenameGroup(group.id, newName))
            }
            pendingRename = null
        },
        onDelete = { header ->
            uiState.displayGroups.find { it.name == header.groupKey }
                ?.let { onEvent(DownloadEvent.OnDeleteGroup(it)) }
            pendingRename = null
        },
    )

    MoveGroupDialog(
        video = pendingMoveVideo,
        groups = uiState.displayGroups,
        onDismiss = { pendingMoveVideo = null },
        onConfirm = { video, groupId ->
            onEvent(DownloadEvent.OnMoveVideoGroup(video, groupId))
            pendingMoveVideo = null
        },
    )

    if (uiState.downloadedNodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyContent(
                hint = stringResource(R.string.empty_content),
                subHint = stringResource(R.string.downloaded),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(uiState.downloadedNodes, key = {
            when (it) {
                is DownloadHeaderNode -> "header-${it.groupKey}"
                is DownloadItemNode -> "item-${it.parentKey}-${it.data.video.id}"
            }
        }) { node ->
            when (node) {
                is DownloadHeaderNode -> DownloadGroupHeader(
                    header = node,
                    onToggle = { onEvent(DownloadEvent.OnToggleGroup(node.groupKey)) },
                    onRename = {
                        val group = uiState.displayGroups.find { it.name == node.groupKey }
                        if (group?.id == DownloadGroupEntity.DEFAULT_GROUP_ID) {
                            // 默认分组不可重命名
                        } else {
                            pendingRename = node
                        }
                    },
                )

                is DownloadItemNode -> DownloadedVideoCard(
                    item = node.data,
                    onOpenVideo = { onEvent(DownloadEvent.OnOpenDownloadedVideo(node.data)) },
                    onLocalPlayback = { onEvent(DownloadEvent.OnLocalPlayback(node.data)) },
                    onExternalPlayback = { onEvent(DownloadEvent.OnExternalPlayback(node.data)) },
                    onDeleteVideo = { onEvent(DownloadEvent.OnDeleteDownloadedVideo(node.data)) },
                    onMoveGroup = { pendingMoveVideo = node.data },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun DownloadedScreenPreview() {
    ComponentPreview {
        DownloadedScreen(
            uiState = DownloadUiState(
                downloadedNodes = fakeDownloadedNodes,
                displayGroups = fakeDownloadedGroups,
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun DownloadedScreenEmptyPreview() {
    ComponentPreview {
        DownloadedScreen(
            uiState = DownloadUiState(
                downloadedNodes = emptyList(),
                displayGroups = emptyList(),
            ),
            onEvent = {},
        )
    }
}
