package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.ReportReason
import com.yenaly.han1meviewer.logic.model.VideoComments
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.EmptyContent
import com.yenaly.han1meviewer.ui.component.ErrorContent
import com.yenaly.han1meviewer.ui.component.VideoCommentCard
import com.yenaly.han1meviewer.ui.fragment.video.CommentFragment
import com.yenaly.han1meviewer.util.parseTimeStrToMinutes
import com.yenaly.han1meviewer.util.safeSortedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CommentScreen(
    commentsFlow: StateFlow<List<VideoComments.VideoComment>>,
    commentStateFlow: StateFlow<WebsiteState<VideoComments>>,
    reportMessageFlow: Flow<CommentMessage>,
    currentSortType: StateFlow<CommentFragment.SortType>,
    reportReasons: List<ReportReason>,
    isPreviewCommentPrefetched: Boolean,
    isAlreadyLogin: Boolean,
    onRefresh: () -> Unit,
    onReply: (VideoComments.VideoComment, String) -> Unit,
    onReport: (VideoComments.VideoComment, ReportReason) -> Unit,
    onThumbUp: (VideoComments.VideoComment) -> Unit,
    onThumbDown: (VideoComments.VideoComment) -> Unit,
    onViewMoreReplies: (VideoComments.VideoComment) -> Unit,
    onSortChange: (CommentFragment.SortType) -> Unit,
    onComposeComment: (String) -> Unit,
) {
    val comments by commentsFlow.collectAsStateWithLifecycle()
    val state by commentStateFlow.collectAsStateWithLifecycle()
    val sortType by currentSortType.collectAsStateWithLifecycle()

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var replyingComment by remember { mutableStateOf<VideoComments.VideoComment?>(null) }
    var reportComment by remember { mutableStateOf<VideoComments.VideoComment?>(null) }
    var showComposeDialog by rememberSaveable { mutableStateOf(false) }
    var replyText by remember { mutableStateOf(TextFieldValue("")) }
    var composeText by remember { mutableStateOf(TextFieldValue("")) }
    var selectedReasonIndex by remember { mutableIntStateOf(-1) }
    var latestReportMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshingState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    LaunchedEffect(reportMessageFlow) {
        reportMessageFlow.collect {
            latestReportMessage = it.text
            if (it.text.isNotBlank()) {
                snackbarHostState.showSnackbar(it.text)
            }
        }
    }

    val sortedComments = remember(comments, sortType) {
        sortComments(comments, sortType)
    }
    val showCommentFab by rememberCommentFabVisibility(listState)

    if (replyingComment != null) {
        CommentInputDialog(
            title = stringResource(R.string.reply),
            label = stringResource(R.string.comment),
            text = replyText,
            onTextChange = { replyText = it },
            onConfirm = {
                replyingComment?.let { onReply(it, replyText.text) }
                replyingComment = null
                replyText = TextFieldValue("")
            },
            onDismiss = {
                replyingComment = null
                replyText = TextFieldValue("")
            },
            confirmText = stringResource(R.string.submit),
        )
    }

    if (showComposeDialog) {
        CommentInputDialog(
            title = stringResource(R.string.comment),
            label = stringResource(R.string.comment),
            text = composeText,
            onTextChange = { composeText = it },
            onConfirm = {
                val text = composeText.text.trim()
                if (text.isNotBlank()) {
                    onComposeComment(text)
                    showComposeDialog = false
                    composeText = TextFieldValue("")
                }
            },
            onDismiss = {
                showComposeDialog = false
                composeText = TextFieldValue("")
            },
            confirmText = stringResource(R.string.submit),
        )
    }

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.sort_comment),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                CommentFragment.SortType.entries.forEach { type ->
                    FilledTonalButton(
                        onClick = {
                            onSortChange(type)
                            showSortSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(sortText(type))
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isAlreadyLogin) {
                AnimatedVisibility(
                    visible = showCommentFab,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    ExtendedFloatingActionButton(
                        text = { Text(stringResource(R.string.comment)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_reply_24),
                                contentDescription = null,
                            )
                        },
                        onClick = { showComposeDialog = true },
                    )
                }
            }
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    bottom = paddingValues.calculateBottomPadding(),
                )
        ) {
        PullToRefreshBox(
            isRefreshing = state is WebsiteState.Loading && !isPreviewCommentPrefetched,
            onRefresh = onRefresh,
            state = refreshingState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = refreshingState,
                    isRefreshing = state is WebsiteState.Loading && !isPreviewCommentPrefetched,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            when {
                state is WebsiteState.Error && sortedComments.isEmpty() -> {
                    ErrorContent(
                        title = stringResource(R.string.load_failed_retry),
                        message = (state as WebsiteState.Error).throwable.message,
                        onRetry = onRefresh,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                    )
                }

                sortedComments.isEmpty() -> {
                    EmptyContent(
                        title = stringResource(R.string.comment_not_found),
                        description = latestReportMessage,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (sortedComments.size >= 3) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    FilledTonalButton(onClick = { showSortSheet = true }) {
                                        Text(sortText(sortType))
                                    }
                                }
                            }
                        }

                        items(sortedComments, key = { it.realReplyId }) { comment ->
                            VideoCommentCard(
                                comment = comment,
                                onReply = { replyingComment = comment },
                                onThumbUp = { onThumbUp(comment) },
                                onThumbDown = { onThumbDown(comment) },
                                onReport = { reportComment = comment },
                                onViewMoreReplies = if (comment.hasMoreReplies) {
                                    { onViewMoreReplies(comment) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }

        }
    }
}

private fun sortComments(
    list: List<VideoComments.VideoComment>,
    type: CommentFragment.SortType,
): List<VideoComments.VideoComment> = when (type) {
    CommentFragment.SortType.LATEST -> list.safeSortedBy({ parseTimeStrToMinutes(it.date) }, descending = false)
    CommentFragment.SortType.EARLIEST -> list.safeSortedBy({ parseTimeStrToMinutes(it.date) }, descending = true)
    CommentFragment.SortType.MOST_REPLY -> list.safeSortedBy({ it.replyCount ?: 0 }, descending = true)
    CommentFragment.SortType.MOST_LIKES -> list.safeSortedBy({ it.realLikesCount ?: 0 }, descending = true)
    CommentFragment.SortType.MOST_DISLIKES -> list.safeSortedBy({ it.realLikesCount ?: 0 }, descending = false)
}

@Composable
private fun sortText(type: CommentFragment.SortType): String = when (type) {
    CommentFragment.SortType.LATEST -> stringResource(R.string.sort_by_newest)
    CommentFragment.SortType.EARLIEST -> stringResource(R.string.sort_by_oldest)
    CommentFragment.SortType.MOST_REPLY -> stringResource(R.string.sort_by_replies)
    CommentFragment.SortType.MOST_LIKES -> stringResource(R.string.sort_most_likes)
    CommentFragment.SortType.MOST_DISLIKES -> stringResource(R.string.sort_most_dislikes)
}

data class CommentMessage(val text: String)

@Composable
private fun rememberCommentFabVisibility(listState: LazyListState): androidx.compose.runtime.State<Boolean> {
    return remember(listState) {
        derivedStateOf {
            val scrollOffset = listState.firstVisibleItemScrollOffset
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val lastScrolledBackward = listState.lastScrolledBackward
            val lastScrolledForward = listState.lastScrolledForward

            when {
                firstVisibleItemIndex == 0 && scrollOffset == 0 -> true
                lastScrolledBackward -> true
                lastScrolledForward -> false
                else -> true
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun CommentScreenPreview() {
    val fakeCommentList = listOf(
        VideoComments.VideoComment(
            avatar = "https://picsum.photos/64/64",
            username = "preview_user",
            date = "2小時前",
            content = "這是一條用於預覽的評論內容。",
            thumbUp = 12,
            isChildComment = false,
            hasMoreReplies = true,
            replyCount = 3,
            id = "1",
            post = VideoComments.VideoComment.POST(
                foreignId = "1",
                likeCommentStatus = false,
                unlikeCommentStatus = false,
            ),
            reportableId = "1",
            reportableType = "comment",
        )
    )
    CommentScreen(
        commentsFlow = MutableStateFlow(fakeCommentList),
        commentStateFlow = MutableStateFlow(WebsiteState.Success(VideoComments(fakeCommentList.toMutableList()))),
        reportMessageFlow = flowOf(CommentMessage("")),
        currentSortType = MutableStateFlow(CommentFragment.SortType.LATEST),
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
        isPreviewCommentPrefetched = false,
        isAlreadyLogin = true,
        onRefresh = {},
        onReply = { _, _ -> },
        onReport = { _, _ -> },
        onThumbUp = {},
        onThumbDown = {},
        onViewMoreReplies = {},
        onSortChange = {},
        onComposeComment = {},
    )
}
