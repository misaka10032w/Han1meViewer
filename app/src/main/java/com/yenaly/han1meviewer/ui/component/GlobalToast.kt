package com.yenaly.han1meviewer.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 全局 Toast 单例，用于替换传统 Toast。
 *
 * 特性：
 * - 全局单例，从任意（含非 Compose）上下文调用；
 * - 连续发出时垂直堆叠（同一位置最多 [MAX_VISIBLE_COUNT] 条，超出丢弃最旧）；
 * - 相同 message + level 会去重，并重置其倒计时；
 * - 支持 [ToastDuration] 多种时长，也可传入自定义毫秒（0 = 常驻，需手动点按关闭）；
 * - 支持 [ToastLevel] 多级强调色（每级不同的背景色），可传入自定义 icon 插槽；
 * - 支持上 / 中 / 下三种位置；
 * - 点按可立即关闭，点击空白区域不会拦截底层交互。
 */
object GlobalToasts {

    enum class ToastLevel {
        INFO, SUCCESS, WARNING, ERROR
    }

    enum class ToastDuration(val millis: Long) {
        SHORT(2_000L),
        NORMAL(3_500L),
        LONG(5_000L),
        EXTRA_LONG(8_000L),
        INDEFINITE(0L),
    }

    enum class ToastPosition {
        TOP, CENTER, BOTTOM
    }

    data class Toast(
        val id: Long,
        val message: String,
        val level: ToastLevel = ToastLevel.INFO,
        val durationMillis: Long = ToastDuration.NORMAL.millis,
        val icon: (@Composable () -> Unit)? = null,
        val position: ToastPosition = ToastPosition.BOTTOM,
    )

    private const val MAX_VISIBLE_COUNT = 3

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val dismissJobs = mutableMapOf<Long, Job>()
    private var nextId = 0L

    private val _toasts = MutableStateFlow<List<Toast>>(emptyList())
    val toasts: StateFlow<List<Toast>> = _toasts.asStateFlow()

    fun show(
        message: String,
        level: ToastLevel = ToastLevel.INFO,
        durationMillis: Long = ToastDuration.NORMAL.millis,
        icon: (@Composable () -> Unit)? = null,
        position: ToastPosition = ToastPosition.BOTTOM,
    ) {
        if (message.isBlank()) return

        val current = _toasts.value
        val existing = current.firstOrNull { it.message == message && it.level == level }
        if (existing != null) {
            _toasts.value = current.map {
                if (it.id == existing.id) {
                    it.copy(durationMillis = durationMillis, icon = icon ?: it.icon, position = position)
                } else {
                    it
                }
            }
            scheduleDismiss(existing.id, durationMillis)
            return
        }

        val id = ++nextId
        val toast = Toast(id, message, level, durationMillis, icon, position)
        _toasts.value = (current + toast).let { list ->
            if (list.size > MAX_VISIBLE_COUNT) {
                val overflow = list.size - MAX_VISIBLE_COUNT
                list.take(overflow).forEach { dropped ->
                    dismissJobs.remove(dropped.id)?.cancel()
                }
                list.drop(overflow)
            } else {
                list
            }
        }
        scheduleDismiss(id, durationMillis)
    }

    fun dismiss(id: Long) {
        dismissJobs.remove(id)?.cancel()
        _toasts.value = _toasts.value.filterNot { it.id == id }
    }

    fun dismissAll() {
        dismissJobs.values.forEach { it.cancel() }
        dismissJobs.clear()
        _toasts.value = emptyList()
    }

    private fun scheduleDismiss(id: Long, durationMillis: Long) {
        dismissJobs.remove(id)?.cancel()
        if (durationMillis <= 0L) return
        dismissJobs[id] = scope.launch {
            delay(durationMillis)
            dismiss(id)
            dismissJobs.remove(id)
        }
    }
}

