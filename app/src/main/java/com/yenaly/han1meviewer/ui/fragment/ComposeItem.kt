package com.yenaly.han1meviewer.ui.fragment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.VideoItemType

@Composable
fun ArtistItem(artist: SubscriptionItem, onClickArtist: (String) -> Unit, onLongClickArtist: (String) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .width(72.dp)
            .combinedClickable(
                onClick = { onClickArtist(artist.artistName) },
                onLongClick = {
                    onLongClickArtist(artist.artistName)
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RetryableImage(
            model = artist.avatar,
            contentDescription = artist.artistName,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(4.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
            placeholder = painterResource(R.drawable.baseline_data_usage_24),
            error = painterResource(R.drawable.baseline_error_outline_24)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = artist.artistName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
@Composable
fun VideoCardItem(
    videoItem: VideoItemType,
    isHorizontalCard: Boolean = true,
    onClickVideosItem: (String) -> Unit,
    onLongClickVideosItem: (String, String) -> Unit
) {
    val cardWidth = if (isHorizontalCard) dimensionResource(id = R.dimen.video_cover_width) else dimensionResource(id = R.dimen.video_cover_simplified_width)
    val cardHeight = if (isHorizontalCard) dimensionResource(id = R.dimen.video_cover_height) else dimensionResource(id = R.dimen.video_cover_simplified_height)
    val textFontSize = dimensionResource(id = R.dimen.video_view_and_time_and_duration).value.sp
    val iconSize = dimensionResource(id = R.dimen.view_view_and_time_icon_size)
    Surface(
        modifier = Modifier
            .padding(6.dp)
            .width(cardWidth)
            .combinedClickable(
                onClick = { onClickVideosItem(videoItem.videoCode) },
                onLongClick = {
                    onLongClickVideosItem(videoItem.videoCode,videoItem.title)
                }
            ),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
            ) {
                RetryableImage(
                    model = videoItem.coverUrl,
                    contentDescription = videoItem.title,
                    modifier = Modifier
                        .fillMaxSize(),
                    placeholder = painterResource(R.drawable.akarin),
                    error = painterResource(android.R.drawable.stat_notify_error),
                    contentScale = ContentScale.Crop
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
                                )
                            )
                        )
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                )

                // 视频底部信息栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        videoItem.views?.let {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_play_circle_outline_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp),
                                text = it,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = textFontSize
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        videoItem.duration?.let {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_access_time_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp),
                                text = it,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = textFontSize
                            )
                        }
                    }
                }
            }

            //  标题
            Text(
                text = videoItem.title,
                maxLines = 2,
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 8.dp)
            )
            // 视频作者
            Text(
                text = videoItem.currentArtist ?: "作者",
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            // 点赞比例
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
            ) {
                if (!videoItem.reviews.isNullOrEmpty()) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_thumb_up_alt_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(iconSize)
                    )
                    Text(
                        text = videoItem.reviews!!,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistItem(
    playlist: Playlists.Playlist,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(100.dp)) {
                RetryableImage(
                    model = playlist.coverUrl ?: "",
                    contentDescription = playlist.title,
                    placeholder = painterResource(R.drawable.akarin),
                    error = painterResource(R.drawable.baseline_error_outline_24),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 封面右下角视频数
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(topStart = 8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.video_count, playlist.total),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun BottomSheetHandler(){
    Box(
        modifier = Modifier
            .height(32.dp)
            .fillMaxWidth()
            .zIndex(1f)
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(3.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50)
                )
                .align(Alignment.Center)
        )
    }
}

@Composable
fun EmptyView(hint: String, picRes: Int = R.drawable.h_chan_speechless) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center // 整体居中
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, // 图片和文字水平居中
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier.padding(16.dp).width(150.dp),
                painter = painterResource(picRes),
                contentDescription = stringResource(R.string.here_is_empty),
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
