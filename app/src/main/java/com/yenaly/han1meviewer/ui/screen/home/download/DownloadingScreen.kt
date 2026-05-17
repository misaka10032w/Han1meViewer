package com.yenaly.han1meviewer.ui.screen.home.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.state.DownloadState
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.yenaly_libs.utils.formatFileSizeV2

@Composable
internal fun DownloadingScreen(
    items: List<HanimeDownloadEntity>,
    onPauseItem: (HanimeDownloadEntity) -> Unit,
    onResumeItem: (HanimeDownloadEntity) -> Unit,
    onDeleteItem: (HanimeDownloadEntity) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<HanimeDownloadEntity?>(null) }

    ConfirmDialog(
        visible = pendingDelete != null,
        title = stringResource(R.string.sure_to_delete),
        message = stringResource(R.string.prepare_to_delete_s, pendingDelete?.title.orEmpty()),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            pendingDelete?.let(onDeleteItem)
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null },
    )

    if (items.isEmpty()) {
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
            items(items, key = { it.id }) { item ->
                DownloadingItemCard(
                    item = item,
                    onPause = { onPauseItem(item) },
                    onResume = { onResumeItem(item) },
                    onDelete = { pendingDelete = item },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadingItemCard(
    item: HanimeDownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onDelete),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = item.coverUri ?: item.coverUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(136.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.quality,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            if (item.state == DownloadState.Downloading) {
                                LoadingIndicator(modifier = Modifier.size(12.dp))
                            } else {
                                Icon(
                                    painter = painterResource(itemStateIcon(item.state)),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = itemStateText(item.state, item.progress),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Text(
                        text = stringResource(
                            R.string.download_progress_size,
                            item.downloadedLength.formatFileSizeV2(),
                            item.length.formatFileSizeV2(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel_download),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    when (item.state) {
                        DownloadState.Downloading -> {
                            FilledTonalIconButton(
                                onClick = onPause,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_baseline_pause_24),
                                    contentDescription = stringResource(R.string.pause_all),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        DownloadState.Paused,
                        DownloadState.Queued,
                        DownloadState.Unknown -> {
                            FilledTonalIconButton(
                                onClick = onResume,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.continues),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        DownloadState.Failed -> {
                            FilledTonalIconButton(
                                onClick = onResume,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.retry),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        DownloadState.Finished -> Unit
                    }
                }
            }

            val animatedProgress by animateFloatAsState(
                targetValue = item.progress / 100f,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            )
            LinearWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun itemStateText(state: DownloadState, progress: Int): String {
    return when (state) {
        DownloadState.Queued -> stringResource(R.string.already_in_queue)
        DownloadState.Downloading -> stringResource(R.string.download_progress_percent, progress)
        DownloadState.Paused -> stringResource(R.string.paused)
        DownloadState.Failed -> stringResource(R.string.retry)
        DownloadState.Finished -> stringResource(R.string.download_complete)
        DownloadState.Unknown -> stringResource(R.string.loading)
    }
}

private fun itemStateIcon(state: DownloadState): Int {
    return when (state) {
        DownloadState.Queued -> R.drawable.ic_baseline_play_arrow_24
        DownloadState.Downloading -> R.drawable.ic_baseline_pause_24
        DownloadState.Paused -> R.drawable.ic_baseline_play_arrow_24
        DownloadState.Failed -> R.drawable.baseline_error_outline_24
        DownloadState.Finished -> R.drawable.ic_baseline_check_circle_24
        DownloadState.Unknown -> R.drawable.ic_baseline_download_24
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
            items = items,
            onPauseItem = {},
            onResumeItem = {},
            onDeleteItem = {},
        )
    }
}
