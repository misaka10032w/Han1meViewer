package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.viewmodel.MonthlyStats
import java.time.LocalDate

/**
 * 成就展示区域。
 *
 * 根据 [checkedDays]、[monthlyTotal]、[bestStreak] 和 [stats] 展示不同等级的成就。
 *
 * @param checkedDays 已打卡天数
 * @param monthlyTotal 月累计打卡次数
 * @param bestStreak 最佳连续天数
 * @param stats 月度统计数据
 * @param modifier 修饰符
 */
@Composable
fun AchievementSection(
    modifier: Modifier = Modifier,
    checkedDays: Int,
    monthlyTotal: Int,
    bestStreak: Int,
    stats: MonthlyStats,
    todayCount: Int = 0,
    yearMonth: java.time.YearMonth = java.time.YearMonth.now(),
    onNavigateToVideo: (String) -> Unit = {},
) {
    val today = LocalDate.now()
    val isSinglesDay =
        today.monthValue == 11 && today.dayOfMonth == 11 && yearMonth.monthValue == 11 && todayCount == 0

    AnimatedVisibility(
        visible = checkedDays > 0 || isSinglesDay,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isSinglesDay) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(
                        text = stringResource(R.string.egg_singles),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val formattedTitles = mapOf(
                        "legend" to stringResource(R.string.legend_title),
                        "champion" to stringResource(R.string.champion_title),
                        "nice" to stringResource(R.string.nice_title),
                        "onFire" to stringResource(R.string.on_fire_title),
                        "great" to stringResource(R.string.great_title),
                        "weekStreak" to stringResource(R.string.week_streak_title),
                        "streak" to stringResource(R.string.streak_title),
                    )

                    val formattedSubs = mapOf(
                        "god" to stringResource(R.string.egg_god, monthlyTotal),
                        "top" to stringResource(R.string.achievement_desc_top, monthlyTotal),
                        "nice" to stringResource(R.string.egg_nice, monthlyTotal),
                        "days" to stringResource(R.string.achievement_desc_days, checkedDays),
                        "streak" to stringResource(R.string.achievement_desc_streak, bestStreak),
                    )

                    val mainRules = buildMainAchievementRules(
                        checkedDays = checkedDays,
                        monthlyTotal = monthlyTotal,
                        bestStreak = bestStreak,
                        formattedTitles = formattedTitles,
                        formattedSubs = formattedSubs,
                    )

                    val mainAchievements = evaluateMainAchievements(
                        rules = mainRules,
                        checkedDays = checkedDays,
                        monthlyTotal = monthlyTotal,
                        bestStreak = bestStreak,
                        defaultTitle = stringResource(R.string.keep_going_title),
                        defaultSubtitle = stringResource(R.string.achievement_desc_keep),
                    )

                    mainAchievements.forEach { achievement ->
                        if (achievement != mainAchievements.first()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(text = achievement.emoji, fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = achievement.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = achievement.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (stats.typeCounts.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stats.typeCounts.entries
                        .sortedByDescending { it.value }
                        .take(STATS_TYPE_DISPLAY_COUNT)
                        .forEach { (type, count) ->
                            AchievementMiniCard(
                                emoji = typeEmoji(type),
                                value = count.toString(),
                                label = type,
                                modifier = Modifier.weight(1f)
                            )
                        }
                }
            }

            val formattedLabels = mapOf(
                "dishVariety" to stringResource(R.string.ach_dish_variety),
                "topDish" to stringResource(R.string.ach_top_dish),
                "multiType" to stringResource(R.string.ach_multi_type),
                "nightOwl" to stringResource(R.string.ach_night_owl),
                "morning" to stringResource(R.string.ach_morning),
                "scholar" to stringResource(R.string.ach_scholar),
                "sixTimes" to stringResource(R.string.sex_times)
            )
            val extraRules = buildExtraAchievementRules(
                stats = stats,
                formattedLabels = formattedLabels,
            )
            val extraAchievements = evaluateExtraAchievements(
                rules = extraRules,
                stats = stats,
                maxCount = EXTRA_ACHIEVEMENT_DISPLAY_COUNT,
            )
            if (extraAchievements.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    extraAchievements.forEach { ach ->
                        AchievementMiniCard(
                            emoji = ach.emoji,
                            value = ach.title,
                            label = ach.subtitle,
                            onClick = if (ach.videoCode.isNotEmpty()) {
                                { onNavigateToVideo(ach.videoCode) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 小型成就卡片，用于 [AchievementSection] 内的横向列表。
 *
 * @param emoji 成就图标 emoji
 * @param value 成就数值
 * @param label 成就描述
 * @param modifier 修饰符
 */
@Composable
fun AchievementMiniCard(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    label: String,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else modifier
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
private fun PreviewAchievementSection() {
    AchievementSection(
        checkedDays = 10,
        monthlyTotal = 50,
        bestStreak = 5,
        stats = MonthlyStats(),
    )
}
