package com.yenaly.han1meviewer.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun AdaptiveListDetail(
    useListDetail: Boolean,
    showDetail: Boolean,
    listWidth: Dp,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    emptyDetail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paneState = rememberSaveableStateHolder()
    Row(modifier = modifier.fillMaxSize()) {
        if (useListDetail || !showDetail) {
            BoxWithConstraints(
                modifier = Modifier
                    .then(if (useListDetail) Modifier.width(listWidth) else Modifier.weight(1f))
                    .fillMaxHeight(),
            ) {
                CompositionLocalProvider(LocalContentWidthDp provides maxWidth) {
                    paneState.SaveableStateProvider("list") {
                        list()
                    }
                }
            }
        }
        if (useListDetail) VerticalDivider()
        if (useListDetail || showDetail) {
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentWidthDp provides maxWidth) {
                    if (showDetail) {
                        paneState.SaveableStateProvider("detail") {
                            Box(modifier = Modifier.fillMaxSize()) {
                                detail()
                            }
                        }
                    } else {
                        emptyDetail()
                    }
                }
            }
        }
    }
}

@Composable
fun TabletEmptyDetail(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
