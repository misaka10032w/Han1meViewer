package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.screen.settings.HKeyframeSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.HKeyframeSettingsUiState
import com.yenaly.han1meviewer.ui.screen.settings.HKeyframesScreen
import com.yenaly.han1meviewer.ui.screen.settings.SharedHKeyframesScreen
import com.yenaly.han1meviewer.ui.viewmodel.SettingsViewModel
import com.yenaly.yenaly_libs.utils.copyToClipboard
import com.yenaly.yenaly_libs.utils.decodeFromStringByBase64
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.serialization.json.Json

private const val H_KEYFRAMES_ENABLE = "h_keyframes_enable"
private const val SHOW_COMMENT_WHEN_COUNTDOWN = "show_comment_when_countdown"
private const val SHARED_H_KEYFRAMES_ENABLE = "shared_h_keyframes_enable"
private const val SHARED_H_KEYFRAMES_USE_FIRST = "shared_h_keyframes_use_first"
private const val WHEN_COUNTDOWN_REMIND = "when_countdown_remind"

@Composable
fun HKeyframesTopBarActions(onImportClick: () -> Unit) {
    FilledIconButton(onClick = onImportClick) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.h_keyframes_import_shared),
        )
    }
}

@Composable
fun HKeyframesRouteScreen(
    onOpenVideo: (String) -> Unit,
    showImportDialog: Boolean,
    onImportDialogDismiss: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel()
    val items by viewModel.loadAllHKeyframes()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var sharedHKeyframeEntity by remember { mutableStateOf<HKeyframeEntity?>(null) }

    if (showImportDialog) {
        ImportSharedHKeyframeDialog(
            onDismiss = onImportDialogDismiss,
            onConfirm = { content ->
                val entity = parseSharedHKeyframe(content)
                if (entity != null) {
                    sharedHKeyframeEntity = entity
                    onImportDialogDismiss()
                } else {
                    showShortToast(R.string.h_keyframes_shared_by_other_not_detected)
                }
            },
        )
    }

    HKeyframesScreen(
        items = items,
        onOpenVideo = onOpenVideo,
        onDeleteEntity = { entity ->
            viewModel.deleteHKeyframes(entity)
        },
        onUpdateEntityTitle = { entity, newTitle ->
            viewModel.updateHKeyframes(entity.copy(title = newTitle))
            showShortToast(R.string.modify_success)
        },
        onDeleteKeyframe = { videoCode, keyframe ->
            viewModel.removeHKeyframe(videoCode, keyframe)
            showShortToast(R.string.delete_success)
        },
        onUpdateKeyframe = { videoCode, oldKeyframe, newKeyframe ->
            viewModel.modifyHKeyframe(videoCode, oldKeyframe, newKeyframe)
            showShortToast(R.string.modify_success)
        },
        onCopyShareContent = {
            it.copyToClipboard()
            showShortToast(R.string.copy_to_clipboard)
        },
    )

    sharedHKeyframeEntity?.let { entity ->
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.h_keyframes_shared_by_other_detected),
            message = stringResource(
                R.string.shared_h_keyframe_detected_msg,
                entity.title,
                entity.videoCode,
                entity.keyframes.size,
            ).trimIndent(),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.insertHKeyframes(entity.copy(lastModifiedTime = System.currentTimeMillis()))
                sharedHKeyframeEntity = null
            },
            onDismiss = { sharedHKeyframeEntity = null },
        )
    }
}

private val shareRegex = Regex(">>>(.+)<<<")

private fun parseSharedHKeyframe(content: String): HKeyframeEntity? {
    return runCatching {
        val matchResult = shareRegex.find(content) ?: return@runCatching null
        val (toBase64) = matchResult.destructured
        val toJson = toBase64.decodeFromStringByBase64()
        Json.decodeFromString<HKeyframeEntity>(toJson)
    }.getOrNull()
}

@Composable
private fun ImportSharedHKeyframeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.h_keyframes_import_shared)) },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.h_keyframes_import_shared_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(content) }) {
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
fun SharedHKeyframesRouteScreen(
    onOpenVideo: (String) -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel()
    val items by viewModel.loadAllSharedHKeyframes()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    SharedHKeyframesScreen(
        items = items,
        onOpenVideo = onOpenVideo,
    )
}

@Composable
fun HKeyframeSettingsRouteScreen(
    onNavigateToHKeyframes: () -> Unit,
    onNavigateToSharedHKeyframes: () -> Unit,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val uiState = remember(refreshKey, context) { buildHKeyframeSettingsUiState(context) }

    HKeyframeSettingsScreen(
        state = uiState,
        onHKeyframesEnableChange = {
            saveBoolean(H_KEYFRAMES_ENABLE, it)
            refreshKey++
        },
        onOpenHKeyframeManage = onNavigateToHKeyframes,
        onSharedHKeyframesEnableChange = {
            saveBoolean(SHARED_H_KEYFRAMES_ENABLE, it)
            refreshKey++
        },
        onSharedHKeyframesUseFirstChange = {
            saveBoolean(SHARED_H_KEYFRAMES_USE_FIRST, it)
            refreshKey++
        },
        onOpenSharedHKeyframeManage = onNavigateToSharedHKeyframes,
        onShowCommentWhenCountdownChange = {
            saveBoolean(SHOW_COMMENT_WHEN_COUNTDOWN, it)
            refreshKey++
        },
        onWhenCountdownRemindChange = {
            Preferences.preferenceSp.edit { putInt(WHEN_COUNTDOWN_REMIND, it) }
            refreshKey++
        },
    )
}

private fun buildHKeyframeSettingsUiState(context: Context): HKeyframeSettingsUiState {
    return HKeyframeSettingsUiState(
        hKeyframesEnable = Preferences.hKeyframesEnable,
        hKeyframesSummary = if (Preferences.hKeyframesEnable) {
            context.getString(R.string.h_keyframes_enable_tip)
        } else {
            context.getString(R.string.h_keyframes_disable_tip)
        },
        sharedHKeyframesEnable = Preferences.sharedHKeyframesEnable,
        sharedHKeyframesUseFirst = Preferences.sharedHKeyframesUseFirst,
        showCommentWhenCountdown = Preferences.showCommentWhenCountdown,
        whenCountdownRemind = Preferences.whenCountdownRemind / 1000,
        whenCountdownRemindSummary = toPrettyCountdownRemindString(
            context,
            Preferences.whenCountdownRemind / 1000
        ),
    )
}
