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


@Composable
fun PaginationPager(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxVisiblePages: Int = 5 // 移动端强烈推荐设为5，防止小屏溢出
) {
    // 强制约定 maxVisiblePages 必须是 >= 5 的奇数，否则算法没有意义
    require(maxVisiblePages >= 5 && maxVisiblePages % 2 != 0) {
        "maxVisiblePages 必须是大于等于 5 的奇数 (如 5, 7, 9)"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 上一页
        IconButton(
            onClick = { if (currentPage > 1) onPageSelected(currentPage - 1) },
            enabled = currentPage > 1
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一页")
        }

        // 核心：严格计算页码
        val pages = remember(currentPage, totalPages, maxVisiblePages) {
            calculatePagination(currentPage, totalPages, maxVisiblePages)
        }

        // 渲染页码区
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

        // 下一页
        IconButton(
            onClick = { if (currentPage < totalPages) onPageSelected(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一页")
        }
    }
}

@Composable
private fun JumpEllipsisItem(
    totalPages: Int,
    onJump: (Int) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var jumpText by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // 【核心修复】追踪是否真的拿到过焦点，防止 onFocusChanged 初始化时瞬间干掉组件
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
                                hasFocused = true // 标记：我们确实拿到焦点了
                            } else if (hasFocused) {
                                // 只有在拿到过焦点后再次失去焦点，才关闭编辑状态
                                isEditing = false
                                hasFocused = false
                                jumpText = "" // 关闭时清空输入
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
                            // 提交后重置状态并收起键盘
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
 * 修复后的核心算法：绝对保证输出长度不超过 maxVisiblePages 限制。
 * 使用 -1 表示左省略号，-2 表示右省略号。
 */
private fun calculatePagination(current: Int, total: Int, maxVisible: Int): List<Int> {
    // 总页数小于等于限制，全部显示
    if (total <= maxVisible) return (1..total).toList()

    val result = mutableListOf<Int>()

    // 判断是否需要显示左、右省略号
    val showLeftEllipsis = current > (maxVisible / 2) + 1
    val showRightEllipsis = current < total - (maxVisible / 2)

    if (!showLeftEllipsis && showRightEllipsis) {
        // 【情况 1：偏向左侧】例如 1 2 3 4 ... 10
        for (i in 1..(maxVisible - 2)) result.add(i)
        result.add(-2) // 右省略
        result.add(total)

    } else if (showLeftEllipsis && !showRightEllipsis) {
        // 【情况 2：偏向右侧】例如 1 ... 7 8 9 10
        result.add(1)
        result.add(-1) // 左省略
        for (i in (total - maxVisible + 3)..total) result.add(i)

    } else {
        // 【情况 3：处于中间】例如 1 ... 4 5 6 ... 10
        result.add(1)
        result.add(-1) // 左省略

        // 算出除了头尾和两个省略号外，中间还能放几个数字
        val midHalf = (maxVisible - 4) / 2
        for (i in (current - midHalf)..(current + midHalf)) {
            result.add(i)
        }

        result.add(-2) // 右省略
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