package com.yenaly.han1meviewer.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadEvent
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadUiState
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadedScreen
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadingScreen
import com.yenaly.han1meviewer.ui.screen.home.download.MoveGroupDialog
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
    var downloadedGroupExpandedState by rememberSaveable(stateSaver = stringBooleanMapSaver()) {
        mutableStateOf(emptyMap())
    }
    val downloadedLazyListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
    val scope = rememberCoroutineScope()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var downloadedHeaderNodes by remember { mutableStateOf<List<DownloadHeaderNode>>(emptyList()) }
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedVideoIds by remember { mutableStateOf(setOf<Int>()) }
    var pendingBatchMove by remember { mutableStateOf(false) }
    var pendingBatchMoveConfirm by remember { mutableStateOf<Pair<List<VideoWithCategories>, Int>?>(null) }

    val displayGroups = downloadedGroups.toDisplayGroups()

    val downloadedNodes =
        remember(downloadedItems, displayGroups, collapseDownloadedGroup, downloadedHeaderNodes, downloadedGroupExpandedState) {
            val groupIdToNameMap = displayGroups.associate { it.id to it.name }
            if (downloadedItems.isEmpty()) {
                downloadedHeaderNodes = emptyList()
                emptyList()
            } else {
                val newHeaders =
                    downloadedItems.toNodeList(groupIdToNameMap, collapseDownloadedGroup)
                downloadedHeaderNodes = newHeaders.map { newHeader ->
                    newHeader.copy(
                        isExpanded = downloadedGroupExpandedState[newHeader.groupKey]
                            ?: downloadedHeaderNodes.firstOrNull { it.groupKey == newHeader.groupKey }?.isExpanded
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
        multiSelectMode = multiSelectMode,
        selectedVideoIds = selectedVideoIds,
    )

    val handleEvent: (DownloadEvent) -> Unit = { event ->
        when (event) {
            // 本地 UI 事件：Screen 自行处理
            is DownloadEvent.OnToggleGroup -> {
                downloadedHeaderNodes = downloadedHeaderNodes.map {
                    if (it.groupKey == event.groupKey) {
                        val expanded = !it.isExpanded
                        downloadedGroupExpandedState = downloadedGroupExpandedState + (it.groupKey to expanded)
                        it.copy(isExpanded = expanded)
                    } else {
                        it
                    }
                }
            }
            is DownloadEvent.OnCreateGroupDialogChange -> showCreateGroupDialog = event.visible
            is DownloadEvent.OnPageChange -> {
                scope.launch { pagerState.animateScrollToPage(event.page) }
            }
            // 多选事件：Screen 自行处理
            is DownloadEvent.OnToggleMultiSelect -> {
                multiSelectMode = !multiSelectMode
                if (!multiSelectMode) selectedVideoIds = emptySet()
            }
            is DownloadEvent.OnToggleVideoSelection -> {
                selectedVideoIds = if (event.videoId in selectedVideoIds) {
                    selectedVideoIds - event.videoId
                } else {
                    selectedVideoIds + event.videoId
                }
            }
            is DownloadEvent.OnSelectAllCurrentGroup -> {
                val groupVideos = downloadedNodes.filterIsInstance<com.yenaly.han1meviewer.logic.model.DownloadItemNode>()
                    .filter { it.parentKey == event.groupKey }
                selectedVideoIds = if (event.select) {
                    selectedVideoIds + groupVideos.map { it.data.video.id }.toSet()
                } else {
                    selectedVideoIds - groupVideos.map { it.data.video.id }.toSet()
                }
            }
            is DownloadEvent.OnBatchMoveRequest -> { pendingBatchMove = true }
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
                if (!uiState.multiSelectMode) {
                    FilledIconButton(
                        onClick = { handleEvent(DownloadEvent.OnToggleMultiSelect) }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_format_list_bulleted_24),
                            contentDescription = stringResource(R.string.edit),
                        )
                    }
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
                        listState = downloadedLazyListState,
                        onEvent = handleEvent,
                    )
                }
            }
        }
    }

    if (pendingBatchMove) {
        val selectedVideos = downloadedNodes
            .filterIsInstance<com.yenaly.han1meviewer.logic.model.DownloadItemNode>()
            .filter { it.data.video.id in selectedVideoIds }
            .map { it.data }
        if (selectedVideos.isNotEmpty()) {
            MoveGroupDialog(
                video = selectedVideos.first(),
                groups = displayGroups,
                onDismiss = { pendingBatchMove = false },
                onConfirm = { _, groupId ->
                    pendingBatchMoveConfirm = Pair(selectedVideos, groupId)
                    pendingBatchMove = false
                },
            )
        } else {
            pendingBatchMove = false
        }
    }

    pendingBatchMoveConfirm?.let { (videos, groupId) ->
        val groupName = displayGroups.find { it.id == groupId }?.name ?: "ID:$groupId"
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.move_group),
            message = stringResource(R.string.confirm_move_videos,videos.size,groupName),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                handleEvent(DownloadEvent.OnBatchMoveGroup(videos, groupId))
                pendingBatchMoveConfirm = null
                multiSelectMode = false
                selectedVideoIds = emptySet()
            },
            onDismiss = { pendingBatchMoveConfirm = null },
        )
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

private fun stringBooleanMapSaver(): Saver<Map<String, Boolean>, ArrayList<String>> {
    return Saver(
        save = { state -> ArrayList(state.map { (key, value) -> "$key=$value" }) },
        restore = { saved ->
            saved.mapNotNull { item ->
                val separatorIndex = item.lastIndexOf('=')
                if (separatorIndex <= 0) return@mapNotNull null
                val key = item.substring(0, separatorIndex)
                val value = item.substring(separatorIndex + 1).toBooleanStrictOrNull()
                    ?: return@mapNotNull null
                key to value
            }.toMap()
        },
    )
}
