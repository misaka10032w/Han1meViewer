package com.yenaly.han1meviewer.ui.fragment.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.yenaly.han1meviewer.COMMENT_TYPE
import com.yenaly.han1meviewer.PREVIEW_COMMENT_PREFIX
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.Preferences.isAlreadyLogin
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.screen.video.ChildCommentScreen
import com.yenaly.han1meviewer.ui.screen.video.CommentMessage
import com.yenaly.han1meviewer.ui.screen.video.CommentScreen
import com.yenaly.han1meviewer.ui.screen.video.CommentSortType
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.han1meviewer.util.checkBadGuy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

class CommentFragment : Fragment() {
    private var commentType: String? = null

    val viewModel: CommentViewModel by lazy {
        val parent = parentFragment
        if (parent == null || commentType == PREVIEW_COMMENT_PREFIX) {
            ViewModelProvider(requireActivity())[CommentViewModel::class.java]
        } else {
            ViewModelProvider(parent)[CommentViewModel::class.java]
        }
    }

    private val reportMessages = MutableSharedFlow<CommentMessage>()
    private val commentTypePrefix: String
        get() = arguments?.getString(COMMENT_TYPE) ?: VIDEO_COMMENT_PREFIX

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        commentType = arguments?.getString(COMMENT_TYPE)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        setContent {
            var childCommentId by remember { mutableStateOf<String?>(null) }
            val childSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            HanimeTheme {
                childCommentId?.let { currentCommentId ->
                    ChildCommentBottomSheet(
                        commentId = currentCommentId,
                        viewModel = viewModel,
                        sheetState = childSheetState,
                        onDismiss = {
                            childCommentId = null
                            viewModel.clearVideoReplyList()
                        },
                    )
                }

                CommentScreen(
                    commentsFlow = viewModel.videoCommentFlow,
                    commentStateFlow = viewModel.videoCommentStateFlow,
                    reportMessageFlow = reportMessages.asSharedFlow(),
                    currentSortType = viewModel.currentSortType,
                    reportReasons = viewModel.reportReason,
                    isPreviewCommentPrefetched = commentType == PREVIEW_COMMENT_PREFIX &&
                        PreviewCommentPrefetcher.here(viewModel).commentFlow.collectAsState().value.isNotEmpty(),
                    isAlreadyLogin = isAlreadyLogin,
                    onRefresh = { viewModel.getComment(commentTypePrefix, viewModel.code) },
                    onReply = { comment, text ->
                        if (!isAlreadyLogin) return@CommentScreen
                        val replyTargetId = comment.replyTargetIdOrNull
                        if (replyTargetId == null) {
                            lifecycleScope.launch {
                                reportMessages.emit(CommentMessage(getString(R.string.there_is_a_small_issue)))
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
                            lifecycleScope.launch {
                                reportMessages.emit(CommentMessage(getString(R.string.there_is_a_small_issue)))
                            }
                            return@CommentScreen
                        }
                        childCommentId = replyTargetId
                    },
                    onSortChange = { viewModel.setSortType(it) },
                    onComposeComment = {
                        viewModel.currentUserId?.let { id ->
                            viewModel.postComment(id, viewModel.code, commentTypePrefix, it)
                        } ?: lifecycleScope.launch {
                            reportMessages.emit(CommentMessage(getString(R.string.there_is_a_small_issue)))
                        }
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkBadGuy(requireContext(), R.raw.akarin)

        if (commentType == PREVIEW_COMMENT_PREFIX) {
            val comments = PreviewCommentPrefetcher.here(viewModel).commentFlow.value
            if (comments.isNotEmpty()) {
                viewModel.updateComments(comments)
            } else {
                viewModel.getComment(commentTypePrefix, viewModel.code)
            }
        } else {
            viewModel.getComment(commentTypePrefix, viewModel.code)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.videoCommentStateFlow.collect { state ->
                if (state is WebsiteState.Success) {
                    viewModel.currentUserId = state.info.currentUserId
                    showRedDotCount(state.info.videoComment.size)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.videoCommentFlow.collect { list ->
                if (commentType == PREVIEW_COMMENT_PREFIX) {
                    PreviewCommentPrefetcher.here(viewModel).update(list)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postCommentFlow.collect { state ->
                when (state) {
                    is WebsiteState.Success -> viewModel.getComment(commentTypePrefix, viewModel.code)
                    else -> Unit
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postReplyFlow.collect { state ->
                when (state) {
                    is WebsiteState.Success -> viewModel.getComment(commentTypePrefix, viewModel.code)
                    else -> Unit
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.commentLikeFlow.collect { state ->
                if (state is WebsiteState.Success) {
                    viewModel.handleCommentLike(state.info)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reportMessage.collect { msg ->
                val text = if (msg.args.isNotEmpty()) {
                    getString(msg.resId, *msg.args.toTypedArray())
                } else {
                    getString(msg.resId)
                }
                reportMessages.emit(CommentMessage(text))
            }
        }
    }

    private fun showRedDotCount(count: Int) {
        val videoFragment = parentFragment as? VideoFragment
        videoFragment?.showRedDotCount(count)
    }

    companion object {
        private const val VIDEO_COMMENT_PREFIX = "video"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildCommentBottomSheet(
    commentId: String,
    viewModel: CommentViewModel,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(commentId) {
        viewModel.getCommentReply(commentId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        ChildCommentScreen(
            commentsFlow = viewModel.videoReplyFlow,
            commentStateFlow = viewModel.videoReplyStateFlow,
            reportMessageFlow = viewModel.reportMessage.map { message ->
                val text = if (message.args.isNotEmpty()) {
                    com.yenaly.yenaly_libs.utils.application.getString(message.resId, *message.args.toTypedArray())
                } else {
                    com.yenaly.yenaly_libs.utils.application.getString(message.resId)
                }
                CommentMessage(text)
            },
            postReplyStateFlow = viewModel.postReplyFlow,
            commentLikeStateFlow = viewModel.commentLikeFlow,
            reportReasons = viewModel.reportReason,
            isAlreadyLogin = Preferences.isAlreadyLogin,
            onRefresh = { viewModel.getCommentReply(commentId) },
            onReply = { _, text ->
                viewModel.postReply(commentId, text)
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
        )
    }
}
