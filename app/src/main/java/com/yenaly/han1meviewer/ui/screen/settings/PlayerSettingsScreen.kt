package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn

data class PlayerSettingsUiState(
    val kernel: String,
    val kernelDisplay: String,
    val mpvSettingsEnabled: Boolean,
    val mpvSettingsSummary: String,
    val showBottomProgress: Boolean,
    val playerSpeed: String,
    val longPressSpeedTimes: String,
    val slideSensitivity: Int,
    val slideSensitivitySummary: String,
)

private enum class PlayerChoiceDialog {
    Kernel,
    Speed,
    LongPressSpeed,
}

@Composable
fun PlayerSettingsScreen(
    state: PlayerSettingsUiState,
    kernelOptions: List<Pair<String, String>>,
    speedOptions: List<Pair<String, String>>,
    longPressSpeedOptions: List<Pair<String, String>>,
    onKernelChange: (String) -> Unit,
    onShowBottomProgressChange: (Boolean) -> Unit,
    onPlayerSpeedChange: (String) -> Unit,
    onLongPressSpeedChange: (String) -> Unit,
    onSlideSensitivityChange: (Int) -> Unit,
    onOpenMpvSettings: () -> Unit,
) {
    var activeDialog by rememberSaveable { mutableStateOf<PlayerChoiceDialog?>(null) }

    PlayerChoiceDialogContent(
        visible = activeDialog == PlayerChoiceDialog.Kernel,
        title = stringResource(R.string.switch_player_kernel),
        options = kernelOptions,
        selectedValue = state.kernel,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onKernelChange(it)
        },
    )

    PlayerChoiceDialogContent(
        visible = activeDialog == PlayerChoiceDialog.Speed,
        title = stringResource(R.string.default_playback_speed),
        options = speedOptions,
        selectedValue = state.playerSpeed,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onPlayerSpeedChange(it)
        },
    )

    PlayerChoiceDialogContent(
        visible = activeDialog == PlayerChoiceDialog.LongPressSpeed,
        title = stringResource(R.string.long_press_speed_multiplier),
        options = longPressSpeedOptions,
        selectedValue = state.longPressSpeedTimes,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onLongPressSpeedChange(it)
        },
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            SettingNavigationItem(
                title = stringResource(R.string.switch_player_kernel),
                valueText = state.kernelDisplay,
                iconRes = R.drawable.baseline_atomic_24,
                onClick = { activeDialog = PlayerChoiceDialog.Kernel },
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.mpv_advanced_settings),
                summary = state.mpvSettingsSummary,
                iconRes = R.drawable.baseline_player_24,
                onClick = onOpenMpvSettings,
                enabled = state.mpvSettingsEnabled,
                valueText = null,
            )
        }

        item {
            SettingSwitchItem(
                title = stringResource(R.string.show_bottom_progress),
                checked = state.showBottomProgress,
                iconRes = R.drawable.baseline_seek_24,
                onCheckedChange = onShowBottomProgressChange,
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.default_playback_speed),
                valueText = state.playerSpeed,
                iconRes = R.drawable.baseline_speed2_24,
                onClick = { activeDialog = PlayerChoiceDialog.Speed },
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.long_press_speed_multiplier),
                valueText = state.longPressSpeedTimes,
                iconRes = R.drawable.baseline_touch_24,
                onClick = { activeDialog = PlayerChoiceDialog.LongPressSpeed },
            )
        }

        item {
            SettingSliderItem(
                title = stringResource(R.string.slide_sensitivity),
                summary = state.slideSensitivitySummary,
                value = state.slideSensitivity,
                valueRange = 1..9,
                iconRes = R.drawable.baseline_speed_24,
                onValueChange = onSlideSensitivityChange,
            )
        }
    }
}

@Composable
private fun PlayerChoiceDialogContent(
    visible: Boolean,
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Preview(showBackground = true, widthDp = 420, heightDp = 760)
@Composable
private fun PlayerSettingsScreenPreview() {
    ComponentPreview {
        PlayerSettingsScreen(
            state = PlayerSettingsUiState(
                kernel = "ExoPlayer",
                kernelDisplay = "ExoPlayer",
                mpvSettingsEnabled = false,
                mpvSettingsSummary = stringResource(R.string.mpv_settings_disabled_summary),
                showBottomProgress = true,
                playerSpeed = "1.0x",
                longPressSpeedTimes = "2.5倍",
                slideSensitivity = 5,
                slideSensitivitySummary = stringResource(R.string.current_slide_sensitivity, stringResource(R.string.moderate)),
            ),
            kernelOptions = listOf("MediaPlayer" to "MediaPlayer", "ExoPlayer" to "ExoPlayer", "MpvPlayer" to "MpvPlayer"),
            speedOptions = listOf("1.0x" to "1.0", "1.25x" to "1.25", "1.5x" to "1.5"),
            longPressSpeedOptions = listOf("2.0倍" to "2", "2.5倍" to "2.5", "3.0倍" to "3"),
            onKernelChange = {},
            onShowBottomProgressChange = {},
            onPlayerSpeedChange = {},
            onLongPressSpeedChange = {},
            onSlideSensitivityChange = {},
            onOpenMpvSettings = {},
        )
    }
}
