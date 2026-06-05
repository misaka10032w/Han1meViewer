package com.yenaly.han1meviewer.ui.screen.home

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.screen.home.dailycheckin.CheckInDialog
import com.yenaly.han1meviewer.ui.screen.home.dailycheckin.ContributionReportDialog
import com.yenaly.han1meviewer.ui.screen.home.dailycheckin.DailyCheckInContent
import com.yenaly.han1meviewer.ui.screen.home.dailycheckin.DailyCheckInEvent
import com.yenaly.han1meviewer.ui.screen.home.dailycheckin.DailyCheckInUiState
import com.yenaly.han1meviewer.ui.screen.home.dailycheckin.createCalendarEvent
import com.yenaly.han1meviewer.ui.screen.home.dailycheckin.updateReportWindowMode
import com.yenaly.han1meviewer.ui.viewmodel.CheckInCalendarViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 打卡日历页面 Screen 层。
 *
 * 作为 V-S-C 架构的胶水层：订阅 ViewModel 状态生成 [DailyCheckInUiState]，
 * 将 [DailyCheckInEvent] 映射到 ViewModel 操作和导航。
 *
 * @param activity 宿主 Activity，用于全屏/方向控制
 * @param onBack 返回回调
 * @param onAddWidget 添加桌面小组件回调
 * @param onNavigateToVideo 跳转到视频详情回调
 * @param viewModel 打卡日历 ViewModel
 */
