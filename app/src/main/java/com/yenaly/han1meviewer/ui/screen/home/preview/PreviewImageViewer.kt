package com.yenaly.han1meviewer.ui.screen.home.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R

/**
 * 预览图片查看器弹窗。支持翻页浏览、双击/双指缩放。
 *
 * @param imageUrls 图片 URL 列表
 * @param initialPage 初始展示的图片索引
 * @param onDismiss 关闭回调
 */
@Composable
fun PreviewImageViewerDialog(
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageUrls.size.coerceAtLeast(1) })
    var isCurrentImageZoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isCurrentImageZoomed,
                    verticalAlignment = Alignment.CenterVertically,
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        AsyncImage(
                            model = imageUrls[page],
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = 0.24f },
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                                        )
                                    )
                                )
                        )
                        AsyncImage(
                            model = imageUrls[page],
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center),
                            contentScale = ContentScale.Fit,
                        )
                        ZoomablePreviewImage(
                            imageUrl = imageUrls[page],
                            isActivePage = pagerState.currentPage == page,
                            onZoomStateChange = { zoomed ->
                                if (pagerState.currentPage == page) {
                                    isCurrentImageZoomed = zoomed
                                }
                            },
                            onDismiss = onDismiss,
                        )
                    }
                }

                LaunchedEffect(pagerState.currentPage) {
                    isCurrentImageZoomed = false
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            CircleShape
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(imageUrls.size) { index ->
                            val selected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 8.dp else 6.dp)
                                    .background(
                                        color = if (selected) Color.White else Color.White.copy(
                                            alpha = 0.45f
                                        ),
                                        shape = CircleShape,
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 可缩放的预览图片组件。
 *
 * @param imageUrl 图片 URL
 * @param isActivePage 是否为当前活跃页
 * @param onZoomStateChange 缩放状态变化回调
 * @param onDismiss 单击关闭回调
 */
@Composable
fun ZoomablePreviewImage(
    imageUrl: String,
    isActivePage: Boolean,
    onZoomStateChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale, isActivePage) {
        if (isActivePage) {
            onZoomStateChange(scale > 1.01f)
        }
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imageUrl, scale) {
                detectTapGestures(
                    onTap = {
                        if (scale <= 1.01f) onDismiss()
                    },
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2f
                        }
                    },
                )
            }
            .then(
                if (scale > 1.01f) {
                    Modifier.pointerInput(imageUrl) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            offset = if (newScale <= 1f) {
                                Offset.Zero
                            } else {
                                offset + pan
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentScale = ContentScale.Fit,
    )
}
