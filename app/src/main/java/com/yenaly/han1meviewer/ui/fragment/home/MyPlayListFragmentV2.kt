package com.yenaly.han1meviewer.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.ui.fragment.LoginNeededFragmentMixin
import com.yenaly.han1meviewer.ui.fragment.home.myplaylist.MyPlayListScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import com.yenaly.yenaly_libs.utils.copyTextToClipboard
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.launch

class MyPlayListFragmentV2 : Fragment(), LoginNeededFragmentMixin {
    private val vm: MyPlayListViewModelV2 by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkLogin()
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            duration = 500L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            duration = 500L
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HanimeTheme {
                    MyPlayListScreen(
                        viewModel = vm,
                        navigateBack = {
                            lifecycleScope.launch { findNavController().navigateUp() }
                        },
                        onClickItem = { videoCode ->
                            navigateToVideo(videoCode)
                        },
                        onLongClickItem = { videoCode, title ->
                            copyTextToClipboard(getHanimeShareText(title, videoCode))
                            showShortToast(R.string.copy_to_clipboard)
                        }
                    )
                }

            }
        }
    }

    private fun navigateToVideo(videoCode: String) {
        val bundle = bundleOf(VIDEO_CODE to videoCode)
        findNavController().navigate(R.id.videoFragment, bundle)
    }
}