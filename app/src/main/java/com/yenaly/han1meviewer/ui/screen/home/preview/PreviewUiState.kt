package com.yenaly.han1meviewer.ui.screen.home.preview

import androidx.compose.runtime.saveable.listSaver
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.state.WebsiteState

/**
 * 预览页面路由状态。
 *
 * @param currentDateCode 当前日期码 (yyyyMM)
 * @param selectedIndex 当前选中的预览项索引
 */
data class PreviewRouteUiState(
    val currentDateCode: String = currentDateCode(),
    val selectedIndex: Int = 0,
) {
    companion object {
        val Saver = listSaver<PreviewRouteUiState, Any>(
            save = { listOf(it.currentDateCode, it.selectedIndex) },
            restore = {
                PreviewRouteUiState(
                    currentDateCode = it[0] as String,
                    selectedIndex = it[1] as Int,
                )
            },
        )
    }
}

/**
 * 预览页面的月份头部展示状态。
 *
 * @param dateCode 日期码
 * @param headerImageUrl 顶部横幅图片 URL
 * @param prevLabel 上一月份标签
 * @param nextLabel 下一月份标签
 * @param canPrev 是否可切换到上一月
 * @param canNext 是否可切换到下一月
 */
data class PreviewMonthHeaderState(
    val dateCode: String,
    val headerImageUrl: String?,
    val prevLabel: String,
    val nextLabel: String,
    val canPrev: Boolean,
    val canNext: Boolean,
)

/**
 * 图片查看器状态。
 *
 * @param imageUrls 图片 URL 列表
 * @param initialPage 初始展示的图片索引
 */
data class PreviewImageViewerState(
    val imageUrls: List<String>,
    val initialPage: Int,
)

/**
 * 预览页面 UI 状态。
 *
 * @param routeState 路由/翻页状态
 * @param currentDateLabel 当前月份标签
 * @param prevDateLabel 上一月份标签
 * @param nextDateLabel 下一月份标签
 * @param monthAnimationDirection 月份切换动画方向
 * @param displayState 当前展示的网络状态
 * @param commentCount 评论区评论数
 * @param canPrev 是否可切换到上一月
 * @param canNext 是否可切换到下一月
 * @param monthHeaderState 月份头部状态
 * @param imageViewerState 图片查看器状态，null 表示未打开
 */
data class PreviewUiState(
    val routeState: PreviewRouteUiState = PreviewRouteUiState(),
    val currentDateLabel: String = "",
    val prevDateLabel: String = "",
    val nextDateLabel: String = "",
    val monthAnimationDirection: Int = 1,
    val displayState: WebsiteState<HanimePreview> = WebsiteState.Loading,
    val commentCount: Int = 0,
    val canPrev: Boolean = false,
    val canNext: Boolean = false,
    val monthHeaderState: PreviewMonthHeaderState,
    val imageViewerState: PreviewImageViewerState? = null,
)

/**
 * 预览页面的用户交互事件。
 */
sealed interface PreviewEvent {
    /** 返回上一页 */
    data object OnBack : PreviewEvent

    /** 点击游览列表中的某个项 */
    data class OnSelectTourItem(val index: Int) : PreviewEvent

    /** 切换到上一个月 */
    data class OnPrevMonth(val fromDateCode: String) : PreviewEvent

    /** 切换到下一个月 */
    data class OnNextMonth(val fromDateCode: String) : PreviewEvent

    /** 打开图片查看器 */
    data class OnOpenImage(val index: Int, val imageUrls: List<String>) : PreviewEvent

    /** 关闭图片查看器 */
    data object OnDismissImageViewer : PreviewEvent

    /** 打开视频详情 */
    data class OnOpenVideo(val videoCode: String?) : PreviewEvent

    /** 打开评论页 */
    data class OnOpenComment(val label: String, val dateCode: String) : PreviewEvent

    /** 重试加载 */
    data object OnRetryLoad : PreviewEvent
}
