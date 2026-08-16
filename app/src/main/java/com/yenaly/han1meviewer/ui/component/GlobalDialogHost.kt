package com.yenaly.han1meviewer.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局弹窗宿主，用于非 Compose 上下文（RecyclerView 适配器、播放器 View、挂起函数等）
 * 触发 Compose 主题弹窗，避免回退到 themes.xml 的传统配色。
 *
 * 弹窗以队列形式串行展示：多次 [show] 会按顺序逐个弹出，互不覆盖。
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

    data class QueuedRequest(
        val id: Long,
        val request: Request,
    )

    private var nextId = 0L
    private val _queue = MutableStateFlow<List<QueuedRequest>>(emptyList())
    val queue: StateFlow<List<QueuedRequest>> = _queue.asStateFlow()

    /**
     * 入队一个弹窗，返回其唯一 id，可用 [dismiss] 按 id 移除。
     */
    fun show(request: Request): Long {
        val id = ++nextId
        _queue.value = _queue.value + QueuedRequest(id, request)
        return id
    }

    fun dismiss(id: Long) {
        _queue.value = _queue.value.filterNot { it.id == id }
    }
}

@Composable
fun GlobalDialogHost() {
    val queue by GlobalDialogs.queue.collectAsState()
    val head = queue.firstOrNull() ?: return
    when (val req = head.request) {
        is GlobalDialogs.ConfirmRequest -> {
            AlertDialog(
                onDismissRequest = {
                    GlobalDialogs.dismiss(head.id)
                    req.onDismissRequest()
                },
                title = { Text(req.title) },
                text = req.message?.let { { Text(it) } },
                confirmButton = {
                    TextButton(onClick = {
                        GlobalDialogs.dismiss(head.id)
                        req.onConfirm()
                    }) {
                        Text(req.confirmText)
                    }
                },
                dismissButton = req.dismissText?.let { dismissText ->
                    {
                        TextButton(onClick = {
                            GlobalDialogs.dismiss(head.id)
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
                    GlobalDialogs.dismiss(head.id)
                    req.onConfirm(values)
                },
                onDismiss = {
                    GlobalDialogs.dismiss(head.id)
                    req.onDismiss()
                },
            )
        }
    }
}
