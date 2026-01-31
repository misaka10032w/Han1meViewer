package com.yenaly.han1meviewer.ui.fragment.dailycheckin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.fragment.generateFakeCheckInRecords
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.CheckInCalendarViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DailyCheckInFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            duration = 500L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            duration = 500L
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HanimeTheme {
                    val scrollBehavior =
                        TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                    Scaffold(
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            CenterAlignedTopAppBar(
                                colors = topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                title = {
                                    Text(
                                        stringResource(R.string.has_cum),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            lifecycleScope.launch { findNavController().navigateUp() }
                                        }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Localized description"
                                        )
                                    }
                                },
                                scrollBehavior = scrollBehavior,
                            )
                        },
                    ) { innerPadding ->
                        CalendarCheckInScreen(innerPadding)
                    }
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarCheckInScreen(
    paddingValues: PaddingValues,
    viewModel: CheckInCalendarViewModel = viewModel()
) {
    val currentMonth by viewModel.currentMonth
    val records = viewModel.records
    val checkedDays by viewModel.checkedDays
    val monthlyTotal by viewModel.monthlyTotal
    val animatedCheckedDays by animateIntAsState(
        targetValue = checkedDays,
        label = "CheckedDaysAnimation"
    )
    val animatedMonthlyTotal by animateIntAsState(
        targetValue = monthlyTotal,
        label = "CheckedDaysAnimation"
    )

    // 锚点月份：用于计算 Pager 的相对偏移量
    val anchorMonth = remember { YearMonth.now() }
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }

    // 1. 监听 ViewModel 的月份变化（比如点击了左右箭头），驱动 Pager 滚动
    LaunchedEffect(currentMonth) {
        val monthsDiff = ChronoUnit.MONTHS.between(anchorMonth, currentMonth).toInt()
        val targetPage = initialPage + monthsDiff
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // 2. 监听 Pager 的滚动，更新 ViewModel
    // 使用 snapshotFlow + distinctUntilChanged 减少不必要的触发，优化性能
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val pageMonth = anchorMonth.plusMonths((page - initialPage).toLong())
                // 只有当计算出的月份与 ViewModel 当前月份不一致时才调用更新
                // 这里使用简单的比较，避免频繁调用 next/prev
                if (pageMonth != currentMonth) {
                    if (pageMonth.isAfter(currentMonth)) {
                        viewModel.nextMonth()
                    } else if (pageMonth.isBefore(currentMonth)) {
                        viewModel.previousMonth()
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 标题和月份导航
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.checkin_calendar),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "pre month")
                }

                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "next month")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 天数统计
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.this_month_checkin),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.days, animatedCheckedDays),
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.has_cum_days),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.counts, animatedMonthlyTotal),
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 日历主体
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Top,
            // 【关键优化】：预加载前后各 1 页。这解决了“不跟手”和“延迟”的问题。
            // 当你开始滑动时，下一页的日历已经被渲染好了，所以非常流畅。
            beyondViewportPageCount = 1,
            // 【关键优化】：为每一页指定唯一的 Key。这有助于 Compose 智能复用缓存。
            key = { page -> page }
        ) { page ->
            val monthForPage = anchorMonth.plusMonths((page - initialPage).toLong())

            // 注意：这里我们传入 records，但实际上如果 records 只是当前月的数据，
            // 预加载的页面可能拿不到数据（取决于 ViewModel 实现）。
            // 但 UI 结构本身会被预加载，保证滑动流畅。
            CalendarGrid(
                yearMonth = monthForPage,
                records = records,
                onDateClick = { date -> viewModel.incrementCheckIn(date) },
                onDateLongClick = { date -> viewModel.clearCheckIn(date) }
            )
        }

        AnimatedVisibility(
            visible = checkedDays > 15,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(
                        text = "66666",
                        fontSize = 40.sp
                    )
                    Text(
                        text = "🐮🍺",
                        fontSize = 60.sp
                    )
                }
            }
        }
        Text(
            text = "Tips: 长按取消"
        )
        Text(
            text = "建设中。。。如果你有好的想法请留言"
        )
    }
}

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    records: Map<LocalDate, Int>,
    onDateClick: (LocalDate) -> Unit,
    onDateLongClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        // 【关键优化】：禁止内部 Grid 的滚动事件。
        // 因为日历通常一页能显示完，禁用后可以将触摸事件完全无冲突地交给 Pager。
        userScrollEnabled = false,
        modifier = modifier.fillMaxWidth()
    ) {
        // 星期标题
        item(span = { GridItemSpan(7) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        items(firstDayOfWeek - 1) {
            Spacer(modifier = Modifier.size(48.dp))
        }

        // 日期单元格
        items(daysInMonth) { day ->
            val date = yearMonth.atDay(day + 1)
            val isChecked = records[date] ?: false
            val transition = updateTransition(targetState = isChecked, label = "CheckInTransition")
            val count = records[date] ?: 0
            val bgColor by transition.animateColor(label = "BackgroundColor") { checked ->
                if (count > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else Color.Transparent
            }

            val borderColor by transition.animateColor(label = "BorderColor") { checked ->
                if (count > 0) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(1.dp, borderColor, CircleShape)
                    .combinedClickable(
                        onClick = { onDateClick(date) },
                        onLongClick = { onDateLongClick(date) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = (day + 1).toString(),
                        color = if (date == LocalDate.now()) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    AnimatedVisibility(visible = count > 0) {
                        Text(
                            text = "\uD83E\uDD8Cx$count",
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
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