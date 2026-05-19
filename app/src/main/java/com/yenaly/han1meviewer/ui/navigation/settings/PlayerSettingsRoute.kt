package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.screen.settings.PlayerSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.PlayerSettingsUiState
import com.yenaly.han1meviewer.ui.view.video.HJzvdStd
import com.yenaly.han1meviewer.ui.view.video.HMediaKernel

private const val PLAYER_SWITCH_PLAYER_KERNEL = "switch_player_kernel"
private const val PLAYER_SHOW_BOTTOM_PROGRESS = "show_bottom_progress"
private const val PLAYER_SPEED = "player_speed"
private const val PLAYER_SLIDE_SENSITIVITY = "slide_sensitivity"
private const val PLAYER_LONG_PRESS_SPEED_TIMES = "long_press_speed_times"

@Composable
fun PlayerSettingsRouteScreen(
    onNavigateToMpvSettings: () -> Unit,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val uiState = remember(refreshKey, context) { buildPlayerSettingsUiState(context) }

    PlayerSettingsScreen(
        state = uiState,
        kernelOptions = HMediaKernel.Type.entries.map { it.name to it.name },
        speedOptions = HJzvdStd.speedStringArray.zip(HJzvdStd.speedArray.map { it.toString() }),
        longPressSpeedOptions = listOf(
            stringResource(R.string.d_speed_times, 1f) to "1",
            stringResource(R.string.d_speed_times, 1.5f) to "1.5",
            stringResource(R.string.d_speed_times, 2f) to "2",
            "${
                stringResource(
                    R.string.d_speed_times,
                    2.5f
                )
            } (${stringResource(R.string.default_)})" to "2.5",
            stringResource(R.string.d_speed_times, 2.8f) to "2.8",
            stringResource(R.string.d_speed_times, 3f) to "3",
            stringResource(R.string.d_speed_times, 3.2f) to "3.2",
            stringResource(R.string.d_speed_times, 3.5f) to "3.5",
            stringResource(R.string.d_speed_times, 3.8f) to "3.8",
            stringResource(R.string.d_speed_times, 4f) to "4",
        ),
        onKernelChange = {
            saveString(PLAYER_SWITCH_PLAYER_KERNEL, it)
            refreshKey++
        },
        onShowBottomProgressChange = {
            saveBoolean(PLAYER_SHOW_BOTTOM_PROGRESS, it)
            refreshKey++
        },
        onPlayerSpeedChange = {
            saveString(PLAYER_SPEED, it)
            refreshKey++
        },
        onLongPressSpeedChange = {
            saveString(PLAYER_LONG_PRESS_SPEED_TIMES, it)
            refreshKey++
        },
        onSlideSensitivityChange = {
            Preferences.preferenceSp.edit { putInt(PLAYER_SLIDE_SENSITIVITY, it) }
            refreshKey++
        },
        onOpenMpvSettings = onNavigateToMpvSettings,
    )
}

private fun buildPlayerSettingsUiState(context: Context): PlayerSettingsUiState {
    val kernel = Preferences.switchPlayerKernel
    val isMpvPlayer = kernel == HMediaKernel.Type.MpvPlayer.name
    val currentSpeed = Preferences.playerSpeed
    val currentLongPressSpeed = Preferences.longPressSpeedTime
    val speedDisplay = HJzvdStd.speedStringArray.getOrElse(
        HJzvdStd.speedArray.indexOfFirst { it == currentSpeed }.takeIf { it >= 0 }
            ?: HJzvdStd.DEF_SPEED_INDEX
    ) { HJzvdStd.speedStringArray[HJzvdStd.DEF_SPEED_INDEX] }
    val longPressDisplay = context.getString(R.string.d_speed_times, currentLongPressSpeed)
    return PlayerSettingsUiState(
        kernel = kernel,
        kernelDisplay = kernel,
        mpvSettingsEnabled = isMpvPlayer,
        mpvSettingsSummary = if (isMpvPlayer) {
            context.getString(R.string.mpv_advanced_settings_summary)
        } else {
            context.getString(R.string.mpv_settings_disabled_summary)
        },
        showBottomProgress = Preferences.showBottomProgress,
        playerSpeed = currentSpeed.toString(),
        playerSpeedLabel = speedDisplay,
        longPressSpeedTimes = currentLongPressSpeed.toString(),
        longPressSpeedTimesLabel = longPressDisplay,
        slideSensitivity = Preferences.slideSensitivity,
        slideSensitivitySummary = toPrettySensitivityString(context, Preferences.slideSensitivity),
    )
}
