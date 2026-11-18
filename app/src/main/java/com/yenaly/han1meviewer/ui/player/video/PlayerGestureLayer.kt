package com.yenaly.han1meviewer.ui.player.video

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.yenaly.han1meviewer.ui.player.PlayerDefaults
import com.yenaly.han1meviewer.ui.player.PlayerUtils
import com.yenaly.han1meviewer.util.appScreenWidth
import com.yenaly.han1meviewer.util.findActivityOrNull
import com.yenaly.han1meviewer.util.statusBarHeight
import kotlin.math.abs

/**
 * 手势指示浮层数据。
 *
 * @param type 手势种类（进度/音量/亮度）
 * @param percent 指示条进度（0..1）
 * @param text 中央数值文本（如 `62%`、`01:23`）
 * @param subtitle 进度手势的次要文本（总时长）
 */
data class GestureIndicatorData(
    val type: GestureIndicatorType,
    val percent: Float,
    val text: String,
    val subtitle: String? = null,
)

/** 手势种类。 */
enum class GestureIndicatorType { Progress, Volume, Brightness }

/**
 * 播放器手势层。
 *
 * 自旧版 `HJzvdStd.onTouch` / `touchActionMove` 迁移全部手势：
 * - 单击：切换控件显隐（[onTap]）
 * - 双击：播放/暂停（[onDoubleTap]）
 * - 长按：临时倍速（[onLongPressStart]，抬起后 [onLongPressEnd]），带触觉反馈由调用方处理
 * - 横滑：调进度（灵敏度 [sensitivity]，越界钳制，ERROR 态禁用）
 * - 左半屏竖滑：调亮度（Activity window brightness）
 * - 右半屏竖滑：调音量（STREAM_MUSIC）
 * - 手势起始点在状态栏/导航栏区域时忽略
 *
 * [enabled] 为 false（手势锁定）时全部手势禁用；
 * [dragEnabled] 为 false（长按倍速中）时拖动手势禁用，保持原实现行为。
 */
