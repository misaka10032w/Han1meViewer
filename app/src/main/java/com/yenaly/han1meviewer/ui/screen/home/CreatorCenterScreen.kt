package com.yenaly.han1meviewer.ui.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.CreatorTab
import com.yenaly.han1meviewer.logic.model.CreatorUploadingItem
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.screen.home.creatorcenter.CreatorCenterEvent
import com.yenaly.han1meviewer.ui.screen.home.creatorcenter.CreatorUploadedPage
import com.yenaly.han1meviewer.ui.screen.home.creatorcenter.CreatorUploadingPage
import com.yenaly.han1meviewer.ui.viewmodel.CreatorCenterViewModel
import kotlinx.coroutines.launch

/**
 * 创作者中心页面 Screen 层。
 *
 * 持有 [CreatorCenterViewModel]，订阅其唯一的 [CreatorCenterViewModel.uiState]，
 * 管理 Tab 切换和帮助弹窗，渲染委托给 Content 组件。
 */
@Composable
fun CreatorCenterScreen(
    viewModel: CreatorCenterViewModel,
    onBack: () -> Unit,
    onOpenUploadedVideo: (HanimeInfo) -> Unit,
    onOpenUploadingVideo: (CreatorUploadingItem) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(if (pagerState.currentPage == 0) CreatorTab.Uploaded else CreatorTab.Uploading)
        if (pagerState.currentPage == 0 && uiState.uploadedItems.isEmpty() && uiState.uploadedPage == 0 && uiState.uploadedState is PageLoadingState.Loading) {
            viewModel.refreshUploaded(uiState.uploadedSort)
        }
        if (pagerState.currentPage == 1 && uiState.uploadingItems.isEmpty() && uiState.uploadingPage == 0 && uiState.uploadingState is PageLoadingState.Loading) {
            viewModel.refreshUploading(uiState.uploadingSort)
        }
    }

    val handleEvent: (CreatorCenterEvent) -> Unit = { event ->
        when (event) {
            CreatorCenterEvent.OnBack -> onBack()
            is CreatorCenterEvent.OnTabChange -> {
                val page = if (event.tab == CreatorTab.Uploaded) 0 else 1
                scope.launch { pagerState.animateScrollToPage(page) }
            }

            is CreatorCenterEvent.OnSortChange -> {
                when (event.tab) {
                    CreatorTab.Uploaded -> viewModel.refreshUploaded(event.sort)
                    CreatorTab.Uploading -> viewModel.refreshUploading(event.sort)
                }
            }

            is CreatorCenterEvent.OnLoadMore -> {
                when (event.tab) {
                    CreatorTab.Uploaded -> viewModel.loadMoreUploaded()
                    CreatorTab.Uploading -> viewModel.loadMoreUploading()
                }
            }

            is CreatorCenterEvent.OnRefresh -> {
                when (event.tab) {
                    CreatorTab.Uploaded -> viewModel.refreshUploaded()
                    CreatorTab.Uploading -> viewModel.refreshUploading()
                }
            }

            is CreatorCenterEvent.OnOpenUploadedVideo -> onOpenUploadedVideo(event.item)
            is CreatorCenterEvent.OnOpenUploadingVideo -> onOpenUploadingVideo(event.item)
        }
    }

    ConfirmDialog(
        visible = showHelpDialog,
        title = stringResource(R.string.help),
        message = stringResource(R.string.creator_center_help),
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.close),
        onConfirm = { showHelpDialog = false },
        onDismiss = { showHelpDialog = false },
    )

    HanimeScaffold(
        title = stringResource(R.string.creator_center),
        onBack = onBack,
        actions = {
            FilledIconButton(onClick = { showHelpDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_help_24),
                    contentDescription = stringResource(R.string.help),
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(R.string.uploaded_videos)) })
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(R.string.uploading_videos)) })
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> CreatorUploadedPage(uiState = uiState, onEvent = handleEvent)
                    else -> CreatorUploadingPage(uiState = uiState, onEvent = handleEvent)
                }
            }
        }
    }
}
