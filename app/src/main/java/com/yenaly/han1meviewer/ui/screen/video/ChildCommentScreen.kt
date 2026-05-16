package com.yenaly.han1meviewer.ui.screen.video

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.ReportReason
import com.yenaly.han1meviewer.logic.model.VideoCommentArgs
import com.yenaly.han1meviewer.logic.model.VideoComments
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.CommentReplyBar
import com.yenaly.han1meviewer.ui.component.CommentReportDialog
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.content.LoadingContent
import com.yenaly.han1meviewer.ui.component.VideoCommentCard
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.fakeCommentList
import com.yenaly.han1meviewer.util.parseTimeStrToMinutes
import com.yenaly.han1meviewer.util.safeSortedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildCommentScreen(
    commentsFlow: StateFlow<List<VideoComments.VideoComment>>,
    commentStateFlow: StateFlow<WebsiteState<VideoComments>>,
    reportMessageFlow: Flow<CommentMessage>,
    postReplyStateFlow: Flow<WebsiteState<Unit>>,
    commentLikeStateFlow: Flow<WebsiteState<VideoCommentArgs>>,
    reportReasons: List<ReportReason>,
    isAlreadyLogin: Boolean,
    onRefresh: () -> Unit,
    onReply: (VideoComments.VideoComment, String) -> Unit,
    onReport: (VideoComments.VideoComment, ReportReason) -> Unit,
    onThumbUp: (VideoComments.VideoComment) -> Unit,
    onThumbDown: (VideoComments.VideoComment) -> Unit,
    onCommentLikeSuccess: (VideoCommentArgs) -> Unit,
    onReplyStateChange: (Boolean) -> Unit = {},
) {
    val comments by commentsFlow.collectAsStateWithLifecycle()
    val state by commentStateFlow.collectAsStateWithLifecycle()
    val containerSize = LocalWindowInfo.current.containerSize
    val maxScreenWidth = containerSize.width.dp

    var replyingComment by remember { mutableStateOf<VideoComments.VideoComment?>(null) }
    var replyText by remember { mutableStateOf(TextFieldValue("")) }
    var reportComment by remember { mutableStateOf<VideoComments.VideoComment?>(null) }
    var selectedReasonIndex by remember { mutableIntStateOf(-1) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val loginFirstText = stringResource(R.string.login_first)
    val sendFailedText = stringResource(R.string.send_failed)
    val sendSuccessText = stringResource(R.string.send_success)
    val sendingReplyText = stringResource(R.string.sending_reply)
    val commentTooShortText = stringResource(R.string.comment_too_short)

    LaunchedEffect(reportMessageFlow) {
        reportMessageFlow.collect { message ->
            if (message.text.isNotBlank()) {
                snackbarHostState.showSnackbar(message.text)
            }
        }
    }

    LaunchedEffect(postReplyStateFlow) {
        postReplyStateFlow.collect { replyState ->
            when (replyState) {
                is WebsiteState.Error -> snackbarHostState.showSnackbar(sendFailedText)
                WebsiteState.Loading -> snackbarHostState.showSnackbar(sendingReplyText)
                is WebsiteState.Success -> {
                    snackbarHostState.showSnackbar(sendSuccessText)
                    onRefresh()
                }
            }
        }
    }

    LaunchedEffect(commentLikeStateFlow) {
        commentLikeStateFlow.collect { likeState ->
            when (likeState) {
                is WebsiteState.Error -> {
                    snackbarHostState.showSnackbar(likeState.throwable.message ?: "unknown")
                }

                WebsiteState.Loading -> Unit

                is WebsiteState.Success -> onCommentLikeSuccess(likeState.info)
            }
        }
    }

    val sortedComments = remember(comments) {
        comments.safeSortedBy({ parseTimeStrToMinutes(it.date) }, descending = false)
    }
    val nestedScrollInterop = rememberNestedScrollInteropConnection()

    BackHandler(enabled = replyingComment != null) {
        replyingComment = null
        replyText = TextFieldValue("")
    }

    LaunchedEffect(replyingComment) {
        if (replyingComment != null) {
            onReplyStateChange(true)
        }
    }

    Scaffold(
        modifier = Modifier.widthIn(max = maxScreenWidth),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = replyingComment != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                CommentReplyBar(
                    text = replyText,
                    onTextChange = { replyText = it },
                    onSend = {
                        replyingComment?.let { target ->
                            val prefix = "@${target.username}"
                            val contentLength = replyText.text.trim().removePrefix(prefix).trimStart().length
                            if (contentLength < 5) {
                                scope.launch { snackbarHostState.showSnackbar(commentTooShortText) }
                            } else {
                                onReply(target, replyText.text)
                                replyingComment = null
                                replyText = TextFieldValue("")
                            }
                        }
                    },
                    placeholder = stringResource(R.string.reply_child_comment),
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.child_comment),
                style = MaterialTheme.typography.headlineSmall,
            )

            if (sortedComments.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.video_count, sortedComments.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                state is WebsiteState.Loading && sortedComments.isEmpty() -> {
                    LoadingContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        message = stringResource(R.string.loading),
                    )
                }

                state is WebsiteState.Error && sortedComments.isEmpty() -> {
                    ErrorContent(
                        title = stringResource(R.string.load_reply_failed),
                        message = (state as WebsiteState.Error).throwable.message,
                        onRetry = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    )
                }

                sortedComments.isEmpty() -> {
                    EmptyContent(
                        hint = stringResource(R.string.comment_not_found)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollInterop),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sortedComments, key = { it.stableKey }) { comment ->
                            VideoCommentCard(
                                comment = comment,
                                onReply = {
                                    if (!isAlreadyLogin) {
                                        scope.launch { snackbarHostState.showSnackbar(loginFirstText) }
                                    } else {
                                        replyingComment = comment
                                        replyText = TextFieldValue("@${comment.username} ")
                                    }
                                },
                                onThumbUp = {
                                    if (!isAlreadyLogin) {
                                        scope.launch { snackbarHostState.showSnackbar(loginFirstText) }
                                    } else {
                                        onThumbUp(comment)
                                    }
                                },
                                onThumbDown = {
                                    if (!isAlreadyLogin) {
                                        scope.launch { snackbarHostState.showSnackbar(loginFirstText) }
                                    } else {
                                        onThumbDown(comment)
                                    }
                                },
                                onReport = {
                                    if (!isAlreadyLogin) {
                                        scope.launch { snackbarHostState.showSnackbar(loginFirstText) }
                                    } else {
                                        reportComment = comment
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (reportComment != null) {
        CommentReportDialog(
            reportReasons = reportReasons,
            selectedReasonIndex = selectedReasonIndex,
            onSelectReason = { selectedReasonIndex = it },
            onConfirm = {
                val reason = reportReasons.getOrNull(selectedReasonIndex)
                val target = reportComment
                if (reason != null && target != null) {
                    onReport(target, reason)
                }
                reportComment = null
                selectedReasonIndex = -1
            },
            onDismiss = {
                reportComment = null
                selectedReasonIndex = -1
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun ChildCommentScreenPreview() {
    ComponentPreview {
        ChildCommentScreen(
            commentsFlow = MutableStateFlow(fakeCommentList),
            commentStateFlow = MutableStateFlow(WebsiteState.Success(VideoComments(fakeCommentList.toMutableList()))),
            reportMessageFlow = flowOf(CommentMessage("")),
            postReplyStateFlow = flowOf(WebsiteState.Success(Unit)),
            commentLikeStateFlow = flowOf(
                WebsiteState.Success(
                    VideoCommentArgs(
                        isPositive = true,
                        commentPosition = 0,
                        comment = fakeCommentList.first(),
                    )
                )
            ),
            reportReasons = listOf(
                ReportReason(
                    lang = ReportReason.Language(
                        zhrTW = "垃圾訊息",
                        zhrCN = "垃圾信息",
                        en = "Spam",
                    ),
                    reasonKey = "spam",
                )
            ),
            isAlreadyLogin = true,
            onRefresh = {},
            onReply = { _, _ -> },
            onReport = { _, _ -> },
            onThumbUp = {},
            onThumbDown = {},
            onCommentLikeSuccess = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun ChildCommentScreenEmptyPreview() {
    ComponentPreview {
        ChildCommentScreen(
            commentsFlow = MutableStateFlow(emptyList()),
            commentStateFlow = MutableStateFlow(WebsiteState.Success(VideoComments(mutableListOf()))),
            reportMessageFlow = flowOf(CommentMessage("")),
            postReplyStateFlow = flowOf(WebsiteState.Loading),
            commentLikeStateFlow = flowOf(WebsiteState.Loading),
            reportReasons = emptyList(),
            isAlreadyLogin = true,
            onRefresh = {},
            onReply = { _, _ -> },
            onReport = { _, _ -> },
            onThumbUp = {},
            onThumbDown = {},
            onCommentLikeSuccess = {},
        )
    }
}
