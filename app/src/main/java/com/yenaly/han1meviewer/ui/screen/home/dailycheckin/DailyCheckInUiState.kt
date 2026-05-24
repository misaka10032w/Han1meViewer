package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import com.yenaly.han1meviewer.ui.viewmodel.MonthlyStats
import java.time.LocalDate
import java.time.YearMonth

/**
 * 打卡日历页面的 UI 状态。
 *
 * @param currentMonth 当前展示的月份
 * @param records 各日期的打卡记录数 (日期 -> 次数)
 * @param checkedDays 当月已打卡天数
 * @param monthlyTotal 当月累计打卡次数
 * @param bestStreakThisMonth 当月最佳连续打卡天数
 * @param monthlyStats 当月统计数据
 * @param today 今天的日期
 * @param todayCount 今天的打卡次数
 * @param showEasterEgg 当前展示的彩蛋文本，空字符串表示无彩蛋
 * @param eggVisible 彩蛋是否可见
 */
data class DailyCheckInUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val records: Map<LocalDate, Int> = emptyMap(),
    val checkedDays: Int = 0,
    val monthlyTotal: Int = 0,
    val bestStreakThisMonth: Int = 0,
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val today: LocalDate = LocalDate.now(),
    val todayCount: Int = 0,
)

/**
 * 打卡日历页面的用户交互事件。
 */
sealed interface DailyCheckInEvent {
    /** 点击某个日期 */
    data class OnDateClick(val date: LocalDate) : DailyCheckInEvent

    /** 长按某个日期 */
    data class OnDateLongClick(val date: LocalDate) : DailyCheckInEvent

    /** 切换到上个月 */
    data object OnPreviousMonth : DailyCheckInEvent

    /** 切换到下个月 */
    data object OnNextMonth : DailyCheckInEvent

    /** 点击今日打卡按钮 */
    data object OnTodayCheckIn : DailyCheckInEvent

    /** 点击今日清除按钮 */
    data object OnTodayClear : DailyCheckInEvent

    /** 打开贡献报表 */
    data object OnShowReport : DailyCheckInEvent
}
