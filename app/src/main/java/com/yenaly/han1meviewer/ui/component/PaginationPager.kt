package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 可复用的分页导航组件。
 *
 * 该组件在屏幕中央显示一组页码按钮，并提供上一页/下一页的导航功能。
 * 当总页数超过最大可见页数时，会自动显示省略号（"..."），
 * 点击省略号可弹出输入框，支持快速跳转到指定页码。
 *
 * @param currentPage 当前选中的页码，从 1 开始
 * @param totalPages 总页数
 * @param onPageSelected 页码选中回调，返回目标页码（1-based）
 * @param modifier 应用于根 Row 的 Modifier
 * @param maxVisiblePages 最多可见的页码数量（不含上一页/下一页按钮），
 *   必须为大于等于 5 的奇数（如 5, 7, 9），默认值为 5
 *
 * @throws IllegalArgumentException 当 maxVisiblePages < 5 或为偶数时抛出
 */
@Composable
fun PaginationPager(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxVisiblePages: Int = 5
) {
    require(maxVisiblePages >= 5 && maxVisiblePages % 2 != 0) {
        "maxVisiblePages 必须是大于等于 5 的奇数 (如 5, 7, 9)"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (currentPage > 1) onPageSelected(currentPage - 1) },
            enabled = currentPage > 1
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "pre")
        }

        val pages = remember(currentPage, totalPages, maxVisiblePages) {
            calculatePagination(currentPage, totalPages, maxVisiblePages)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.forEach { page ->
                when (page) {
                    -1, -2 -> {
                        JumpEllipsisItem(
                            totalPages = totalPages,
                            onJump = onPageSelected
                        )
                    }
                    else -> {
                        PageItem(
                            page = page,
                            isSelected = page == currentPage,
                            onClick = { onPageSelected(page) }
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = { if (currentPage < totalPages) onPageSelected(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "next")
        }
    }
}

/**
 * 跳转省略号组件（"..."）。
 *
 * 点击省略号时切换为输入框，用户可输入目标页码并提交，
 * 提交后调用 [onJump] 回调并自动退出编辑状态。
 * 输入框会限制输入长度为总页数的位数，并仅允许数字输入。
 *
 * @param totalPages 总页数，用于输入校验和输入长度限制
 * @param onJump 用户确认跳转时的回调，返回目标页码（1-based）
 */
@Composable
private fun JumpEllipsisItem(
    totalPages: Int,
    onJump: (Int) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var jumpText by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var hasFocused by remember { mutableStateOf(false) }

    if (isEditing) {
        Surface(
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .width(56.dp)
                .height(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = jumpText,
                    onValueChange = { input ->
                        if (input.length <= totalPages.toString().length) {
                            jumpText = input.filter { it.isDigit() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                hasFocused = true
                            } else if (hasFocused) {
                                isEditing = false
                                hasFocused = false
                                jumpText = ""
                            }
                        },
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val targetPage = jumpText.toIntOrNull()
                            if (targetPage != null && targetPage in 1..totalPages) {
                                onJump(targetPage)
                            }
                            isEditing = false
                            hasFocused = false
                            jumpText = ""
                            focusManager.clearFocus()
                        }
                    )
                )
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    } else {
        Surface(
            onClick = { isEditing = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * 单个页码按钮组件。
 *
 * 使用 [Surface] 实现圆形按钮，选中状态和未选中状态
 * 分别使用不同的主题色进行区分。
 *
 * @param page 页码（从 1 开始）
 * @param isSelected 是否为当前选中页
 * @param onClick 点击该页码时的回调
 */
@Composable
private fun PageItem(
    page: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = page.toString(),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}


/**
 * 计算分页导航显示的页码列表。
 *
 * 根据当前页码、总页数和最大可见页数，生成用于渲染的页码序列。
 * 返回列表中的 -1 表示左侧省略号，-2 表示右侧省略号，
 * 其他正整数表示具体的页码。
 *
 * 逻辑规则：
 * - 当总页数 ≤ 最大可见页数时，显示全部页码
 * - 当当前页靠近左侧时，右侧显示省略号
 * - 当当前页靠近右侧时，左侧显示省略号
 * - 当当前页在中间时，左右两侧均显示省略号，
 *   并在中间显示以当前页为中心的连续页码段
 *
 * @param current 当前页码（1-based）
 * @param total 总页数
 * @param maxVisible 最大可见页码数量，必须是奇数
 * @return 包含页码和省略号标记（-1/-2）的列表
 */
private fun calculatePagination(current: Int, total: Int, maxVisible: Int): List<Int> {
    if (total <= maxVisible) return (1..total).toList()

    val result = mutableListOf<Int>()
    val showLeftEllipsis = current > (maxVisible / 2) + 1
    val showRightEllipsis = current < total - (maxVisible / 2)

    if (!showLeftEllipsis && showRightEllipsis) {
        for (i in 1..(maxVisible - 2)) result.add(i)
        result.add(-2) // 右省略
        result.add(total)

    } else if (showLeftEllipsis && !showRightEllipsis) {
        result.add(1)
        result.add(-1)
        for (i in (total - maxVisible + 3)..total) result.add(i)

    } else {
        result.add(1)
        result.add(-1)

        val midHalf = (maxVisible - 4) / 2
        for (i in (current - midHalf)..(current + midHalf)) {
            result.add(i)
        }

        result.add(-2)
        result.add(total)
    }

    return result
}

@Composable
@Preview(showBackground = true)
fun PaginationPagerPreview(){
    PaginationPager(
        currentPage =2,
        totalPages = 10,
        onPageSelected = {},
        maxVisiblePages = 6
    )
}