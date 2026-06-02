package com.yenaly.han1meviewer.ui.screen.home.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.ui.component.TagChipGroup
import com.yenaly.han1meviewer.ui.component.lazy.LazyRow
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeNewHanimeInfo

/**
 * 预览详情卡片。
 *
 * @param previewInfo 预览信息
 * @param onOpenVideo 打开视频详情回调
 * @param onOpenImage 打开图片查看器回调 (index, imageUrls)
 */
@Composable
fun PreviewInfoCard(
    previewInfo: HanimePreview.PreviewInfo,
    onOpenVideo: (String?) -> Unit,
    onOpenImage: (Int, List<String>) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                AsyncImage(
                    model = previewInfo.coverUrl,
                    contentDescription = previewInfo.videoTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                SelectionContainer(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = previewInfo.videoTitle.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!previewInfo.title.isNullOrBlank()) {
                            Text(
                                text = previewInfo.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!previewInfo.brand.isNullOrBlank()) {
                            Text(
                                text = "${stringResource(R.string.brand)}: ${previewInfo.brand}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (!previewInfo.releaseDate.isNullOrBlank()) {
                            Text(
                                text = "${stringResource(R.string.release_date)}: ${previewInfo.releaseDate}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (!previewInfo.videoCode.isNullOrBlank()) {
                        Button(onClick = { onOpenVideo(previewInfo.videoCode) }) {
                            Text(stringResource(R.string.play_trailer))
                        }
                    }
                }
                if (!previewInfo.introduction.isNullOrBlank()) {
                    SelectionContainer {
                        Text(
                            text = previewInfo.introduction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (previewInfo.tags.isNotEmpty()) {
                    TagChipGroup(tags = previewInfo.tags)
                }

                if (previewInfo.relatedPicsUrl.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(
                            previewInfo.relatedPicsUrl,
                            key = { index, _ -> "$index-${previewInfo.videoCode}" }
                        ) { index, url ->
                            ElevatedCard(
                                onClick = { onOpenImage(index, previewInfo.relatedPicsUrl) },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(88.dp),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun PreviewInfoCardPreview() {
    ComponentPreview {
        PreviewInfoCard(
            previewInfo = fakeNewHanimeInfo.first(),
            onOpenVideo = {},
            onOpenImage = { _, _ -> },
        )
    }
}
