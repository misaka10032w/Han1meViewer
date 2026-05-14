package com.yenaly.han1meviewer.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.ui.component.ComponentPreview
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.EmptyContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WatchHistoryScreen(
    historiesFlow: Flow<List<WatchHistoryEntity>>,
    onBack: () -> Unit,
    onOpenVideo: (WatchHistoryEntity) -> Unit,
    onDeleteHistory: (WatchHistoryEntity) -> Unit,
    onDeleteAllHistories: () -> Unit,
) {
    val histories by historiesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    WatchHistoryScreen(
        histories = histories,
        onBack = onBack,
        onOpenVideo = onOpenVideo,
        onDeleteHistory = onDeleteHistory,
        onDeleteAllHistories = onDeleteAllHistories,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchHistoryScreen(
    histories: List<WatchHistoryEntity>,
    onBack: () -> Unit,
    onOpenVideo: (WatchHistoryEntity) -> Unit,
    onDeleteHistory: (WatchHistoryEntity) -> Unit,
    onDeleteAllHistories: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<WatchHistoryEntity?>(null) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }

    ConfirmDialog(
        visible = pendingDelete != null,
        title = stringResource(R.string.delete_history),
        message = stringResource(R.string.sure_to_delete_s, pendingDelete?.title.orEmpty()),
        confirmText = stringResource(R.string.delete),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            pendingDelete?.let(onDeleteHistory)
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null },
    )

    ConfirmDialog(
        visible = showDeleteAllDialog,
        title = stringResource(R.string.watch_history_delete_all_title),
        message = stringResource(R.string.sure_to_delete_all_histories),
        confirmText = stringResource(R.string.watch_history_clear_all),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            onDeleteAllHistories()
            showDeleteAllDialog = false
        },
        onDismiss = { showDeleteAllDialog = false },
    )

    ConfirmDialog(
        visible = showHelpDialog,
        title = stringResource(R.string.help),
        message = stringResource(R.string.long_press_to_delete_all_histories),
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.close),
        onConfirm = { showHelpDialog = false },
        onDismiss = { showHelpDialog = false },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.watch_history))
                        Text(
                            text = stringResource(R.string.watch_history_total_count, histories.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    FilledIconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    FilledIconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_help_24),
                            contentDescription = stringResource(R.string.help),
                        )
                    }
                    FilledIconButton(onClick = { showDeleteAllDialog = true }, enabled = histories.isNotEmpty()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_delete_24),
                            contentDescription = stringResource(R.string.watch_history_clear_all),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (histories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                EmptyContent(
                    hint = stringResource(R.string.watch_history_empty_title),
                    subHint = stringResource(R.string.watch_history_empty_description),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(histories, key = { it.id }) { history ->
                    WatchHistoryCard(
                        history = history,
                        onClick = { onOpenVideo(history) },
                        onLongClick = { pendingDelete = history },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WatchHistoryCard(
    history: WatchHistoryEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val watchDate = remember(history.watchDate) { dateFormatter.format(Date(history.watchDate)) }
    val releaseDate = remember(history.releaseDate) { dateFormatter.format(Date(history.releaseDate)) }
    val progressMinutes = remember(history.progress) { history.progress / 60_000 }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AsyncImage(
                model = history.coverUrl,
                contentDescription = history.title,
                modifier = Modifier
                    .width(108.dp)
                    .height(152.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = history.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    BadgedBox(
                        badge = {
                            if (progressMinutes > 0) {
                                Badge { Text(stringResource(R.string.watch_history_minutes_short, progressMinutes)) }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_play_circle_outline_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                AssistChip(
                    onClick = onClick,
                    label = { Text(stringResource(R.string.watch_history_resume_watch)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_history_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WatchHistoryMeta(
                        iconRes = R.drawable.ic_baseline_access_time_24,
                        label = stringResource(R.string.watch_history_watched_at, watchDate),
                    )
                }

                HorizontalDivider()

                WatchHistoryMeta(
                    iconRes = R.drawable.ic_baseline_play_circle_outline_24,
                    label = stringResource(R.string.watch_history_released_at, releaseDate),
                )
                if (history.progress > 0L) {
                    WatchHistoryMeta(
                        iconRes = R.drawable.ic_baseline_history_24,
                        label = stringResource(R.string.watch_history_progress_minutes, progressMinutes),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onLongClick, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.delete_history))
                }
            }
        }
    }
}

@Composable
private fun WatchHistoryMeta(
    iconRes: Int,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun WatchHistoryScreenPreview() {
    val previews = fakeHomePageVideos.take(3).mapIndexed { index, item ->
        WatchHistoryEntity(
            id = index + 1,
            title = item.title,
            coverUrl = item.coverUrl,
            videoCode = item.videoCode,
            releaseDate = System.currentTimeMillis() - (index + 10) * 86_400_000L,
            watchDate = System.currentTimeMillis() - index * 3_600_000L,
            progress = (index + 1) * 12L * 60_000L,
        )
    }
    ComponentPreview {
        WatchHistoryScreen(
            histories = previews,
            onBack = {},
            onOpenVideo = {},
            onDeleteHistory = {},
            onDeleteAllHistories = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchHistoryEmptyPreview() {
    ComponentPreview {
        WatchHistoryScreen(
            histories = emptyList(),
            onBack = {},
            onOpenVideo = {},
            onDeleteHistory = {},
            onDeleteAllHistories = {},
        )
    }
}
