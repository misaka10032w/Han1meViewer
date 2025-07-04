package com.yenaly.han1meviewer.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.yenaly.han1meviewer.ADVANCED_SEARCH_MAP
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
import com.yenaly.han1meviewer.ui.fragment.LoginNeededFragmentMixin
import com.yenaly.han1meviewer.ui.fragment.home.subscription.SubscriptionApp
import com.yenaly.han1meviewer.ui.fragment.home.subscription.SubscriptionAppPreviewBody
import com.yenaly.han1meviewer.ui.viewmodel.MySubscriptionsViewModel
import kotlinx.coroutines.launch

class SubscriptionFragment : Fragment(), LoginNeededFragmentMixin {
    private val vm: MySubscriptionsViewModel by viewModels()
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
                SubscriptionApp(
                    viewModel = vm,
                    navigateBack = {
                        lifecycleScope.launch { findNavController().navigateUp() }
                    },
                    onClickArtist = { artistName ->
                        navigateToSearch(artistName)
                    },
                    onClickVideosItem = { videoCode ->
                        navigateToVideo(videoCode)
                    }
                )
            }
        }
    }

    private fun navigateToSearch(artistName: String) {
        val bundle = bundleOf(ADVANCED_SEARCH_MAP to artistName)
        findNavController().navigate(R.id.searchFragment, bundle)
    }

    private fun navigateToVideo(videoCode: String) {
        val bundle = bundleOf(VIDEO_CODE to videoCode)
        findNavController().navigate(R.id.videoFragment, bundle)
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun SubscriptionAppPreview() {
    val fakeArtists = listOf(
        SubscriptionItem("初音未来", "null"),
        SubscriptionItem("绫波丽", "null"),
        SubscriptionItem("阿库娅", "null"),
        SubscriptionItem("初音未来", "null"),
        SubscriptionItem("绫波丽", "null"),
        SubscriptionItem("阿库娅", "null"),
        SubscriptionItem("初音未来", "null"),
        SubscriptionItem("绫波丽", "null"),
        SubscriptionItem("阿库娅", "null")
    )
    val fakeVideos = listOf(
        SubscriptionVideosItem(
            title = "小恶魔的补习计划",
            coverUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
            videoCode = "101573",
            duration = "04:34",
            views = "44.9万次",
            reviews = "100%"
        ),
        SubscriptionVideosItem(
            title = "姐姐的秘密训练",
            coverUrl = "https://vdownload.hembed.com/image/thumbnail/101574l.jpg",
            videoCode = "101574",
            duration = "23:15",
            views = "22.1万次",
            reviews = "95%"
        ),
        SubscriptionVideosItem(
            title = "放学后的约定",
            coverUrl = "https://vdownload.hembed.com/image/thumbnail/101575l.jpg",
            videoCode = "101575",
            duration = "18:02",
            views = "58.3万次",
            reviews = "97%"
        ),
        SubscriptionVideosItem(
            title = "班长的福利日",
            coverUrl = "https://vdownload.hembed.com/image/thumbnail/101576l.jpg",
            videoCode = "101576",
            duration = "12:47",
            views = "30.0万次",
            reviews = "92%"
        ),
        SubscriptionVideosItem(
            title = "图书馆的秘密角落",
            coverUrl = "https://vdownload.hembed.com/image/thumbnail/101577l.jpg",
            videoCode = "101577",
            duration = "15:20",
            views = "61.7万次",
            reviews = "99%"
        )
    )

    SubscriptionAppPreviewBody(
        artists = fakeArtists,
        onClickArtist = {},
        videos = fakeVideos,
        navigateBack = {}
    )
}