@Composable
fun Modifier.playerGestures(
    enabled: Boolean,
    dragEnabled: Boolean,
    seekEnabled: Boolean,
    sensitivity: Int,
    durationMsProvider: () -> Long,
    positionMsProvider: () -> Long,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onIndicator: (GestureIndicatorData?) -> Unit,
): Modifier {
    val view = LocalView.current
    val window = remember(view) {
        view.context.findActivityOrNull<Activity>()?.window
    }
    val audioManager = remember(view) {
        view.context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)
    val currentOnSeekTo by rememberUpdatedState(onSeekTo)
    val currentOnIndicator by rememberUpdatedState(onIndicator)
    val currentDuration by rememberUpdatedState(durationMsProvider)
    val currentPosition by rememberUpdatedState(positionMsProvider)
    val currentSeekEnabled by rememberUpdatedState(seekEnabled)

    return this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onDoubleTap = { currentOnDoubleTap() },
                onTap = { currentOnTap() },
                onLongPress = { currentOnLongPressStart() },
                onPress = {
                    tryAwaitRelease()
                    // 抬起或取消都结束长按倍速（幂等）
                    currentOnLongPressEnd()
                },
            )
        }
        .pointerInput(enabled, dragEnabled, sensitivity) {
            if (!enabled || !dragEnabled) return@pointerInput
            val threshold = PlayerDefaults.GESTURE_THRESHOLD.toFloat()
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                val downX = down.position.x
                val downY = down.position.y
                // 拖动的起点落在状态栏或导航栏区域时忽略
                if (downY < statusBarHeight || downX > appScreenWidth) {
                    return@awaitEachGesture
                }

                var dragging = false
                var horizontal = false
                var changeBrightness = false
                var gestureDownPosition = 0L
                var gestureDownVolume = 0
                var gestureDownBrightness = 0f
                var targetPosition = 0L
                // 时长未知时（C.TIME_UNSET / <=0，如视频仍在加载、流信息未就绪），
                // 进度手势不产出有效目标：coerceIn(0, TIME_UNSET) 是空区间会直接抛
                // IllegalArgumentException（线上崩溃），结束时也不能 seek 到无效位置。
                var targetPositionValid = false

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    // 事件已被子组件（弹窗列表、进度条等）消费时，本次手势让位，
                    // 否则拖动列表仍会触发音量/亮度/进度手势。
                    if (change.isConsumed) return@awaitEachGesture
                    if (!change.pressed) break

                    val dx = change.position.x - downX
                    val dy = change.position.y - downY

                    if (!dragging) {
                        val absDx = abs(dx)
                        val absDy = abs(dy)
                        if (absDx > threshold || absDy > threshold) {
                            if (absDx >= threshold) {
                                // 横滑调进度；ERROR 态不响应
                                if (!currentSeekEnabled) return@awaitEachGesture
                                horizontal = true
                                dragging = true
                                gestureDownPosition = currentPosition()
                            } else {
                                dragging = true
                                if (downX < appScreenWidth * 0.5f) {
                                    // 左半屏调亮度
                                    changeBrightness = true
                                    gestureDownBrightness = readCurrentBrightness(window)
                                } else {
                                    // 右半屏调音量
                                    gestureDownVolume =
                                        audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                }
                            }
                        }
                    }

                    if (dragging) {
                        if (horizontal) {
                            val duration = currentDuration()
                            if (duration > 0) {
                                targetPositionValid = true
                                val delta = dx * duration / (size.width * sensitivity)
                                targetPosition = (gestureDownPosition + delta.toLong())
                                    .coerceIn(0L, duration)
                                currentOnIndicator(
                                    GestureIndicatorData(
                                        type = GestureIndicatorType.Progress,
                                        percent = targetPosition.toFloat() / duration,
                                        text = PlayerUtils.stringForTime(targetPosition),
                                        subtitle = PlayerUtils.stringForTime(duration),
                                    )
                                )
                            }
                        } else {
                            val deltaY = -dy
                            if (changeBrightness) {
                                val deltaV = 255f * deltaY * 3f / size.height
                                val newBrightness =
                                    ((gestureDownBrightness + deltaV) / 255f).coerceIn(0.01f, 1f)
                                window?.let { w ->
                                    w.attributes = w.attributes.apply {
                                        screenBrightness = newBrightness
                                    }
                                }
                                val percent =
                                    ((gestureDownBrightness + deltaV) / 255f).coerceIn(0f, 1f)
                                currentOnIndicator(
                                    GestureIndicatorData(
                                        type = GestureIndicatorType.Brightness,
                                        percent = percent,
                                        text = "${(percent * 100).toInt()}%",
                                    )
                                )
                            } else {
                                val max =
                                    audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        ?: break
                                val deltaV = (max * deltaY * 3f / size.height).toInt()
                                val newVolume = (gestureDownVolume + deltaV).coerceIn(0, max)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                val percent =
                                    ((gestureDownVolume + deltaV).coerceIn(0, max)).toFloat() / max
                                currentOnIndicator(
                                    GestureIndicatorData(
                                        type = GestureIndicatorType.Volume,
                                        percent = percent,
                                        text = "${(percent * 100).toInt()}%",
                                    )
                                )
                            }
                        }
                        change.consume()
                    }
                }

                if (dragging) {
                    if (horizontal && targetPositionValid) {
                        currentOnSeekTo(targetPosition)
                    }
                    currentOnIndicator(null)
                }
            }
        }
}

/**
 * 读取当前生效的亮度（0..255）。
 *
 * window 未设置过亮度（`screenBrightness < 0`，跟随系统）时读系统设置值。
 */
private fun readCurrentBrightness(window: android.view.Window?): Float {
    val lp = window?.attributes
    if (lp != null && lp.screenBrightness >= 0) {
        return lp.screenBrightness * 255f
    }
    return try {
        Settings.System.getInt(
            window?.context?.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
        ).toFloat()
    } catch (_: Settings.SettingNotFoundException) {
        128f
    }
}
