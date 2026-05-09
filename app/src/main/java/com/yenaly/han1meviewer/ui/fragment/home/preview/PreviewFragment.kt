package com.yenaly.han1meviewer.ui.fragment.home.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.yenaly.han1meviewer.DATE_CODE
import com.yenaly.han1meviewer.PREVIEW_COMMENT_PREFIX
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.home.preview.PreviewScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.han1meviewer.ui.viewmodel.PreviewViewModel
import com.yenaly.han1meviewer.util.openVideo

class PreviewFragment : Fragment() {

    private val viewModel by viewModels<PreviewViewModel>()
    private val commentViewModel: CommentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val previewState = viewModel.previewFlow.collectAsStateWithLifecycle().value
                val commentCount = PreviewCommentPrefetcher.here(commentViewModel)
                    .commentFlow
                    .collectAsStateWithLifecycle()
                    .value
                    .size

                HanimeTheme {
                    PreviewScreen(
                        previewState = previewState,
                        commentCount = commentCount,
                        onBack = { findNavController().navigateUp() },
                        onLoadDate = { code ->
                            viewModel.getHanimePreview(code)
                            PreviewCommentPrefetcher.here(commentViewModel).fetch(PREVIEW_COMMENT_PREFIX, code)
                        },
                        onOpenComment = { dateLabel, dateCode ->
                            findNavController().navigate(
                                com.yenaly.han1meviewer.R.id.action_nv_preview_to_nv_preview_comment,
                                Bundle().apply {
                                    putString("date", dateLabel)
                                    putString(DATE_CODE, dateCode)
                                }
                            )
                        },
                        onOpenVideo = { code -> openVideo(code) },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        PreviewCommentPrefetcher.here(commentViewModel)
            .tag(PreviewCommentPrefetcher.Scope.PREVIEW_ACTIVITY)
        (activity as? ToolbarHost)?.hideToolbar()
    }

    override fun onStop() {
        super.onStop()
        (activity as? ToolbarHost)?.showToolbar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PreviewCommentPrefetcher.bye(PreviewCommentPrefetcher.Scope.PREVIEW_ACTIVITY)
    }

    override fun onDestroy() {
        super.onDestroy()
        commentViewModel.clearCommentData()
    }
}
