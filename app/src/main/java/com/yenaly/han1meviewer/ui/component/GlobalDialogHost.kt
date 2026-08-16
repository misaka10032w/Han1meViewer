package com.yenaly.han1meviewer.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局弹窗宿主，用于非 Compose 上下文（RecyclerView 适配器、播放器 View、挂起函数等）
 * 触发 Compose 主题弹窗，避免回退到 themes.xml 的传统配色。
 */
object GlobalDialogs {

    sealed interface Request {
        val title: String
    }

    data class ConfirmRequest(
        override val title: String,
        val message: String? = null,
        val confirmText: String,
        val dismissText: String? = null,
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit = {},
        val onDismissRequest: () -> Unit = {},
    ) : Request

    data class InputRequest(
        override val title: String,
        val fields: List<TextInputField>,
        val confirmText: String,
        val dismissText: String,
        val onConfirm: (List<String>) -> Unit,
        val onDismiss: () -> Unit = {},
    ) : Request

    private val _request = MutableStateFlow<Request?>(null)
    val request = _request.asStateFlow()

    fun show(request: Request) {
        _request.value = request
    }

    fun dismiss() {
        _request.value = null
    }
}

@Composable
fun GlobalDialogHost() {
    val request by GlobalDialogs.request.collectAsState()
    when (val req = request) {
        is GlobalDialogs.ConfirmRequest -> {
            AlertDialog(
                onDismissRequest = {
                    GlobalDialogs.dismiss()
                    req.onDismissRequest()
                },
                title = { Text(req.title) },
                text = req.message?.let { { Text(it) } },
                confirmButton = {
                    TextButton(onClick = {
                        GlobalDialogs.dismiss()
                        req.onConfirm()
                    }) {
                        Text(req.confirmText)
                    }
                },
                dismissButton = req.dismissText?.let { dismissText ->
                    {
                        TextButton(onClick = {
                            GlobalDialogs.dismiss()
                            req.onCancel()
                        }) {
                            Text(dismissText)
                        }
                    }
                },
            )
        }

        is GlobalDialogs.InputRequest -> {
            TextInputDialog(
                visible = true,
                title = req.title,
                fields = req.fields,
                confirmText = req.confirmText,
                dismissText = req.dismissText,
                onConfirm = { values ->
                    GlobalDialogs.dismiss()
                    req.onConfirm(values)
                },
                onDismiss = {
                    GlobalDialogs.dismiss()
                    req.onDismiss()
                },
            )
        }

        null -> Unit
    }
}
