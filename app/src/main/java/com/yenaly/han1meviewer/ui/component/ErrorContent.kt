package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 错误状态内容组件。
 *
 * 展示错误图标、标题和可选的错误详情，支持重试按钮。
 *
 * @param modifier 修饰符
 * @param title 错误标题，默认为"加载失败"
 * @param message 错误详情，为 null 或空白时不显示
 * @param onRetry 重试回调，为 null 时不显示重试按钮
 */
@Composable
fun ErrorContent(
    modifier: Modifier = Modifier,
    title: String = "加载失败",
    message: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onRetry != null) {
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorContentPreview() {
    ComponentPreview {
        ErrorContent(message = "网络连接失败", onRetry = {})
    }
}
