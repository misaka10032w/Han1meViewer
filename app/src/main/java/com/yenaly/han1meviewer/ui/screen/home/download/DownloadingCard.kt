package com.yenaly.han1meviewer.ui.screen.home.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.yenaly_libs.utils.formatFileSizeV2

/**
 * 下载中任务卡片。
 *
 * @param item 下载实体
 * @param onPause 暂停回调
 * @param onResume 恢复回调
 * @param onDelete 删除回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DownloadingItemCard(
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
                                    painter = painterResource(downloadStateIcon(item.state)),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = downloadStateText(item.state, item.progress),
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

@Preview(showBackground = true, widthDp = 420, heightDp = 200)
@Composable
private fun PreviewDownloadingItemCard() {
    ComponentPreview {
        DownloadingItemCard(
            item = HanimeDownloadEntity(
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
            ),
            onPause = {},
            onResume = {},
            onDelete = {},
        )
    }
}
