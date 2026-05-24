package com.yenaly.han1meviewer.ui.screen.home.download

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.LOCAL_DATE_TIME_FORMAT
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeDownloadedNodes
import com.yenaly.yenaly_libs.utils.formatFileSizeV2
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 已下载视频分组头部卡片。
 *
 * @param header 分组节点
 * @param onToggle 展开/折叠回调
 * @param onRename 重命名回调
 */
@Composable
fun DownloadGroupHeader(
    header: DownloadHeaderNode,
    onToggle: () -> Unit,
    onRename: () -> Unit,
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onToggle, onLongClick = onRename)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledIconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(
                        if (header.isExpanded) R.drawable.ic_baseline_fold_24
                        else R.drawable.ic_baseline_list_24
                    ),
                    contentDescription = null,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = header.groupKey,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.video_count, header.originalVideos.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = onToggle,
                label = {
                    Text(
                        if (header.isExpanded) stringResource(R.string.collapse)
                        else stringResource(R.string.expand)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

/**
 * 已下载视频卡片。
 *
 * @param item 视频及分组信息
 * @param onOpenVideo 打开视频详情
 * @param onLocalPlayback 本地播放
 * @param onExternalPlayback 外部播放器
 * @param onDeleteVideo 删除视频
 * @param onMoveGroup 移动到其他分组
 * @param isMultiSelect 是否多选模式
 * @param isSelected 是否已选中
 * @param onToggleSelect 切换选中状态
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalTime::class)
@Composable
fun DownloadedVideoCard(
    item: VideoWithCategories,
    onOpenVideo: () -> Unit,
    onLocalPlayback: () -> Unit,
    onExternalPlayback: () -> Unit,
    onDeleteVideo: () -> Unit,
    onMoveGroup: () -> Unit,
    isMultiSelect: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
) {
    val addedTime = if (!LocalInspectionMode.current) {
        remember(item.video.addDate) {
            Instant.fromEpochMilliseconds(item.video.addDate)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .format(LOCAL_DATE_TIME_FORMAT)
        }
    } else {
        "2024-01-01 12:00"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isMultiSelect) Modifier.combinedClickable(
                    onClick = { onToggleSelect?.invoke() },
                    onLongClick = {},
                ) else Modifier.combinedClickable(
                    onClick = onOpenVideo,
                    onLongClick = onMoveGroup,
                )
            ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box {
                    AsyncImage(
                        model = item.video.coverUri ?: item.video.coverUrl,
                        contentDescription = item.video.title,
                        placeholder = painterResource(R.drawable.h_chan_loading),
                        error = painterResource(R.drawable.h_chan_load_failed),
                        modifier = Modifier
                            .width(150.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    if (isMultiSelect) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleSelect?.invoke() },
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = addedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = onOpenVideo,
                            label = { Text(item.video.videoCode) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                        )
                        Text(
                            text = item.video.quality,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        VerticalDivider(
                            modifier = Modifier.height(14.dp),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = item.video.length.formatFileSizeV2(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = onDeleteVideo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_delete_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(stringResource(R.string.delete))
                    }
                }

                Button(
                    onClick = onExternalPlayback,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ext_link),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(stringResource(R.string.ext_player))
                    }
                }

                Button(
                    onClick = onLocalPlayback,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_play_arrow_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(stringResource(R.string.local_playback))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun PreviewDownloadGroupHeader() {
    val header = (fakeDownloadedNodes.first() as? DownloadHeaderNode)
        ?: return
    ComponentPreview {
        DownloadGroupHeader(
            header = header,
            onToggle = {},
            onRename = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun PreviewDownloadedVideoCard() {
    val node = fakeDownloadedNodes
        .filterIsInstance<com.yenaly.han1meviewer.logic.model.DownloadItemNode>()
        .firstOrNull()?.data
        ?: return
    ComponentPreview {
        DownloadedVideoCard(
            item = node,
            onOpenVideo = {},
            onLocalPlayback = {},
            onExternalPlayback = {},
            onDeleteVideo = {},
            onMoveGroup = {},
            isMultiSelect = true,
            isSelected = true
        )
    }
}
