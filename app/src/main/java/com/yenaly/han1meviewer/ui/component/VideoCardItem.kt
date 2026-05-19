package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.VideoItemType
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeVideosItem
import com.yenaly.han1meviewer.ui.screen.RetryableImage


/**
 * 标准视频卡片项组件。
 *
 * 展示视频封面、标题等信息，支持水平和垂直两种布局，支持点击和长按交互。
 *
 * @param videoItem 视频数据
 * @param isHorizontalCard 是否为水平卡片布局，默认为 true
 * @param onClickVideosItem 点击回调，参数为视频 ID
 * @param onLongClickVideosItem 长按回调，参数为视频 ID 和视频标题
 */
@Composable
fun VideoCardItem(
    videoItem: VideoItemType,
    isHorizontalCard: Boolean = true,
    onClickVideosItem: (String) -> Unit,
    onLongClickVideosItem: (String, String) -> Unit,
) {
    val cardWidth = if (isHorizontalCard) {
        dimensionResource(id = R.dimen.video_cover_width)
    } else {
        dimensionResource(id = R.dimen.video_cover_simplified_width)
    }
    val cardHeight = if (isHorizontalCard) {
        dimensionResource(id = R.dimen.video_cover_height)
    } else {
        dimensionResource(id = R.dimen.video_cover_simplified_height)
    }
    val textFontSize = dimensionResource(id = R.dimen.video_view_and_time_and_duration).value.sp
    val iconSize = dimensionResource(id = R.dimen.view_view_and_time_icon_size)

    Surface(
        modifier = Modifier
            .padding(6.dp)
            .width(cardWidth)
            .combinedClickable(
                onClick = { onClickVideosItem(videoItem.videoCode) },
                onLongClick = { onLongClickVideosItem(videoItem.videoCode, videoItem.title) },
            ),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight),
            ) {
                RetryableImage(
                    model = videoItem.coverUrl,
                    contentDescription = videoItem.title,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = painterResource(R.drawable.akarin),
                    error = painterResource(android.R.drawable.stat_notify_error),
                    contentScale = ContentScale.FillHeight,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(25.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceVariant
                                ),
                            ),
                        ),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        videoItem.views?.let {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_play_circle_outline_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(iconSize),
                            )
                            Text(
                                modifier = Modifier.padding(horizontal = 2.dp),
                                text = it,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = textFontSize,
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        videoItem.duration?.let {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_access_time_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(iconSize),
                            )
                            Text(
                                modifier = Modifier.padding(horizontal = 2.dp),
                                text = it,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = textFontSize,
                            )
                        }
                    }
                }
            }

            Text(
                text = videoItem.title,
                maxLines = 2,
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 8.dp),
            )
            Text(
                text = videoItem.currentArtist ?: "作者",
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
            ) {
                if (!videoItem.reviews.isNullOrEmpty()) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_thumb_up_alt_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(iconSize),
                    )
                    Text(
                        text = videoItem.reviews!!,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (!videoItem.uploadTime.isNullOrEmpty()) {
                    Text(
                        text = videoItem.uploadTime!!,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoCardItemPreview() {
    ComponentPreview {
        VideoCardItem(
            videoItem = fakeVideosItem,
            onClickVideosItem = {},
            onLongClickVideosItem = { _, _ -> },
        )
    }
}
