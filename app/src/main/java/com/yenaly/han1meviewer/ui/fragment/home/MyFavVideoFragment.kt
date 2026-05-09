package com.yenaly.han1meviewer.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.ui.fragment.LoginNeededFragmentMixin
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.home.MyFavVideoScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.MyListViewModel
import com.yenaly.han1meviewer.util.openVideo

class MyFavVideoFragment : Fragment(), LoginNeededFragmentMixin {

    private val viewModel by viewModels<MyListViewModel>()

    private var page: Int
        set(value) {
            viewModel.fav.favVideoPage = value
        }
        get() = viewModel.fav.favVideoPage

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
                    MyFavVideoScreen(
                        favVideoFlow = viewModel.fav.favVideoFlow,
                        favVideoStateFlow = viewModel.fav.favVideoStateFlow,
                        deleteStateFlow = viewModel.fav.deleteMyFavVideoFlow,
                        loadedPageCountFlow = viewModel.fav.loadedPageCount,
                        isLoadingMoreFlow = viewModel.fav.isLoadingMore,
                        onBack = { findNavController().navigateUp() },
                        onOpenVideo = { openVideo(it.videoCode) },
                        onDeleteFavorite = ::deleteFavorite,
                        onRefresh = ::refreshFavorites,
                        onLoadMore = ::loadMoreFavorites,
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

    private fun loadMoreFavorites() {
        val userId = Preferences.savedUserId
        viewModel.fav.getMyFavVideoItems(userId, page)
        page++
    }

    private fun refreshFavorites() {
        page = 1
        viewModel.fav.clearMyListItems()
        loadMoreFavorites()
    }

    private fun deleteFavorite(item: HanimeInfo) {
        val position = viewModel.fav.favVideoFlow.value.indexOfFirst { it.videoCode == item.videoCode }
        if (position >= 0) {
            viewModel.fav.deleteMyFavVideo(item.videoCode, position)
        }
    }
}
