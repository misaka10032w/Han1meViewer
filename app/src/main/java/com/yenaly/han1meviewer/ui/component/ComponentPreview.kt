package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 组件预览包装器。
 *
 * 提供带主题和背景的预览容器，自动添加 16dp 内边距。
 *
 * @param content 要预览的组件内容
 */
@Composable
fun ComponentPreview(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}
