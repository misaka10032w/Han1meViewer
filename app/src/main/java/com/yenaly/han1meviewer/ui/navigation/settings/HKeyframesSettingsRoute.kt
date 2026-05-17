package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.yenaly.yenaly_libs.utils.textFromClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val H_KEYFRAMES_ENABLE = "h_keyframes_enable"
private const val SHOW_COMMENT_WHEN_COUNTDOWN = "show_comment_when_countdown"
private const val SHARED_H_KEYFRAMES_ENABLE = "shared_h_keyframes_enable"
private const val SHARED_H_KEYFRAMES_USE_FIRST = "shared_h_keyframes_use_first"
private const val WHEN_COUNTDOWN_REMIND = "when_countdown_remind"

@Composable
fun HKeyframesRouteScreen(
    onOpenVideo: (String) -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel()
    val items by viewModel.loadAllHKeyframes().collectAsStateWithLifecycle(initialValue = emptyList())
    val shareRegex = remember { Regex(">>>(.+)<<<") }
    var sharedHKeyframeEntity by remember { mutableStateOf<HKeyframeEntity?>(null) }

    LaunchedEffect(Unit) {
        val text = textFromClipboard
        val entity = withContext(Dispatchers.Default) {
            val matchResult = text?.let(shareRegex::find) ?: return@withContext null
            val (toBase64) = matchResult.destructured
            val toJson = toBase64.decodeFromStringByBase64()
            Json.decodeFromString<HKeyframeEntity>(toJson)
        }
        if (entity != null) {
            sharedHKeyframeEntity = entity
        } else {
            showShortToast(R.string.h_keyframes_shared_by_other_not_detected)
        }
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

@Composable
fun SharedHKeyframesRouteScreen(
    onOpenVideo: (String) -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel()
    val items by viewModel.loadAllSharedHKeyframes().collectAsStateWithLifecycle(initialValue = emptyList())

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
