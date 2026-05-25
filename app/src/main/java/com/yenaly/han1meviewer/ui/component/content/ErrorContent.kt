package com.yenaly.han1meviewer.ui.component.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

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
    title: String? = null,
    message: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    val resolvedTitle = title ?: stringResource(R.string.load_failed_retry)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            modifier = Modifier.size(150.dp),
            painter = painterResource(R.drawable.h_chan_sad),
            contentDescription = resolvedTitle
        )
        Text(
            text = resolvedTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,)
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (onRetry != null) {
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorContentPreview() {
    ComponentPreview {
        ErrorContent(message = "network error", onRetry = {})
    }
}
