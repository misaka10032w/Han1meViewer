package com.yenaly.han1meviewer.ui.screen.home.homepage.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.ui.component.AutoScrollEffect
import com.yenaly.han1meviewer.ui.component.PageIndicator
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeAnnouncements
import com.yenaly.han1meviewer.ui.screen.home.homepage.formatTimestamp
import kotlin.time.Duration.Companion.seconds

/** 公告轮播的间隔，比图片轮播长一些，留出阅读时间。 */
internal val AnnouncementAutoScrollInterval = 8.seconds

/**
 * 显示首页紧凑公告轮播卡片。
 *
 * @param announcements 要展示的公告列表。
 * @param onAnnouncementClick 点击公告时调用，参数为被点击公告。
 * @param onClose 点击关闭按钮时调用。
 * @param modifier 应用于公告卡片根布局的修饰符。
 */
@Composable
fun AnnouncementCard(
    announcements: List<Announcement>,
    onAnnouncementClick: (Announcement) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (announcements.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { announcements.size })
    var showAllDialog by remember { mutableStateOf(false) }

    AutoScrollEffect(
        pagerState = pagerState,
        pageCount = announcements.size,
        interval = AnnouncementAutoScrollInterval
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val item = announcements[page]
                val time = formatTimestamp(item.timestamp)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(onClick = { onAnnouncementClick(item) })
                        .padding(start = 12.dp, end = 40.dp, top = 10.dp, bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.TopStart)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = time,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            PageIndicator(
                pageCount = announcements.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.view_all),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .clickable { showAllDialog = true }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }

    if (showAllDialog) {
        AnnouncementListDialog(
            announcements = announcements,
            onDismiss = { showAllDialog = false }
        )
    }
}

@Preview(showBackground = true, name = "公告卡片（单条）")
@Composable
private fun AnnouncementCardSinglePreview() {
    ComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            AnnouncementCard(
                announcements = fakeAnnouncements.take(1),
                onAnnouncementClick = {},
                onClose = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "公告卡片（多条可滑动）")
@Composable
private fun AnnouncementCardMultiplePreview() {
    ComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            AnnouncementCard(
                announcements = fakeAnnouncements,
                onAnnouncementClick = {},
                onClose = {}
            )
        }
    }
}
