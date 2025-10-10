package com.yenaly.han1meviewer.ui.fragment.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.view.pref.MaterialDialogPreference
import com.yenaly.yenaly_libs.base.settings.YenalySettingsFragment

class MpvPlayerSettings: YenalySettingsFragment(R.xml.setting_mpv_player) {
    companion object {
        const val MPV_PROFILE = "mpv_profile"
        const val MPV_INTERPOLATION = "mpv_interpolation"
        const val MPV_DEBAND = "mpv_deband"
        const val MPV_FRAMEDROP = "mpv_framedrop"
        const val MPV_HWDEC = "mpv_hwdec"
        const val MPV_CACHE_SECS = "mpv_cache_secs"
        const val MPV_TLS_VERIFY = "mpv_tls_verify"
        const val MPV_NETWORK_TIMEOUT = "mpv_network_timeout"
        const val ENABLE_GPU_NEXT_RENDERER = "mpv_gpu_next_render"
        const val CUSTOM_PARAMS = "mpv_custom_parameters"
    }
    private val mpvProfile by safePreference<MaterialDialogPreference>(MPV_PROFILE)
    private val mpvInterpolation by safePreference<SwitchPreferenceCompat>(MPV_INTERPOLATION)
    private val mpvDeband by safePreference<SwitchPreferenceCompat>(MPV_DEBAND)
    private val mpvFramedrop by safePreference<SwitchPreferenceCompat>(MPV_FRAMEDROP)
    private val mpvHwdec by safePreference<SwitchPreferenceCompat>(MPV_HWDEC)
    private val mpvCacheSecs by safePreference<SeekBarPreference>(MPV_CACHE_SECS)
    private val mpvTlsVerify by safePreference<SwitchPreferenceCompat>(MPV_TLS_VERIFY)
    private val mpvNetworkTimeout by safePreference<SeekBarPreference>(MPV_NETWORK_TIMEOUT)
    private val customMpvParams by safePreference<EditTextPreference>(CUSTOM_PARAMS)

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.mpv_advanced_settings),
            canNavigateBack = true
        )
    }

    override fun onPreferencesCreated(savedInstanceState: Bundle?) {
        mpvProfile.apply {
            entries = arrayOf(
                getString(R.string.profile_fast),
                getString(R.string.profile_gpu_hq)
            )
            entryValues = arrayOf("fast", "gpu-hq")
            if (value == null) setValueIndex(0)
        }

        mpvInterpolation.apply {
            summary = getString(R.string.mpv_interpolation_summary)
        }

        mpvDeband.apply {
            summary = getString(R.string.mpv_deband_summary)
        }

        mpvFramedrop.apply {
            summary = getString(R.string.mpv_framedrop_summary)
        }

        mpvHwdec.apply {
            summary = getString(R.string.mpv_hwdec_summary)
        }

        mpvCacheSecs.apply {
            summary = "${getString(R.string.mpv_cache_secs_summary)} (${Preferences.mpvCacheSecs} S)"
            setOnPreferenceChangeListener { _, newValue ->
                summary = "${getString(R.string.mpv_cache_secs_summary)} ($newValue S)"
                return@setOnPreferenceChangeListener true
            }
        }

        mpvTlsVerify.apply {
            summary = getString(R.string.mpv_tls_verify_summary)
        }

        mpvNetworkTimeout.apply {
            summary = "${getString(R.string.mpv_network_timeout_summary)} (${Preferences.mpvNetworkTimeout} S)"
            setOnPreferenceChangeListener { _, newValue ->
                summary = "${getString(R.string.mpv_network_timeout_summary)} ($newValue S)"
                return@setOnPreferenceChangeListener true
            }
        }
        customMpvParams.apply {

        }
    }
}