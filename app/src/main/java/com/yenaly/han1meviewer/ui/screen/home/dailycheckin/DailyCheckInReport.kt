package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.viewmodel.MonthlyStats
import java.time.LocalDate
import java.time.YearMonth

/**
 * 贡献报表弹窗。以日历热力图形式展示年/月打卡分布。
 *
 * @param selectedYear 当前选择的年份
 * @param viewMode 视图模式 "year" 或 "month"
 * @param selectedMonth 当前选择的月份
 * @param yearRecords 年度打卡记录
 * @param yearStats 年度统计数据
 * @param onYearChange 年份变更回调
 * @param onViewModeChange 视图模式变更回调
 * @param onMonthChange 月份变更回调
 * @param onDismiss 关闭弹窗回调
 * @param isFullscreen 是否全屏显示
 * @param onToggleFullscreen 全屏切换回调
 * @param onLoadYearRecords 加载指定年份记录的回调
 */
@Composable
fun ContributionReportDialog(
    selectedYear: Int,
    viewMode: String,
    selectedMonth: Int,
    yearRecords: Map<LocalDate, Int>,
    yearStats: MonthlyStats,
    onYearChange: (Int) -> Unit,
    onViewModeChange: (String) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    onLoadYearRecords: (Int) -> Unit,
) {
    val today = LocalDate.now()

    LaunchedEffect(selectedYear) {
        onLoadYearRecords(selectedYear)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        HanimeScaffold(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(R.string.checkin_report),
            onBack = onDismiss,
            actions = {
                TextButton(onClick = { onViewModeChange("year") }) {
                    Text(
                        stringResource(R.string.report_year),
                        fontWeight = if (viewMode == "year") FontWeight.Bold else FontWeight.Normal,
                        color = if (viewMode == "year") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = { onViewModeChange("month") }) {
                    Text(
                        stringResource(R.string.report_month),
                        fontWeight = if (viewMode == "month") FontWeight.Bold else FontWeight.Normal,
                        color = if (viewMode == "month") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                FilledIconButton(onClick = onToggleFullscreen) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_screen_rotation_24),
                        contentDescription = if (isFullscreen)
                            stringResource(R.string.report_portrait)
                        else
                            stringResource(R.string.report_landscape),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                if (viewMode == "year") {
                    YearContributionView(
                        year = selectedYear,
                        records = yearRecords,
                        today = today,
                        onYearChange = onYearChange
                    )
                } else {
                    MonthContributionView(
                        year = selectedYear,
                        month = selectedMonth,
                        records = yearRecords,
                        today = today,
                        onYearChange = onYearChange,
                        onMonthChange = onMonthChange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val filteredRecords = if (viewMode == "year") {
                    yearRecords.filterKeys { it.year == selectedYear }
                } else {
                    yearRecords.filterKeys {
                        it.year == selectedYear && it.monthValue == selectedMonth
                    }
                }
                val totalCount = filteredRecords.values.sum()
                val totalDays = filteredRecords.count { it.value > 0 }
                val maxDay = filteredRecords.maxByOrNull { it.value }?.value ?: 0

                if (totalDays > 0) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                icon = Icons.Filled.DateRange,
                                label = stringResource(R.string.report_total),
                                value = totalCount.toString()
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(1.dp)
                            )
                            StatItem(
                                icon = Icons.Filled.Star,
                                label = stringResource(R.string.report_days),
                                value = totalDays.toString()
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(1.dp)
                            )
                            StatItem(
                                icon = Icons.Filled.Favorite,
                                label = stringResource(R.string.report_max_day),
                                value = maxDay.toString()
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.report_no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val stats = if (viewMode == "year") yearStats else yearStats
                if (stats.typeCounts.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            stats.typeCounts.entries
                                .sortedByDescending { it.value }
                                .take(5)
                                .forEach { (type, count) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = typeEmoji(type), fontSize = 20.sp)
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = type,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ContributionLegend()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * GitHub 风格年度热力图。
 *
 * @param year 目标年份
 * @param records 各日期打卡记录数
 * @param today 今天的日期
 * @param onYearChange 年份切换回调
 */
@Composable
fun YearContributionView(
    year: Int,
    records: Map<LocalDate, Int>,
    today: LocalDate,
    onYearChange: (Int) -> Unit,
) {
    val weeks = remember(year) { buildYearWeeks(year) }
    val monthFormat = stringResource(R.string.report_month_format)
    val monthLabels = remember(year) { buildMonthLabels(year, weeks, monthFormat) }
    val dayLabels = listOf(
        stringResource(R.string.mon), stringResource(R.string.tue),
        stringResource(R.string.wed), stringResource(R.string.thu),
        stringResource(R.string.fri), stringResource(R.string.sat),
        stringResource(R.string.sun)
    )
    val cellSize = 14.dp
    val cellPadding = 1.dp
    val columnWidth = cellSize + cellPadding * 2
    val labelColWidth = 24.dp
    val scrollState = rememberScrollState()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onYearChange(year - 1) }) {
                Icon(
                    painterResource(R.drawable.previous_double_arrow_24),
                    stringResource(R.string.previous_year)
                )
            }
            Text(
                text = stringResource(R.string.report_year_format, year),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { onYearChange(year + 1) },
                enabled = year < today.year
            ) {
                Icon(
                    painterResource(R.drawable.next_double_arrow_24),
                    stringResource(R.string.next_year)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            Row(
                modifier = Modifier.height(20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Spacer(modifier = Modifier.width(labelColWidth))
                Box(modifier = Modifier.width(columnWidth * weeks.size)) {
                    monthLabels.forEach { (label, weekIdx) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .offset(x = columnWidth * weekIdx)
                                .width(columnWidth * 4)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            for (dayIdx in 0..6) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dayLabels[dayIdx],
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(labelColWidth),
                        textAlign = TextAlign.Center
                    )
                    weeks.forEach { week ->
                        val date = week.getOrNull(dayIdx)
                        val count = date?.let { records[it] } ?: 0
                        val level = getContributionLevel(count)
                        val isToday = date == today
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .padding(cellPadding)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (count > 0) contributionColors[level]
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                                .then(
                                    if (isToday) Modifier.border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    ) else Modifier
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 月度打卡网格视图。
 *
 * @param year 目标年份
 * @param month 目标月份
 * @param records 各日期打卡记录数
 * @param today 今天的日期
 * @param onYearChange 年份切换回调
 * @param onMonthChange 月份切换回调
 */
@Composable
fun MonthContributionView(
    year: Int,
    month: Int,
    records: Map<LocalDate, Int>,
    today: LocalDate,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
) {
    val yearMonth = YearMonth.of(year, month)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value
    val dayLabels = listOf(
        stringResource(R.string.mon), stringResource(R.string.tue),
        stringResource(R.string.wed), stringResource(R.string.thu),
        stringResource(R.string.fri), stringResource(R.string.sat),
        stringResource(R.string.sun)
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (month == 1) {
                    onYearChange(year - 1)
                    onMonthChange(12)
                } else {
                    onMonthChange(month - 1)
                }
            }) {
                Icon(
                    painterResource(R.drawable.previous_double_arrow_24),
                    stringResource(R.string.previous_month)
                )
            }
            Text(
                text = stringResource(R.string.report_year_month_format, year, month),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    val now = YearMonth.now()
                    val current = YearMonth.of(year, month)
                    if (current.isBefore(now)) {
                        if (month == 12) {
                            onYearChange(year + 1)
                            onMonthChange(1)
                        } else {
                            onMonthChange(month + 1)
                        }
                    }
                },
                enabled = YearMonth.of(year, month).isBefore(YearMonth.now())
            ) {
                Icon(
                    painterResource(R.drawable.next_double_arrow_24),
                    stringResource(R.string.next_month)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayLabels.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(((firstDayOfWeek - 1 + daysInMonth + 6) / 7 * 52).dp)
        ) {
            items(firstDayOfWeek - 1) {
                Spacer(modifier = Modifier.size(48.dp))
            }
            items(daysInMonth) { day ->
                val date = yearMonth.atDay(day + 1)
                val count = records[date] ?: 0
                val level = getContributionLevel(count)
                val isToday = date == today
                val cellBg = if (count > 0) contributionColors[level]
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(cellBg)
                        .then(
                            if (isToday) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (day + 1).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (count > 0)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        if (count > 0) {
                            Text(
                                text = stringResource(R.string.checkin_count_format, count),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 热力图图例，展示颜色与打卡次数的对应关系。
 */
@Composable
fun ContributionLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.report_legend_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        contributionColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .padding(1.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (color == Color.Transparent)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        else color
                    )
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.report_legend_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun PreviewContributionLegend() {
    ContributionLegend()
}
