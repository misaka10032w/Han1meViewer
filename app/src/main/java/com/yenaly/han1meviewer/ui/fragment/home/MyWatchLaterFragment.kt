package com.yenaly.han1meviewer.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.ui.fragment.LoginNeededFragmentMixin
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.home.MyWatchLaterScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.MyListViewModel
import com.yenaly.han1meviewer.util.openVideo

class MyWatchLaterFragment : Fragment(), LoginNeededFragmentMixin {

    private val viewModel by viewModels<MyListViewModel>()

    private var page: Int
        set(value) {
            viewModel.watchLater.watchLaterPage = value
        }
        get() = viewModel.watchLater.watchLaterPage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkLogin()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HanimeTheme {
                    MyWatchLaterScreen(
                        watchLaterFlow = viewModel.watchLater.watchLaterFlow,
                        watchLaterStateFlow = viewModel.watchLater.watchLaterStateFlow,
                        deleteStateFlow = viewModel.watchLater.deleteMyWatchLaterFlow,
                        loadedPageCountFlow = viewModel.watchLater.loadedPageCount,
                        isLoadingMoreFlow = viewModel.watchLater.isLoadingMore,
                        onBack = { findNavController().navigateUp() },
                        onOpenVideo = { openVideo(it.videoCode) },
                        onDeleteWatchLater = ::deleteWatchLater,
                        onRefresh = ::refreshWatchLater,
                        onLoadMore = ::loadMoreWatchLater,
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

    private fun loadMoreWatchLater() {
        viewModel.watchLater.getMyWatchLaterItems(page)
        page++
    }

    private fun refreshWatchLater() {
        page = 1
        viewModel.watchLater.clearMyListItems()
        loadMoreWatchLater()
    }

    private fun deleteWatchLater(item: HanimeInfo) {
        val position = viewModel.watchLater.watchLaterFlow.value.indexOfFirst { it.videoCode == item.videoCode }
        if (position >= 0) {
            viewModel.watchLater.deleteMyWatchLater(item.videoCode, position)
        }
    }
}
