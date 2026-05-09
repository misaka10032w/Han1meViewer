package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun BottomSheetHandler() {
    Box(
        modifier = Modifier
            .height(32.dp)
            .fillMaxWidth()
            .zIndex(1f),
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(3.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50),
                )
                .align(Alignment.Center),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomSheetHandlerPreview() {
    ComponentPreview {
        BottomSheetHandler()
    }
}
