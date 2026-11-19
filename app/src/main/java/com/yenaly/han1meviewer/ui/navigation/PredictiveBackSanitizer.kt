package com.yenaly.han1meviewer.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.findViewTreeNavigationEventDispatcherOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.findActivityOrNull

/**
 * 预测性返回（Predictive Back）progress 清洗层。
 *
 * ## 背景
 *
 * Navigation 3 的 NavDisplay 通过 `LocalNavigationEventDispatcherOwner` 获取
 * 事件分发器，把系统返回手势的 progress 直接喂给内部的
 * `SeekableTransitionState.seekTo(progress)`。部分 OEM ROM 的
 * `BackEvent.getProgress()` 会下发 NaN（线上崩溃 be2c60c93bf91a82759adbdccb82807c：
 * `IllegalArgumentException: Expecting fraction between 0 and 1. Got NaN`），
 * 而 androidx.navigation / androidx.navigationevent（截至 1.2.0）与
 * androidx.activity（截至 1.13.0）在整条链路上都未做清洗，seekTo 收到 NaN
 * 即 fatal。
 *
 * ## 方案
 *
 * 建立一个"自带清洗输入源"的独立 [NavigationEventDispatcher]，并把它注入到
 * Activity decorView 的 ViewTree 上：
 *
 * - 桥接器以一个 [OnBackPressedCallback] 的身份挂在 Activity 的
 *   OnBackPressedDispatcher 上（与官方 OnBackInvokedInput 的行为一致：当
 *   没有 enabled 的 handler 时自动 disable，把事件还给排在后面的其他
 *   callback 或系统默认行为）；
 * - 手势事件经 [BackEventCompat] 到达后，将 progress 清洗为有限值并夹取到
 *   [0, 1]，再送入独立分发器；
 * - NavDisplay 及其他组件通过 ViewTree 解析出清洗后的分发器，拿到的事件
 *   永远是安全的。
 *
 * ## 为什么用 ViewTree 而不是 CompositionLocalProvider
 *
 * `LocalNavigationEventDispatcherOwner` 的解析优先级是「显式 provide >
 * ViewTree」。M3 的 ModalBottomSheet 等组件自身是独立 Dialog 窗口，
 * androidx.activity 的 ComponentDialog 会把自己的 dispatcher 挂到
 * Dialog decorView 的 ViewTree 上，sheet 内的 PredictiveBackHandler
 * 据此注册到 Dialog 级 dispatcher。若这里用 CompositionLocalProvider
 * 显式 provide，会遮蔽 Dialog 子树的 ViewTree 值，导致 sheet 的返回
 * handler 注册到 NavDisplay 的分发器（收不到 Dialog 窗口的返回事件），
 * 且 M3 Wrapper 的 cancel() 被置空，表现为返回手势/按键完全无响应。
 * 注入 ViewTree 则天然分层：Dialog 子树命中自己 decorView 上的 owner，
 * Activity 子树命中清洗层。
 */
@Composable
fun SanitizedPredictiveBackOwner(content: @Composable () -> Unit) {
    val activity = LocalContext.current.findActivityOrNull<ComponentActivity>()
    if (activity == null) {
        // 非 ComponentActivity 宿主（理论上不会发生），退化为官方默认行为。
        content()
        return
    }
    // 在 remember 计算块内同步挂 ViewTree tag（父级先于 NavDisplay 组合执行，
    // 保证 NavDisplay 组合期读取 LocalNavigationEventDispatcherOwner 时 tag 已就位）。
    val bridge = remember(activity) {
        val decorView = activity.window?.decorView
        val previousOwner = decorView?.findViewTreeNavigationEventDispatcherOwner()
        val bridge = SanitizingBackBridge(activity)
        decorView?.setViewTreeNavigationEventDispatcherOwner(bridge)
        bridge.restoreViewTreeOwner = {
            decorView?.setViewTreeNavigationEventDispatcherOwner(previousOwner)
        }
        bridge
    }
    DisposableEffect(activity, bridge) {
        onDispose {
            bridge.restoreViewTreeOwner?.invoke()
            bridge.restoreViewTreeOwner = null
            bridge.dispose()
        }
    }
    content()
}

