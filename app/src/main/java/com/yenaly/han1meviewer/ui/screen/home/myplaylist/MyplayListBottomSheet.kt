package com.yenaly.han1meviewer.ui.screen.home.myplaylist

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.BottomSheetHandler
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBottomSheet(
    listCode: String,
    onDismiss: () -> Unit,
    playListTitle: String,
    onClickItem: (String) -> Unit,
    onLongClickItem: (String, String) -> Unit,
    vm: MyPlayListViewModelV2,
    context: Context
) {
    val playlistState by vm.playlistStateFlow.collectAsState()
    val playlist by vm.playlistFlow.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    if (listCode.isNotEmpty()) {
        vm.setListInfo(listCode, playListTitle)
    }

    val listInfo by vm.currentListInfo.collectAsState()
    val currentCode = listInfo?.first ?: ""
    val currentTitle = listInfo?.second ?: ""
    val savedScrollState = remember(currentCode, vm) {
        vm.getPlaylistSheetScrollState(currentCode)
    }
    val gridState = remember(currentCode) {
        LazyGridState(
            firstVisibleItemIndex = savedScrollState.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = savedScrollState.firstVisibleItemScrollOffset,
        )
    }

    LaunchedEffect(currentCode) {
        if (currentCode.isNotEmpty()) {
            if (playlist.isEmpty()) {
                vm.getPlaylistItems(1, currentCode, true)
            }
        } else {
            showShortToast(R.string.unknown_error)
        }
    }

    LaunchedEffect(Unit) {
        sheetState.show()
    }

    LaunchedEffect(gridState, currentCode) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            vm.updatePlaylistSheetScrollState(
                listCode = currentCode,
                firstVisibleItemIndex = index,
                firstVisibleItemScrollOffset = offset,
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        if (playlist.isEmpty() && playlistState is PageLoadingState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (playlist.isEmpty() && playlistState is PageLoadingState.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.load_failed_retry))
            }
        } else {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn()
            ) {
                PlaylistDetailContent(
                    gridState,
                    currentCode,
                    playlist,
                    onDismiss = onDismiss,
                    currentTitle,
                    vm.playlistDesc,
                    onClickItem = onClickItem,
                    onLongClickItem,
                    vm,
                    context
                )
                if (playlist.isEmpty()) {
                    EmptyContent(stringResource(R.string.empty_content))
                }
            }
        }
    }
