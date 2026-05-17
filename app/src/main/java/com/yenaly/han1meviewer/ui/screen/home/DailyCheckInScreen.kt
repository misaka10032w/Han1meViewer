package com.yenaly.han1meviewer.ui.screen.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.provider.CalendarContract
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.entity.CheckInType
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.viewmodel.CheckInCalendarViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MonthlyStats
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun DailyCheckInScreen(
    activity: Activity,
    onBack: () -> Unit,
    onAddWidget: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    viewModel: CheckInCalendarViewModel = viewModel()
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
        CalendarCheckInScreen(
            paddingValues = innerPadding,
            viewModel = viewModel,
            showReport = showReport,
            onDismissReport = {
                showReport = false
                isReportFullscreen = false
            },
            isReportFullscreen = isReportFullscreen,
            onToggleReportFullscreen = { isReportFullscreen = !isReportFullscreen },
            onNavigateToVideo = onNavigateToVideo
        )
    }
}

private fun Activity.updateReportWindowMode(isFullscreen: Boolean) {
    requestedOrientation = if (isFullscreen) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.apply {
            if (isFullscreen) {
                hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
            }
        }
    } else {
        @Suppress("DEPRECATION")
        run {
            window.decorView.systemUiVisibility = if (isFullscreen) {
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
            } else {
                View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
}

// ═══════════════════════════════════════════
//  Contribution Report (GitHub-style calendar heatmap)
// ═══════════════════════════════════════════

private val contributionColors = listOf(
    Color.Transparent,
    Color(0xFF9BE9A8),
    Color(0xFF40C463),
    Color(0xFF30A14E),
    Color(0xFF216E39),
)

private fun getContributionLevel(count: Int): Int = when {
    count <= 0 -> 0
    count == 1 -> 1
    count == 2 -> 2
    count in 3..4 -> 3
    else -> 4
}

private fun buildYearWeeks(year: Int): List<List<LocalDate?>> {
    val start = LocalDate.of(year, 1, 1)
    val end = LocalDate.of(year, 12, 31)
    val weeks = mutableListOf<MutableList<LocalDate?>>()
    var currentWeek = MutableList<LocalDate?>(7) { null }
    var dayIndex = start.dayOfWeek.value - 1
    var date = start
    while (!date.isAfter(end)) {
        currentWeek[dayIndex] = date
        dayIndex++
        if (dayIndex == 7) {
            weeks.add(currentWeek)
            currentWeek = MutableList(7) { null }
            dayIndex = 0
        }
        date = date.plusDays(1)
    }
    if (currentWeek.any { it != null }) {
        weeks.add(currentWeek)
    }
    return weeks
}

private fun buildMonthLabels(year: Int, weeks: List<List<LocalDate?>>, monthFormat: String): List<Pair<String, Int>> {
    val labels = mutableListOf<Pair<String, Int>>()
    for (month in 1..12) {
        val firstDay = LocalDate.of(year, month, 1)
        val weekIdx = weeks.indexOfFirst { week -> firstDay in week }
        if (weekIdx >= 0) {
            labels.add(monthFormat.format(month) to weekIdx)
        }
    }
    return labels
}

@Composable
private fun ContributionReportDialog(
    viewModel: CheckInCalendarViewModel,
    onDismiss: () -> Unit,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {}
) {
    val today = LocalDate.now()
    var selectedYear by remember { mutableIntStateOf(today.year) }
    var viewMode by remember { mutableStateOf("year") }
    var selectedMonth by remember { mutableIntStateOf(today.monthValue) }
    val yearRecords = viewModel.yearRecords

    LaunchedEffect(selectedYear) {
        viewModel.loadYearRecords(selectedYear)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.checkin_report)) },
                    navigationIcon = {
                        FilledIconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewMode = "year" }) {
                            Text(
                                stringResource(R.string.report_year),
                                fontWeight = if (viewMode == "year") FontWeight.Bold else FontWeight.Normal,
                                color = if (viewMode == "year") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(onClick = { viewMode = "month" }) {
                            Text(
                                stringResource(R.string.report_month),
                                fontWeight = if (viewMode == "month") FontWeight.Bold else FontWeight.Normal,
                                color = if (viewMode == "month") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FilledIconButton(onClick = onToggleFullscreen) {
                            Image(
                                painter = painterResource(R.drawable.baseline_screen_rotation_24),
                                contentDescription = if (isFullscreen)
                                    stringResource(R.string.report_portrait)
                                else
                                    stringResource(R.string.report_landscape),
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                )
            }
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
                        onYearChange = { selectedYear = it }
                    )
                } else {
                    MonthContributionView(
                        year = selectedYear,
                        month = selectedMonth,
                        records = yearRecords,
                        today = today,
                        onYearChange = { selectedYear = it },
                        onMonthChange = { selectedMonth = it }
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

                val stats = if (viewMode == "year") viewModel.yearStats.value else viewModel.monthlyStats.value
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

@Composable
private fun YearContributionView(
    year: Int,
    records: Map<LocalDate, Int>,
    today: LocalDate,
    onYearChange: (Int) -> Unit
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

@Composable
private fun MonthContributionView(
    year: Int,
    month: Int,
    records: Map<LocalDate, Int>,
    today: LocalDate,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContributionLegend() {
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

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun CalendarCheckInScreen(
    paddingValues: PaddingValues,
    viewModel: CheckInCalendarViewModel = viewModel(),
    showReport: Boolean = false,
    onDismissReport: () -> Unit = {},
    isReportFullscreen: Boolean = false,
    onToggleReportFullscreen: () -> Unit = {},
    onNavigateToVideo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentMonth by viewModel.currentMonth
    val records = viewModel.records
    val checkedDays by viewModel.checkedDays
    val monthlyTotal by viewModel.monthlyTotal
    val today = LocalDate.now()
    val todayCount = records[today] ?: 0

    var forgotDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var suckBackDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var calendarDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var checkInDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var showEasterEgg by remember { mutableStateOf("") }
    var eggVisible by remember { mutableStateOf(false) }

    val (_, bestStreakThisMonth) = computeStreaks(records, currentMonth)

    val animatedCheckedDays by animateIntAsState(targetValue = checkedDays, label = "days")
    val animatedMonthlyTotal by animateIntAsState(targetValue = monthlyTotal, label = "total")
    val animatedBestStreak by animateIntAsState(targetValue = bestStreakThisMonth, label = "streak")

    val anchorMonth = remember { YearMonth.now() }
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }

    LaunchedEffect(currentMonth) {
        val monthsDiff = ChronoUnit.MONTHS.between(anchorMonth, currentMonth).toInt()
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
                if (pageMonth != currentMonth) {
                    if (pageMonth.isAfter(currentMonth)) viewModel.nextMonth()
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

    val handleDateClick: (LocalDate) -> Unit = { date ->
        when {
            date.isAfter(today) -> {
                calendarDialogDate = date
            }
            date.isBefore(today) && (records[date] ?: 0) == 0 -> {
                forgotDialogDate = date
            }
            else -> {
                checkInDialogDate = date
            }
        }
    }

    val handleDateLongClick: (LocalDate) -> Unit = { date ->
        val count = records[date] ?: 0
        if (count > 0 && date.isBefore(today)) {
            suckBackDialogDate = date
        } else if (count > 0) {
            viewModel.clearCheckIn(date)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        TodayCheckInCard(
            today = today,
            count = todayCount,
            maxCount = 20,
            onCheckIn = { checkInDialogDate = today },
            onClear = { viewModel.clearCheckIn(today) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatsCard(
            checkedDays = animatedCheckedDays,
            monthlyTotal = animatedMonthlyTotal,
            bestStreak = animatedBestStreak
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
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(painterResource(R.drawable.previous_double_arrow_24), "previous")
                }
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
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
                records = records,
                today = today,
                onDateClick = handleDateClick,
                onDateLongClick = handleDateLongClick
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedVisibility(
            visible = eggVisible,
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
                    text = showEasterEgg,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val stats by viewModel.monthlyStats
        AchievementSection(
            checkedDays = checkedDays,
            monthlyTotal = monthlyTotal,
            bestStreak = bestStreakThisMonth,
            stats = stats
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

    ConfirmDialog(
        visible = forgotDialogDate != null,
        title = stringResource(R.string.forgot_title),
        message = forgotDialogDate?.let { stringResource(R.string.forgot_message, it.format(DateTimeFormatter.ofPattern("MM月dd日"))) } ?: "",
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
        message = calendarDialogDate?.let { stringResource(R.string.calendar_dialog_message, it.format(DateTimeFormatter.ofPattern("MM月dd日"))) } ?: "",
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
        message = suckBackDialogDate?.let { stringResource(R.string.suck_back_message, it.format(DateTimeFormatter.ofPattern("MM月dd日")), records[it] ?: 0) } ?: "",
        confirmText = stringResource(R.string.suck_back_confirm),
        dismissText = stringResource(R.string.suck_back_dismiss),
        onConfirm = {
            suckBackDialogDate?.let { viewModel.clearCheckIn(it); Toast.makeText(context, R.string.suck_back_done, Toast.LENGTH_SHORT).show() }
            suckBackDialogDate = null
        },
        onDismiss = { suckBackDialogDate = null },
    )

    checkInDialogDate?.let { date ->
        CheckInDialog(
            date = date,
            viewModel = viewModel,
            onNavigateToVideo = onNavigateToVideo,
            onEasterEgg = { msg -> showEasterEgg = msg },
            onDismiss = { checkInDialogDate = null }
        )
    }

    if (showReport) {
        ContributionReportDialog(
            viewModel = viewModel,
            onDismiss = onDismissReport,
            isFullscreen = isReportFullscreen,
            onToggleFullscreen = onToggleReportFullscreen
        )
    }
}

private fun createCalendarEvent(context: android.content.Context, date: LocalDate) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.dir/event")
        putExtra(CalendarContract.Events.TITLE, context.getString(R.string.calendar_title, date.monthValue, date.dayOfMonth))
        putExtra(CalendarContract.Events.DESCRIPTION, context.getString(R.string.calendar_desc))
        putExtra(CalendarContract.Events.EVENT_LOCATION, context.getString(R.string.calendar_location))
        putExtra(
            CalendarContract.EXTRA_EVENT_BEGIN_TIME,
            date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        putExtra(
            CalendarContract.EXTRA_EVENT_END_TIME,
            date.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        putExtra(CalendarContract.Events.ALL_DAY, true)
        putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
    }
    try {
        context.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_calendar_app, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun TodayCheckInCard(
    today: LocalDate,
    count: Int,
    maxCount: Int = 20,
    onCheckIn: () -> Unit,
    onClear: () -> Unit
) {
    val isMaxed = count >= maxCount
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = if (count > 0) "${stringResource(R.string.today_checked)} $count/$maxCount ${stringResource(R.string.times)}"
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

@Composable
private fun StatsCard(
    checkedDays: Int,
    monthlyTotal: Int,
    bestStreak: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun RowScope.StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
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

@Composable
private fun AchievementSection(
    checkedDays: Int,
    monthlyTotal: Int,
    bestStreak: Int,
    stats: MonthlyStats
) {
    AnimatedVisibility(
        visible = checkedDays > 0,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    val (emoji, title, subtitle) = when {
                        monthlyTotal >= 200 -> Triple(
                            "\uD83E\uDD34", stringResource(R.string.legend_title),
                            stringResource(R.string.egg_god, monthlyTotal)
                        )
                        monthlyTotal >= 100 -> Triple(
                            "\uD83D\uDC51", stringResource(R.string.champion_title),
                            stringResource(R.string.achievement_desc_top, monthlyTotal)
                        )
                        monthlyTotal >= 69 -> Triple(
                            "\uD83D\uDE0F", stringResource(R.string.nice_title),
                            stringResource(R.string.egg_nice, monthlyTotal)
                        )
                        monthlyTotal >= 50 -> Triple(
                            "\uD83C\uDFC6", stringResource(R.string.champion_title),
                            stringResource(R.string.achievement_desc_top, monthlyTotal)
                        )
                        checkedDays >= 25 -> Triple(
                            "\uD83D\uDD25", stringResource(R.string.on_fire_title),
                            stringResource(R.string.achievement_desc_days, checkedDays)
                        )
                        checkedDays >= 15 -> Triple(
                            "\uD83D\uDE80", stringResource(R.string.great_title),
                            stringResource(R.string.achievement_desc_days, checkedDays)
                        )
                        bestStreak >= 7 -> Triple(
                            "\u2B50", stringResource(R.string.week_streak_title),
                            stringResource(R.string.achievement_desc_streak, bestStreak)
                        )
                        bestStreak >= 3 -> Triple(
                            "\uD83D\uDCAA", stringResource(R.string.streak_title),
                            stringResource(R.string.egg_streak, bestStreak)
                        )
                        else -> Triple(
                            "\uD83D\uDC4D", stringResource(R.string.keep_going_title),
                            stringResource(R.string.achievement_desc_keep)
                        )
                    }
                    Text(text = emoji, fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    val today = LocalDate.now()
                    if (today.monthValue == 11 && today.dayOfMonth == 11 && checkedDays == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.egg_singles),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
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
                        .take(4)
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

            val extraAchievements = buildList {
                if (stats.uniqueDishes >= 3) add(Triple("\uD83C\uDF7D\uFE0F", stringResource(R.string.ach_dish_variety), "${stats.uniqueDishes}种"))
                if (stats.topDishCount >= 3) add(Triple("\uD83C\uDFAF", stats.topDish, "${stringResource(R.string.ach_top_dish)}·${stats.topDishCount}次"))
                if (stats.maxDailyTypes >= 3) add(Triple("\uD83C\uDF08", stringResource(R.string.ach_multi_type), "${stats.maxDailyTypes}种"))
                if (stats.dominantPeriod == "22~02") add(Triple("\uD83E\uDD71", stringResource(R.string.ach_night_owl), "22~02時"))
                if (stats.dominantPeriod == "05~10") add(Triple("\uD83C\uDF05", stringResource(R.string.ach_morning), "05~10時"))
                if (stats.totalFeelingChars >= 100) add(Triple("\uD83D\uDCDD", stringResource(R.string.ach_scholar), stats.scholarDate))
            }
            if (extraAchievements.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    extraAchievements.take(3).forEach { (emoji, label, value) ->
                        AchievementMiniCard(
                            emoji = emoji,
                            value = value,
                            label = label,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementMiniCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun typeEmoji(type: String): String = when (type) {
    CheckInType.MASTURBATION.storeName -> "\uD83E\uDD1C"
    CheckInType.WET_DREAM.storeName -> "\uD83D\uDCA4"
    CheckInType.SEX.storeName -> "\uD83D\uDC91"
    CheckInType.ORAL.storeName -> "\uD83D\uDC45"
    CheckInType.OTHER.storeName -> "\u2753"
    else -> "\uD83D\uDCCA"
}

private fun computeStreaks(
    records: Map<LocalDate, Int>,
    month: YearMonth
): Pair<Int, Int> {
    val today = LocalDate.now()
    var currentStreak = 0
    var cursor = today
    while ((records[cursor] ?: 0) > 0) {
        currentStreak++
        cursor = cursor.minusDays(1)
    }

    var bestStreak = 0
    var streak = 0
    for (day in 1..month.lengthOfMonth()) {
        val date = month.atDay(day)
        if ((records[date] ?: 0) > 0) {
            streak++
            if (streak > bestStreak) bestStreak = streak
        } else {
            streak = 0
        }
    }
    return currentStreak to bestStreak
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    records: Map<LocalDate, Int>,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    onDateLongClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        userScrollEnabled = false,
        modifier = modifier.fillMaxWidth()
    ) {
        item(span = { GridItemSpan(7) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(
                    stringResource(R.string.mon), stringResource(R.string.tue),
                    stringResource(R.string.wed), stringResource(R.string.thu),
                    stringResource(R.string.fri), stringResource(R.string.sat),
                    stringResource(R.string.sun)
                ).forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        items(firstDayOfWeek - 1) {
            Spacer(modifier = Modifier.size(48.dp))
        }

        items(daysInMonth) { day ->
            val date = yearMonth.atDay(day + 1)
            val count = records[date] ?: 0
            val isToday = date == today
            val transition = updateTransition(targetState = count > 0, label = "check")

            val bgColor by transition.animateColor(label = "bg") { checked ->
                when {
                    checked -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    else -> Color.Transparent
                }
            }
            val borderColor by transition.animateColor(label = "border") { checked ->
                when {
                    isToday && checked -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.tertiary
                    checked -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .border(
                        width = if (isToday) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .combinedClickable(
                        onClick = { onDateClick(date) },
                        onLongClick = { onDateLongClick(date) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = (day + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isToday -> MaterialTheme.colorScheme.onTertiaryContainer
                            count > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (count > 0) {
                        Text(
                            text = "x$count",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckInDialog(
    date: LocalDate,
    viewModel: CheckInCalendarViewModel,
    onNavigateToVideo: (String) -> Unit,
    onEasterEgg: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var existingRecords by remember { mutableStateOf<List<CheckInRecordEntity>>(emptyList()) }
    var watchHistory by remember { mutableStateOf<List<WatchHistoryEntity>>(emptyList()) }
    var todayCount by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        viewModel.getRecordsByDate(date) { existingRecords = it }
        viewModel.getRecentWatchHistory(10) { watchHistory = it }
        viewModel.getCountByDate(date) { todayCount = it; loaded = true }
    }

    if (!loaded) return

    val canAddMore = todayCount < 20
    val coverUrlMap = remember(watchHistory) {
        watchHistory.associate { it.videoCode to it.coverUrl }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxSize(0.85f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("yyyy\u5E74MM\u6708dd\u65E5")),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                if (existingRecords.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.dialog_existing_records),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )
                    existingRecords.forEachIndexed { index, record ->
                        ExistingRecordItem(
                            index = index + 1,
                            record = record,
                            coverUrlMap = coverUrlMap,
                            onNavigateToVideo = onNavigateToVideo,
                            onDelete = {
                                viewModel.deleteRecord(record)
                                viewModel.getRecordsByDate(date) { existingRecords = it }
                                viewModel.getCountByDate(date) { todayCount = it }
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }

                if (canAddMore) {
                    AddCheckInForm(
                        date = date,
                        watchHistory = watchHistory,
                        viewModel = viewModel,
                        onNavigateToVideo = onNavigateToVideo,
                        onEasterEgg = onEasterEgg,
                        onDismiss = onDismiss
                    )
                } else if (todayCount >= 20) {
                    Text(
                        text = stringResource(R.string.dialog_max_reached),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCheckInForm(
    date: LocalDate,
    watchHistory: List<WatchHistoryEntity>,
    viewModel: CheckInCalendarViewModel,
    onNavigateToVideo: (String) -> Unit,
    onEasterEgg: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(CheckInType.MASTURBATION) }
    val sideDishes = remember { mutableStateListOf<String>() }
    var sideDishInput by remember { mutableStateOf("") }
    var feeling by remember { mutableStateOf("") }
    val sep = "\u001E"

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.dialog_type_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CheckInType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(stringResource(type.displayNameRes)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.dialog_sidedish_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (sideDishes.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                sideDishes.forEach { dish ->
                    val dishIdx = sideDishes.indexOf(dish)
                    FilterChip(
                        selected = true,
                        onClick = { sideDishes.removeAt(dishIdx) },
                        label = { Text(dish.substringBefore(sep)) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.remove),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        if (sideDishes.size < 5) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = sideDishInput,
                    onValueChange = { sideDishInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.dialog_sidedish_hint)) },
                    singleLine = true
                )
                Button(
                    onClick = {
                        val trimmed = sideDishInput.trim()
                        if (trimmed.isNotEmpty() && !sideDishes.any { it.substringBefore(sep) == trimmed }) {
                            sideDishes.add("$trimmed$sep")
                            sideDishInput = ""
                        }
                    },
                    enabled = sideDishInput.trim().isNotEmpty() && sideDishes.size < 5,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.add))
                }
            }
        }

        if (watchHistory.isNotEmpty() && sideDishes.size < 5) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.dialog_recent_watched),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            watchHistory.forEach { watch ->
                WatchHistoryItem(
                    watch = watch,
                    onNavigateToVideo = { onNavigateToVideo(watch.videoCode) },
                    onClick = {
                        val dishStr = "${watch.title}$sep${watch.videoCode}"
                        if (!sideDishes.any { it.substringBefore(sep) == watch.title } && sideDishes.size < 5) {
                            sideDishes.add(dishStr)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.dialog_feeling_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = feeling,
            onValueChange = { feeling = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            placeholder = { Text(stringResource(R.string.dialog_feeling_hint)) },
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            val eggSex = stringResource(R.string.egg_six)
            val eggNine = stringResource(R.string.egg_nine)
            val eggGod = stringResource(R.string.egg_god, 20)
            val eggRoundTemplate = stringResource(R.string.egg_round)
            Button(
                onClick = {
                    val dishes = sideDishes.joinToString(",")
                    val now = LocalTime.now()
                    viewModel.addRecord(date, now.format(DateTimeFormatter.ofPattern("HH:mm")), selectedType.storeName, dishes, feeling)

                    viewModel.getCountByDate(date) { newCount ->
                        when {
                            newCount == 6 -> onEasterEgg(eggSex)
                            newCount == 9 -> onEasterEgg(eggNine)
                            newCount == 20 -> onEasterEgg(eggGod)
                            newCount % 10 == 0 -> onEasterEgg(eggRoundTemplate.format(newCount))
                        }
                    }

                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ExistingRecordItem(
    index: Int,
    record: CheckInRecordEntity,
    coverUrlMap: Map<String, String>,
    onNavigateToVideo: (String) -> Unit,
    onDelete: () -> Unit
) {
    val sep = "\u001E"
    val sideDishItems = remember(record.sideDishes) {
        record.sideDishes.split(",").filter { it.isNotBlank() }.map { item ->
            val parts = if (item.contains(sep)) {
                item.split(sep)
            } else {
                val p = item.split("|")
                if (p.size >= 2) listOf(p[0], p[1]) else listOf(item, "")
            }
            val title = parts.getOrElse(0) { item }
            val videoCode = parts.getOrElse(1) { "" }
            title to videoCode
        }
    }
    val coverItems = sideDishItems.filter { (_, code) -> code.isNotBlank() }
    val customItems = sideDishItems.filter { (_, code) -> code.isBlank() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$index",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = stringResource(CheckInType.fromDisplayName(record.type).displayNameRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (record.time.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = record.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (coverItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    coverItems.forEach { (title, code) ->
                        val coverUrl = coverUrlMap[code]
                        if (coverUrl != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f).widthIn(max = 140.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 90.dp)
                                        .clickable { onNavigateToVideo(code) }
                                ) {
                                    AsyncImage(
                                        model = coverUrl,
                                        contentDescription = title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (customItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = customItems.joinToString(", ") { it.first },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.feeling.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = record.feeling,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchHistoryItem(
    watch: WatchHistoryEntity,
    onNavigateToVideo: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = watch.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onNavigateToVideo),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Text(
                    text = watch.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stringResource(R.string.dialog_add_sidedish),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onClick)
            )
        }
    }
}
