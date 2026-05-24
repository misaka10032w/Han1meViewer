package com.yenaly.han1meviewer.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadEvent
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadUiState
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadedScreen
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadingScreen
import com.yenaly.han1meviewer.ui.screen.home.download.toDisplayGroups
import com.yenaly.han1meviewer.ui.screen.home.download.toFlatNodeList
import com.yenaly.han1meviewer.ui.screen.home.download.toNodeList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * 下载页面 Screen 层。
 *
 * 接收数据流和 [DownloadEvent] 回调，管理 Tab、分组展开等 UI 编排状态。
 * 本地 UI 事件（Tab 切换、分组折叠）由 Screen 处理；业务事件透传给 Route。
 * Content 组件仅接收 UiState + Event 回调。
 *
 * @param downloadingFlow 下载中任务流
 * @param downloadedFlow 已下载视频流
 * @param downloadedGroupsFlow 分组列表流
 * @param collapseDownloadedGroup 默认折叠分组
 * @param onBack 返回回调
 * @param onLoadDownloaded 初始加载已下载列表（一次性调用）
 * @param onEvent 业务事件回调（透传给 Route 层处理）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadScreen(
    downloadingFlow: Flow<List<HanimeDownloadEntity>>,
    downloadedFlow: StateFlow<List<VideoWithCategories>>,
    downloadedGroupsFlow: StateFlow<List<DownloadGroupEntity>>,
    collapseDownloadedGroup: Boolean,
    onBack: () -> Unit,
    onLoadDownloaded: () -> Unit,
    onEvent: (DownloadEvent) -> Unit,
) {
    val downloadingItems by downloadingFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val downloadedItems by downloadedFlow.collectAsStateWithLifecycle()
    val downloadedGroups by downloadedGroupsFlow.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var downloadedHeaderNodes by remember { mutableStateOf<List<DownloadHeaderNode>>(emptyList()) }

    val displayGroups = downloadedGroups.toDisplayGroups()

    val downloadedNodes =
        remember(downloadedItems, displayGroups, collapseDownloadedGroup, downloadedHeaderNodes) {
            val groupIdToNameMap = displayGroups.associate { it.id to it.name }
            if (downloadedItems.isEmpty()) {
                downloadedHeaderNodes = emptyList()
                emptyList()
            } else {
                val newHeaders =
                    downloadedItems.toNodeList(groupIdToNameMap, collapseDownloadedGroup)
                val oldExpandedByKey =
                    downloadedHeaderNodes.associate { it.groupKey to it.isExpanded }
                downloadedHeaderNodes = newHeaders.map { newHeader ->
                    newHeader.copy(
                        isExpanded = oldExpandedByKey[newHeader.groupKey]
                            ?: !collapseDownloadedGroup
                    )
                }
                downloadedHeaderNodes.toFlatNodeList()
            }
        }

    val uiState = DownloadUiState(
        downloadingItems = downloadingItems,
        downloadedNodes = downloadedNodes,
        displayGroups = displayGroups,
        currentPage = pagerState.currentPage,
        showCreateGroupDialog = showCreateGroupDialog,
    )

    val handleEvent: (DownloadEvent) -> Unit = { event ->
        when (event) {
            // 本地 UI 事件：Screen 自行处理
            is DownloadEvent.OnToggleGroup -> {
                downloadedHeaderNodes = downloadedHeaderNodes.map {
                    if (it.groupKey == event.groupKey) {
                        it.copy(isExpanded = !it.isExpanded)
                    } else {
                        it
                    }
                }
            }
            is DownloadEvent.OnCreateGroupDialogChange -> showCreateGroupDialog = event.visible
            is DownloadEvent.OnPageChange -> {
                scope.launch { pagerState.animateScrollToPage(event.page) }
            }
            // 业务事件：透传给 Route
            else -> onEvent(event)
        }
    }

    LaunchedEffect(Unit) {
        onLoadDownloaded()
    }

    HanimeScaffold(
        title = stringResource(R.string.download),
        onBack = onBack,
        actions = {
            if (uiState.currentPage == 0) {
                FilledIconButton(
                    onClick = { handleEvent(DownloadEvent.OnResumeAll(uiState.downloadingItems)) },
                    enabled = uiState.downloadingItems.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_play_arrow_24),
                        contentDescription = stringResource(R.string.start_all),
                    )
                }
                FilledIconButton(
                    onClick = { handleEvent(DownloadEvent.OnPauseAll(uiState.downloadingItems)) },
                    enabled = uiState.downloadingItems.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_pause_24),
                        contentDescription = stringResource(R.string.pause_all),
                    )
                }
            } else {
                FilledIconButton(
                    onClick = { handleEvent(DownloadEvent.OnCreateGroupDialogChange(true)) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_add_24),
                        contentDescription = stringResource(R.string.create_new_group),
                    )
                }
                FilledIconButton(
                    onClick = { handleEvent(DownloadEvent.OnImportDownloaded) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_download_24),
                        contentDescription = stringResource(R.string.read_download_dir_title),
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(selectedTabIndex = uiState.currentPage) {
                Tab(
                    selected = uiState.currentPage == 0,
                    onClick = { handleEvent(DownloadEvent.OnPageChange(0)) },
                    text = { Text(stringResource(R.string.downloading)) },
                )
                Tab(
                    selected = uiState.currentPage == 1,
                    onClick = { handleEvent(DownloadEvent.OnPageChange(1)) },
                    text = { Text(stringResource(R.string.downloaded)) },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> DownloadingScreen(
                        uiState = uiState,
                        onEvent = handleEvent,
                    )

                    else -> DownloadedScreen(
                        uiState = uiState,
                        onEvent = handleEvent,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadScreenPreview() {
    ComponentPreview {
        DownloadScreen(
            downloadingFlow = flowOf(emptyList()),
            downloadedFlow = MutableStateFlow(emptyList()),
            downloadedGroupsFlow = MutableStateFlow(emptyList()),
            collapseDownloadedGroup = false,
            onBack = {},
            onLoadDownloaded = {},
            onEvent = {},
        )
    }
}
