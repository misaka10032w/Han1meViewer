package com.yenaly.han1meviewer.ui.screen.home.homepage.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Campaign
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

/**
 * 平板大屏下与 Banner 并排显示的公告面板。
 *
 * 与 [AnnouncementCard] 展示同样的数据，但纵向铺满给定高度，以便填充 Banner 限高后空出的
 * 横向空间。
 *
 * @param announcements 要展示的公告列表。
 * @param onAnnouncementClick 点击公告时调用，参数为被点击公告。
 * @param onClose 点击关闭按钮时调用。
 * @param modifier 应用于面板根布局的修饰符，需要由调用方给定宽高。
 */
@Composable
fun AnnouncementPanel(
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

    Box(
        modifier = modifier
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAnnouncementClick(item) }
                    .padding(start = 16.dp, end = 48.dp, top = 16.dp, bottom = 44.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.announcement),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatTimestamp(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageIndicator(
                pageCount = announcements.size,
                currentPage = pagerState.currentPage
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.view_all),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showAllDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
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

    if (showAllDialog) {
        AnnouncementListDialog(
            announcements = announcements,
            onDismiss = { showAllDialog = false }
        )
    }
}

@Preview(showBackground = true, name = "公告面板", widthDp = 420, heightDp = 340)
@Composable
private fun AnnouncementPanelPreview() {
    ComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            AnnouncementPanel(
                announcements = fakeAnnouncements,
                onAnnouncementClick = {},
                onClose = {},
                modifier = Modifier.fillMaxWidth().height(320.dp)
            )
        }
    }
}
