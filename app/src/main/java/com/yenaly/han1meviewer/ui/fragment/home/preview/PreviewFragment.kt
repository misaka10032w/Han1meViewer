package com.yenaly.han1meviewer.ui.fragment.home.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yenaly.han1meviewer.DATE_CODE
import com.yenaly.han1meviewer.PREVIEW_COMMENT_PREFIX
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.state.WebsiteState
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
                val context = LocalContext.current
                val imageLoader = SingletonImageLoader.get(context)
                val previewState = viewModel.previewFlow.collectAsStateWithLifecycle().value
                val commentCount = PreviewCommentPrefetcher.here(commentViewModel)
                    .commentFlow
                    .collectAsStateWithLifecycle()
                    .value
                    .size

                fun preloadImages(preview: HanimePreview?) {
                    if (preview == null) return
                    buildList {
                        preview.headerPicUrl?.let(::add)
                        addAll(preview.latestHanime.map { it.coverUrl })
                        addAll(preview.previewInfo.mapNotNull { it.coverUrl })
                    }.distinct().forEach { url ->
                        imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .data(url)
                                .crossfade(true)
                                .build()
                        )
                    }
                }

                LaunchedEffect(previewState) {
                    when (val state = previewState) {
                        is WebsiteState.Success -> {
                            preloadImages(state.info)
                        }
                        else -> Unit
                    }
                    val currentDateCode = arguments?.getString(DATE_CODE)
                    if (currentDateCode != null) {
                        val prevCode = shiftMonthCodeForPreview(currentDateCode, -1)
                        val nextCode = shiftMonthCodeForPreview(currentDateCode, 1)
                        viewModel.preloadPreview(prevCode)
                        viewModel.preloadPreview(nextCode)
                        (viewModel.getCachedPreview(prevCode) as? WebsiteState.Success)?.info?.let(::preloadImages)
                        (viewModel.getCachedPreview(nextCode) as? WebsiteState.Success)?.info?.let(::preloadImages)
                    }
                }

                HanimeTheme {
                    PreviewScreen(
                        previewState = previewState,
                        getCachedPreviewState = viewModel::getCachedPreview,
                        commentCount = commentCount,
                        onBack = { findNavController().navigateUp() },
                        onLoadDate = { code ->
                            viewModel.getHanimePreview(code)
                            viewModel.preloadPreview(shiftMonthCodeForPreview(code, -1))
                            viewModel.preloadPreview(shiftMonthCodeForPreview(code, 1))
                            PreviewCommentPrefetcher.here(commentViewModel).fetch(PREVIEW_COMMENT_PREFIX, code)
                        },
                        onOpenComment = { dateLabel, dateCode ->
                            findNavController().navigate(
                                R.id.action_nv_preview_to_nv_preview_comment,
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

private fun shiftMonthCodeForPreview(code: String, delta: Int): String {
    var year = code.substring(0, 4).toInt()
    var month = code.substring(4, 6).toInt() + delta
    while (month < 1) {
        month += 12
        year -= 1
    }
    while (month > 12) {
        month -= 12
        year += 1
    }
    return "%04d%02d".format(year, month)
}
