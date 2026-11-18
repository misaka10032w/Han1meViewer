package com.yenaly.han1meviewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.yenaly.han1meviewer.ui.navigation.main.HomeRoute
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.main.MainTopLevelDestination

/**
 * Navigation 3 导航状态：每个顶层 Tab 一条独立返回栈（对应 Navigation 2 时代的
 * `popUpTo(Home){saveState=true} + restoreState` 语义），栈内容跨配置变更与进程死亡保留。
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    /** 当前顶层 Tab 的返回栈。 */
    val currentStack: NavBackStack<NavKey>
        get() = backStacks.getValue(topLevelRoute)

    /** 当前栈顶 key（无栈时返回 null）。 */
    val currentKey: NavKey?
        get() = backStacks[topLevelRoute]?.lastOrNull()

    /**
     * 需要交给 NavDisplay 渲染的条目：Home 栈 + 当前 Tab 栈。
     * SinglePaneScene 只渲染最后一个，其余仅保留状态。
     */
    val entriesInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            backStacks.getValue(startRoute).toList()
        } else {
            backStacks.getValue(startRoute) + backStacks.getValue(topLevelRoute)
        }
}

/**
 * 处理导航事件的唯一入口（单向数据流：Navigator 改 [NavigationState]，NavDisplay 观察渲染）。
 *
 * Navigation 2 时代 `navigateSafely` 的 RESUMED 门槛防的是库内部
 * “捕获过期 entry 再 pop”的竞态；Navigation 3 的返回栈由应用自己持有，
 * 该竞态结构性消失，这里只保留 450ms 节流用于吸收转场中的重复点击。
 */
class Navigator(val state: NavigationState) {

    private var lastNavigateAtMillis = 0L

    /** 顶层 Tab 集合。 */
    val topLevelRoutes: Set<NavKey> = state.backStacks.keys

    /**
     * 常规导航：目标为顶层 Tab 时仅切换栈；否则压入当前 Tab 的栈。
     */
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.currentStack.add(route)
        }
    }

    /**
     * 带节流的安全导航（吸收 450ms 页面转场内的重复点击）。
     */
    fun navigateSafely(route: NavKey) {
        val now = System.currentTimeMillis()
        if (now - lastNavigateAtMillis < NAVIGATE_THROTTLE_MILLIS) return
        lastNavigateAtMillis = now
        navigate(route)
    }

    /**
     * 返回：弹出当前栈顶；若当前 Tab 已在栈底则回到起始 Tab。
     * 在 Home 栈底时不会被执行（NavDisplay 的返回处理在该状态下不启用），
     * 系统返回自然落到 Activity 默认行为（退出确认 / finish）。
     */
    fun goBack(): Boolean {
        val stack = state.currentStack
        if (stack.size > 1) {
            stack.removeLastOrNull()
            return true
        }
        if (state.topLevelRoute != state.startRoute) {
            state.topLevelRoute = state.startRoute
            return true
        }
        return false
    }

    /**
     * 替换栈顶（自动连播：弹掉当前视频页、压入下一集），
     * 对应 Navigation 2 时代 `popUpTo(currentDestination.id){inclusive=true}`。
     */
    fun replaceTop(route: NavKey) {
        val stack = state.currentStack
        if (stack.isNotEmpty()) {
            stack[stack.lastIndex] = route
        } else {
            stack.add(route)
        }
    }

    /**
     * 回到主页：切换到 Home 栈并把 Home 栈修剪到栈底。
     * 其他 Tab 的栈保留（用户点返回主页按钮通常是结束当前浏览，而非清空全部 Tab 状态）。
     */
    fun goHome() {
        state.topLevelRoute = state.startRoute
        val homeStack = state.backStacks.getValue(state.startRoute)
        while (homeStack.size > 1) {
            homeStack.removeLastOrNull()
        }
    }

    /** 当前是否在某个顶层 Tab 的栈底。 */
    fun isAtTabRoot(): Boolean = state.currentStack.size <= 1

    companion object {
        private const val NAVIGATE_THROTTLE_MILLIS = 450L
    }
}

/**
 * 创建跨配置变更与进程死亡保留的导航状态。
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey = HomeRoute,
    topLevelRoutes: Set<NavKey>,
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute,
        topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * 由顶层 Tab 推导 NavigationSuite 应选中的目标。
 */
fun topLevelKeyFor(destination: MainTopLevelDestination, state: NavigationState): NavKey =
    state.backStacks.keys.first { key ->
        MainDestinationSpec.fromKey(key)?.topLevelDestination == destination
    }
