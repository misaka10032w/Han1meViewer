package com.yenaly.han1meviewer.ui.fragment.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
import com.yenaly.han1meviewer.ui.screen.video.CommentMessage
import com.yenaly.han1meviewer.ui.screen.video.CommentScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.han1meviewer.util.checkBadGuy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    enum class SortType {
        LATEST, EARLIEST, MOST_REPLY, MOST_LIKES, MOST_DISLIKES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        commentType = arguments?.getString(COMMENT_TYPE)
    }

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
            HanimeTheme {
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
                        ChildCommentPopupFragment().apply {
                            arguments = Bundle().apply {
                                putString(com.yenaly.han1meviewer.COMMENT_ID, replyTargetId)
                            }
                        }.show(childFragmentManager, "child_comment_popup_$replyTargetId")
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