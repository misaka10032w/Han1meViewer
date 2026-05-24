package com.yenaly.han1meviewer.ui.screen.home.myplaylist

import android.content.Context
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberBottomSheetState
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
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import com.yenaly.han1meviewer.ui.theme.VideoNormalCardMinWidth
import com.yenaly.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.utils.showShortToast

/**
 * 播放列表详情底部弹窗。
 *
 * @param listCode 播放列表代码
 * @param onDismiss 关闭回调
 * @param playListTitle 播放列表标题
 * @param onClickItem 点击视频项回调
 * @param onLongClickItem 长按视频项回调
 * @param vm 播放列表 ViewModel（弹窗内需要直接观察 ViewModel StateFlow）
 * @param context Android Context
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBottomSheet(
    listCode: String,
    onDismiss: () -> Unit,
    playListTitle: String,
    onClickItem: (String) -> Unit,
    onLongClickItem: (String, String) -> Unit,
    vm: MyPlayListViewModelV2,
    context: Context,
) {
    val playlistState by vm.playlistStateFlow.collectAsState()
    val playlist by vm.playlistFlow.collectAsState()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
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

    LaunchedEffect(Unit) { sheetState.show() }

    LaunchedEffect(gridState, currentCode) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                vm.updatePlaylistSheetScrollState(currentCode, index, offset)
            }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = null) {
        if (playlist.isEmpty() && playlistState is PageLoadingState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (playlist.isEmpty() && playlistState is PageLoadingState.Error) {
            Box(Modifier
                .fillMaxSize()
                .height(200.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.load_failed_retry))
            }
        } else {
            AnimatedVisibility(visible = true, enter = fadeIn()) {
                PlaylistSheetContent(
                    gridState = gridState,
                    listCode = currentCode,
                    playlist = playlist,
                    onDismiss = onDismiss,
                    playListTitle = currentTitle,
                    playlistDesc = vm.playlistDesc,
                    playlistState = playlistState,
                    onClickItem = onClickItem,
                    onLongClickItem = onLongClickItem,
                    vm = vm,
                    context = context,
                )
                if (playlist.isEmpty()) {
                    EmptyContent(stringResource(R.string.empty_content))
                }
            }
        }
    }

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

    LaunchedEffect(Unit) {
        vm.deleteFromPlaylistFlow.collect { result ->
            when (result) {
                is WebsiteState.Error -> showShortToast(R.string.delete_failed)
                is WebsiteState.Loading -> {}
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
private fun PlaylistSheetContent(
    gridState: LazyGridState,
    listCode: String,
    playlist: List<HanimeInfo>,
    onDismiss: () -> Unit,
    playListTitle: String,
    playlistDesc: kotlinx.coroutines.flow.StateFlow<String?>,
    playlistState: PageLoadingState<*>,
    onClickItem: (String) -> Unit,
    onLongClickItem: (String, String) -> Unit,
    vm: MyPlayListViewModelV2,
    context: Context,
) {
    var showDeletePlaylistConfirm by remember { mutableStateOf(false) }
    var showDeleteItemConfirm by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    val desc by playlistDesc.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(Modifier
            .fillMaxWidth()
            .height(240.dp)) {
            BottomSheetHandler()

            if (playlist.isNotEmpty()) {
                RetryableImage(
                    model = playlist.first().coverUrl,
                    contentDescription = playlist.first().title,
                    placeholder = painterResource(R.drawable.h_chan_loading),
                    error = painterResource(R.drawable.h_chan_load_failed),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            ),
                            startY = 0f, endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            TopAppBar(
                title = { Text(playListTitle, color = Color.White) },
                colors = topAppBarColors(containerColor = Color.Transparent)
            )

            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier
                        .weight(1f)
                        .padding(start = 8.dp)) {
                        Text(
                            desc ?: "",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = { showDeletePlaylistConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_delete_24),
                            stringResource(R.string.delete),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            context.showAlertDialog {
                                setTitle(R.string.modify_title_or_desc)
                                val etView = LayoutInflater.from(context)
                                    .inflate(R.layout.dialog_playlist_modify_edit_text, null)
                                val etTitle = etView.findViewById<EditText>(R.id.et_title)
                                val etDesc = etView.findViewById<EditText>(R.id.et_desc)
                                etTitle.setText(playListTitle)
                                etDesc.setText(desc)
                                setView(etView)
                                setPositiveButton(R.string.confirm) { _, _ ->
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
                            painterResource(R.drawable.baseline_edit_24),
                            stringResource(R.string.edit),
                            tint = Color.Red
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = maxOf(2, (maxWidth / VideoNormalCardMinWidth).toInt())

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = VideoNormalCardMinWidth),
                contentPadding = PaddingValues(SpacingNormal),
                horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
                verticalArrangement = Arrangement.spacedBy(SpacingNormal)
            ) {
                itemsIndexed(playlist) { index, item ->
                    VideoCardItem(
                        videoItem = item,
                        isHorizontalCard = true,
                        onClickVideosItem = onClickItem
                    ) { videoCode, _ ->
                        showDeleteItemConfirm = Triple(listCode, videoCode, index)
                    }
                }

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

                if (playlistState is PageLoadingState.NoMoreData && playlist.isNotEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(
                                    R.string.load_complete_with_pages,
                                    vm.currentPage - 1
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }

            val currentListCode = listCode
            LaunchedEffect(gridState, playlistState) {
                snapshotFlow { gridState.layoutInfo }.collect { layoutInfo ->
                    val totalItems = layoutInfo.totalItemsCount
                    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (lastVisibleItem >= totalItems - 3 &&
                        playlistState !is PageLoadingState.Loading &&
                        playlistState !is PageLoadingState.NoMoreData &&
                        !vm.isLoadingMore
                    ) {
                        vm.currentPage++
                        vm.getPlaylistItems(vm.currentPage, currentListCode)
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
                        vm.deleteFromPlaylist(
                            code,
                            videoCode,
                            index
                        ); showDeleteItemConfirm = null
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
                    vm.modifyPlaylist(
                        listCode,
                        playListTitle,
                        desc.orEmpty(),
                        true
                    ); showDeletePlaylistConfirm = false
                },
                onDismiss = { showDeletePlaylistConfirm = false },
            )
        }
    }
}
