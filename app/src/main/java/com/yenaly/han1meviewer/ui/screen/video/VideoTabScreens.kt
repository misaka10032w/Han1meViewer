package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.Preferences.isAlreadyLogin
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_COMMENT_PREFIX
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.bridge.VideoPageHost
import com.yenaly.han1meviewer.ui.component.BottomSheetHandler
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun RenderVideoIntroductionContent(
    videoCode: String,
    viewModel: VideoViewModel,
    pendingDownloadPrompt: DownloadPromptState?,
    onPendingDownloadPromptChange: (DownloadPromptState?) -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    onOpenArtist: (HanimeVideo.Artist) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onToggleSubscribe: (HanimeVideo.Artist) -> Unit,
    onToggleFavorite: (HanimeVideo) -> Unit,
    onManageMyList: (HanimeVideo.MyList?, List<Boolean>) -> Unit,
    onQuickCheckIn: (CheckInRecordEntity) -> Unit,
    onPrepareDownload: (String, HanimeVideo?) -> Unit,
    onConfirmDownloadPrompt: (HanimeVideo?) -> Unit,
    onRequestOpenOfficialDownloadPage: () -> Unit,
    onRequestOpenDownloadPermissionSettings: () -> Unit,
    onOpenWebPage: () -> Unit,
    onOpenOriginalComic: (String) -> Unit,
    onOpenShare: (String, String) -> Unit,
    onCopyText: (String) -> Unit,
    onIntroductionLinkClick: (String) -> Unit,
    stringLongPressShare: String,
) {
    val videoState = viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle().value
    val video = viewModel.hanimeVideoFlow.collectAsStateWithLifecycle().value
    val videoShareText = video?.title?.let { title ->
        getHanimeShareText(title, videoCode)
    }.orEmpty()
    val introScrollState = viewModel.getIntroScrollState(videoCode)

    HanimeTheme {
        VideoIntroductionScreen(
            video = video,
            state = videoState,
            fromDownload = viewModel.fromDownload,
            hideRelatedInIntro = viewModel.hideRelatedInIntro,
            shareText = videoShareText,
            playlistInitialIndex = viewModel.getPlaylistFirstVisibleIndex(videoCode) ?: 0,
            introFirstVisibleItemIndex = introScrollState.firstVisibleItemIndex,
            introFirstVisibleItemScrollOffset = introScrollState.firstVisibleItemScrollOffset,
            downloadPrompt = pendingDownloadPrompt,
            onRetry = { viewModel.getHanimeVideo(videoCode) },
            onOpenVideo = onOpenVideo,
            onOpenArtist = onOpenArtist,
            onNavigateToSearch = onNavigateToSearch,
            onToggleSubscribe = onToggleSubscribe,
            onToggleFavorite = { video?.let(onToggleFavorite) },
            onManageMyList = { _, selectedStates ->
                onManageMyList(video?.myList, selectedStates)
            },
            onQuickCheckIn = onQuickCheckIn,
            onPrepareDownload = { quality ->
                onPrepareDownload(quality, video)
            },
            onDismissDownloadPrompt = {
                onPendingDownloadPromptChange(null)
            },
            onConfirmDownloadPrompt = {
                onConfirmDownloadPrompt(video)
            },
            onRequestOpenOfficialDownloadPage = onRequestOpenOfficialDownloadPage,
            onRequestOpenDownloadPermissionSettings = onRequestOpenDownloadPermissionSettings,
            onShare = {
                onOpenShare(videoShareText, stringLongPressShare)
            },
            onCopyShareText = {
                if (videoShareText.isNotBlank()) {
                    onCopyText(videoShareText)
                }
            },
            onOpenWebPage = onOpenWebPage,
            onOpenOriginalComic = video?.originalComic
                ?.takeIf { it.isNotBlank() }
                ?.let { comicLink -> { onOpenOriginalComic(comicLink) } },
            onCopyText = onCopyText,
            onShowAllPlaylist = if (!viewModel.fromDownload && video?.playlist != null) {
                {}
            } else {
                null
            },
            onPlaylistScrollChange = { index ->
                viewModel.setPlaylistFirstVisibleIndex(videoCode, index)
            },
            onIntroductionScrollChange = { index, offset ->
                viewModel.setIntroScrollState(videoCode, index, offset)
            },
            onIntroductionLinkClick = onIntroductionLinkClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderVideoCommentContent(
    viewModel: CommentViewModel,
    reportMessages: MutableSharedFlow<CommentMessage>,
    getMessageText: (CommentViewModel.Message) -> String,
    pageHost: VideoPageHost? = null,
) {
    val commentUiState = remember(viewModel.code) {
        viewModel.getCommentUiState(viewModel.code)
    }
    var childCommentId by remember { mutableStateOf(commentUiState.childCommentId) }
    val childSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    HanimeTheme {
        LaunchedEffect(viewModel.code) {
            viewModel.getComment(VIDEO_COMMENT_PREFIX, viewModel.code)
        }

        LaunchedEffect(Unit) {
            viewModel.videoCommentStateFlow.collect { state ->
                if (state is WebsiteState.Success) {
                    viewModel.currentUserId = state.info.currentUserId
                    pageHost?.showCommentBadge(state.info.videoComment.size)
                }
            }
        }

        childCommentId?.let { currentCommentId ->
            ModalBottomSheet(
                onDismissRequest = {
                    childCommentId = null
                    viewModel.setChildCommentId(viewModel.code, null)
                    viewModel.clearVideoReplyList()
                },
                sheetState = childSheetState,
                dragHandle = null,
            ) {
                LaunchedEffect(currentCommentId) {
                    viewModel.getCommentReply(currentCommentId)
                }
                BottomSheetHandler()
                val childReportFlow = remember(viewModel.reportMessage) {
                    viewModel.reportMessage.map { message ->
                        val text = if (message.args.isNotEmpty()) {
                            com.yenaly.yenaly_libs.utils.application.getString(
                                message.resId,
                                *message.args.toTypedArray()
                            )
                        } else {
                            com.yenaly.yenaly_libs.utils.application.getString(message.resId)
                        }
                        CommentMessage(text)
                    }
                }
                ChildCommentScreen(
                    commentsFlow = viewModel.videoReplyFlow,
                    commentStateFlow = viewModel.videoReplyStateFlow,
                    reportMessageFlow = childReportFlow,
                    postReplyStateFlow = viewModel.postReplyFlow,
                    commentLikeStateFlow = viewModel.commentLikeFlow,
                    reportReasons = viewModel.reportReason,
                    isAlreadyLogin = isAlreadyLogin,
                    onRefresh = { viewModel.getCommentReply(currentCommentId) },
                    onReply = { _, text ->
                        viewModel.postReply(currentCommentId, text)
                    },
                    onReport = { comment, reason ->
                        viewModel.reportComment(
                            reason.reasonKey ?: reason.value,
                            viewModel.currentUserId,
                            "${Preferences.baseUrl}watch?v=${viewModel.code}",
                            comment.reportableType,
                            comment.reportableId,
                        )
                    },
                    onThumbUp = { comment ->
                        viewModel.likeChildComment(
                            true,
                            0,
                            comment,
                            likeCommentStatus = comment.post.likeCommentStatus,
                        )
                    },
                    onThumbDown = { comment ->
                        viewModel.likeChildComment(
                            false,
                            0,
                            comment,
                            unlikeCommentStatus = comment.post.unlikeCommentStatus,
                        )
                    },
                    onCommentLikeSuccess = viewModel::handleCommentLike,
                    onReplyStateChange = { isReplying ->
                        if (isReplying) {
                            scope.launch { childSheetState.expand() }
                        }
                    },
                )
            }
        }

        val sharedReportFlow = remember(reportMessages) { reportMessages.asSharedFlow() }
        CommentScreen(
            commentsFlow = viewModel.videoCommentFlow,
            commentStateFlow = viewModel.videoCommentStateFlow,
            reportMessageFlow = sharedReportFlow,
            currentSortType = viewModel.currentSortType,
            reportReasons = viewModel.reportReason,
            isPreviewCommentPrefetched = false,
            isAlreadyLogin = isAlreadyLogin,
            onRefresh = { viewModel.getComment(VIDEO_COMMENT_PREFIX, viewModel.code) },
            onReply = { comment, text ->
                if (!isAlreadyLogin) return@CommentScreen
                val replyTargetId = comment.replyTargetIdOrNull
                if (replyTargetId == null) {
                    scope.launch {
                        reportMessages.emit(CommentMessage(getMessageText(CommentViewModel.Message(R.string.there_is_a_small_issue))))
                    }
                    return@CommentScreen
                }
                viewModel.postReply(replyTargetId, text)
            },
            onReport = { comment, reason ->
                viewModel.reportComment(
                    reason.reasonKey ?: reason.value,
                    viewModel.currentUserId,
                    "${Preferences.baseUrl}watch?v=${viewModel.code}",
                    comment.reportableType,
                    comment.reportableId,
                )
            },
            onThumbUp = { comment ->
                if (!isAlreadyLogin) return@CommentScreen
                if (comment.isChildComment) {
                    viewModel.likeChildComment(
                        true,
                        0,
                        comment,
                        likeCommentStatus = comment.post.likeCommentStatus,
                    )
                } else {
                    viewModel.likeComment(
                        true,
                        0,
                        comment,
                        likeCommentStatus = comment.post.likeCommentStatus,
                    )
                }
            },
            onThumbDown = { comment ->
                if (!isAlreadyLogin) return@CommentScreen
                if (comment.isChildComment) {
                    viewModel.likeChildComment(
                        false,
                        0,
                        comment,
                        unlikeCommentStatus = comment.post.unlikeCommentStatus,
                    )
                } else {
                    viewModel.likeComment(
                        false,
                        0,
                        comment,
                        unlikeCommentStatus = comment.post.unlikeCommentStatus,
                    )
                }
            },
            onViewMoreReplies = { comment ->
                val replyTargetId = comment.replyTargetIdOrNull
                if (replyTargetId == null) {
                    scope.launch {
                        reportMessages.emit(CommentMessage(getMessageText(CommentViewModel.Message(R.string.there_is_a_small_issue))))
                    }
                    return@CommentScreen
                }
                childCommentId = replyTargetId
                viewModel.setChildCommentId(viewModel.code, replyTargetId)
            },
            onSortChange = { viewModel.setSortType(it) },
            onComposeComment = {
                viewModel.currentUserId?.let { id ->
                    viewModel.postComment(id, viewModel.code, VIDEO_COMMENT_PREFIX, it)
                } ?: scope.launch {
                    reportMessages.emit(CommentMessage(getMessageText(CommentViewModel.Message(R.string.there_is_a_small_issue))))
                }
            },
            initialFirstVisibleItemIndex = commentUiState.firstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset = commentUiState.firstVisibleItemScrollOffset,
            onCommentScrollChange = { index, offset ->
                viewModel.setCommentScrollState(viewModel.code, index, offset)
            },
        )
    }
}
