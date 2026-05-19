package com.yenaly.han1meviewer.ui.model

import com.yenaly.han1meviewer.logic.model.SearchOption

data class SearchScopeSection(
    val titleRes: Int,
    val options: List<SearchOption>,
    val spanCount: Int = 3,
)