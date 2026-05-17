package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.content.LoadingContent

@Composable
fun PageContent(
    isLoading: Boolean,
    isError: Boolean,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    errorMessage: String = "",
    onRetry: () -> Unit = {},
    loading: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingContent()
        }
    },
    error: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorContent(message = errorMessage, onRetry = onRetry)
        }
    },
    empty: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyContent(hint = "")
        }
    },
    content: @Composable () -> Unit,
) {
    when {
        isError && isEmpty -> error()
        isLoading && isEmpty -> loading()
        isEmpty -> empty()
        else -> content()
    }
}
