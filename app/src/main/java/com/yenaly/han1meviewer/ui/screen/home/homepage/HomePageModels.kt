package com.yenaly.han1meviewer.ui.screen.home.homepage

import androidx.annotation.StringRes
import androidx.compose.runtime.staticCompositionLocalOf
import com.yenaly.han1meviewer.logic.model.HanimeInfo

/**
 * 为首页搜索相关组件提供搜索历史查询能力。
 */
val LocalSearchHistoryQuery = staticCompositionLocalOf<suspend (String) -> List<String>> {
    { emptyList() }
}

/**
 * 首页视频分类行数据。
 *
 * @param titleRes 分类标题的字符串资源。
 * @param genre 高级搜索使用的可选类型参数。
 * @param sort 高级搜索使用的可选排序参数。
 * @param tags 高级搜索使用的可选标签参数。
 * @param videos 当前分类下展示的视频列表。
 */
data class HomeCategory(
    @param:StringRes val titleRes: Int,
    val genre: String? = null,
    val sort: String? = null,
    val tags: String? = null,
    val videos: List<HanimeInfo>
)