@Composable
fun GlobalToastHost(modifier: Modifier = Modifier) {
    val toasts by GlobalToasts.toasts.collectAsState()

    val rendered = remember { mutableStateListOf<GlobalToasts.Toast>() }
    LaunchedEffect(toasts) {
        toasts.forEach { toast ->
            if (rendered.none { it.id == toast.id }) {
                rendered.add(toast)
            }
        }
    }

    if (rendered.isEmpty()) return

    val activeIds = remember(toasts) { toasts.mapTo(mutableSetOf()) { it.id } }

    Box(modifier = modifier.fillMaxSize()) {
        ToastColumn(
            toasts = rendered.filter { it.position == GlobalToasts.ToastPosition.TOP },
            activeIds = activeIds,
            onExitFinished = { id -> rendered.removeAll { it.id == id } },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp),
        )
        ToastColumn(
            toasts = rendered.filter { it.position == GlobalToasts.ToastPosition.CENTER },
            activeIds = activeIds,
            onExitFinished = { id -> rendered.removeAll { it.id == id } },
            modifier = Modifier.align(Alignment.Center),
        )
        ToastColumn(
            toasts = rendered.filter { it.position == GlobalToasts.ToastPosition.BOTTOM },
            activeIds = activeIds,
            onExitFinished = { id -> rendered.removeAll { it.id == id } },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun ToastColumn(
    toasts: List<GlobalToasts.Toast>,
    activeIds: Set<Long>,
    onExitFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        toasts.forEach { toast ->
            key(toast.id) {
                AnimatedToastItem(
                    toast = toast,
                    isActive = toast.id in activeIds,
                    onExitFinished = { onExitFinished(toast.id) },
                )
            }
        }
    }
}

@Composable
private fun AnimatedToastItem(
    toast: GlobalToasts.Toast,
    isActive: Boolean,
    onExitFinished: () -> Unit,
) {
    val visibleState = remember { MutableTransitionState(!isActive) }
    LaunchedEffect(isActive) {
        visibleState.targetState = isActive
    }
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (!isActive && visibleState.isIdle && !visibleState.currentState) {
            onExitFinished()
        }
    }

    val fromTop = toast.position == GlobalToasts.ToastPosition.TOP
    val fromBottom = toast.position == GlobalToasts.ToastPosition.BOTTOM

    AnimatedVisibility(
        visibleState = visibleState,
        enter = when {
            fromTop -> fadeIn() + slideInVertically(initialOffsetY = { -it })
            fromBottom -> fadeIn() + slideInVertically(initialOffsetY = { it })
            else -> fadeIn() + scaleIn(initialScale = 0.9f)
        },
        exit = when {
            fromTop -> fadeOut() + slideOutVertically(targetOffsetY = { -it })
            fromBottom -> fadeOut() + slideOutVertically(targetOffsetY = { it })
            else -> fadeOut() + scaleOut(targetScale = 0.9f)
        },
    ) {
        ToastItem(toast)
    }
}

@Composable
private fun ToastItem(toast: GlobalToasts.Toast) {
    val (container, content) = toastColors(toast.level)

    Surface(
        onClick = { GlobalToasts.dismiss(toast.id) },
        shape = RoundedCornerShape(50),
        color = container.copy(alpha = 0.8f),
        contentColor = content,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val customIcon = toast.icon
            if (customIcon != null) {
                customIcon()
            } else {
                Icon(imageVector = toast.level.defaultIcon, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = toast.message,
                color = content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }
    }
}

private val GlobalToasts.ToastLevel.defaultIcon: ImageVector
    get() = when (this) {
        GlobalToasts.ToastLevel.INFO -> Icons.Default.Info
        GlobalToasts.ToastLevel.SUCCESS -> Icons.Default.Check
        GlobalToasts.ToastLevel.WARNING -> Icons.Default.Warning
        GlobalToasts.ToastLevel.ERROR -> Icons.Default.Close
    }

@Composable
private fun toastColors(level: GlobalToasts.ToastLevel): Pair<Color, Color> = when (level) {
    GlobalToasts.ToastLevel.INFO -> Color(0xFF1565C0) to Color.White
    GlobalToasts.ToastLevel.SUCCESS -> Color(0xFF2E7D32) to Color.White
    GlobalToasts.ToastLevel.WARNING -> Color(0xFFEF6C00) to Color.White
    GlobalToasts.ToastLevel.ERROR -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun GlobalToastHostPreview() {
    ComponentPreview {
        LaunchedEffect(Unit) {
            GlobalToasts.dismissAll()
            GlobalToasts.show(
                message = "顶部信息提示",
                level = GlobalToasts.ToastLevel.INFO,
                durationMillis = GlobalToasts.ToastDuration.INDEFINITE.millis,
                position = GlobalToasts.ToastPosition.TOP,
            )
            GlobalToasts.show(
                message = "居中的警告",
                level = GlobalToasts.ToastLevel.WARNING,
                durationMillis = GlobalToasts.ToastDuration.INDEFINITE.millis,
                position = GlobalToasts.ToastPosition.CENTER,
            )
            GlobalToasts.show(
                message = "保存成功",
                level = GlobalToasts.ToastLevel.SUCCESS,
                durationMillis = GlobalToasts.ToastDuration.INDEFINITE.millis,
                position = GlobalToasts.ToastPosition.BOTTOM,
            )
            GlobalToasts.show(
                message = "操作失败，请重试",
                level = GlobalToasts.ToastLevel.ERROR,
                durationMillis = GlobalToasts.ToastDuration.INDEFINITE.millis,
                position = GlobalToasts.ToastPosition.BOTTOM,
            )
        }
        GlobalToastHost()
    }
}
