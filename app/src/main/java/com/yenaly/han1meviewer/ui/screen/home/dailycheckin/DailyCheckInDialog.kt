package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.entity.CheckInType
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 打卡弹窗。展示历史记录、添加新记录的表单。
 *
 *
 * @param date 打卡日期
 * @param onLoadRecords 加载该日期已有记录的回调
 * @param onLoadWatchHistory 加载最近观看历史的回调
 * @param onGetCountByDate 查询该日期打卡次数的回调
 * @param onAddRecord 新增打卡记录的回调
 * @param onDeleteRecord 删除单条记录的回调
 * @param onNavigateToVideo 跳转到视频详情的回调
 * @param onEasterEgg 触发彩蛋文字的回调
 * @param onDismiss 关闭弹窗的回调
 */
@Composable
fun CheckInDialog(
    date: LocalDate,
    onLoadRecords: (LocalDate, (List<CheckInRecordEntity>) -> Unit) -> Unit,
    onLoadWatchHistory: (Int, (List<WatchHistoryEntity>) -> Unit) -> Unit,
    onGetCountByDate: (LocalDate, (Int) -> Unit) -> Unit,
    onAddRecord: (LocalDate, String, String, String, String) -> Unit,
    onDeleteRecord: (CheckInRecordEntity) -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onEasterEgg: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var existingRecords by remember { mutableStateOf<List<CheckInRecordEntity>>(emptyList()) }
    var watchHistory by remember { mutableStateOf<List<WatchHistoryEntity>>(emptyList()) }
    var todayCount by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        onLoadRecords(date) { existingRecords = it }
        onLoadWatchHistory(10) { watchHistory = it }
        onGetCountByDate(date) { todayCount = it; loaded = true }
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
                                onDeleteRecord(record)
                                onLoadRecords(date) { existingRecords = it }
                                onGetCountByDate(date) { todayCount = it }
                            }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                    )
                }

                if (canAddMore) {
                    val eggSex = stringResource(R.string.egg_three)
                    val eggNine = stringResource(R.string.egg_four)
                    val eggGod = stringResource(R.string.egg_god, 6)
                    val eggRoundTemplate = stringResource(R.string.egg_round)
                    AddCheckInForm(
                        watchHistory = watchHistory,
                        onNavigateToVideo = onNavigateToVideo,
                        onAddRecord = { time, type, sideDishes, feeling ->
                            onAddRecord(date, time, type, sideDishes, feeling)
                            onGetCountByDate(date) { newCount ->
                                when {
                                    newCount + 1 == 3 -> onEasterEgg(eggSex)
                                    newCount + 1 == 4 -> onEasterEgg(eggNine)
                                    newCount + 1 == 6 -> onEasterEgg(eggGod)
                                    newCount + 1 % 10 == 0 -> onEasterEgg(
                                        eggRoundTemplate.format(
                                            newCount
                                        )
                                    )
                                }
                                onDismiss()
                            }
                        },
                        onDismiss = onDismiss,
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

/**
 * 新增打卡表单。
 *
 * @param watchHistory 最近观看记录
 * @param onNavigateToVideo 跳转到视频详情的回调
 * @param onAddRecord 提交打卡记录的回调 (time, type, sideDishes, feeling)
 * @param onDismiss 取消/关闭回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCheckInForm(
    watchHistory: List<WatchHistoryEntity>,
    onNavigateToVideo: (String) -> Unit,
    onAddRecord: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
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
            Button(
                onClick = {
                    val dishes = sideDishes.joinToString(",")
                    val now = LocalTime.now()
                    onAddRecord(
                        now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        selectedType.storeName,
                        dishes,
                        feeling
                    )
                }
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 已有打卡记录项。展示单条打卡的类型、时间、配菜视频、感想等。
 *
 * @param index 序号
 * @param record 打卡记录实体
 * @param coverUrlMap 视频 code -> 封面 URL 的映射
 * @param onNavigateToVideo 跳转到视频详情的回调
 * @param onDelete 删除此记录的回调
 */
@Composable
fun ExistingRecordItem(
    index: Int,
    record: CheckInRecordEntity,
    coverUrlMap: Map<String, String>,
    onNavigateToVideo: (String) -> Unit,
    onDelete: () -> Unit,
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
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(max = 140.dp)
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

/**
 * 播放历史条目，在打卡弹窗中用作"配菜"快速选择。
 *
 * @param watch 播放历史实体
 * @param onNavigateToVideo 跳转到视频详情的回调
 * @param onClick 点击添加到配菜的回调
 */
@Composable
fun WatchHistoryItem(
    watch: WatchHistoryEntity,
    onNavigateToVideo: () -> Unit,
    onClick: () -> Unit,
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

@Preview
@Composable
private fun PreviewWatchHistoryItem() {
    WatchHistoryItem(
        watch = WatchHistoryEntity(
            coverUrl = "",
            title = "Sample video",
            releaseDate = 0,
            watchDate = 0,
            videoCode = "abc123",
        ),
        onNavigateToVideo = {},
        onClick = {},
    )
}
