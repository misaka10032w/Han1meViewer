package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

@Composable
fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    visible: Boolean = true,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (label, value) ->
                    SettingChoiceItem(
                        title = label,
                        selected = selectedValue == value,
                        onClick = { onSelect(value) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChoiceDialogPreview() {
    ComponentPreview {
        ChoiceDialog(
            title = "选择播放器",
            options = listOf("ExoPlayer" to "exo", "MPV" to "mpv"),
            selectedValue = "exo",
            onDismiss = {},
            onSelect = {},
            visible = true,
        )
    }
}
