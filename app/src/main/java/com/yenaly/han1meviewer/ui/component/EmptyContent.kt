package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R

/**
 * 空状态视图组件。
 *
 * 展示占位图片和提示文本，用于列表或内容为空时的视觉反馈。
 *
 * @param hint 主提示文本
 * @param subHint 副提示文本，默认为空
 * @param picRes 占位图片资源 ID，默认为 h_chan_speechless
 */
@Composable
fun EmptyContent(
    hint: String,
    subHint: String = "",
    picRes: Int = R.drawable.h_chan_speechless,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                modifier = Modifier
                    .padding(16.dp)
                    .width(150.dp),
                painter = painterResource(picRes),
                contentDescription = stringResource(R.string.here_is_empty),
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subHint,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyViewPreview() {
    ComponentPreview {
        EmptyContent(hint = "发生了一些事情", subHint = "reason: 404 Not Found")
    }
}
