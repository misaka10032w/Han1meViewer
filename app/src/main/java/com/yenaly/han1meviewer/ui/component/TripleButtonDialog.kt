package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

@Composable
fun TripleButtonDialog(
    visible: Boolean,
    title: String,
    message: String? = null,
    negativeText: String,
    neutralText: String,
    positiveText: String,
    onNegative: () -> Unit,
    onNeutral: () -> Unit,
    onPositive: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = message?.let { { Text(it) } },
        confirmButton = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onNegative) { DialogButtonText(negativeText) }
                TextButton(onClick = onNeutral) { DialogButtonText(neutralText) }
                TextButton(onClick = onPositive) { DialogButtonText(positiveText) }
            }
        },
    )
}

@Composable
private fun DialogButtonText(text: String) {
    Text(
        text = text,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TripleButtonDialogPreview(){
    ComponentPreview {
        TripleButtonDialog(
            visible = true,
            title = "这是标题",
            message = "这是信息",
            negativeText = "取消并关闭",
            neutralText = "恢复默认下载目录",
            positiveText = "选择自定义下载文件夹",
            onNegative = { },
            onNeutral = { },
            onPositive = { },
        ) { }
    }
}
