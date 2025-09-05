package com.yenaly.han1meviewer.ui.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.activity.CookieGuideDialog
import com.yenaly.han1meviewer.ui.fragment.dailycheckin.CalendarGrid
import com.yenaly.han1meviewer.ui.fragment.home.subscription.SubscriptionPageContent
import com.yenaly.han1meviewer.ui.fragment.home.subscription.VideoCardItem
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import java.time.YearMonth

@Preview(showBackground = true)
@Composable
fun VideoCardPreview(){
    HanimeTheme {
        VideoCardItem(fakeVideosItem, onClickVideosItem = { }, onLongClickVideosItem =  {_,_->})
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun SubscriptionAppPreview() {
    HanimeTheme { SubscriptionAppPreviewBody() }
}

@Preview(showBackground = true)
@Composable
fun CookiesImportDialogPreview() {
    HanimeTheme { CookieGuideDialog {} }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun CalendarGridPreview() {
    val yearMonth = YearMonth.now()
    val fakeRecords = generateFakeCheckInRecords(yearMonth)
    CalendarGrid(
        yearMonth = yearMonth,
        records = fakeRecords,
        onDateClick = { date ->
            println("点击了: $date")
        },
        onDateLongClick = { date ->
            println("长按了: $date")
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Preview
@Composable
fun DarkThemePreview() {
    HanimeTheme() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 主色调预览
                ColorPreviewBox(
                    color = MaterialTheme.colorScheme.primary,
                    onColor = MaterialTheme.colorScheme.onPrimary,
                    label = "Primary"
                )

                // 背景和表面预览
                ColorPreviewBox(
                    color = MaterialTheme.colorScheme.background,
                    onColor = MaterialTheme.colorScheme.onBackground,
                    label = "Background"
                )

                ColorPreviewBox(
                    color = MaterialTheme.colorScheme.surface,
                    onColor = MaterialTheme.colorScheme.onSurface,
                    label = "Surface"
                )

                // 容器预览
                ColorPreviewBox(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    label = "Primary Container"
                )
            }
        }
    }
}

@Composable
fun ColorPreviewBox(color: Color, onColor: Color, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(color)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = onColor, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionAppPreviewBody() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val isRefreshing = false
    val refreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .pullToRefresh(
                state = refreshState,
                isRefreshing = isRefreshing,
                onRefresh = {}
            ),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的订阅") },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            SubscriptionPageContent(
                gridState = LazyGridState(),
                videos = fakeVideos,
                onClickVideosItem = {},
                onLoadMore = { },
                canLoadMore = false,
                artists = fakeArtists,
                onClickArtist = {},
                onLongClickVideosItem = {_,_->},
                onLongClickArtist = {_->}
            )
        }
    }
}