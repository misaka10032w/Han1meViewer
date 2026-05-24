package com.yenaly.han1meviewer.ui.screen.home.download

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.sharp.Create
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.component.verticalScrollbar

/**
 * 新建/管理分组对话框。
 *
 * @param visible 是否可见
 * @param groups 当前所有分组
 * @param onDismiss 关闭回调
 * @param onConfirm 确认创建回调
 * @param onDeleteGroup 删除分组回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    visible: Boolean,
    groups: List<DownloadGroupEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDeleteGroup: (DownloadGroupEntity) -> Unit,
) {
    if (!visible) return
    var name by remember { mutableStateOf("") }
    var pendingDeleteGroup by remember { mutableStateOf<DownloadGroupEntity?>(null) }

    if (pendingDeleteGroup != null) {
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.delete_group),
            message = stringResource(R.string.delete_group_confirm, pendingDeleteGroup!!.name),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                pendingDeleteGroup?.let { onDeleteGroup(it) }
                pendingDeleteGroup = null
            },
            onDismiss = { pendingDeleteGroup = null },
        )
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        ElevatedCard(shape = RoundedCornerShape(28.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.create_new_group),
                    style = MaterialTheme.typography.titleLarge
                )
                if (groups.isNotEmpty()) {
                    val scrollState = rememberLazyListState()
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 240.dp)
                            .verticalScrollbar(
                                state = scrollState,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                width = 4.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items = groups, key = { it.id }) { group ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                val isDefaultGroup =
                                    group.id == DownloadGroupEntity.DEFAULT_GROUP_ID
                                FilledTonalIconButton(
                                    onClick = { pendingDeleteGroup = group },
                                    modifier = Modifier.size(30.dp),
                                    enabled = !isDefaultGroup,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.delete_group),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.new_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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

/**
 * 分组重命名对话框。
 *
 * @param header 目标分组节点
 * @param groups 当前所有分组
 * @param onDismiss 关闭回调
 * @param onConfirm 确认重命名回调
 * @param onDelete 删除此分组回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupRenameDialog(
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
                Text(
                    stringResource(R.string.rename_group),
                    style = MaterialTheme.typography.titleLarge
                )
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

/**
 * 移动视频到其他分组对话框。
 *
 * @param video 目标视频
 * @param groups 当前所有分组
 * @param onDismiss 关闭回调
 * @param onConfirm 确认移动回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveGroupDialog(
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
                    text = stringResource(R.string.modify_video_group, video.video.title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (groups.isNotEmpty()) {
                    val scrollState = rememberLazyListState()
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .verticalScrollbar(
                                state = scrollState,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                width = 4.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(items = groups, key = { it.id }) { group ->
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Sharp.Create,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onConfirm(video, group.id) }
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
