package com.yenaly.han1meviewer.ui.screen.video

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.yenaly.han1meviewer.ui.component.CommentReplyBar
import com.yenaly.han1meviewer.ui.component.CommentReportDialog
import com.yenaly.han1meviewer.ui.component.PageContent
import com.yenaly.han1meviewer.ui.component.VideoCommentCard
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.fakeCommentList
import com.yenaly.han1meviewer.util.parseTimeStrToMinutes
import com.yenaly.han1meviewer.util.safeSortedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CommentScreen(
    modifier: Modifier = Modifier,
    commentsFlow: StateFlow<List<VideoComments.VideoComment>>,
    commentStateFlow: StateFlow<WebsiteState<VideoComments>>,
    reportMessageFlow: Flow<CommentMessage>,
    currentSortType: StateFlow<CommentSortType>,
    reportReasons: List<ReportReason>,
    isPreviewCommentPrefetched: Boolean,
    isAlreadyLogin: Boolean,
    onRefresh: () -> Unit,
    onReply: (VideoComments.VideoComment, String) -> Unit,
    onReport: (VideoComments.VideoComment, ReportReason) -> Unit,
    onThumbUp: (VideoComments.VideoComment) -> Unit,
    onThumbDown: (VideoComments.VideoComment) -> Unit,
    onViewMoreReplies: (VideoComments.VideoComment) -> Unit,
    onSortChange: (CommentSortType) -> Unit,
    onComposeComment: (String) -> Unit,
    // 2025-11 改版：评论分批加载（每批 100 条），canLoadMore 为真时滚动到底部自动拉取下一批
    canLoadMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    onCommentScrollChange: (Int, Int) -> Unit = { _, _ -> },
) {
    val comments by commentsFlow.collectAsStateWithLifecycle()
    val state by commentStateFlow.collectAsStateWithLifecycle()
    val sortType by currentSortType.collectAsStateWithLifecycle()
    val containerSize = LocalWindowInfo.current.containerSize
    val maxScreenWidth = containerSize.width.dp

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var replyingComment by remember { mutableStateOf<VideoComments.VideoComment?>(null) }
    var reportComment by remember { mutableStateOf<VideoComments.VideoComment?>(null) }
    var showCommentBar by rememberSaveable { mutableStateOf(false) }
    var replyText by remember { mutableStateOf(TextFieldValue("")) }
    var composeText by remember { mutableStateOf(TextFieldValue("")) }
    var selectedReasonIndex by remember { mutableIntStateOf(-1) }
    var latestReportMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshingState = rememberPullToRefreshState()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset,
    )
    val scope = rememberCoroutineScope()
    val loginFirstText = stringResource(R.string.login_first)
    val commentTooShortText = stringResource(R.string.comment_too_short)
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

    // 分批加载：滚动接近列表底部时自动拉取下一批评论。
    // 状态里带上 totalItemsCount：append 新一批后若仍接近底部（比如快速 fling 到底
    // 或列表本就滚不动），nearEnd 恒为 true 时布尔快照不会再次发射，
    // 需要靠条目数变化再次触发（对齐网站 JS append 后的续拉检查）。
    LaunchedEffect(canLoadMore, listState) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            NearEndState(
                nearEnd = info.totalItemsCount > 0 &&
                        (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 4,
                totalItemsCount = info.totalItemsCount,
            )
        }.distinctUntilChanged().collect { state ->
            if (state.nearEnd) onLoadMore()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            onCommentScrollChange(index, offset)
        }
    }

    BackHandler(enabled = replyingComment != null || showCommentBar) {
        replyingComment = null
        replyText = TextFieldValue("")
        showCommentBar = false
        composeText = TextFieldValue("")
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
                CommentSortType.entries.forEach { type ->
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
        modifier = modifier.widthIn(max = maxScreenWidth),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isAlreadyLogin) {
                AnimatedVisibility(
                    visible = showCommentFab && replyingComment == null && !showCommentBar,
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
                        onClick = { showCommentBar = true },
                    )
                }
            }
        },
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                    )
            ) {
                // 评论列表主体。下拉刷新常驻——头部折叠态的下拉由外层折叠头
                // 在 preScroll 阶段优先拦截（父层先于子组件），不会触达这里的
                // PullToRefresh，无需按折叠状态切换容器（切换会导致整个列表
                // 重组重建，滚动折叠时列表闪烁）。
                @Composable
                fun BoxScope.ListBody() {
                    val loadError = state is WebsiteState.Error && sortedComments.isEmpty()
                    PageContent(
                        isLoading = false,
                        isError = loadError,
                        isEmpty = sortedComments.isEmpty(),
                        errorMessage = (state as? WebsiteState.Error)?.throwable?.message ?: "",
                        onRetry = onRefresh,
                        error = {
                            ErrorContent(
                                title = stringResource(R.string.load_failed_retry),
                                message = (state as WebsiteState.Error).throwable.message,
                                onRetry = onRefresh,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                            )
                        },
                        empty = {
                            EmptyContent(
                                hint = stringResource(R.string.comment_not_found),
                                subHint = latestReportMessage ?: ""
                            )
                        },
                    ) {
                        // 播放页禁用 item 入场动画：折叠联动会持续扩展视口，
                        // 新进入的评论全部以淡入动画进场会造成闪烁；
                        // 每项动画协程在快速滚动时也会掉帧。
                        LazyColumn(
                            state = listState,
                            enableItemAnimation = false,
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

                            items(sortedComments, key = { it.stableKey }) { comment ->
                                VideoCommentCard(
                                    comment = comment,
                                    onReply = {
                                        if (!isAlreadyLogin) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    loginFirstText
                                                )
                                            }
                                        } else {
                                            replyingComment = comment
                                        }
                                    },
                                    onThumbUp = {
                                        if (!isAlreadyLogin) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    loginFirstText
                                                )
                                            }
                                        } else {
                                            onThumbUp(comment)
                                        }
                                    },
                                    onThumbDown = {
                                        if (!isAlreadyLogin) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    loginFirstText
                                                )
                                            }
                                        } else {
                                            onThumbDown(comment)
                                        }
                                    },
                                    onReport = {
                                        if (!isAlreadyLogin) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    loginFirstText
                                                )
                                            }
                                        } else {
                                            reportComment = comment
                                        }
                                    },
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
                    ListBody()
                }
            }
            AnimatedVisibility(
                visible = replyingComment != null || showCommentBar,
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                if (replyingComment != null) {
                    CommentReplyBar(
                        text = replyText,
                        onTextChange = { replyText = it },
                        onSend = {
                            val text = replyText.text.trim()
                            if (text.length < 5) {
                                scope.launch { snackbarHostState.showSnackbar(commentTooShortText) }
                            } else {
                                replyingComment?.let { onReply(it, replyText.text) }
                                replyingComment = null
                                replyText = TextFieldValue("")
                            }
                        },
                        placeholder = stringResource(R.string.comment),
                    )
                } else {
                    CommentReplyBar(
                        text = composeText,
                        onTextChange = { composeText = it },
                        onSend = {
                            val text = composeText.text.trim()
                            if (text.length < 5) {
                                scope.launch { snackbarHostState.showSnackbar(commentTooShortText) }
                            } else {
                                onComposeComment(text)
                                showCommentBar = false
                                composeText = TextFieldValue("")
                            }
                        },
                        placeholder = stringResource(R.string.comment),
                    )
                }
            }
        }
    }
}

