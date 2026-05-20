package com.yenaly.han1meviewer.ui.component.lazy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn as FoundationLazyColumn
import androidx.compose.foundation.lazy.LazyRow as FoundationLazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid as FoundationLazyVerticalGrid

/**
 * 带通用 item 动画的 LazyColumn 封装。
 *
 * 目标是尽量兼容原生 `LazyColumn` / `LazyRow` / `LazyVerticalGrid` 的常用调用方式，
 * 并通过统一的轻量入场动画让列表在大多数页面里获得更自然的观感。
 */
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    enableItemAnimation: Boolean = true,
    staggerStepMillis: Int = 12,
    maxStaggerIndex: Int = 6,
    content: AnimatedLazyListScope.() -> Unit,
) {
    FoundationLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        userScrollEnabled = userScrollEnabled,
    ) {
        AnimatedLazyListScope(
            scope = this,
            enableItemAnimation = enableItemAnimation,
            staggerStepMillis = staggerStepMillis,
            maxStaggerIndex = maxStaggerIndex,
        ).content()
    }
}

@Composable
fun LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    userScrollEnabled: Boolean = true,
    enableItemAnimation: Boolean = true,
    staggerStepMillis: Int = 12,
    maxStaggerIndex: Int = 6,
    content: AnimatedLazyListScope.() -> Unit,
) {
    FoundationLazyRow(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        userScrollEnabled = userScrollEnabled,
    ) {
        AnimatedLazyListScope(
            scope = this,
            enableItemAnimation = enableItemAnimation,
            staggerStepMillis = staggerStepMillis,
            maxStaggerIndex = maxStaggerIndex,
        ).content()
    }
}

@Composable
fun LazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    userScrollEnabled: Boolean = true,
    enableItemAnimation: Boolean = true,
    staggerStepMillis: Int = 12,
    maxStaggerIndex: Int = 6,
    content: AnimatedLazyGridScope.() -> Unit,
) {
    FoundationLazyVerticalGrid(
        columns = columns,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        userScrollEnabled = userScrollEnabled,
    ) {
        AnimatedLazyGridScope(
            scope = this,
            enableItemAnimation = enableItemAnimation,
            staggerStepMillis = staggerStepMillis,
            maxStaggerIndex = maxStaggerIndex,
        ).content()
    }
}

class AnimatedLazyListScope internal constructor(
    private val scope: LazyListScope,
    private val enableItemAnimation: Boolean,
    private val staggerStepMillis: Int,
    private val maxStaggerIndex: Int,
) {
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.() -> Unit,
    ) {
        scope.item(key = key, contentType = contentType) {
            AnimatedListItem(
                enableItemAnimation = enableItemAnimation,
                animationIndex = 0,
                staggerStepMillis = staggerStepMillis,
                maxStaggerIndex = maxStaggerIndex,
                content = content,
            )
        }
    }

    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        scope.items(
            count = count,
            key = key,
            contentType = contentType,
        ) { index ->
            AnimatedListItem(
                enableItemAnimation = enableItemAnimation,
                animationIndex = index,
                staggerStepMillis = staggerStepMillis,
                maxStaggerIndex = maxStaggerIndex,
            ) { itemContent(index) }
        }
    }

    fun <T> items(
        items: List<T>,
        key: ((item: T) -> Any)? = null,
        contentType: (item: T) -> Any? = { null },
        itemContent: @Composable LazyItemScope.(item: T) -> Unit,
    ) {
        scope.items(
            count = items.size,
            key = key?.let { itemKey -> { index -> itemKey(items[index]) } },
            contentType = { index -> contentType(items[index]) },
        ) { index ->
            AnimatedListItem(
                enableItemAnimation = enableItemAnimation,
                animationIndex = index,
                staggerStepMillis = staggerStepMillis,
                maxStaggerIndex = maxStaggerIndex,
            ) { itemContent(items[index]) }
        }
    }

    fun <T> itemsIndexed(
        items: List<T>,
        key: ((index: Int, item: T) -> Any)? = null,
        contentType: ((index: Int, item: T) -> Any?)? = null,
        itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
    ) {
        scope.items(
            count = items.size,
            key = key?.let { itemKey -> { index -> itemKey(index, items[index]) } },
            contentType = { index -> contentType?.invoke(index, items[index]) },
        ) { index ->
            AnimatedListItem(
                enableItemAnimation = enableItemAnimation,
                animationIndex = index,
                staggerStepMillis = staggerStepMillis,
                maxStaggerIndex = maxStaggerIndex,
            ) { itemContent(index, items[index]) }
        }
    }
}

