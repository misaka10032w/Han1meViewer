package com.yenaly.han1meviewer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

sealed interface StateScreenState {
    data object Loading : StateScreenState
    data object Content : StateScreenState
    data class Empty(val title: String, val description: String? = null) : StateScreenState
    data class Error(val title: String = "加载失败", val message: String? = null) : StateScreenState
}

@Composable
fun StateScreen(
    state: StateScreenState,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    when (state) {
        StateScreenState.Content -> content()
        StateScreenState.Loading -> LoadingContent(modifier = modifier)
        is StateScreenState.Empty -> EmptyContent(
            title = state.title,
            description = state.description,
            modifier = modifier,
        )

        is StateScreenState.Error -> ErrorContent(
            title = state.title,
            message = state.message,
            modifier = modifier,
            onRetry = onRetry,
        )
    }
}
