package com.yenaly.han1meviewer.ui.fragment.settings

import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.settings.SharedHKeyframesScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.SettingsViewModel

class SharedHKeyframesFragment : androidx.fragment.app.Fragment() {

    private val viewModel by activityViewModels<SettingsViewModel>()

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
        setContent {
            HanimeTheme {
                val items = viewModel.loadAllSharedHKeyframes().collectAsStateWithLifecycle(initialValue = emptyList()).value
                SharedHKeyframesScreen(
                    items = items,
                    onOpenVideo = { videoCode ->
                        (activity as? com.yenaly.han1meviewer.ui.activity.MainActivity)?.showVideoDetailFragment(videoCode)
                    },
                )
            }
        }
    }
}
