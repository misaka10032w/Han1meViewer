package com.yenaly.han1meviewer.ui.screen.home.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.state.DownloadState
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos

/**
 * 下载中 Tab 页面（Content 层）。
 *
 * 接收 [DownloadUiState] + [DownloadEvent] 回调，不持有 ViewModel。
 *
 * @param uiState 页面 UI 状态
 * @param onEvent 用户交互事件回调
 */
@Composable
fun DownloadingScreen(
    uiState: DownloadUiState,
    onEvent: (DownloadEvent) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<HanimeDownloadEntity?>(null) }

    ConfirmDialog(
        visible = pendingDelete != null,
        title = stringResource(R.string.sure_to_delete),
        message = stringResource(R.string.prepare_to_delete_s, pendingDelete?.title.orEmpty()),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            pendingDelete?.let { onEvent(DownloadEvent.OnDeleteDownloadingItem(it)) }
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null },
    )

    if (uiState.downloadingItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyContent(
                hint = stringResource(R.string.empty_content)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.downloadingItems, key = { it.id }) { item ->
                DownloadingItemCard(
                    item = item,
                    onPause = { onEvent(DownloadEvent.OnPauseItem(item)) },
                    onResume = { onEvent(DownloadEvent.OnResumeItem(item)) },
                    onDelete = { pendingDelete = item },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun DownloadingScreenPreview() {
    val items = listOf(
        HanimeDownloadEntity(
            coverUrl = fakeHomePageVideos.first().coverUrl,
            coverUri = null,
            title = fakeHomePageVideos.first().title,
            addDate = System.currentTimeMillis(),
            videoCode = fakeHomePageVideos.first().videoCode,
            videoUri = "sample.mp4",
            quality = "720P",
            videoUrl = "https://example.com/sample.mp4",
            length = 100L * 1024 * 1024,
            downloadedLength = 45L * 1024 * 1024,
            state = DownloadState.Downloading,
            id = 1,
        )
    )
    ComponentPreview {
        DownloadingScreen(
            uiState = DownloadUiState(downloadingItems = items),
            onEvent = {},
        )
    }
}