//    LaunchedEffect(sheetState) {
//        sheetState.partialExpand()
//    }
    // 收集修改playlist结果
    LaunchedEffect(Unit) {
        vm.modifyPlaylistFlow.collect { result ->
            when (result) {
                is WebsiteState.Error -> showShortToast(R.string.modify_failed)
                WebsiteState.Loading -> {}
                is WebsiteState.Success -> {
                    if (result.info.isDeleted) {
                        sheetState.hide()
                        onDismiss()
                        showShortToast(R.string.delete_success)
                        vm.loadMyPlayList()
                        return@collect
                    }
                    showShortToast(R.string.modify_success)
                    vm.getPlaylistItems(1, currentCode, true)
                    vm.loadMyPlayList()
                }
            }
        }
    }
    // 收集详情页删除视频的结果
    LaunchedEffect(Unit) {
        vm.deleteFromPlaylistFlow.collect { result ->
            when (result) {
                is WebsiteState.Error -> {
                    showShortToast(R.string.delete_failed)
                }

                is WebsiteState.Loading -> {
                }

                is WebsiteState.Success -> {
                    showShortToast(R.string.delete_success)
                    vm.loadMyPlayList()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailContent(
    gridState: LazyGridState,
    listCode: String,
    playlist: List<HanimeInfo>,
    onDismiss: () -> Unit,
    playListTitle: String,
    playlistDesc: StateFlow<String?>,
    onClickItem: (String) -> Unit,
    onLongClickItem: (String, String) -> Unit,
    vm: MyPlayListViewModelV2,
    context: Context,
    onlyEdit: Boolean = false
) {
    val playlistState by vm.playlistStateFlow.collectAsState()
    var showDeletePlaylistConfirm by remember { mutableStateOf(false) }
    var showDeleteItemConfirm by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部图片区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            // 自制bottomSheet把手
            BottomSheetHandler()

            if (playlist.isNotEmpty() && !onlyEdit) {
                // banner 背景图
                RetryableImage(
                    model = playlist.first().coverUrl,
                    contentDescription = playlist.first().title,
                    placeholder = painterResource(R.drawable.akarin),
                    error = painterResource(R.drawable.baseline_error_outline_24),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 渐变覆盖
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // 顶部标题栏
            TopAppBar(
                title = { Text(playListTitle, color = Color.White) },
                colors = topAppBarColors(containerColor = Color.Transparent)
            )

            // 图片下方描述栏
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = playlistDesc.value ?: "",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // 删除按钮
                    Button(
                        onClick = { showDeletePlaylistConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_delete_24),
                            contentDescription = stringResource(R.string.delete),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    // 编辑按钮
                    Button(
                        onClick = {
                            context.showAlertDialog {
                                setTitle(R.string.modify_title_or_desc)
                                val etView =
                                    LayoutInflater.from(context)
                                        .inflate(R.layout.dialog_playlist_modify_edit_text, null)
                                val etTitle = etView.findViewById<EditText>(R.id.et_title)
                                val etDesc = etView.findViewById<EditText>(R.id.et_desc)
                                etTitle.setText(playListTitle)
                                etDesc.setText(playlistDesc.value)
                                setView(etView)
                                setPositiveButton(R.string.confirm) { _, _ ->
                                    Log.i(
                                        "modify_playlist",
                                        "${listCode},${etTitle.text},${etDesc.text}"
                                    )
                                    vm.modifyPlaylist(
                                        listCode,
                                        etTitle.text.toString(),
                                        etDesc.text.toString(),
                                        false
                                    )
                                }
                                setNegativeButton(R.string.cancel, null)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.7f)),
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_edit_24),
                            contentDescription = stringResource(R.string.edit),
                            tint = Color.Red
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 视频列表
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sheetWidth = maxWidth
            val cardWidth = dimensionResource(id = R.dimen.video_cover_width)
            val columns = maxOf(2, (sheetWidth / cardWidth).toInt())

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(playlist) { index, item ->
                    VideoCardItem(
                        item,
                        isHorizontalCard = true,
                        onClickItem,
                    ) { videoCode, _ ->
                        showDeleteItemConfirm = Triple(listCode, videoCode, index)
                    }
                }

                // 加载中占位
                item(span = { GridItemSpan(columns) }) {
                    if (playlistState is PageLoadingState.Loading && vm.currentPage > 1) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // 没有更多数据提示
                if (playlistState is PageLoadingState.NoMoreData && playlist.isNotEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.load_complete_with_pages,
                                    vm.currentPage - 1
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // 加载更多
            LaunchedEffect(gridState) {
                snapshotFlow { gridState.layoutInfo }
                    .collect { layoutInfo ->
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        // 滑到倒数第3个时触发加载下一页
                        if (lastVisibleItem >= totalItems - 3 &&
                            playlistState !is PageLoadingState.Loading &&
                            playlistState !is PageLoadingState.NoMoreData &&
                            !vm.isLoadingMore
                        ) {
                            vm.currentPage++
                            vm.getPlaylistItems(vm.currentPage, listCode)
                        }
                    }
            }

            showDeleteItemConfirm?.let { (code, videoCode, index) ->
                val item = playlist.find { it.videoCode == videoCode }
                ConfirmDialog(
                    visible = true,
                    title = context.getString(R.string.delete_playlist),
                    message = context.getString(R.string.sure_to_delete_s, item?.title ?: ""),
                    confirmText = context.getString(R.string.confirm),
                    dismissText = context.getString(R.string.cancel),
                    onConfirm = {
                        vm.deleteFromPlaylist(code, videoCode, index)
                        showDeleteItemConfirm = null
                    },
                    onDismiss = { showDeleteItemConfirm = null },
                )
            }

            ConfirmDialog(
                visible = showDeletePlaylistConfirm,
                title = context.getString(R.string.delete_the_playlist),
                message = context.getString(R.string.sure_to_delete),
                confirmText = context.getString(R.string.confirm),
                dismissText = context.getString(R.string.cancel),
                onConfirm = {
                    vm.modifyPlaylist(listCode, playListTitle, playlistDesc.value.orEmpty(), true)
                    showDeletePlaylistConfirm = false
                },
                onDismiss = { showDeletePlaylistConfirm = false },
            )
        }
    }
}