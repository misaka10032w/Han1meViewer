package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.content.LoadingContent

@Composable
fun VideoScreen(
    state: VideoLoadingState<*>,
    onRetry: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()

        if (LocalInspectionMode.current) {
            when (state) {
                is VideoLoadingState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingContent(message = "载入视频页面中…")
                    }
                }

                is VideoLoadingState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorContent(
                            title = "视频加载失败",
                            message = state.throwable.message,
                            onRetry = onRetry,
                        )
                    }
                }

                is VideoLoadingState.NoContent -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyContent(hint = "该影片可能不存在")
                    }
                }

                else -> Unit
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun VideoScreenLoadingPreview() {
    ComponentPreview {
        VideoScreen(
            state = VideoLoadingState.Loading,
            onRetry = {},
            content = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun VideoScreenErrorPreview() {
    ComponentPreview {
        VideoScreen(
            state = VideoLoadingState.Error(Throwable("network error")),
            onRetry = {},
            content = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun VideoScreenNoContentPreview() {
    ComponentPreview {
        VideoScreen(
            state = VideoLoadingState.NoContent,
            onRetry = {},
            content = {},
        )
    }
}
