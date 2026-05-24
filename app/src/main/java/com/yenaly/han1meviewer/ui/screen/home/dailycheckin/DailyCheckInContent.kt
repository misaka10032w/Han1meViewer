package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 打卡日历页面的纯 UI Content 层。
 *
 * 只接收 [DailyCheckInUiState] 和 [DailyCheckInEvent] 回调，不持有 ViewModel。
 *
 * @param paddingValues 从 Scaffold 传入的内边距
 * @param uiState 页面 UI 状态
 * @param onEvent 用户交互事件回调
 * @param pagerState 月份翻页状态（UI 框架层，不属于业务状态）
 * @param anchorMonth 翻页锚点月份
 * @param initialPage 初始页索引
 * @param modifier 修饰符
 */
@Composable
fun DailyCheckInContent(
    paddingValues: PaddingValues,
    uiState: DailyCheckInUiState,
    onEvent: (DailyCheckInEvent) -> Unit,
    pagerState: PagerState,
    anchorMonth: YearMonth,
    initialPage: Int,
    modifier: Modifier = Modifier,
) {
    val animatedCheckedDays by animateIntAsState(
        targetValue = uiState.checkedDays, label = "days"
    )
    val animatedMonthlyTotal by animateIntAsState(
        targetValue = uiState.monthlyTotal, label = "total"
    )
    val animatedBestStreak by animateIntAsState(
        targetValue = uiState.bestStreakThisMonth, label = "streak"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        TodayCheckInCard(
            today = uiState.today,
            count = uiState.todayCount,
            maxCount = 20,
            onCheckIn = { onEvent(DailyCheckInEvent.OnTodayCheckIn) },
            onClear = { onEvent(DailyCheckInEvent.OnTodayClear) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatsCard(
            checkedDays = animatedCheckedDays,
            monthlyTotal = animatedMonthlyTotal,
            bestStreak = animatedBestStreak,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.checkin_calendar),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onEvent(DailyCheckInEvent.OnPreviousMonth) }) {
                    Icon(painterResource(R.drawable.previous_double_arrow_24), "previous")
                }
                Text(
                    text = uiState.currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { onEvent(DailyCheckInEvent.OnNextMonth) }) {
                    Icon(painterResource(R.drawable.next_double_arrow_24), "next")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            verticalAlignment = Alignment.Top,
            beyondViewportPageCount = 1,
            key = { page -> page }
        ) { page ->
            val monthForPage = anchorMonth.plusMonths((page - initialPage).toLong())
            CalendarGrid(
                yearMonth = monthForPage,
                records = uiState.records,
                today = uiState.today,
                onDateClick = { onEvent(DailyCheckInEvent.OnDateClick(it)) },
                onDateLongClick = { onEvent(DailyCheckInEvent.OnDateLongClick(it)) },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedVisibility(
            visible = uiState.eggVisible,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = uiState.showEasterEgg,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        AchievementSection(
            checkedDays = uiState.checkedDays,
            monthlyTotal = uiState.monthlyTotal,
            bestStreak = uiState.bestStreakThisMonth,
            stats = uiState.monthlyStats,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.checkin_tip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 预览用 Content 样例。
 */
@Preview
@Composable
private fun PreviewDailyCheckInContent() {
    DailyCheckInContent(
        paddingValues = PaddingValues(0.dp),
        uiState = DailyCheckInUiState(),
        onEvent = {},
        pagerState = androidx.compose.foundation.pager.rememberPagerState { 1 },
        anchorMonth = YearMonth.now(),
        initialPage = 0,
    )
}
