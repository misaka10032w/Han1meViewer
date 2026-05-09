package com.yenaly.han1meviewer.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.home.WatchHistoryScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.han1meviewer.util.checkBadGuy
import com.yenaly.han1meviewer.util.openVideo

class WatchHistoryFragment : Fragment() {

    private val viewModel by activityViewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkBadGuy(requireContext(), R.raw.akarin)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HanimeTheme {
                    WatchHistoryScreen(
                        historiesFlow = viewModel.loadAllWatchHistories(),
                        onBack = { findNavController().navigateUp() },
                        onOpenVideo = { openVideo(it.videoCode) },
                        onDeleteHistory = viewModel::deleteWatchHistory,
                        onDeleteAllHistories = viewModel::deleteAllWatchHistories,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.hideToolbar()
    }

    override fun onStop() {
        super.onStop()
        (activity as? ToolbarHost)?.showToolbar()
    }
}
