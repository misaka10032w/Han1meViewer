package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_COMMENT_PREFIX
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.bridge.VideoPageHost
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.application
import kotlinx.coroutines.launch

@Composable
fun VideoRouteContent(
    videoCode: String,
    videoState: VideoLoadingState<*>,
    videoViewModel: VideoViewModel,
    commentViewModel: CommentViewModel,
    fromDownload: Boolean,
    pendingDownloadPrompt: DownloadPromptState?,
    onPendingDownloadPromptChange: (DownloadPromptState?) -> Unit,
    onRetry: () -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    onOpenArtist: (com.yenaly.han1meviewer.logic.model.HanimeVideo.Artist) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onToggleSubscribe: (com.yenaly.han1meviewer.logic.model.HanimeVideo.Artist) -> Unit,
    onToggleFavorite: (com.yenaly.han1meviewer.logic.model.HanimeVideo) -> Unit,
    onRateVideo: (com.yenaly.han1meviewer.logic.model.HanimeVideo, Boolean) -> Unit,
    onManageMyList: (com.yenaly.han1meviewer.logic.model.HanimeVideo.MyList?, List<Boolean>) -> Unit,
    onQuickCheckIn: (com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity) -> Unit,
    onPrepareDownload: (String, com.yenaly.han1meviewer.logic.model.HanimeVideo?) -> Unit,
    onConfirmDownloadPrompt: (com.yenaly.han1meviewer.logic.model.HanimeVideo?, String?) -> Unit,
    onRequestOpenOfficialDownloadPage: () -> Unit,
    onRequestOpenDownloadPermissionSettings: () -> Unit,
    onOpenWebPage: () -> Unit,
    onOpenOriginalComic: (String) -> Unit,
    onOpenShare: (String, String) -> Unit,
    onCopyText: (String) -> Unit,
    onIntroductionLinkClick: (String) -> Unit,
    stringLongPressShare: String,
    pageHost: VideoPageHost,
) {
    val hostUiState by videoViewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()
    val commentBadgeCount by videoViewModel.commentBadgeCount.collectAsStateWithLifecycle()
    val disableComments = remember {
        PreferenceManager.getDefaultSharedPreferences(application)
            .getBoolean("disable_comments", false)
    }
    val relatedAsTab = !fromDownload && com.yenaly.han1meviewer.Preferences.relatedVideoAsTab
    val tabs = remember(disableComments, commentBadgeCount, fromDownload, relatedAsTab) {
        buildList {
            add(VideoTabItem(VideoTabKind.INTRODUCTION, R.string.introduction))
            if (!fromDownload && !disableComments) {
                add(VideoTabItem(VideoTabKind.COMMENT, R.string.comment, badgeCount = commentBadgeCount))
            }
            if (relatedAsTab) {
                add(VideoTabItem(VideoTabKind.RELATED, R.string.related_video))
            }
        }
    }

    // 评论预取上提到 tabs 层：评论页不再预组合（见 VideoTabsContent），
    // 这里在进场时即请求评论数据并更新角标，首次切到评论 tab 时数据已就绪。
    // 用 getCommentIfNeeded：全屏切换等导致的子树重组重跑本效应时不重拉
    // （同码已成功加载过即跳过），评论区不再闪加载。
    LaunchedEffect(commentViewModel.code) {
        commentViewModel.getCommentIfNeeded(VIDEO_COMMENT_PREFIX, commentViewModel.code)
        // 角标优先取影片页解析出的评论总数（#tab-comments-count）：
        // 2025-11 改版后评论分批加载（每批 100 条），列表大小只是已加载条数，
        // 大于 100 时用 size 会错误地显示 100。
        launch {
            videoViewModel.hanimeVideoFlow.collect { video ->
                val total = video?.commentCount
                if (total != null && total > 0) {
                    videoViewModel.setCommentBadgeCount(total)
                }
            }
        }
        commentViewModel.videoCommentStateFlow.collect { state ->
            if (state is WebsiteState.Success) {
                commentViewModel.currentUserId = state.info.currentUserId
                // 影片页总数可用时不覆盖；下载播放等无总数的场景退回列表大小
                val pageTotal = videoViewModel.hanimeVideoFlow.value?.commentCount
                if (pageTotal == null || pageTotal <= 0) {
                    videoViewModel.setCommentBadgeCount(state.info.videoComment.size)
                }
            }
        }
    }

    // 發表評論成功後，響應帶新的評論總數，直接更新角標
    LaunchedEffect(commentViewModel.code) {
        commentViewModel.postCommentFlow.collect { state ->
            val count = (state as? WebsiteState.Success)?.info?.commentCount
            if (count != null && count > 0) {
                videoViewModel.setCommentBadgeCount(count)
            }
        }
    }

    VideoScreen(
        state = videoState,
        onRetry = onRetry,
    ) {
        VideoTabsContent(
            tabs = tabs,
            selectedTabIndex = hostUiState.selectedTabIndex,
            onSelectedTabChange = { videoViewModel.setSelectedTabIndex(videoCode, it) },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (tabs.getOrNull(page)?.kind) {
                VideoTabKind.INTRODUCTION -> {
                    RenderVideoIntroductionContent(
                        videoCode = videoCode,
                        viewModel = videoViewModel,
                        pendingDownloadPrompt = pendingDownloadPrompt,
                        onPendingDownloadPromptChange = onPendingDownloadPromptChange,
                        onOpenVideo = onOpenVideo,
                        onOpenArtist = onOpenArtist,
                        onNavigateToSearch = onNavigateToSearch,
                        onToggleSubscribe = onToggleSubscribe,
                        onToggleFavorite = onToggleFavorite,
                        onRateVideo = onRateVideo,
                        onManageMyList = onManageMyList,
                        onQuickCheckIn = onQuickCheckIn,
                        onPrepareDownload = onPrepareDownload,
                        onConfirmDownloadPrompt = onConfirmDownloadPrompt,
                        onRequestOpenOfficialDownloadPage = onRequestOpenOfficialDownloadPage,
                        onRequestOpenDownloadPermissionSettings = onRequestOpenDownloadPermissionSettings,
                        onOpenWebPage = onOpenWebPage,
                        onOpenOriginalComic = onOpenOriginalComic,
                        onOpenShare = onOpenShare,
                        onCopyText = onCopyText,
                        onIntroductionLinkClick = onIntroductionLinkClick,
                        stringLongPressShare = stringLongPressShare,
                    )
                }

                VideoTabKind.RELATED -> {
                    RenderVideoRelatedContent(
                        viewModel = videoViewModel,
                        onOpenVideo = onOpenVideo,
                    )
                }

                else -> {
                    RenderVideoCommentContent(
                        viewModel = commentViewModel,
                        reportMessages = remember { kotlinx.coroutines.flow.MutableSharedFlow() },
                        getMessageText = { message ->
                            if (message.args.isNotEmpty()) {
                                application.getString(message.resId, *message.args.toTypedArray())
                            } else {
                                application.getString(message.resId)
                            }
                        },
                    )
                }
            }
        }
    }
}
