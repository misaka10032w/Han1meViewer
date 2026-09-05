package com.yenaly.han1meviewer.logic.model

/**
 * 搜索结果：一页的影片列表 + 总页数
 */
data class HanimeSearchResult(
    val list: List<HanimeInfo>,
    val totalPages: Int,
)
