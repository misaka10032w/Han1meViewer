package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.ReportReason

@Composable
internal fun CommentInputDialog(
    title: String,
    label: String,
    text: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String,
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                enabled = confirmEnabled,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = dismissEnabled) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
internal fun CommentReportDialog(
    reportReasons: List<ReportReason>,
    selectedReasonIndex: Int,
    onSelectReason: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_wrong_with_him)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reportReasons.forEachIndexed { index, reason ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (selectedReasonIndex == index) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedReasonIndex == index,
                                    onClick = { onSelectReason(index) },
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (selectedReasonIndex == index) {
                                        R.drawable.ic_baseline_check_circle_24
                                    } else {
                                        R.drawable.baseline_remove_circle_24
                                    }
                                ),
                                contentDescription = null,
                                tint = if (selectedReasonIndex == index) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Text(
                                text = reason.value,
                                color = if (selectedReasonIndex == index) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedReasonIndex >= 0,
                onClick = onConfirm,
            ) {
                Text(stringResource(R.string.submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
