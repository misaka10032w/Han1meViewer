package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

data class TextInputField(
    val label: String,
    val initialValue: String = "",
    val keyboardType: KeyboardType = KeyboardType.Text,
)

@Composable
fun TextInputDialog(
    visible: Boolean,
    title: String,
    fields: List<TextInputField>,
    confirmText: String,
    dismissText: String,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val values = remember(fields) {
        mutableStateListOf<String>().apply { addAll(fields.map { it.initialValue }) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = values[index],
                        onValueChange = { values[index] = it },
                        label = { Text(field.label) },
                        singleLine = field.keyboardType == KeyboardType.Number,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = field.keyboardType,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(values.toList()) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TextInputDialogPreview() {
    ComponentPreview {
        TextInputDialog(
            visible = true,
            title = "创建新清单",
            fields = listOf(
                TextInputField(label = "标题"),
                TextInputField(label = "介绍"),
            ),
            confirmText = "确认",
            dismissText = "取消",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
