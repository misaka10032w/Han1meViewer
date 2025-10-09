package com.yenaly.han1meviewer.ui.fragment.home.myplaylist

import android.view.View
import android.widget.EditText
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.fragment.EmptyView
import com.yenaly.han1meviewer.ui.fragment.PlaylistItem
import com.yenaly.han1meviewer.ui.fragment.getColumnCount
import com.yenaly.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.utils.showShortToast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MyPlayListScreen(
    viewModel: MyPlayListViewModelV2,
    navigateBack: () -> Unit,
    onClickItem: (String) -> Unit,
    onLongClickItem: (String, String) -> Unit,
){
    val state by viewModel.myPlaylistsFlow.collectAsState()
    val playlists by viewModel.cachedMyPlayList.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    val showSheet by viewModel.showSheet.collectAsState()
    val selectedListCode = remember { mutableStateOf("") }
    val listTitle = remember { mutableStateOf("") }
    val context = LocalContext.current

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.loadMyPlayList(forceReload = true)
    }

    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(refreshState.distanceFraction).coerceIn(0f, 1f)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshCompleted.collect {
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        if (playlists.isEmpty()) {
            viewModel.loadMyPlayList()
        }
    }
    // 收集添加列表结果
    LaunchedEffect(Unit) {
        viewModel.createPlaylistFlow.collect { result ->
            when (result) {
                is WebsiteState.Error -> {
                    showShortToast(R.string.add_failed)
                }

                is WebsiteState.Loading -> Unit
                is WebsiteState.Success -> {
                    showShortToast(R.string.add_success)
                    viewModel.loadMyPlayList()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text(
                        stringResource(R.string.my_list),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back button",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = {
                        context.showAlertDialog {
                            setTitle(R.string.create_new_playlist)
                            val etView = View.inflate(context, R.layout.dialog_playlist_modify_edit_text, null)
                            val etTitle = etView.findViewById<EditText>(R.id.et_title)
                            val etDesc = etView.findViewById<EditText>(R.id.et_desc)
                            setView(etView)
                            setPositiveButton(R.string.confirm) { _, _ ->
                                viewModel.createPlaylist(
                                    etTitle.text.toString(),
                                    etDesc.text.toString()
                                )
                            }
                            setNegativeButton(R.string.cancel, null)

                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.create_new_playlist),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pullToRefresh(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val result = state) {
                is WebsiteState.Loading -> {
                    if (playlists.isEmpty()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else {
                        // 显示旧数据，刷新中不替换内容
                        AnimatedPageContent(
                            state,
                            playlists = playlists
                        ){ listCode, playListTitle ->
                            selectedListCode.value = listCode
                            viewModel.setShowSheet(true)
                            listTitle.value = playListTitle
                        }
                    }
                }

                is WebsiteState.Error -> {
                    if (playlists.isEmpty()) {
                        Text(
                            "加载失败: ${result.throwable.message}",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        // 显示旧缓存内容（保持体验）
                        AnimatedPageContent(
                            state,
                            playlists = playlists
                        ){ listCode, playListTitle ->
                            selectedListCode.value = listCode
                            viewModel.setShowSheet(true)
                            listTitle.value = playListTitle
                        }
                    }
                }

                else -> {
                    // 统一渲染缓存（成功后缓存已更新）
                    AnimatedPageContent(
                        state,
                        playlists = playlists
                    ) { listCode, playListTitle ->
                        selectedListCode.value = listCode
                        viewModel.setShowSheet(true)
                        listTitle.value = playListTitle
                    }
                }
            }

            if (isRefreshing || scaleFraction() > 0f) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            scaleX = scaleFraction()
                            scaleY = scaleFraction()
                        }
                        .zIndex(1f)
                ) {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = refreshState,
                        isRefreshing = isRefreshing
                    )
                }
            }
            if (showSheet) {
                PlaylistBottomSheet(
                    listCode = selectedListCode.value,
                    onDismiss = {
                        viewModel.setShowSheet(false)
                        viewModel.currentPage = 1
                                },
                    playListTitle = listTitle.value,
                    onClickItem = onClickItem ,
                    onLongClickItem = onLongClickItem,
                    vm = viewModel,
                    context = context
                )
            }
        }
    }
}

@Composable
fun AnimatedPageContent(
    state: WebsiteState<Playlists>,
    playlists: List<Playlists.Playlist>,
    onRetry: () -> Unit = {},
    onPlaylistClick: (listCode: String, playListTitle: String) -> Unit
) {

    AnimatedContent(
        targetState = state,
        label = "my-playlist-content-animation",
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(200))
        }
    ) { target ->
        when (target) {
            is WebsiteState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is WebsiteState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${stringResource(R.string.unknow)}: ${target.throwable.message}")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            is WebsiteState.Success -> {
                if (target.info.playlists.isEmpty()){
                    EmptyView(stringResource(R.string.empty_content))
                    return@AnimatedContent
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(getColumnCount(180)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(playlists) { playlist ->
                        // 播放清单卡片
                        PlaylistItem(
                            playlist = playlist,
                            modifier = Modifier.height(140.dp)
                        ) {
                            onPlaylistClick(playlist.listCode, playlist.title)
                        }
                    }
                }
            }
        }
    }
}