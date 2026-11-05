package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.yenaly.han1meviewer.ui.screen.home.preview.currentDateCode as currentMonthCode
import com.yenaly.han1meviewer.ui.screen.home.preview.shiftMonthCode
import com.yenaly.han1meviewer.ui.screen.home.preview.toNormalDateLabel
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.han1meviewer.util.application
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
    val currentMonth = remember { currentMonthCode() }
    var currentDateCode by rememberSaveable { mutableStateOf(route.dateCode) }
    val currentDateLabel = remember(currentDateCode) { toNormalDateLabel(currentDateCode) }
    val commentUiState = remember(currentDateCode) {
        viewModel.getCommentUiState(currentDateCode)
    }
    var childCommentId by rememberSaveable { mutableStateOf(commentUiState.childCommentId) }
    var initialPrefetchConsumed by rememberSaveable { mutableStateOf(false) }

    fun shiftCommentMonth(delta: Int) {
        val targetDateCode = shiftMonthCode(currentDateCode, delta)
        if (targetDateCode == currentDateCode) return
        if (targetDateCode > currentMonth) return
        childCommentId = null
        viewModel.setChildCommentId(targetDateCode, null)
        viewModel.clearVideoReplyList()
        viewModel.clearCommentData()
        viewModel.code = targetDateCode
        currentDateCode = targetDateCode
    }
    val childSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(
            SheetValue.Hidden,
            SheetValue.PartiallyExpanded,
            SheetValue.Expanded,
        ),
    )
    val scope = rememberCoroutineScope()
    val prefetchedComments = PreviewCommentPrefetcher.here(viewModel)
        .commentFlow
        .collectAsStateWithLifecycle()
        .value
    val hasPrefetchedComments = prefetchedComments.isNotEmpty() &&
            viewModel.lastCommentCode == route.dateCode
    val reportMessages = remember { kotlinx.coroutines.flow.MutableSharedFlow<CommentMessage>() }

    LaunchedEffect(currentDateCode) {
        viewModel.code = currentDateCode
        if (!initialPrefetchConsumed &&
            currentDateCode == route.dateCode &&
            hasPrefetchedComments
        ) {
            initialPrefetchConsumed = true
            viewModel.updateComments(prefetchedComments)
        } else {
            initialPrefetchConsumed = true
            viewModel.getComment(PREVIEW_COMMENT_PREFIX, currentDateCode)
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

    LaunchedEffect(currentDateCode) {
        viewModel.postCommentFlow.collect { state ->
            if (state is WebsiteState.Success) {
                viewModel.getComment(PREVIEW_COMMENT_PREFIX, currentDateCode)
            }
        }
    }

    LaunchedEffect(currentDateCode) {
        viewModel.postReplyFlow.collect { state ->
            if (state is WebsiteState.Success) {
                viewModel.getComment(PREVIEW_COMMENT_PREFIX, currentDateCode)
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
                viewModel.setChildCommentId(currentDateCode, null)
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
                        "${Preferences.baseUrl}watch?v=$currentDateCode",
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
                title = {
                    Text(stringResource(R.string.latest_hanime_comment, currentDateLabel))
                },
                navigationIcon = {
                    FilledIconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    FilledIconButton(onClick = { shiftCommentMonth(-1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.previous_month),
                        )
                    }
                    FilledIconButton(
                        onClick = { shiftCommentMonth(1) },
                        enabled = currentDateCode < currentMonth,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.next_month),
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        key(currentDateCode) {
            CommentScreen(
                commentsFlow = comments,
                commentStateFlow = commentState,
                reportMessageFlow = reportMessages,
                currentSortType = viewModel.currentSortType,
                reportReasons = viewModel.reportReason,
                isPreviewCommentPrefetched = hasPrefetchedComments,
                isAlreadyLogin = Preferences.isAlreadyLogin,
                onRefresh = { viewModel.getComment(PREVIEW_COMMENT_PREFIX, currentDateCode) },
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
                        "${Preferences.baseUrl}watch?v=$currentDateCode",
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
                        viewModel.setChildCommentId(currentDateCode, it)
                    }
                },
                onSortChange = viewModel::setSortType,
                onComposeComment = { text ->
                    viewModel.currentUserId?.let { id ->
                        viewModel.postComment(id, currentDateCode, PREVIEW_COMMENT_PREFIX, text)
                    } ?: scope.launch {
                        reportMessages.emit(CommentMessage(activity.getString(R.string.there_is_a_small_issue)))
                    }
                },
                initialFirstVisibleItemIndex = commentUiState.firstVisibleItemIndex,
                initialFirstVisibleItemScrollOffset = commentUiState.firstVisibleItemScrollOffset,
                onCommentScrollChange = { index, offset ->
                    viewModel.setCommentScrollState(currentDateCode, index, offset)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}