private fun sortComments(
    list: List<VideoComments.VideoComment>,
    type: CommentSortType,
): List<VideoComments.VideoComment> = when (type) {
    CommentSortType.LATEST -> list.safeSortedBy(
        { parseTimeStrToMinutes(it.date) },
        descending = false
    )

    CommentSortType.EARLIEST -> list.safeSortedBy(
        { parseTimeStrToMinutes(it.date) },
        descending = true
    )

    CommentSortType.MOST_REPLY -> list.safeSortedBy({ it.replyCount ?: 0 }, descending = true)
    CommentSortType.MOST_LIKES -> list.safeSortedBy({ it.realLikesCount ?: 0 }, descending = true)
    CommentSortType.MOST_DISLIKES -> list.safeSortedBy(
        { it.realLikesCount ?: 0 },
        descending = false
    )
}

@Composable
private fun sortText(type: CommentSortType): String = when (type) {
    CommentSortType.LATEST -> stringResource(R.string.sort_by_newest)
    CommentSortType.EARLIEST -> stringResource(R.string.sort_by_oldest)
    CommentSortType.MOST_REPLY -> stringResource(R.string.sort_by_replies)
    CommentSortType.MOST_LIKES -> stringResource(R.string.sort_most_likes)
    CommentSortType.MOST_DISLIKES -> stringResource(R.string.sort_most_dislikes)
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
    CommentScreen(
        commentsFlow = MutableStateFlow(fakeCommentList),
        commentStateFlow = MutableStateFlow(WebsiteState.Success(VideoComments(fakeCommentList.toMutableList()))),
        reportMessageFlow = flowOf(CommentMessage("")),
        currentSortType = MutableStateFlow(CommentSortType.LATEST),
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

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun CommentScreenEmptyPreview() {
    CommentScreen(
        commentsFlow = MutableStateFlow(emptyList()),
        commentStateFlow = MutableStateFlow(WebsiteState.Success(VideoComments(fakeCommentList.toMutableList()))),
        reportMessageFlow = flowOf(CommentMessage("")),
        currentSortType = MutableStateFlow(CommentSortType.LATEST),
        reportReasons = emptyList(),
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

/**
 * 分批加载触发状态：nearEnd 表示视口接近列表底部；
 * totalItemsCount 参与相等判断，append 新一批后仍处于底部时能再次触发加载。
 */
private data class NearEndState(
    val nearEnd: Boolean,
    val totalItemsCount: Int,
)
