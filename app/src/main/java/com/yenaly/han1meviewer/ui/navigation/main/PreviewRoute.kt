package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yenaly.han1meviewer.PREVIEW_COMMENT_PREFIX
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.home.preview.PreviewScreen
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.han1meviewer.ui.viewmodel.PreviewViewModel

@Composable
fun PreviewRouteScreen(
    activity: MainActivity,
    onBack: () -> Unit,
    onNavigateToPreviewComment: (String, String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: PreviewViewModel = viewModel()
    val commentViewModel: CommentViewModel = viewModel(viewModelStoreOwner = activity)
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
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
        when (previewState) {
            is WebsiteState.Success -> preloadImages(previewState.info)
            else -> Unit
        }
    }

    DisposableEffect(Unit) {
        PreviewCommentPrefetcher.here(commentViewModel)
            .tag(PreviewCommentPrefetcher.Scope.PREVIEW_ACTIVITY)
        onDispose {
            PreviewCommentPrefetcher.bye(PreviewCommentPrefetcher.Scope.PREVIEW_ACTIVITY)
        }
    }

    PreviewScreen(
        previewState = previewState,
        getCachedPreviewState = viewModel::getCachedPreview,
        commentCount = commentCount,
        onBack = onBack,
        onLoadDate = { code ->
            viewModel.getHanimePreview(code)
            viewModel.preloadPreview(shiftMonthCodeForPreview(code, -1))
            viewModel.preloadPreview(shiftMonthCodeForPreview(code, 1))
            PreviewCommentPrefetcher.here(commentViewModel).fetch(PREVIEW_COMMENT_PREFIX, code)
        },
        onOpenComment = onNavigateToPreviewComment,
        onOpenVideo = onNavigateToVideo,
    )
}
