package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.PREVIEW_COMMENT_PREFIX
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.BottomSheetHandler
import com.yenaly.han1meviewer.ui.screen.video.ChildCommentScreen
import com.yenaly.han1meviewer.ui.screen.video.CommentMessage
import com.yenaly.han1meviewer.ui.screen.video.CommentScreen
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.yenaly_libs.utils.application
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewCommentRouteScreen(
    activity: MainActivity,
    route: PreviewCommentRoute,
    onBack: () -> Unit,
) {
    val viewModel: CommentViewModel = viewModel(viewModelStoreOwner = activity)
    val comments = viewModel.videoCommentFlow
    val commentState = viewModel.videoCommentStateFlow
    val commentUiState = remember(route.dateCode) {
        viewModel.getCommentUiState(route.dateCode)
    }
    var childCommentId by rememberSaveable { mutableStateOf(commentUiState.childCommentId) }
    val childSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    val prefetchedComments = PreviewCommentPrefetcher.here(viewModel)
        .commentFlow
        .collectAsStateWithLifecycle()
        .value
    val hasPrefetchedComments = prefetchedComments.isNotEmpty()
    val reportMessages = remember { kotlinx.coroutines.flow.MutableSharedFlow<CommentMessage>() }

    LaunchedEffect(route.dateCode, hasPrefetchedComments, prefetchedComments) {
        viewModel.code = route.dateCode
        if (hasPrefetchedComments) {
            viewModel.updateComments(prefetchedComments)
        } else {
            viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode)
        }
    }

    DisposableEffect(Unit) {
        PreviewCommentPrefetcher.here(viewModel)
            .tag(PreviewCommentPrefetcher.Scope.PREVIEW_COMMENT_ACTIVITY)
        onDispose {
            PreviewCommentPrefetcher.bye(PreviewCommentPrefetcher.Scope.PREVIEW_COMMENT_ACTIVITY)
        }
    }

    LaunchedEffect(Unit) {
        commentState.collect { state ->
            if (state is WebsiteState.Success) {
                viewModel.currentUserId = state.info.currentUserId
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.postCommentFlow.collect { state ->
            if (state is WebsiteState.Success) {
                viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.postReplyFlow.collect { state ->
            if (state is WebsiteState.Success) {
                viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.commentLikeFlow.collect { state ->
            if (state is WebsiteState.Success) {
                viewModel.handleCommentLike(state.info)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reportMessage.collect { msg ->
            val text = if (msg.args.isNotEmpty()) {
                activity.getString(msg.resId, *msg.args.toTypedArray())
            } else {
                activity.getString(msg.resId)
            }
            reportMessages.emit(CommentMessage(text))
        }
    }

    childCommentId?.let { currentCommentId ->
        ModalBottomSheet(
            onDismissRequest = {
                childCommentId = null
                viewModel.setChildCommentId(route.dateCode, null)
                viewModel.clearVideoReplyList()
            },
            sheetState = childSheetState,
            dragHandle = null
        ) {
            LaunchedEffect(currentCommentId) {
                viewModel.getCommentReply(currentCommentId)
            }
            BottomSheetHandler()
            val mappedReportFlow = remember(viewModel.reportMessage) {
                viewModel.reportMessage.map { message ->
                    val text = if (message.args.isNotEmpty()) {
                        application.getString(message.resId, *message.args.toTypedArray())
                    } else {
                        application.getString(message.resId)
                    }
                    CommentMessage(text)
                }
            }
            ChildCommentScreen(
                commentsFlow = viewModel.videoReplyFlow,
                commentStateFlow = viewModel.videoReplyStateFlow,
                reportMessageFlow = mappedReportFlow,
                postReplyStateFlow = viewModel.postReplyFlow,
                commentLikeStateFlow = viewModel.commentLikeFlow,
                reportReasons = viewModel.reportReason,
                isAlreadyLogin = Preferences.isAlreadyLogin,
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
                        true, 0, comment,
                        likeCommentStatus = comment.post.likeCommentStatus,
                    )
                },
                onThumbDown = { comment ->
                    viewModel.likeChildComment(
                        false, 0, comment,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.latest_hanime_comment, route.date)) },
                navigationIcon = {
                    FilledIconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        CommentScreen(
            commentsFlow = comments,
            commentStateFlow = commentState,
            reportMessageFlow = reportMessages,
            currentSortType = viewModel.currentSortType,
            reportReasons = viewModel.reportReason,
            isPreviewCommentPrefetched = hasPrefetchedComments,
            isAlreadyLogin = Preferences.isAlreadyLogin,
            onRefresh = { viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode) },
            onReply = { comment, text ->
                if (!Preferences.isAlreadyLogin) return@CommentScreen
                val replyTargetId = comment.replyTargetIdOrNull
                if (replyTargetId == null) {
                    scope.launch {
                        reportMessages.emit(CommentMessage(activity.getString(R.string.there_is_a_small_issue)))
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
                if (!Preferences.isAlreadyLogin) return@CommentScreen
                if (comment.isChildComment) {
                    viewModel.likeChildComment(
                        true,
                        0,
                        comment,
                        likeCommentStatus = comment.post.likeCommentStatus
                    )
                } else {
                    viewModel.likeComment(
                        true,
                        0,
                        comment,
                        likeCommentStatus = comment.post.likeCommentStatus
                    )
                }
            },
            onThumbDown = { comment ->
                if (!Preferences.isAlreadyLogin) return@CommentScreen
                if (comment.isChildComment) {
                    viewModel.likeChildComment(
                        false,
                        0,
                        comment,
                        unlikeCommentStatus = comment.post.unlikeCommentStatus
                    )
                } else {
                    viewModel.likeComment(
                        false,
                        0,
                        comment,
                        unlikeCommentStatus = comment.post.unlikeCommentStatus
                    )
                }
            },
            onViewMoreReplies = { comment ->
                comment.replyTargetIdOrNull?.let {
                    childCommentId = it
                    viewModel.setChildCommentId(route.dateCode, it)
                }
            },
            onSortChange = viewModel::setSortType,
            onComposeComment = { text ->
                viewModel.currentUserId?.let { id ->
                    viewModel.postComment(id, viewModel.code, PREVIEW_COMMENT_PREFIX, text)
                } ?: scope.launch {
                    reportMessages.emit(CommentMessage(activity.getString(R.string.there_is_a_small_issue)))
                }
            },
            initialFirstVisibleItemIndex = commentUiState.firstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset = commentUiState.firstVisibleItemScrollOffset,
            onCommentScrollChange = { index, offset ->
                viewModel.setCommentScrollState(route.dateCode, index, offset)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}