@Composable
fun DailyCheckInScreen(
    activity: Activity,
    onBack: () -> Unit,
    onAddWidget: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    viewModel: CheckInCalendarViewModel = viewModel(),
) {
    var showReport by rememberSaveable { mutableStateOf(false) }
    var isReportFullscreen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(activity, isReportFullscreen) {
        activity.updateReportWindowMode(isReportFullscreen)
    }

    DisposableEffect(activity) {
        onDispose {
            activity.updateReportWindowMode(false)
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val yearRecords by viewModel.yearRecords.collectAsStateWithLifecycle()
    val yearStats by viewModel.yearStats.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }

    var forgotDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var suckBackDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var calendarDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var checkInDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var showEasterEgg by remember { mutableStateOf("") }
    var eggVisible by remember { mutableStateOf(false) }

    var reportSelectedYear by remember { mutableIntStateOf(today.year) }
    var reportViewMode by remember { mutableStateOf("year") }
    var reportSelectedMonth by remember { mutableIntStateOf(today.monthValue) }

    val anchorMonth = remember { YearMonth.now() }
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }

    LaunchedEffect(uiState.currentMonth) {
        val monthsDiff = ChronoUnit.MONTHS.between(anchorMonth, uiState.currentMonth).toInt()
        val targetPage = initialPage + monthsDiff
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val pageMonth = anchorMonth.plusMonths((page - initialPage).toLong())
                if (pageMonth != uiState.currentMonth) {
                    if (pageMonth.isAfter(uiState.currentMonth)) viewModel.nextMonth()
                    else viewModel.previousMonth()
                }
            }
    }

    LaunchedEffect(showEasterEgg) {
        if (showEasterEgg.isNotEmpty()) {
            eggVisible = true
            kotlinx.coroutines.delay(1500)
            eggVisible = false
        }
    }

    val context = LocalContext.current

    val handleEvent: (DailyCheckInEvent) -> Unit = { event ->
        when (event) {
            is DailyCheckInEvent.OnDateClick -> {
                when {
                    event.date.isAfter(today) -> {
                        calendarDialogDate = event.date
                    }

                    event.date.isBefore(today) && (uiState.records[event.date] ?: 0) == 0 -> {
                        forgotDialogDate = event.date
                    }

                    else -> {
                        checkInDialogDate = event.date
                    }
                }
            }

            is DailyCheckInEvent.OnDateLongClick -> {
                val count = uiState.records[event.date] ?: 0
                if (count > 0 && event.date.isBefore(today)) {
                    suckBackDialogDate = event.date
                } else if (count > 0) {
                    viewModel.clearCheckIn(event.date)
                }
            }

            DailyCheckInEvent.OnPreviousMonth -> viewModel.previousMonth()
            DailyCheckInEvent.OnNextMonth -> viewModel.nextMonth()
            DailyCheckInEvent.OnTodayCheckIn -> {
                checkInDialogDate = today
            }

            DailyCheckInEvent.OnTodayClear -> viewModel.clearCheckIn(today)
            DailyCheckInEvent.OnShowReport -> {
                showReport = true
            }
        }
    }

    val scrollBehavior = pinnedScrollBehavior(rememberTopAppBarState())
    HanimeScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        title = stringResource(R.string.has_cum),
        onBack = onBack,
        scrollBehavior = scrollBehavior,
        actions = {
            FilledIconButton(onClick = { showReport = true }) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.checkin_report)
                )
            }
            FilledIconButton(onClick = onAddWidget) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_widget)
                )
            }
        },
    ) { innerPadding ->
        DailyCheckInContent(
            paddingValues = innerPadding,
            uiState = uiState,
            onEvent = handleEvent,
            onNavigateToVideo = onNavigateToVideo,
            showEasterEgg = showEasterEgg,
            eggVisible = eggVisible,
            pagerState = pagerState,
            anchorMonth = anchorMonth,
            initialPage = initialPage,
        )
    }

    ConfirmDialog(
        visible = forgotDialogDate != null,
        title = stringResource(R.string.forgot_title),
        message = forgotDialogDate?.let {
            stringResource(
                R.string.forgot_message,
                it.format(DateTimeFormatter.ofPattern("MM月dd日"))
            )
        } ?: "",
        confirmText = stringResource(R.string.forgot_confirm),
        dismissText = stringResource(R.string.forgot_dismiss),
        onConfirm = {
            forgotDialogDate?.let { checkInDialogDate = it }
            forgotDialogDate = null
        },
        onDismiss = { forgotDialogDate = null },
    )

    ConfirmDialog(
        visible = calendarDialogDate != null,
        title = stringResource(R.string.calendar_dialog_title),
        message = calendarDialogDate?.let {
            stringResource(
                R.string.calendar_dialog_message,
                it.format(DateTimeFormatter.ofPattern("MM月dd日"))
            )
        } ?: "",
        confirmText = stringResource(R.string.calendar_dialog_confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            calendarDialogDate?.let { createCalendarEvent(context, it) }
            calendarDialogDate = null
        },
        onDismiss = { calendarDialogDate = null },
    )

    ConfirmDialog(
        visible = suckBackDialogDate != null,
        title = stringResource(R.string.suck_back_title),
        message = suckBackDialogDate?.let {
            stringResource(
                R.string.suck_back_message,
                it.format(DateTimeFormatter.ofPattern("MM月dd日")),
                uiState.records[it] ?: 0
            )
        } ?: "",
        confirmText = stringResource(R.string.suck_back_confirm),
        dismissText = stringResource(R.string.suck_back_dismiss),
        onConfirm = {
            suckBackDialogDate?.let {
                viewModel.clearCheckIn(it)
                Toast.makeText(
                    context,
                    R.string.suck_back_done,
                    Toast.LENGTH_SHORT
                ).show()
            }
            suckBackDialogDate = null
        },
        onDismiss = { suckBackDialogDate = null },
    )

    checkInDialogDate?.let { date ->
        CheckInDialog(
            date = date,
            onLoadRecords = { d, cb -> viewModel.getRecordsByDate(d, cb) },
            onLoadWatchHistory = { limit, cb -> viewModel.getRecentWatchHistory(limit, cb) },
            onLoadSideDishCoverMap = { records, cb -> viewModel.getSideDishCoverMap(records, cb) },
            onGetCountByDate = { d, cb -> viewModel.getCountByDate(d, cb) },
            onAddRecord = { d, time, type, sideDishes, feeling ->
                viewModel.addRecord(d, time, type, sideDishes, feeling)
            },
            onDeleteRecord = { record, onDone -> viewModel.deleteRecord(record, onDone) },
            onNavigateToVideo = onNavigateToVideo,
            onEasterEgg = { msg -> showEasterEgg = msg },
            onDismiss = { checkInDialogDate = null },
        )
    }

    if (showReport) {
        ContributionReportDialog(
            selectedYear = reportSelectedYear,
            viewMode = reportViewMode,
            selectedMonth = reportSelectedMonth,
            yearRecords = yearRecords,
            yearStats = yearStats,
            onYearChange = { reportSelectedYear = it },
            onViewModeChange = { reportViewMode = it },
            onMonthChange = { reportSelectedMonth = it },
            onDismiss = {
                showReport = false
                isReportFullscreen = false
            },
            isFullscreen = isReportFullscreen,
            onToggleFullscreen = { isReportFullscreen = !isReportFullscreen },
            onLoadYearRecords = { viewModel.loadYearRecords(it) },
        )
    }
}
