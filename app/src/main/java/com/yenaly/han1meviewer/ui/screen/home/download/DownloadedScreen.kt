package com.yenaly.han1meviewer.ui.screen.home.download

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.LOCAL_DATE_TIME_FORMAT
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.logic.model.DownloadItemNode
import com.yenaly.han1meviewer.logic.model.DownloadedNode
import com.yenaly.han1meviewer.ui.component.ComponentPreview
import com.yenaly.han1meviewer.ui.component.EmptyContent
import com.yenaly.han1meviewer.ui.preview.fakeDownloadedGroups
import com.yenaly.han1meviewer.ui.preview.fakeDownloadedNodes
import com.yenaly.yenaly_libs.utils.formatFileSizeV2
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun DownloadedScreen(
    nodes: List<DownloadedNode>,
    groups: List<DownloadGroupEntity>,
    showCreateGroupDialog: Boolean,
    onToggleGroup: (DownloadHeaderNode) -> Unit,
    onHeaderLongClick: (DownloadHeaderNode) -> Unit,
    onOpenVideo: (VideoWithCategories) -> Unit,
    onLocalPlayback: (VideoWithCategories) -> Unit,
    onExternalPlayback: (VideoWithCategories) -> Unit,
    onDeleteVideo: (VideoWithCategories) -> Unit,
    onMoveVideoGroup: (VideoWithCategories, Int) -> Unit,
    onRenameGroup: (Int, String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onDeleteGroup: (DownloadGroupEntity) -> Unit,
    onCreateGroupDialogChange: (Boolean) -> Unit,
) {
    var pendingRename by remember { mutableStateOf<DownloadHeaderNode?>(null) }
    var pendingMoveVideo by remember { mutableStateOf<VideoWithCategories?>(null) }

    CreateGroupDialog(
        visible = showCreateGroupDialog,
        onDismiss = { onCreateGroupDialogChange(false) },
        onConfirm = {
            onCreateGroup(it)
            onCreateGroupDialogChange(false)
        },
    )

    GroupRenameDialog(
        header = pendingRename,
        groups = groups,
        onDismiss = { pendingRename = null },
        onConfirm = { header, newName ->
            groups.find { it.name == header.groupKey }?.let { group ->
                onRenameGroup(group.id, newName)
            }
            pendingRename = null
        },
        onDelete = { header ->
            groups.find { it.name == header.groupKey }?.let(onDeleteGroup)
            pendingRename = null
        },
    )

    MoveGroupDialog(
        video = pendingMoveVideo,
        groups = groups,
        onDismiss = { pendingMoveVideo = null },
        onConfirm = { video, groupId ->
            onMoveVideoGroup(video, groupId)
            pendingMoveVideo = null
        },
    )

    if (nodes.isEmpty()) {
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
        items(nodes, key = {
            when (it) {
                is DownloadHeaderNode -> "header-${it.groupKey}"
                is DownloadItemNode -> "item-${it.parentKey}-${it.data.video.id}"
            }
        }) { node ->
            when (node) {
                is DownloadHeaderNode -> DownloadGroupHeader(
                    header = node,
                    onToggle = { onToggleGroup(node) },
                    onRename = {
                        val group = groups.find { it.name == node.groupKey }
                        if (group?.id == DownloadGroupEntity.DEFAULT_GROUP_ID) {
                            onHeaderLongClick(node)
                        } else {
                            pendingRename = node
                        }
                    },
                )

                is DownloadItemNode -> DownloadedVideoCard(
                    item = node.data,
                    onOpenVideo = { onOpenVideo(node.data) },
                    onLocalPlayback = { onLocalPlayback(node.data) },
                    onExternalPlayback = { onExternalPlayback(node.data) },
                    onDeleteVideo = { onDeleteVideo(node.data) },
                    onMoveGroup = { pendingMoveVideo = node.data },
                )
            }
        }
    }
}

@Composable
private fun DownloadGroupHeader(
    header: DownloadHeaderNode,
    onToggle: () -> Unit,
    onRename: () -> Unit,
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onToggle, onLongClick = onRename)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledIconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(
                        if (header.isExpanded) R.drawable.ic_baseline_fold_24 else R.drawable.ic_baseline_list_24
                    ),
                    contentDescription = null,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = header.groupKey,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.video_count, header.originalVideos.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = onToggle,
                label = {
                    Text(
                        if (header.isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalTime::class)
@Composable
private fun DownloadedVideoCard(
    item: VideoWithCategories,
    onOpenVideo: () -> Unit,
    onLocalPlayback: () -> Unit,
    onExternalPlayback: () -> Unit,
    onDeleteVideo: () -> Unit,
    onMoveGroup: () -> Unit,
) {
    val addedTime = if ( !LocalInspectionMode.current) {
        remember(item.video.addDate) {
            Instant.fromEpochMilliseconds(item.video.addDate)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .format(LOCAL_DATE_TIME_FORMAT)
        }
    } else {
        "2024-01-01 12:00"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpenVideo, onLongClick = onMoveGroup),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column  {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AsyncImage(
                    model = item.video.coverUri ?: item.video.coverUrl,
                    contentDescription = item.video.title,
                    modifier = Modifier
                        .width(150.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = addedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = onOpenVideo,
                            label = { Text(item.video.videoCode) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                        )
                        Text(
                            text = item.video.quality,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        VerticalDivider(
                            modifier = Modifier.height(14.dp),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = item.video.length.formatFileSizeV2(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 删除按钮
                Button(
                    onClick = onDeleteVideo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_delete_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(stringResource(R.string.delete))
                    }
                }

                // 外部播放按钮
                Button(
                    onClick = onExternalPlayback,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ext_link),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(stringResource(R.string.ext_player))
                    }
                }

                // 本地播放按钮
                Button(
                    onClick = onLocalPlayback,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_play_arrow_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(stringResource(R.string.local_playback))
                    }
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    if (!visible) return
    var name by remember { mutableStateOf("") }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        ElevatedCard(shape = RoundedCornerShape(28.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.create_new_group), style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.new_group_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val trimmed = name.trim()
                        if (trimmed.isNotBlank()) onConfirm(trimmed)
                    }),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotBlank()) onConfirm(trimmed)
                    }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupRenameDialog(
    header: DownloadHeaderNode?,
    groups: List<DownloadGroupEntity>,
    onDismiss: () -> Unit,
    onConfirm: (DownloadHeaderNode, String) -> Unit,
    onDelete: (DownloadHeaderNode) -> Unit,
) {
    if (header == null) return
    var name by remember(header.groupKey) { mutableStateOf(header.groupKey) }
    val group = groups.find { it.name == header.groupKey }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        ElevatedCard(shape = RoundedCornerShape(28.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.rename_group), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.current_group_name, header.groupKey),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.new_group_name)) },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    if (group != null && group.id != DownloadGroupEntity.DEFAULT_GROUP_ID) {
                        TextButton(onClick = { onDelete(header) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Text(stringResource(R.string.delete_group))
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotBlank() && trimmed != header.groupKey) {
                            onConfirm(header, trimmed)
                        }
                    }) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveGroupDialog(
    video: VideoWithCategories?,
    groups: List<DownloadGroupEntity>,
    onDismiss: () -> Unit,
    onConfirm: (VideoWithCategories, Int) -> Unit,
) {
    if (video == null) return
    BasicAlertDialog(onDismissRequest = onDismiss) {
        ElevatedCard(shape = RoundedCornerShape(28.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.modify_video_group, video.video.title),
                    style = MaterialTheme.typography.titleLarge,
                )
                groups.forEach { group ->
                    TextButton(
                        onClick = { onConfirm(video, group.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                            Text(group.name)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun DownloadedScreenPreview() {
    ComponentPreview {
        DownloadedScreen(
            nodes = fakeDownloadedNodes,
            groups = fakeDownloadedGroups,
            showCreateGroupDialog = false,
            onToggleGroup = {},
            onHeaderLongClick = {},
            onOpenVideo = {},
            onLocalPlayback = {},
            onExternalPlayback = {},
            onDeleteVideo = {},
            onMoveVideoGroup = { _, _ -> },
            onRenameGroup = { _, _ -> },
            onCreateGroup = {},
            onDeleteGroup = {},
            onCreateGroupDialogChange = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun DownloadedScreenEmptyPreview() {
    ComponentPreview {
        DownloadedScreen(
            nodes = emptyList(),
            groups = emptyList(),
            showCreateGroupDialog = false,
            onToggleGroup = {},
            onHeaderLongClick = {},
            onOpenVideo = {},
            onLocalPlayback = {},
            onExternalPlayback = {},
            onDeleteVideo = {},
            onMoveVideoGroup = { _, _ -> },
            onRenameGroup = { _, _ -> },
            onCreateGroup = {},
            onDeleteGroup = {},
            onCreateGroupDialogChange = {},
        )
    }
}
