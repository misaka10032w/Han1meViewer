package com.yenaly.han1meviewer.ui.fragment.home.subscription

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun RetryableImage(
    model: Any,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    retryLimit: Int = 1,
    placeholder: Painter,
    error: Painter,
    contentScale: ContentScale? = ContentScale.Fit
) {
    val context = LocalContext.current
    var retryCount by remember { mutableIntStateOf(0) }
    var currentModel by remember { mutableStateOf(model) }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(currentModel)
            .crossfade(true)
            .listener(
                onError = { _, result ->
                    Log.e("CoilError", "Image load failed", result.throwable)
                }
            ).build(),
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = error,
        modifier = modifier,
        onError = {
            if (retryCount < retryLimit) {
                retryCount++
                currentModel = "$model?retry=$retryCount"
            }
        },
        contentScale = contentScale?: ContentScale.Fit
    )
}