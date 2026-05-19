package com.yenaly.han1meviewer.ui.screen.home.download

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
import com.yenaly.han1meviewer.logic.model.DownloadItemNode
import com.yenaly.han1meviewer.logic.model.DownloadedNode
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadScreen(
    downloadingFlow: Flow<List<HanimeDownloadEntity>>,
    downloadedFlow: StateFlow<List<VideoWithCategories>>,
    downloadedGroupsFlow: StateFlow<List<DownloadGroupEntity>>,
    collapseDownloadedGroup: Boolean,
    onBack: () -> Unit,
    onPauseAll: (List<HanimeDownloadEntity>) -> Unit,
    onResumeAll: (List<HanimeDownloadEntity>) -> Unit,
    onPauseItem: (HanimeDownloadEntity) -> Unit,
    onResumeItem: (HanimeDownloadEntity) -> Unit,
    onDeleteDownloadingItem: (HanimeDownloadEntity) -> Unit,
    onImportDownloaded: () -> Unit,
    onLoadDownloaded: () -> Unit,
    onOpenDownloadedVideo: (VideoWithCategories) -> Unit,
    onLocalPlayback: (VideoWithCategories) -> Unit,
    onExternalPlayback: (VideoWithCategories) -> Unit,
    onDeleteDownloadedVideo: (VideoWithCategories) -> Unit,
    onMoveVideoGroup: (VideoWithCategories, Int) -> Unit,
    onRenameGroup: (Int, String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onDeleteGroup: (DownloadGroupEntity) -> Unit,
) {
    val downloadingItems by downloadingFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val downloadedItems by downloadedFlow.collectAsStateWithLifecycle()
    val downloadedGroups by downloadedGroupsFlow.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var downloadedHeaderNodes by remember { mutableStateOf<List<DownloadHeaderNode>>(emptyList()) }

    val displayGroups = downloadedGroups.map { group ->
        if (group.id == DownloadGroupEntity.DEFAULT_GROUP_ID) {
            group.copy(name = stringResource(R.string.ungrouped))
        } else {
            group
        }
    }

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

    LaunchedEffect(Unit) {
        onLoadDownloaded()
    }

    HanimeScaffold(
        title = stringResource(R.string.download),
        onBack = onBack,
        actions = {
            if (pagerState.currentPage == 0) {
                FilledIconButton(
                    onClick = { onResumeAll(downloadingItems) },
                    enabled = downloadingItems.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_play_arrow_24),
                        contentDescription = stringResource(R.string.start_all),
                    )
                }
                FilledIconButton(
                    onClick = { onPauseAll(downloadingItems) },
                    enabled = downloadingItems.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_pause_24),
                        contentDescription = stringResource(R.string.pause_all),
                    )
                }
            } else {
                FilledIconButton(onClick = { showCreateGroupDialog = true }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_add_24),
                        contentDescription = stringResource(R.string.create_new_group),
                    )
                }
                FilledIconButton(onClick = onImportDownloaded) {
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
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(R.string.downloading)) },
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
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
                        items = downloadingItems,
                        onPauseItem = onPauseItem,
                        onResumeItem = onResumeItem,
                        onDeleteItem = onDeleteDownloadingItem,
                    )

                    else -> DownloadedScreen(
                        nodes = downloadedNodes,
                        groups = displayGroups,
                        showCreateGroupDialog = showCreateGroupDialog,
                        onToggleGroup = { header ->
                            downloadedHeaderNodes = downloadedHeaderNodes.map {
                                if (it.groupKey == header.groupKey) {
                                    it.copy(isExpanded = !it.isExpanded)
                                } else {
                                    it
                                }
                            }
                        },
                        onHeaderLongClick = {},
                        onOpenVideo = onOpenDownloadedVideo,
                        onLocalPlayback = onLocalPlayback,
                        onExternalPlayback = onExternalPlayback,
                        onDeleteVideo = onDeleteDownloadedVideo,
                        onMoveVideoGroup = onMoveVideoGroup,
                        onRenameGroup = onRenameGroup,
                        onCreateGroup = onCreateGroup,
                        onDeleteGroup = onDeleteGroup,
                        onCreateGroupDialogChange = { showCreateGroupDialog = it },
                    )
                }
            }
        }
    }
}

private fun List<VideoWithCategories>.toNodeList(
    groupIdToNameMap: Map<Int, String>,
    collapseDownloadedGroup: Boolean,
): List<DownloadHeaderNode> {
    val groupedData = this.groupBy { it.video.groupId }.toSortedMap()
    return buildList {
        for ((groupId, videos) in groupedData) {
            add(
                DownloadHeaderNode(
                    groupKey = groupIdToNameMap[groupId] ?: "ID: $groupId",
                    originalVideos = videos,
                    isExpanded = !collapseDownloadedGroup,
                )
            )
        }
    }
}

private fun List<DownloadHeaderNode>.toFlatNodeList(): List<DownloadedNode> {
    val flatList = mutableListOf<DownloadedNode>()
    for (header in this) {
        flatList.add(header)
        if (header.isExpanded) {
            header.originalVideos.forEach { video ->
                flatList.add(DownloadItemNode(video, header.groupKey))
            }
        }
    }
    return flatList
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
            onPauseAll = {},
            onResumeAll = {},
            onPauseItem = {},
            onResumeItem = {},
            onDeleteDownloadingItem = {},
            onImportDownloaded = {},
            onLoadDownloaded = {},
            onOpenDownloadedVideo = {},
            onLocalPlayback = {},
            onExternalPlayback = {},
            onDeleteDownloadedVideo = {},
            onMoveVideoGroup = { _, _ -> },
            onRenameGroup = { _, _ -> },
            onCreateGroup = {},
            onDeleteGroup = {},
        )
    }
}