private class SanitizingBackBridge(
    activity: ComponentActivity,
) : NavigationEventDispatcherOwner {

    /** 退出组合时恢复 decorView 上原有的 ViewTree owner（由 [SanitizedPredictiveBackOwner] 注入）。 */
    var restoreViewTreeOwner: (() -> Unit)? = null

    /** 提供给 NavDisplay 使用的独立分发器。 */
    val dispatcher = NavigationEventDispatcher()

    /**
     * 最近一次有效的手势进度。
     *
     * vivo Android 14（OriginOS 4）在快速轻扫返回时会在 onBackProgressed
     * 中下发 NaN（线上 be2c60c9 共 579 次，全部为 vivo & Android 14），
     * onBackStarted 的值通常仍有效。NaN 时回退到该值而不是 0，
     * 预览动画不会突然跳回起点。
     */
    private var lastGoodProgress = 0f

    /**
     * 分发器的输入源：暴露安全的事件转发方法，并跟随 NavHost 是否存在
     * enabled handler 来启停系统侧 callback。
     */
    private val input = object : NavigationEventInput() {

        var hostCallback: OnBackPressedCallback? = null

        override fun onHasEnabledBackHandlersChanged(hasEnabledBackHandlers: Boolean) {
            // 与官方 OnBackInvokedInput 行为对齐：NavHost 没有 enabled 的
            // handler（例如已回到首页）时禁用 callback，事件按原有优先级
            // 顺序流转给其他 callback / Activity 默认行为（finish 等）。
            hostCallback?.isEnabled = hasEnabledBackHandlers
        }

        fun backStarted(event: NavigationEvent) = dispatchOnBackStarted(event)

        fun backProgressed(event: NavigationEvent) = dispatchOnBackProgressed(event)

        fun backCompleted() = dispatchOnBackCompleted()

        fun backCancelled() = dispatchOnBackCancelled()
    }

    private val callback = object : OnBackPressedCallback(false) {
        override fun handleOnBackStarted(backEvent: BackEventCompat) {
            // 用户开启了「禁用预测性返回」时不转发进度事件：
            // 拖动过程无预览动画，松手提交时照常返回（走普通 pop 转场）。
            if (Preferences.disablePredictiveBack) return
            input.backStarted(toSanitizedNavigationEvent(backEvent))
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            if (Preferences.disablePredictiveBack) return
            input.backProgressed(toSanitizedNavigationEvent(backEvent))
        }

        override fun handleOnBackPressed() {
            lastGoodProgress = 0f
            input.backCompleted()
        }

        override fun handleOnBackCancelled() {
            lastGoodProgress = 0f
            input.backCancelled()
        }
    }

    init {
        input.hostCallback = callback
        dispatcher.addInput(input)
        activity.onBackPressedDispatcher.addCallback(callback)
    }

    fun dispose() {
        callback.remove()
        dispatcher.dispose()
    }

    override val navigationEventDispatcher: NavigationEventDispatcher
        get() = dispatcher

    /**
     * 把系统的 [BackEventCompat] 转成 progress 安全的 [NavigationEvent]：
     * NaN / 无限值回退为最近一次有效进度（[lastGoodProgress]），越界值夹取到 [0, 1]。
     */
    private fun toSanitizedNavigationEvent(backEvent: BackEventCompat): NavigationEvent {
        val raw = backEvent.progress
        val progress = if (raw.isFinite() && raw >= 0f) {
            raw.coerceAtMost(1f).also { lastGoodProgress = it }
        } else {
            lastGoodProgress
        }
        return NavigationEvent(
            swipeEdge = when (backEvent.swipeEdge) {
                BackEventCompat.EDGE_LEFT -> NavigationEvent.EDGE_LEFT
                BackEventCompat.EDGE_RIGHT -> NavigationEvent.EDGE_RIGHT
                else -> NavigationEvent.EDGE_NONE
            },
            progress = progress,
            touchX = backEvent.touchX,
            touchY = backEvent.touchY,
        )
    }
}
