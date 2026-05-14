package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.ComponentPreview
import com.yenaly.han1meviewer.ui.component.SettingChoiceItem
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSliderItem
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem

data class MpvPlayerSettingsUiState(
    val profile: String,
    val profileDisplay: String,
    val enableGpuNextRenderer: Boolean,
    val interpolation: Boolean,
    val deband: Boolean,
    val framedrop: Boolean,
    val hwdec: String,
    val hwdecDisplay: String,
    val cacheSecs: Int,
    val cacheSecsSummary: String,
    val tlsVerify: Boolean,
    val networkTimeout: Int,
    val networkTimeoutSummary: String,
    val customParams: String,
)

enum class MpvChoiceDialog {
    Profile,
    Hwdec,
    CustomParams,
}

@Composable
fun MpvPlayerSettingsScreen(
    state: MpvPlayerSettingsUiState,
    profileOptions: List<Pair<String, String>>,
    hwdecOptions: List<Pair<String, String>>,
    activeDialog: MpvChoiceDialog?,
    onOpenProfileDialog: () -> Unit,
    onOpenHwdecDialog: () -> Unit,
    onOpenCustomParamsDialog: () -> Unit,
    onDismissDialog: () -> Unit,
    onProfileChange: (String) -> Unit,
    onEnableGpuNextRendererChange: (Boolean) -> Unit,
    onInterpolationChange: (Boolean) -> Unit,
    onDebandChange: (Boolean) -> Unit,
    onFramedropChange: (Boolean) -> Unit,
    onHwdecChange: (String) -> Unit,
    onCacheSecsChange: (Int) -> Unit,
    onTlsVerifyChange: (Boolean) -> Unit,
    onNetworkTimeoutChange: (Int) -> Unit,
    onCustomParamsChange: (String) -> Unit,
) {
    if (activeDialog == MpvChoiceDialog.Profile) {
        ChoiceDialog(
            title = stringResource(R.string.mpv_profile),
            options = profileOptions,
            selectedValue = state.profile,
            onDismiss = onDismissDialog,
            onSelect = { onDismissDialog(); onProfileChange(it) },
        )
    }

    if (activeDialog == MpvChoiceDialog.Hwdec) {
        ChoiceDialog(
            title = stringResource(R.string.mpv_hwdec),
            options = hwdecOptions,
            selectedValue = state.hwdec,
            onDismiss = onDismissDialog,
            onSelect = { onDismissDialog(); onHwdecChange(it) },
        )
    }

    if (activeDialog == MpvChoiceDialog.CustomParams) {
        CustomParamsDialog(
            value = state.customParams,
            onDismiss = onDismissDialog,
            onConfirm = { onDismissDialog(); onCustomParamsChange(it) },
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item { MpvGroupTitle(stringResource(R.string.player_settings_quality_performance)) }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.mpv_profile),
                valueText = state.profileDisplay,
                iconRes = R.drawable.baseline_render_24,
                onClick = onOpenProfileDialog,
            )
        }

        item {
            SettingSwitchItem(
                title = stringResource(R.string.enable_gpu_next),
                summary = stringResource(R.string.enable_gpu_next_summary),
                checked = state.enableGpuNextRenderer,
                iconRes = R.drawable.baseline_chip_24,
                onCheckedChange = onEnableGpuNextRendererChange,
            )
        }

        item {
            SettingSwitchItem(
                title = stringResource(R.string.mpv_interpolation),
                summary = stringResource(R.string.mpv_interpolation_summary),
                checked = state.interpolation,
                iconRes = R.drawable.baseline_frame_inter_24,
                onCheckedChange = onInterpolationChange,
            )
        }

        item {
            SettingSwitchItem(
                title = stringResource(R.string.mpv_deband),
                summary = stringResource(R.string.mpv_deband_summary),
                checked = state.deband,
                iconRes = R.drawable.baseline_deband_24,
                onCheckedChange = onDebandChange,
            )
        }

        item {
            SettingSwitchItem(
                title = stringResource(R.string.mpv_framedrop),
                summary = stringResource(R.string.mpv_framedrop_summary),
                checked = state.framedrop,
                iconRes = R.drawable.baseline_frame_jump_24,
                onCheckedChange = onFramedropChange,
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.mpv_hwdec),
                summary = state.hwdecDisplay,
                iconRes = R.drawable.baseline_decoder_24,
                onClick = onOpenHwdecDialog,
            )
        }

        item { MpvGroupTitle(stringResource(R.string.player_settings_network_cache)) }

        item {
            SettingSliderItem(
                title = stringResource(R.string.mpv_cache_secs),
                summary = state.cacheSecsSummary,
                value = state.cacheSecs,
                valueRange = 10..120,
                iconRes = R.drawable.baseline_cache_24,
                onValueChange = onCacheSecsChange,
            )
        }

        item {
            SettingSwitchItem(
                title = stringResource(R.string.mpv_tls_verify),
                summary = stringResource(R.string.mpv_tls_verify_summary),
                checked = state.tlsVerify,
                iconRes = R.drawable.baseline_cert_24,
                onCheckedChange = onTlsVerifyChange,
            )
        }

        item {
            SettingSliderItem(
                title = stringResource(R.string.mpv_network_timeout),
                summary = state.networkTimeoutSummary,
                value = state.networkTimeout,
                valueRange = 5..30,
                iconRes = R.drawable.baseline_overtime_24,
                onValueChange = onNetworkTimeoutChange,
            )
        }

        item { MpvGroupTitle(stringResource(R.string.advanced)) }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.custom_parameters),
                summary = state.customParams.ifBlank { stringResource(R.string.custom_parameters_summary) },
                iconRes = R.drawable.baseline_custom_24,
                onClick = onOpenCustomParamsDialog,
            )
        }
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { (label, value) ->
                    SettingChoiceItem(
                        title = label,
                        selected = selectedValue == value,
                        onClick = { onSelect(value) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun CustomParamsDialog(
    value: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val text = remember { mutableStateOf(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_parameters_title)) },
        text = {
            OutlinedTextField(
                value = text.value,
                onValueChange = { text.value = it },
                label = { Text(stringResource(R.string.custom_parameters_example)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.value) }) {
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
private fun MpvGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun MpvPlayerSettingsScreenPreview() {
    ComponentPreview {
        MpvPlayerSettingsScreen(
            state = MpvPlayerSettingsUiState(
                profile = "fast",
                profileDisplay = "效能優先 (fast)",
                enableGpuNextRenderer = false,
                interpolation = false,
                deband = true,
                framedrop = true,
                hwdec = "Auto",
                hwdecDisplay = "利用硬體加速提升播放效能 (Auto)",
                cacheSecs = 60,
                cacheSecsSummary = "調整快取大小以適應網路波動 (60 S)",
                tlsVerify = true,
                networkTimeout = 10,
                networkTimeoutSummary = "控制請求等待時間 (10 S)",
                customParams = "",
            ),
            profileOptions = listOf(
                "效能優先 (fast)" to "fast",
                "畫質優先 (gpu-hq)" to "gpu-hq",
            ),
            hwdecOptions = listOf(
                "自動選擇 (auto)" to "Auto",
                "硬體解碼 (HW: mediacodec-copy)" to "HW",
            ),
            activeDialog = null,
            onOpenProfileDialog = {},
            onOpenHwdecDialog = {},
            onOpenCustomParamsDialog = {},
            onDismissDialog = {},
            onProfileChange = {},
            onEnableGpuNextRendererChange = {},
            onInterpolationChange = {},
            onDebandChange = {},
            onFramedropChange = {},
            onHwdecChange = {},
            onCacheSecsChange = {},
            onTlsVerifyChange = {},
            onNetworkTimeoutChange = {},
            onCustomParamsChange = {},
        )
    }
}
