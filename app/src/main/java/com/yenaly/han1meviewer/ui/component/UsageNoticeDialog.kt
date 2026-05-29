package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import kotlinx.coroutines.delay

@Composable
fun UsageNoticeDialog(
    visible: Boolean,
    onAccepted: () -> Unit,
    onDeclined: () -> Unit,
) {
    if (!visible) return

    var remainingSeconds by remember { mutableIntStateOf(20) }
    var isResumed by remember { mutableStateOf(true) }
    var resetVersion by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    remainingSeconds = 20
                    isResumed = true
                    resetVersion++
                }

                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    remainingSeconds = 20
                    isResumed = false
                    resetVersion++
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(visible, isResumed, resetVersion) {
        while (visible && isResumed && remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds--
        }
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.usage_notice_title)) },
        text = {
            Text(
                text = stringResource(R.string.usage_notice_content),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                textAlign = TextAlign.Start,
            )
        },
        confirmButton = {
            TextButton(
                enabled = remainingSeconds == 0,
                onClick = onAccepted,
            ) {
                Text(
                    text = if (remainingSeconds == 0) {
                        stringResource(R.string.usage_notice_accept)
                    } else {
                        stringResource(R.string.usage_notice_accept_countdown, remainingSeconds)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDeclined) {
                Text(stringResource(R.string.usage_notice_decline))
            }
        },
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun UsageNoticeDialogPreview(){
    ComponentPreview {
        UsageNoticeDialog(
            visible = true,
            onAccepted = { },
            onDeclined = { }
        )
    }
}