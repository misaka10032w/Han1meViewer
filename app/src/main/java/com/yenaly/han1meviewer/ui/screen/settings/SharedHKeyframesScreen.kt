package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.HKeyframeHeader
import com.yenaly.han1meviewer.logic.entity.HKeyframeType
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn

@Composable
fun SharedHKeyframesScreen(
    items: List<HKeyframeType>,
    onOpenVideo: (String) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyContent(hint = stringResource(R.string.here_is_empty))
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items) { item ->
            when (item) {
                is HKeyframeHeader -> SharedHeader(item)
                is HKeyframeEntity -> SharedEntityCard(item, onOpenVideo = { onOpenVideo(item.videoCode) })
            }
        }
    }
}

@Composable
private fun SharedHeader(header: HKeyframeHeader) {
    Text(
        text = header.title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SharedEntityCard(
    entity: HKeyframeEntity,
    onOpenVideo: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onOpenVideo)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(entity.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.h_keyframe_title_prefix) + entity.videoCode,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "@${entity.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            entity.keyframes.forEach { keyframe ->
                HorizontalDivider()
                Text(
                    text = cn.jzvd.JZUtils.stringForTime(keyframe.position),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                keyframe.prompt?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "➥ $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SharedHKeyframesScreenPreview() {
    ComponentPreview {
        SharedHKeyframesScreen(
            items = listOf(
                HKeyframeHeader("示例系列", emptyList()),
                HKeyframeEntity(
                    videoCode = "123456",
                    title = "图书室の彼女",
                    keyframes = mutableListOf(
                        HKeyframeEntity.Keyframe(12_000, "进入正题"),
                        HKeyframeEntity.Keyframe(36_000, "高能部分"),
                    ),
                    author = "tester",
                ),
            ),
            onOpenVideo = {},
        )
    }
}
