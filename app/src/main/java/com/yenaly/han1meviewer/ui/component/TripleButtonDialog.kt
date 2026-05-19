package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onNegative) { Text(negativeText) }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onNeutral) { Text(neutralText) }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onPositive) { Text(positiveText) }
            }
        },
    )
}
