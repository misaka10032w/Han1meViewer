package com.yenaly.han1meviewer.ui.fragment.settings

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.settings.MpvChoiceDialog
import com.yenaly.han1meviewer.ui.screen.settings.MpvPlayerSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.MpvPlayerSettingsUiState
import com.yenaly.han1meviewer.ui.theme.HanimeTheme

class MpvPlayerSettings : androidx.fragment.app.Fragment() {
    companion object {
        const val MPV_PROFILE = "mpv_profile"
        const val MPV_INTERPOLATION = "mpv_interpolation"
        const val MPV_DEBAND = "mpv_deband"
        const val MPV_FRAMEDROP = "mpv_framedrop"
        const val MPV_HWDEC = "mpv_hwdecx"
        const val MPV_CACHE_SECS = "mpv_cache_secs"
        const val MPV_TLS_VERIFY = "mpv_tls_verify"
        const val MPV_NETWORK_TIMEOUT = "mpv_network_timeout"
        const val ENABLE_GPU_NEXT_RENDERER = "mpv_gpu_next_render"
        const val CUSTOM_PARAMS = "mpv_custom_parameters"
    }

    private var uiState by mutableStateOf<MpvPlayerSettingsUiState?>(null)

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.mpv_advanced_settings),
            canNavigateBack = true,
        )
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        uiState = buildUiState()
        var activeDialog by mutableStateOf<MpvChoiceDialog?>(null)
        setContent {
            HanimeTheme {
                uiState?.let { state ->
                    MpvPlayerSettingsScreen(
                        state = state,
                        profileOptions = listOf(
                            getString(R.string.profile_fast) to "fast",
                            getString(R.string.profile_gpu_hq) to "gpu-hq",
                        ),
                        hwdecOptions = listOf(
                            getString(R.string.decoding_auto) to "Auto",
                            getString(R.string.decoding_hw) to "HW",
                            getString(R.string.decoding_hw_plus) to "HW+",
                            getString(R.string.decoding_vulkan_copy) to "Vulkan",
                            getString(R.string.decoding_vulkan) to "Vulkan+",
                            getString(R.string.decoding_sw) to "SW",
                        ),
                        activeDialog = activeDialog,
                        onOpenProfileDialog = { activeDialog = MpvChoiceDialog.Profile },
                        onOpenHwdecDialog = { activeDialog = MpvChoiceDialog.Hwdec },
                        onOpenCustomParamsDialog = { activeDialog = MpvChoiceDialog.CustomParams },
                        onDismissDialog = { activeDialog = null },
                        onProfileChange = { saveString(MPV_PROFILE, it) },
                        onEnableGpuNextRendererChange = { saveBoolean(ENABLE_GPU_NEXT_RENDERER, it) },
                        onInterpolationChange = { saveBoolean(MPV_INTERPOLATION, it) },
                        onDebandChange = { saveBoolean(MPV_DEBAND, it) },
                        onFramedropChange = { saveBoolean(MPV_FRAMEDROP, it) },
                        onHwdecChange = { saveString(MPV_HWDEC, it) },
                        onCacheSecsChange = { saveInt(MPV_CACHE_SECS, it) },
                        onTlsVerifyChange = { saveBoolean(MPV_TLS_VERIFY, it) },
                        onNetworkTimeoutChange = { saveInt(MPV_NETWORK_TIMEOUT, it) },
                        onCustomParamsChange = { saveString(CUSTOM_PARAMS, it) },
                    )
                }
            }
        }
    }

    private fun buildUiState(): MpvPlayerSettingsUiState {
        val profile = Preferences.mpvProfile
        val hwdec = Preferences.mpvHwdec
        return MpvPlayerSettingsUiState(
            profile = profile,
            profileDisplay = when (profile) {
                "fast" -> getString(R.string.profile_fast)
                "gpu-hq" -> getString(R.string.profile_gpu_hq)
                else -> profile
            },
            enableGpuNextRenderer = Preferences.enableGPUNextRenderer,
            interpolation = Preferences.mpvInterpolation,
            deband = Preferences.mpvDeband,
            framedrop = Preferences.mpvFramedrop,
            hwdec = hwdec,
            hwdecDisplay = "${getString(R.string.mpv_hwdec_summary)} ($hwdec)",
            cacheSecs = Preferences.mpvCacheSecs,
            cacheSecsSummary = "${getString(R.string.mpv_cache_secs_summary)} (${Preferences.mpvCacheSecs} S)",
            tlsVerify = Preferences.mpvTlsVerify,
            networkTimeout = Preferences.mpvNetworkTimeout,
            networkTimeoutSummary = "${getString(R.string.mpv_network_timeout_summary)} (${Preferences.mpvNetworkTimeout} S)",
            customParams = Preferences.customMpvParams,
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
}
