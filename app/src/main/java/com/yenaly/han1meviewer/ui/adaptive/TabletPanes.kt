package com.yenaly.han1meviewer.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun TabletListDetail(
    showDetail: Boolean,
    listWidth: Dp,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    emptyDetail: @Composable () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(listWidth)
                .fillMaxHeight(),
        ) {
            list()
        }
        VerticalDivider()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            if (showDetail) {
                Box(modifier = Modifier.fillMaxSize()) {
                    detail()
                }
            } else {
                emptyDetail()
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
