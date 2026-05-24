package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import com.yenaly.han1meviewer.ui.viewmodel.MonthlyStats

/** 月度统计中打卡类型卡片最大展示数量 */
internal const val STATS_TYPE_DISPLAY_COUNT = 4

/** 附加成就卡片最大展示数量 */
internal const val EXTRA_ACHIEVEMENT_DISPLAY_COUNT = 6

/**
 * 成就展示数据。
 *
 * @param emoji 成就图标
 * @param title 成就标题
 * @param subtitle 成就描述
 */
data class Achievement(
    val emoji: String,
    val title: String,
    val subtitle: String,
)

/**
 * 主成就判定规则。
 *
 * @param check 判定条件
 * @param emoji 图标
 * @param title 已格式化的标题
 * @param subtitle 已格式化的副标题
 */
data class MainAchievementRule(
    val check: (checkedDays: Int, monthlyTotal: Int, bestStreak: Int) -> Boolean,
    val emoji: String,
    val title: String,
    val subtitle: String,
)

/**
 * 附加成就判定规则。
 *
 * @param check 判定条件
 * @param emoji 图标
 * @param label 已格式化的标签
 * @param value 已格式化的数值
 */
data class ExtraAchievementRule(
    val check: (MonthlyStats) -> Boolean,
    val emoji: String,
    val label: String,
    val value: String,
)

/**
 * 构建主成就规则表。规则按数组顺序判定，首个匹配即返回。
 *
 * 调用方需在 @Composable 上下文中预先格式化所有字符串后传入。
 *
 * 新增成就：在返回列表追加一条 [MainAchievementRule] 即可。
 *
 * @param checkedDays 当月已打卡天数
 * @param monthlyTotal 当月累计打卡次数
 * @param bestStreak 当月最佳连续天数
 * @param formattedTitles 已格式化的标题 Map（resourceName -> 格式化后文本）
 * @param formattedSubs 已格式化的副标题 Map（resourceName -> 格式化后文本）
 */
fun buildMainAchievementRules(
    checkedDays: Int,
    monthlyTotal: Int,
    bestStreak: Int,
    formattedTitles: Map<String, String>,
    formattedSubs: Map<String, String>,
): List<MainAchievementRule> = buildList {
    val tl = { key: String -> formattedTitles[key] ?: key }
    val sb = { key: String -> formattedSubs[key] ?: key }

    if (monthlyTotal >= 200) add(
        MainAchievementRule(
            check = { _, t, _ -> t >= 200 },
            emoji = "\uD83E\uDD34",
            title = tl("legend"),
            subtitle = sb("god"),
        )
    )

    if (monthlyTotal >= 100) add(
        MainAchievementRule(
            check = { _, t, _ -> t >= 100 },
            emoji = "\uD83D\uDC51",
            title = tl("champion"),
            subtitle = sb("top"),
        )
    )

    if (monthlyTotal >= 69) add(
        MainAchievementRule(
            check = { _, t, _ -> t >= 69 },
            emoji = "\uD83D\uDE0F",
            title = tl("nice"),
            subtitle = sb("nice"),
        )
    )

    if (monthlyTotal >= 50) add(
        MainAchievementRule(
            check = { _, t, _ -> t >= 50 },
            emoji = "\uD83C\uDFC6",
            title = tl("champion"),
            subtitle = sb("top"),
        )
    )

    if (checkedDays >= 25) add(
        MainAchievementRule(
            check = { d, _, _ -> d >= 25 },
            emoji = "\uD83D\uDD25",
            title = tl("onFire"),
            subtitle = sb("days"),
        )
    )

    if (checkedDays >= 15) add(
        MainAchievementRule(
            check = { d, _, _ -> d >= 15 },
            emoji = "\uD83D\uDE80",
            title = tl("great"),
            subtitle = sb("days"),
        )
    )

    if (bestStreak >= 7) add(
        MainAchievementRule(
            check = { _, _, s -> s >= 7 },
            emoji = "\u2B50",
            title = tl("weekStreak"),
            subtitle = sb("streak"),
        )
    )

    if (bestStreak >= 3) add(
        MainAchievementRule(
            check = { _, _, s -> s >= 3 },
            emoji = "\uD83D\uDCAA",
            title = tl("streak"),
            subtitle = sb("streak"),
        )
    )
}

/**
 * 构建附加成就规则表。
 *
 * 调用方需在 @Composable 上下文中预先格式化所有标签后传入。
 *
 * 新增成就：在返回列表追加一条 [ExtraAchievementRule] 即可。
 *
 * @param stats 月度统计数据
 * @param formattedLabels 已格式化的标签 Map（resourceName -> 格式化后文本）
 */
fun buildExtraAchievementRules(
    stats: MonthlyStats,
    formattedLabels: Map<String, String>,
): List<ExtraAchievementRule> = buildList {
    val lb = { key: String -> formattedLabels[key] ?: key }

    if (stats.uniqueDishes >= 3) add(
        ExtraAchievementRule(
            check = { it.uniqueDishes >= 3 },
            emoji = "\uD83C\uDF7D\uFE0F",
            label = lb("dishVariety"),
            value = "${stats.uniqueDishes}种",
        )
    )

    if (stats.topDishCount >= 3) add(
        ExtraAchievementRule(
            check = { it.topDishCount >= 3 },
            emoji = "\uD83C\uDFAF",
            label = stats.topDish,
            value = "${lb("topDish")}·${stats.topDishCount}次",
        )
    )

    if (stats.maxDailyTypes >= 3) add(
        ExtraAchievementRule(
            check = { it.maxDailyTypes >= 3 },
            emoji = "\uD83C\uDF08",
            label = lb("multiType"),
            value = "${stats.maxDailyTypes}种",
        )
    )

    if (stats.dominantPeriod == "22~02") add(
        ExtraAchievementRule(
            check = { it.dominantPeriod == "22~02" },
            emoji = "\uD83E\uDD71",
            label = lb("nightOwl"),
            value = "22~02時",
        )
    )

    if (stats.dominantPeriod == "05~10") add(
        ExtraAchievementRule(
            check = { it.dominantPeriod == "05~10" },
            emoji = "\uD83C\uDF05",
            label = lb("morning"),
            value = "05~10時",
        )
    )

    if (stats.totalFeelingChars >= 100) add(
        ExtraAchievementRule(
            check = { it.totalFeelingChars >= 100 },
            emoji = "\uD83D\uDCDD",
            label = lb("scholar"),
            value = stats.scholarDate,
        )
    )
}

/**
 * 从规则表中选出首个匹配的主成就，无匹配时返回默认成就。
 */
fun evaluateMainAchievement(
    rules: List<MainAchievementRule>,
    checkedDays: Int,
    monthlyTotal: Int,
    bestStreak: Int,
    defaultTitle: String,
    defaultSubtitle: String,
): Achievement {
    val match = rules.firstOrNull { it.check(checkedDays, monthlyTotal, bestStreak) }
    return if (match != null) {
        Achievement(emoji = match.emoji, title = match.title, subtitle = match.subtitle)
    } else {
        Achievement(emoji = "\uD83D\uDC4D", title = defaultTitle, subtitle = defaultSubtitle)
    }
}

/**
 * 从规则表中筛选所有满足条件的附加成就（最多返回 [maxCount] 条）。
 */
fun evaluateExtraAchievements(
    rules: List<ExtraAchievementRule>,
    stats: MonthlyStats,
    maxCount: Int,
): List<Achievement> = rules
    .filter { it.check(stats) }
    .take(maxCount)
    .map { Achievement(emoji = it.emoji, title = it.value, subtitle = it.label) }
