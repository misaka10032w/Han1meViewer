package com.yenaly.han1meviewer.ui.screen.home.creatorcenter

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.state.PageLoadingState

/**
 * 判断网格是否需要加载更多。
 *
 * 当最后可见项距离末尾不足 4 项时触发，排除 Loading/NoMoreData/Error 状态。
 *
 * @param state 当前页面加载状态
 * @return 是否需要加载更多
 */
fun LazyGridState.canLoadMore(state: PageLoadingState<*>): Boolean {
    if (state is PageLoadingState.Loading || state is PageLoadingState.NoMoreData || state is PageLoadingState.Error) return false
    val total = layoutInfo.totalItemsCount
    if (total == 0) return false
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    return lastVisible >= total - 4
}

/**
 * 将服务器返回的审核状态文本转换为本地化字符串。
 *
 * 服务器返回繁体中文状态值（"已上傳"/"排隊"/"待處理"/"轉檔"），
 * 通过包含匹配映射到对应字符串资源。
 *
 * @receiver 服务器原始审核状态文本
 * @return 本地化后的状态文本，无匹配时返回原值
 */
@Composable
fun String.toLocalizedReviewStatus(): String = when {
    contains("已上傳") -> stringResource(R.string.creator_status_uploaded)
    contains("排隊") -> stringResource(R.string.creator_status_queued)
    contains("待處理") -> stringResource(R.string.creator_status_pending)
    contains("轉檔") -> stringResource(R.string.creator_status_transcoding)
    else -> this
}

/**
 * 根据审核状态返回对应的颜色。
 *
 * 颜色映射：已上传→绿色，排队→黄色，待处理→橙色，转档→蓝色。
 *
 * @receiver 服务器原始审核状态文本
 * @return 对应颜色
 */
@Composable
fun String.toReviewStatusColor(): Color = when {
    contains("已上傳") -> Color(0xFF27C93F)
    contains("排隊") -> Color(0xFFF9A825)
    contains("待處理") -> Color(0xFFFF9800)
    contains("轉檔") -> Color(0xFF42A5F5)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}