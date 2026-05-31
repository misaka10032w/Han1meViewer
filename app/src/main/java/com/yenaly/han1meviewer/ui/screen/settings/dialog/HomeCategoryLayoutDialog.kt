package com.yenaly.han1meviewer.ui.screen.settings.dialog

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.verticalScrollbar
import com.yenaly.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn as ComposeLazyColumn

@Composable
internal fun HomeCategoryLayoutDialog(
    state: HomeSettingsUiState,
    onDismiss: () -> Unit,
    onConfirm: (List<String>, Set<String>) -> Unit,
) {
    var order by rememberSaveable(state.homeCategoryOrder) { mutableStateOf(state.homeCategoryOrder) }
    var hiddenKeys by rememberSaveable(state.hiddenHomeCategoryKeys) {
        mutableStateOf(state.hiddenHomeCategoryKeys.toList())
    }
    val itemByKey = state.homeCategoryItems.associateBy { it.key }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragAccumulatedOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val spacingPx = remember(density) {
        with(density) { 8.dp.toPx() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_category_layout)) },
        text = {
            ComposeLazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScrollbar(lazyListState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "summary") {
                    Text(
                        text = stringResource(R.string.home_category_layout_dialog_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                items(
                    items = order,
                    key = { it }
                ) { key ->
                    val item = itemByKey[key] ?: return@items
                    val isDragging = draggedKey == key
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.03f else 1f,
                        label = "drag-scale"
                    )
                    val titleRes = if (state.useAvHomeCategoryTitles) {
                        item.avTitleRes ?: item.normalTitleRes
                    } else {
                        item.normalTitleRes
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                placementSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                )
                            )
                            .graphicsLayer {
                                translationY = if (isDragging) dragAccumulatedOffset else 0f
                                scaleX = scale
                                scaleY = scale
                            }
                            .zIndex(if (isDragging) 1f else 0f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedKey = key
                                            dragAccumulatedOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggedKey = null
                                            dragAccumulatedOffset = 0f
                                        },
                                        onDragEnd = {
                                            draggedKey = null
                                            dragAccumulatedOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->

                                            change.consume()
                                            dragAccumulatedOffset += dragAmount.y

                                            val currentLayoutInfo =
                                                lazyListState.layoutInfo.visibleItemsInfo
                                                    .firstOrNull { it.key == key }
                                                    ?: return@detectDragGesturesAfterLongPress

                                            val itemHeight =
                                                currentLayoutInfo.size + spacingPx

                                            val currentIndex =
                                                order.indexOf(key)
                                                    .takeIf { it >= 0 }
                                                    ?: return@detectDragGesturesAfterLongPress

                                            if (
                                                dragAccumulatedOffset > itemHeight / 2f &&
                                                currentIndex < order.lastIndex
                                            ) {

                                                order = order.move(
                                                    currentIndex,
                                                    currentIndex + 1
                                                )

                                                dragAccumulatedOffset -= itemHeight
                                            } else if (
                                                dragAccumulatedOffset < -itemHeight / 2f &&
                                                currentIndex > 0
                                            ) {

                                                order = order.move(
                                                    currentIndex,
                                                    currentIndex - 1
                                                )

                                                dragAccumulatedOffset += itemHeight
                                            }

                                            val containerHeight =
                                                lazyListState.layoutInfo.viewportSize.height

                                            val itemTop =
                                                currentLayoutInfo.offset + dragAccumulatedOffset

                                            if (
                                                itemTop + currentLayoutInfo.size > containerHeight &&
                                                lazyListState.canScrollForward
                                            ) {

                                                coroutineScope.launch {
                                                    lazyListState.scrollBy(16f)
                                                }

                                            } else if (
                                                itemTop < 0 &&
                                                lazyListState.canScrollBackward
                                            ) {

                                                coroutineScope.launch {
                                                    lazyListState.scrollBy(-16f)
                                                }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = stringResource(titleRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = if (key in hiddenKeys) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )

                        Switch(
                            modifier = Modifier.padding(end = 10.dp),
                            checked = key !in hiddenKeys,
                            onCheckedChange = { checked ->
                                hiddenKeys =
                                    if (checked)
                                        hiddenKeys - key
                                    else
                                        hiddenKeys + key
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(order, hiddenKeys.toSet()) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        order = state.homeCategoryItems.map { it.key }
                        hiddenKeys = emptyList()
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}

private fun List<String>.move(fromIndex: Int, toIndex: Int): List<String> {
    if (fromIndex !in indices || toIndex !in indices) return this
    return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}