package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.jzvd.JZUtils
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn

private enum class HKeyframeDialog {
    EditEntity,
    ShareEntity,
    DeleteEntity,
    EditKeyframe,
    DeleteKeyframe,
}

@Composable
fun HKeyframesScreen(
    items: List<HKeyframeEntity>,
    onOpenVideo: (String) -> Unit,
    onDeleteEntity: (HKeyframeEntity) -> Unit,
    onUpdateEntityTitle: (HKeyframeEntity, String) -> Unit,
    onDeleteKeyframe: (String, HKeyframeEntity.Keyframe) -> Unit,
    onUpdateKeyframe: (String, HKeyframeEntity.Keyframe, HKeyframeEntity.Keyframe) -> Unit,
    onCopyShareContent: (String) -> Unit,
) {
    var selectedEntity by remember { mutableStateOf<HKeyframeEntity?>(null) }
    var selectedKeyframe by remember { mutableStateOf<Pair<String, HKeyframeEntity.Keyframe>?>(null) }
    var activeDialog by rememberSaveable { mutableStateOf<HKeyframeDialog?>(null) }

    selectedEntity?.takeIf { activeDialog == HKeyframeDialog.EditEntity }?.let { entity ->
        EditEntityDialog(
            entity = entity,
            onDismiss = {
                activeDialog = null
                selectedEntity = null
            },
            onConfirm = { newTitle ->
                onUpdateEntityTitle(entity, newTitle)
                activeDialog = null
                selectedEntity = null
            },
        )
    }

    selectedEntity?.takeIf { activeDialog == HKeyframeDialog.DeleteEntity }?.let { entity ->
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.sure_to_delete),
            message = entity.title,
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            onDismiss = {
                activeDialog = null
                selectedEntity = null
            },
            onConfirm = {
                onDeleteEntity(entity)
                activeDialog = null
                selectedEntity = null
            },
        )
    }

    selectedEntity?.takeIf { activeDialog == HKeyframeDialog.ShareEntity }?.let { entity ->
        ShareEntityDialog(
            entity = entity,
            onDismiss = {
                activeDialog = null
                selectedEntity = null
            },
            onCopy = {
                onCopyShareContent(it)
                activeDialog = null
                selectedEntity = null
            },
        )
    }

    selectedKeyframe?.takeIf { activeDialog == HKeyframeDialog.EditKeyframe }?.let { (videoCode, keyframe) ->
        EditKeyframeDialog(
            keyframe = keyframe,
            onDismiss = {
                activeDialog = null
                selectedKeyframe = null
            },
            onConfirm = { newKeyframe ->
                onUpdateKeyframe(videoCode, keyframe, newKeyframe)
                activeDialog = null
                selectedKeyframe = null
            },
        )
    }

    selectedKeyframe?.takeIf { activeDialog == HKeyframeDialog.DeleteKeyframe }?.let { (videoCode, keyframe) ->
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.sure_to_delete),
            message = JZUtils.stringForTime(keyframe.position),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            onDismiss = {
                activeDialog = null
                selectedKeyframe = null
            },
            onConfirm = {
                onDeleteKeyframe(videoCode, keyframe)
                activeDialog = null
                selectedKeyframe = null
            },
        )
    }

    if (items.isEmpty()) {
        EmptyContent(hint = stringResource(R.string.here_is_empty))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.videoCode }) { entity ->
            HKeyframeEntityCard(
                entity = entity,
                onOpenVideo = { onOpenVideo(entity.videoCode) },
                onEdit = {
                    selectedEntity = entity
                    activeDialog = HKeyframeDialog.EditEntity
                },
                onDelete = {
                    selectedEntity = entity
                    activeDialog = HKeyframeDialog.DeleteEntity
                },
                onShare = {
                    selectedEntity = entity
                    activeDialog = HKeyframeDialog.ShareEntity
                },
                onEditKeyframe = { keyframe ->
                    selectedKeyframe = entity.videoCode to keyframe
                    activeDialog = HKeyframeDialog.EditKeyframe
                },
                onDeleteKeyframe = { keyframe ->
                    selectedKeyframe = entity.videoCode to keyframe
                    activeDialog = HKeyframeDialog.DeleteKeyframe
                },
            )
        }
    }
}

@Composable
private fun HKeyframeEntityCard(
    entity: HKeyframeEntity,
    onOpenVideo: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onEditKeyframe: (HKeyframeEntity.Keyframe) -> Unit,
    onDeleteKeyframe: (HKeyframeEntity.Keyframe) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = entity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
                    TextButton(onClick = onShare) { Text(stringResource(R.string.share)) }
                }
            }

            Text(
                text = stringResource(R.string.h_keyframe_title_prefix) + entity.videoCode,
                modifier = Modifier.clickable(onClick = onOpenVideo),
                color = MaterialTheme.colorScheme.primary,
            )

            HorizontalDivider()

            entity.keyframes.forEachIndexed { index, keyframe ->
                HKeyframeRow(
                    index = index + 1,
                    keyframe = keyframe,
                    onEdit = { onEditKeyframe(keyframe) },
                    onDelete = { onDeleteKeyframe(keyframe) },
                )
            }
        }
    }
}

@Composable
private fun HKeyframeRow(
    index: Int,
    keyframe: HKeyframeEntity.Keyframe,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = JZUtils.stringForTime(keyframe.position),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "#$index",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            keyframe.prompt?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "➥ $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
        }
    }
}

@Composable
private fun EditEntityDialog(
    entity: HKeyframeEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var title by remember(entity.title) { mutableStateOf(entity.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.modify_h_keyframe)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = entity.videoCode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.video_code)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun EditKeyframeDialog(
    keyframe: HKeyframeEntity.Keyframe,
    onDismiss: () -> Unit,
    onConfirm: (HKeyframeEntity.Keyframe) -> Unit,
) {
    var positionText by remember(keyframe.position) { mutableStateOf(keyframe.position.toString()) }
    var prompt by remember(keyframe.prompt) { mutableStateOf(keyframe.prompt.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.modify_h_keyframe)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = positionText,
                    onValueChange = { positionText = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.position_ms)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(stringResource(R.string.prompt)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    HKeyframeEntity.Keyframe(
                        position = positionText.toLongOrNull() ?: keyframe.position,
                        prompt = prompt,
                    )
                )
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ShareEntityDialog(
    entity: HKeyframeEntity,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val content = remember(entity) {
        val toJson = kotlinx.serialization.json.Json.encodeToString(entity)
        val toBase64 = toJson.encodeToByteArray().let {
            android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
        }
        ">>>${toBase64}<<<"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_to_others)) },
        text = {
            Text(
                text = stringResource(R.string.share_to_others_tip, content),
                modifier = Modifier.heightIn(max = 260.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onCopy(content) }) {
                Text(stringResource(R.string.copy_))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun HKeyframesScreenPreview() {
    ComponentPreview {
        HKeyframesScreen(
            items = listOf(
                HKeyframeEntity(
                    videoCode = "123456",
                    title = "図書室ノ彼女 THE ANIMATION",
                    keyframes = mutableListOf(
                        HKeyframeEntity.Keyframe(12_000, "进入正题"),
                        HKeyframeEntity.Keyframe(36_000, "高能部分"),
                    ),
                    createdTime = System.currentTimeMillis(),
                )
            ),
            onOpenVideo = {},
            onDeleteEntity = {},
            onUpdateEntityTitle = { _, _ -> },
            onDeleteKeyframe = { _, _ -> },
            onUpdateKeyframe = { _, _, _ -> },
            onCopyShareContent = {},
        )
    }
}
