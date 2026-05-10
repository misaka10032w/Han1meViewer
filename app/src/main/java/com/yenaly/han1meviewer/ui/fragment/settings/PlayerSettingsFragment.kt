package com.yenaly.han1meviewer.ui.fragment.settings

import android.os.Bundle
import androidx.annotation.IntRange
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.activity.SettingsRouter
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.settings.PlayerSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.PlayerSettingsUiState
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.view.video.HJzvdStd
import com.yenaly.han1meviewer.ui.view.video.HMediaKernel
import com.yenaly.yenaly_libs.utils.toStringArray

class PlayerSettingsFragment : androidx.fragment.app.Fragment() {
    companion object {
        const val SWITCH_PLAYER_KERNEL = "switch_player_kernel"
        const val MPV_PLAYER_SETTINGS = "mpv_advanced_settings"
        const val SHOW_BOTTOM_PROGRESS = "show_bottom_progress"
        const val PLAYER_SPEED = "player_speed"
        const val SLIDE_SENSITIVITY = "slide_sensitivity"
        const val LONG_PRESS_SPEED_TIMES = "long_press_speed_times"
    }

    private var uiState by mutableStateOf<PlayerSettingsUiState?>(null)

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.player_settings),
            canNavigateBack = true,
        )
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        uiState = buildUiState()
        setContent {
            HanimeTheme {
                uiState?.let { state ->
                    PlayerSettingsScreen(
                        state = state,
                        kernelOptions = HMediaKernel.Type.entries.map { it.name to it.name },
                        speedOptions = HJzvdStd.speedStringArray.zip(HJzvdStd.speedArray.map { it.toString() }),
                        longPressSpeedOptions = listOf(
                            getString(R.string.d_speed_times, 1f) to "1",
                            getString(R.string.d_speed_times, 1.5f) to "1.5",
                            getString(R.string.d_speed_times, 2f) to "2",
                            "${getString(R.string.d_speed_times, 2.5f)} (${getString(R.string.default_)})" to "2.5",
                            getString(R.string.d_speed_times, 2.8f) to "2.8",
                            getString(R.string.d_speed_times, 3f) to "3",
                            getString(R.string.d_speed_times, 3.2f) to "3.2",
                            getString(R.string.d_speed_times, 3.5f) to "3.5",
                            getString(R.string.d_speed_times, 3.8f) to "3.8",
                            getString(R.string.d_speed_times, 4f) to "4",
                        ),
                        onKernelChange = ::onKernelChanged,
                        onShowBottomProgressChange = { saveBoolean(SHOW_BOTTOM_PROGRESS, it) },
                        onPlayerSpeedChange = { saveString(PLAYER_SPEED, it) },
                        onLongPressSpeedChange = { saveString(LONG_PRESS_SPEED_TIMES, it) },
                        onSlideSensitivityChange = { saveInt(SLIDE_SENSITIVITY, it) },
                        onOpenMpvSettings = {
                            SettingsRouter.with(this@PlayerSettingsFragment)
                                .navigateWithinSettings(R.id.mpvPlayerSettings)
                        },
                    )
                }
            }
        }
    }

    private fun buildUiState(): PlayerSettingsUiState {
        val kernel = Preferences.switchPlayerKernel
        val isMpvPlayer = kernel == HMediaKernel.Type.MpvPlayer.name
        val currentSpeed = Preferences.playerSpeed
        val currentLongPressSpeed = Preferences.longPressSpeedTime
        val speedDisplay = HJzvdStd.speedStringArray.getOrElse(
            HJzvdStd.speedArray.indexOfFirst { it == currentSpeed }.takeIf { it >= 0 } ?: HJzvdStd.DEF_SPEED_INDEX
        ) { HJzvdStd.speedStringArray[HJzvdStd.DEF_SPEED_INDEX] }
        return PlayerSettingsUiState(
            kernel = kernel,
            kernelDisplay = kernel,
            mpvSettingsEnabled = isMpvPlayer,
            mpvSettingsSummary = if (isMpvPlayer) {
                getString(R.string.mpv_advanced_settings_summary)
            } else {
                getString(R.string.mpv_settings_disabled_summary)
            },
            showBottomProgress = Preferences.showBottomProgress,
            playerSpeed = speedDisplay,
            longPressSpeedTimes = getString(R.string.d_speed_times, currentLongPressSpeed),
            slideSensitivity = Preferences.slideSensitivity,
            slideSensitivitySummary = toPrettySensitivityString(Preferences.slideSensitivity),
        )
    }

    private fun saveString(key: String, value: String) {
        Preferences.preferenceSp.edit { putString(key, value) }
        uiState = buildUiState()
    }

    private fun saveBoolean(key: String, value: Boolean) {
        Preferences.preferenceSp.edit { putBoolean(key, value) }
        uiState = buildUiState()
    }

    private fun saveInt(key: String, value: Int) {
        Preferences.preferenceSp.edit { putInt(key, value) }
        uiState = buildUiState()
    }

    private fun onKernelChanged(value: String) {
        saveString(SWITCH_PLAYER_KERNEL, value)
    }

    private fun toPrettySensitivityString(@IntRange(from = 1, to = 9) value: Int): String {
        val pretty = when (value) {
            1, 2 -> getString(R.string.high)
            3, 4 -> getString(R.string.moderately_high)
            5 -> getString(R.string.moderate)
            6 -> getString(R.string.slightly_low)
            7 -> getString(R.string.low)
            8 -> getString(R.string.very_low)
            9 -> getString(R.string.extremely_low)
            else -> throw IllegalStateException("Invalid sensitivity value: $value")
        }
        return getString(R.string.current_slide_sensitivity, pretty)
    }
}
