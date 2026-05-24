package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 今日打卡卡片。
 *
 * @param today 今天的日期
 * @param count 今日已打卡次数
 * @param maxCount 每日最大打卡数
 * @param onCheckIn 打卡按钮回调
 * @param onClear 清除按钮回调
 * @param modifier 修饰符
 */
@Composable
fun TodayCheckInCard(
    modifier: Modifier = Modifier,
    today: LocalDate,
    count: Int,
    maxCount: Int = 20,
    onCheckIn: () -> Unit,
    onClear: () -> Unit,
) {
    val isMaxed = count >= maxCount
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (count > 0)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("MM月dd日 EEEE")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (count > 0) "${stringResource(R.string.today_checked)} $count/$maxCount ${
                        stringResource(
                            R.string.times
                        )
                    }"
                    else stringResource(R.string.not_checked_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onCheckIn,
                    enabled = !isMaxed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (count > 0) stringResource(R.string.view_checkin) else stringResource(R.string.checkin))
                }
                if (count > 0) {
                    TextButton(onClick = onClear) {
                        Text(
                            stringResource(R.string.clear_checkin),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 月度统计卡片。
 *
 * @param checkedDays 已打卡天数
 * @param monthlyTotal 累计打卡次数
 * @param bestStreak 最佳连续天数
 * @param modifier 修饰符
 */
@Composable
fun StatsCard(
    checkedDays: Int,
    monthlyTotal: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.Filled.Star,
                label = stringResource(R.string.this_month_checkin),
                value = stringResource(R.string.days, checkedDays)
            )
            HorizontalDivider(
                modifier = Modifier
                    .height(48.dp)
                    .width(1.dp)
            )
            StatItem(
                icon = Icons.Filled.Favorite,
                label = stringResource(R.string.has_cum_days),
                value = stringResource(R.string.counts, monthlyTotal)
            )
            HorizontalDivider(
                modifier = Modifier
                    .height(48.dp)
                    .width(1.dp)
            )
            StatItem(
                icon = Icons.Filled.Favorite,
                label = stringResource(R.string.best_streak),
                value = "${bestStreak}${stringResource(R.string.day_unit)}"
            )
        }
    }
}

/**
 * 统计项组件，在 [StatsCard] 和报表中复用。
 *
 * @param icon 图标
 * @param label 标签文字
 * @param value 统计值
 */
@Composable
fun RowScope.StatItem(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
private fun PreviewTodayCheckInCard() {
    TodayCheckInCard(
        today = LocalDate.now(),
        count = 3,
        onCheckIn = {},
        onClear = {},
    )
}

@Preview
@Composable
private fun PreviewStatsCard() {
    StatsCard(
        checkedDays = 15,
        monthlyTotal = 42,
        bestStreak = 7,
    )
}
