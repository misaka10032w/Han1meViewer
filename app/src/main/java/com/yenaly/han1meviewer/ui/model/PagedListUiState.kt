package com.yenaly.han1meviewer.ui.model

data class PagedListUiState<T>(
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val noMoreData: Boolean = false,
    val items: List<T> = emptyList(),
    val errorMessage: String? = null,
)
