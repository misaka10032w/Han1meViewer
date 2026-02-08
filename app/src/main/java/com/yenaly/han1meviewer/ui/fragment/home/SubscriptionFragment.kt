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
import com.yenaly.han1meviewer.ADVANCED_SEARCH_MAP
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeSearchShareText
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.ui.fragment.LoginNeededFragmentMixin
import com.yenaly.han1meviewer.ui.fragment.home.subscription.SubscriptionApp
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.MySubscriptionsViewModel
import com.yenaly.han1meviewer.util.openVideo
import com.yenaly.yenaly_libs.utils.copyTextToClipboard
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.launch

class SubscriptionFragment : Fragment(), LoginNeededFragmentMixin {
    private val vm: MySubscriptionsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkLogin()
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HanimeTheme {
                    SubscriptionApp(
                        viewModel = vm,
                        navigateBack = {
                            lifecycleScope.launch { findNavController().navigateUp() }
                        },
                        onClickArtist = { artistName ->
                            navigateToSearch(artistName)
                        },
                        onLongClickArtist = { artistName ->
                            copyTextToClipboard(getHanimeSearchShareText(artistName))
                            showShortToast(R.string.copy_to_clipboard)
                        },
                        onClickVideosItem = { videoCode ->
                            openVideo(videoCode)
                        },
                        onLongClickVideosItem = { videoCode, title ->
                            copyTextToClipboard(getHanimeShareText(title, videoCode))
                            showShortToast(R.string.copy_to_clipboard)
                        }
                    )
                }

            }
        }
    }

    private fun navigateToSearch(artistName: String) {
        val bundle = bundleOf(ADVANCED_SEARCH_MAP to artistName)
        findNavController().navigate(R.id.searchFragment, bundle)
    }
}



