package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.VideoComments

/**
 * 视频评论卡片组件。
 *
 * 展示单条评论，支持回复、点赞、点踩、举报等操作，
 * 可选的更多回复入口。
 *
 * @param modifier 修饰符
 * @param comment 评论数据
 * @param onReply 回复回调
 * @param onThumbUp 点赞回调
 * @param onThumbDown 点踩回调
 * @param onReport 举报回调
 * @param onViewMoreReplies 查看更多回复回调，为 null 时不显示入口
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCommentCard(
    modifier: Modifier = Modifier,
    comment: VideoComments.VideoComment,
    onReply: () -> Unit,
    onThumbUp: () -> Unit,
    onThumbDown: () -> Unit,
    onReport: () -> Unit,
    onViewMoreReplies: (() -> Unit)? = null,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = {}),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AsyncImage(
                    model = comment.avatar,
                    contentDescription = comment.username,
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Crop,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = comment.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = comment.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(onClick = onReport, modifier = Modifier.size(36.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_report_24),
                        contentDescription = stringResource(R.string.submit_bug),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onThumbUp, colors = ButtonDefaults.textButtonColors()) {
                    Icon(
                        painter = painterResource(
                            if (comment.post.likeCommentStatus) {
                                R.drawable.ic_baseline_thumb_up_alt_24
                            } else {
                                R.drawable.ic_baseline_thumb_up_off_alt_24
                            }
                        ),
                        contentDescription = null,
                    )
                    Text(comment.realLikesCount?.toString().orEmpty())
                }

                TextButton(onClick = onThumbDown, colors = ButtonDefaults.textButtonColors()) {
                    Icon(
                        painter = painterResource(
                            if (comment.post.unlikeCommentStatus) {
                                R.drawable.ic_baseline_thumb_down_alt_24
                            } else {
                                R.drawable.ic_baseline_thumb_down_off_alt_24
                            }
                        ),
                        contentDescription = null,
                    )
                }

                TextButton(onClick = onReply) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_reply_24),
                        contentDescription = null,
                    )
                    Text(stringResource(R.string.reply))
                }
            }

            if (comment.hasMoreReplies && onViewMoreReplies != null) {
                TextButton(onClick = onViewMoreReplies) {
                    Text(stringResource(R.string.view_more_replies, comment.replyCount ?: 0))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoCommentCardPreview() {
    val fakeComment = VideoComments.VideoComment(
        avatar = "https://picsum.photos/64/64",
        username = "preview_user",
        date = "2小時前",
        content = "這是一條用於預覽的評論內容，用來確認 Compose 卡片樣式是否接近原版 item_video_comment.xml。",
        thumbUp = 12,
        isChildComment = false,
        hasMoreReplies = true,
        replyCount = 3,
        id = "1",
        post = VideoComments.VideoComment.POST(
            foreignId = "1",
            likeCommentStatus = false,
            unlikeCommentStatus = false,
        ),
        reportableId = "1",
        reportableType = "comment",
    )
    ComponentPreview {
        VideoCommentCard(
            comment = fakeComment,
            onReply = {},
            onThumbUp = {},
            onThumbDown = {},
            onReport = {},
            onViewMoreReplies = {},
        )
    }
}
