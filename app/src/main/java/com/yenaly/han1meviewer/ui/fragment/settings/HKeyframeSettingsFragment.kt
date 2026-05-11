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
import com.yenaly.han1meviewer.ui.screen.settings.HKeyframeSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.HKeyframeSettingsUiState
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.view.video.HJzvdStd

class HKeyframeSettingsFragment : androidx.fragment.app.Fragment() {

    companion object {
        const val H_KEYFRAMES_ENABLE = "h_keyframes_enable"
        const val H_KEYFRAME_MANAGE = "h_keyframe_manage"
        const val SHOW_COMMENT_WHEN_COUNTDOWN = "show_comment_when_countdown"
        const val SHARED_H_KEYFRAMES_ENABLE = "shared_h_keyframes_enable"
        const val SHARED_H_KEYFRAMES_USE_FIRST = "shared_h_keyframes_use_first"
        const val SHARED_H_KEYFRAME_MANAGE = "shared_h_keyframe_manage"
        const val WHEN_COUNTDOWN_REMIND = "when_countdown_remind"
    }

    private var uiState by mutableStateOf<HKeyframeSettingsUiState?>(null)

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.h_keyframe_settings),
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
                    HKeyframeSettingsScreen(
                        state = state,
                        onHKeyframesEnableChange = {
                            saveBoolean(H_KEYFRAMES_ENABLE, it)
                        },
                        onOpenHKeyframeManage = {
                            SettingsRouter.with(this@HKeyframeSettingsFragment)
                                .navigateWithinSettings(R.id.hKeyframesFragment)
                        },
                        onSharedHKeyframesEnableChange = {
                            saveBoolean(SHARED_H_KEYFRAMES_ENABLE, it)
                        },
                        onSharedHKeyframesUseFirstChange = {
                            saveBoolean(SHARED_H_KEYFRAMES_USE_FIRST, it)
                        },
                        onOpenSharedHKeyframeManage = {
                            SettingsRouter.with(this@HKeyframeSettingsFragment)
                                .navigateWithinSettings(R.id.sharedHKeyframesFragment)
                        },
                        onShowCommentWhenCountdownChange = {
                            saveBoolean(SHOW_COMMENT_WHEN_COUNTDOWN, it)
                        },
                        onWhenCountdownRemindChange = {
                            saveInt(WHEN_COUNTDOWN_REMIND, it)
                        },
                    )
                }
            }
        }
    }

    private fun buildUiState(): HKeyframeSettingsUiState {
        return HKeyframeSettingsUiState(
            hKeyframesEnable = Preferences.hKeyframesEnable,
            hKeyframesSummary = keyframeTip(Preferences.hKeyframesEnable),
            sharedHKeyframesEnable = Preferences.sharedHKeyframesEnable,
            sharedHKeyframesUseFirst = Preferences.sharedHKeyframesUseFirst,
            showCommentWhenCountdown = Preferences.showCommentWhenCountdown,
            whenCountdownRemind = Preferences.whenCountdownRemind / 1000,
            whenCountdownRemindSummary = toPrettyCountdownRemindString(Preferences.whenCountdownRemind / 1000),
        )
    }

    private fun saveBoolean(key: String, value: Boolean) {
        Preferences.preferenceSp.edit { putBoolean(key, value) }
        uiState = buildUiState()
    }

    private fun saveInt(key: String, value: Int) {
        Preferences.preferenceSp.edit { putInt(key, value) }
        uiState = buildUiState()
    }

    private fun toPrettyCountdownRemindString(@IntRange(from = 5, to = 30) value: Int): String {
        return buildString {
            append(getString(R.string.will_remind_before_d_seconds, value))
            if (value == HJzvdStd.DEF_COUNTDOWN_SEC) append(" (${getString(R.string.default_)})")
        }
    }

    private fun keyframeTip(isChecked: Boolean) = if (isChecked) {
        getString(R.string.h_keyframes_enable_tip)
    } else {
        getString(R.string.h_keyframes_disable_tip)
    }
}
