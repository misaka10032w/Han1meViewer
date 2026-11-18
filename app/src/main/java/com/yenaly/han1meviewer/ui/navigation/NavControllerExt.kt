package com.yenaly.han1meviewer.ui.navigation

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * 判断当前能否安全地变更返回栈。
 *
 * 仅当栈顶 entry 处于 [Lifecycle.State.RESUMED] 时才返回 true：
 * 预测性返回手势（Predictive Back）进行中、或页面转场尚未结束时，
 * Navigation 库会把栈顶 entry 的 Lifecycle 压到 STARTED。此时若再
 * navigate/pop，会与 NavHost 内部的返回事件处理器竞态——它在手势开始
 * 时捕获栈顶 entry、手势提交时无条件弹出该 entry，栈一旦被并发修改就会
 * 抛出 IllegalStateException: Attempted to pop ..., which is not the
 * top of the back stack（线上崩溃 c3387ecdf1e52599f5c2b3e8d39d655c）。
 *
 * 因此这里用 RESUMED（而不是 STARTED）作为门槛：手势/转场进行中一律拦截，
 * 顺带也消除了 450ms 转场内的重复点击导航。
 */
fun NavController.canNavigateSafely(): Boolean {
    val currentState = currentBackStackEntry?.lifecycle?.currentState
    val canNavigate = currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true
    if (!canNavigate) {
        Log.i(
            "nav_state",
            "Navigation blocked: current entry state = $currentState " +
                "(返回手势或页面转场进行中)，已忽略本次返回栈变更",
        )
    }
    return canNavigate
}

/**
 * 安全导航。
 *
 * @param force 为 true 时跳过 [canNavigateSafely] 检查，用于深链/Intent
 * 等外部驱动的导航——它们到达时用户必然不在应用内的返回手势中，且冷启动
 * 首帧栈顶可能尚未 RESUMED，不强制放行会被静默丢弃。
 */
fun <T : Any> NavController.navigateSafely(
    route: T,
    force: Boolean = false,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    if (!force && !canNavigateSafely()) return
    navigate(route) {
        launchSingleTop = false
        builder()
    }
}

/**
 * 程序化返回的安全封装，语义同 [NavController.popBackStack]。
 *
 * 返回 false 表示：当前处于返回手势/转场进行中而被跳过，或返回栈已空。
 */
fun NavController.popBackStackSafely(): Boolean {
    if (!canNavigateSafely()) return false
    return popBackStack()
}

/**
 * 弹出到指定路由的安全封装，语义同 [NavController.popBackStack]（type-safe 重载）。
 */
fun <T : Any> NavController.popBackStackSafely(
    route: T,
    inclusive: Boolean = false,
    saveState: Boolean = false,
): Boolean {
    if (!canNavigateSafely()) return false
    return popBackStack(route, inclusive, saveState)
}