class AnimatedLazyGridScope internal constructor(
    private val scope: LazyGridScope,
    private val enableItemAnimation: Boolean,
    private val staggerStepMillis: Int,
    private val maxStaggerIndex: Int,
) {
    fun item(
        key: Any? = null,
        span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null,
        contentType: Any? = null,
        content: @Composable LazyGridItemScope.() -> Unit,
    ) {
        scope.item(
            key = key,
            span = span,
            contentType = contentType,
        ) {
            AnimatedGridItem(
                enableItemAnimation = enableItemAnimation,
                animationIndex = 0,
                staggerStepMillis = staggerStepMillis,
                maxStaggerIndex = maxStaggerIndex,
                content = content,
            )
        }
    }

    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        span: (LazyGridItemSpanScope.(index: Int) -> GridItemSpan)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable LazyGridItemScope.(index: Int) -> Unit,
    ) {
        scope.items(
            count = count,
            key = key,
            span = span,
            contentType = contentType,
        ) { index ->
            AnimatedGridItem(
                enableItemAnimation = enableItemAnimation,
                animationIndex = index,
                staggerStepMillis = staggerStepMillis,
                maxStaggerIndex = maxStaggerIndex,
            ) { itemContent(index) }
        }
    }

    fun <T> items(
        items: List<T>,
        key: ((item: T) -> Any)? = null,
        span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
        contentType: (item: T) -> Any? = { null },
        itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
    ) {
        scope.items(
            count = items.size,
            key = key?.let { itemKey -> { index -> itemKey(items[index]) } },
            span = span?.let { itemSpan -> { index -> itemSpan.invoke(this, items[index]) } },
            contentType = { index -> contentType(items[index]) },
        ) { index ->
            AnimatedGridItem(
                enableItemAnimation = enableItemAnimation,
                animationIndex = index,
                staggerStepMillis = staggerStepMillis,
                maxStaggerIndex = maxStaggerIndex,
            ) { itemContent(items[index]) }
        }
    }

    fun <T> itemsIndexed(
        items: List<T>,
        key: ((index: Int, item: T) -> Any)? = null,
        span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
        contentType: ((index: Int, item: T) -> Any?)? = null,
        itemContent: @Composable LazyGridItemScope.(index: Int, item: T) -> Unit,
    ) {
        scope.items(
            count = items.size,
            key = key?.let { itemKey -> { index -> itemKey(index, items[index]) } },
            span = span?.let { itemSpan ->
                { index ->
                    itemSpan.invoke(
                        this,
                        index,
                        items[index]
                    )
                }
            },
            contentType = { index -> contentType?.invoke(index, items[index]) },
        ) { index ->
            AnimatedGridItem(
                enableItemAnimation = enableItemAnimation,
                animationIndex = index,
                staggerStepMillis = staggerStepMillis,
                maxStaggerIndex = maxStaggerIndex,
            ) { itemContent(index, items[index]) }
        }
    }
}

@Composable
private fun LazyItemScope.AnimatedListItem(
    enableItemAnimation: Boolean,
    animationIndex: Int,
    staggerStepMillis: Int,
    maxStaggerIndex: Int,
    content: @Composable LazyItemScope.() -> Unit,
) {
    AnimatedLazyItemContainer(
        enableItemAnimation = enableItemAnimation,
        animationIndex = animationIndex,
        staggerStepMillis = staggerStepMillis,
        maxStaggerIndex = maxStaggerIndex,
    ) {
        Box {
            content()
        }
    }
}

@Composable
private fun LazyGridItemScope.AnimatedGridItem(
    enableItemAnimation: Boolean,
    animationIndex: Int,
    staggerStepMillis: Int,
    maxStaggerIndex: Int,
    content: @Composable LazyGridItemScope.() -> Unit,
) {
    AnimatedLazyItemContainer(
        enableItemAnimation = enableItemAnimation,
        animationIndex = animationIndex,
        staggerStepMillis = staggerStepMillis,
        maxStaggerIndex = maxStaggerIndex,
    ) {
        Box {
            content()
        }
    }
}

@Composable
private fun AnimatedLazyItemContainer(
    enableItemAnimation: Boolean,
    animationIndex: Int,
    staggerStepMillis: Int,
    maxStaggerIndex: Int,
    content: @Composable () -> Unit,
) {
    if (!enableItemAnimation) {
        content()
        return
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val clampedIndex = animationIndex.coerceIn(0, maxStaggerIndex)
        val delayMillis = (clampedIndex * staggerStepMillis).coerceAtLeast(0)
        if (delayMillis > 0) {
            kotlinx.coroutines.delay(delayMillis.toLong())
        }
        visible = true
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "lazy-item-alpha",
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.985f,
        animationSpec = tween(durationMillis = 180),
        label = "lazy-item-scale",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = animatedAlpha
            scaleX = animatedScale
            scaleY = animatedScale
        }
    ) {
        content()
    }
}
