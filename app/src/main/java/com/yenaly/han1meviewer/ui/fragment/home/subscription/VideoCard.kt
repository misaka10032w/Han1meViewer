package com.yenaly.han1meviewer.ui.fragment.home.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem

@Composable
fun VideoCard(
    videoItem: SubscriptionVideosItem,
    onClickVideosItem: (String) -> Unit
) {
    val cardWidth = dimensionResource(id = R.dimen.video_cover_width)
    val cardHeight = dimensionResource(id = R.dimen.video_cover_height)
    val textFontSize = dimensionResource(id = R.dimen.video_view_and_time_and_duration).value.sp
    val iconSize = dimensionResource(id = R.dimen.view_view_and_time_icon_size)
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .width(cardWidth)
            .clickable { onClickVideosItem(videoItem.videoCode) },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
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
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = painterResource(R.drawable.baseline_data_usage_24),
                    error = painterResource(R.drawable.baseline_error_outline_24)
                )

                // 视频底部信息栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_play_circle_outline_24),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                        videoItem.views?.let {
                            Text(
                                text = it,
                                color = Color.White,
                                fontSize = textFontSize
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_access_time_24),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                        videoItem.duration?.let {
                            Text(
                                text = it,
                                color = Color.White,
                                fontSize = textFontSize
                            )
                        }
                    }
                }
            }

            // 视频标题
            Text(
                text = videoItem.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            // 类型 + 上传者
            videoItem.reviews?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}