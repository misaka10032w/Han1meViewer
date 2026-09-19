package com.yenaly.han1meviewer.ui.screen.home.homepage

import androidx.annotation.StringRes
import androidx.compose.runtime.staticCompositionLocalOf
import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HomePage

/**
 * 首页主要数据源
 * @param page 主页主要数据
 * @param announcements 从firebase获取的公告列表
 */
data class HomeData(
    val page: HomePage,
    val announcements: List<Announcement> = emptyList()
)

/**
 * 为首页搜索相关组件提供搜索历史查询能力。
 */
val LocalSearchHistoryQuery = staticCompositionLocalOf<suspend (String) -> List<String>> {
    { emptyList() }
}

/**
 * 首页 Hero 区域的轮播项。
 *
 * 官网运营位与下方分类行中的视频共用该模型，图片同为 16:9，可直接混排在同一个轮播里。
 *
 * @param imageUrl 图片地址。
 * @param title 主标题。
 * @param subtitle 副标题，视频条目为「艺术家 · 观看数 · 上传时间」。
 * @param videoCode 点击后打开的视频编号，官网运营位可能为空。
 */
data class HomeHeroItem(
    val imageUrl: String,
    val title: String,
    val subtitle: String?,
    val videoCode: String?,
)

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
    val key: String,
    @param:StringRes val titleRes: Int,
    val genre: String? = null,
    val sort: String? = null,
    val tags: String? = null,
    val videos: List<HanimeInfo>
)
