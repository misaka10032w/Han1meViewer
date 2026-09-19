package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** 图片轮播的默认翻页间隔。 */
val AutoScrollInterval = 5.seconds

/**
 * 让 pager 在无用户交互时自动翻页。
 *
 * 自动翻页的运行期间不能被重组打断：`animateScrollToPage` 会在动画起步时就把 `currentPage`
 * 改到目标页，若把它读进组合期当 key，动画会被自己的重组取消而停在两页之间。因此页码只在
 * 协程内读取，用于在用户滑动或外部切页后重新计时。拖动期间计时取消，应用退到后台时暂停，
 * 只有一页以及预览下不滚动。
 *
 * @param pagerState 目标 pager 的状态。
 * @param pageCount 页数，与 [pagerState] 的页数保持一致。
 * @param interval 两次自动翻页之间的间隔，以文字为主的轮播可以给更长的时间。
 */
@Composable
fun AutoScrollEffect(
    pagerState: PagerState,
    pageCount: Int,
    interval: Duration = AutoScrollInterval,
) {
    if (LocalInspectionMode.current || pageCount <= 1) return

    val lifecycleOwner = LocalLifecycleOwner.current
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(pagerState, pageCount, isDragged, interval) {
        if (isDragged) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var settledPage = pagerState.currentPage
            while (true) {
                delay(interval)
                // 惯性滑动尚未结束时不抢页码
                if (pagerState.isScrollInProgress) continue
                // 期间被用户滑动或外部切过页，重新完整计时
                if (pagerState.currentPage != settledPage) {
                    settledPage = pagerState.currentPage
                    continue
                }
                pagerState.animateScrollToPage((settledPage + 1) % pageCount)
                settledPage = pagerState.currentPage
            }
        }
    }
}
