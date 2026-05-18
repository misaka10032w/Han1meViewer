package com.yenaly.han1meviewer.ui.screen.video

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Brightness7
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoPlayerUi(
    modifier: Modifier = Modifier,
    playerView: View? = null,
    title: String = "[中字候补] 一眼顶针，鉴定为纯纯的初生",
    currentTime: String = "12:36",
    totalTime: String = "24:12",
    progress: Float = 0.45f,
    bufferedProgress: Float = 0.72f,
    showControls: Boolean = true,
    isPlaying: Boolean = false,
    isLocked: Boolean = false,
    showResumeButton: Boolean = false,
    showLoading: Boolean = false,
    showRetry: Boolean = false,
    onPlayClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onFullscreenClick: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onProgressChange: (Float) -> Unit = {},
) {
    var showControlsState by remember { mutableStateOf(true) }

    LaunchedEffect(showControlsState, isPlaying) {
        if (showControlsState && isPlaying) {
            kotlinx.coroutines.delay(3000)
            showControlsState = false
        }
    }

    val effectiveShowControls = showControlsState

    Box(
        modifier = modifier
            .background(Color.Black)
    ) {

        /**
         * 视频渲染层
         */
        if (playerView != null) {
            AndroidView(
                factory = { playerView },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        /**
         * 封面
         */
        AsyncImage(
            model = null,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        /**
         * 顶部渐变
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
        )

        /**
         * 底部渐变
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        /**
         * 顶部控制栏
         */
        AnimatedVisibility(
            visible = effectiveShowControls,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
            ) {

                /**
                 * Background
                 */
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(20.dp))
                ) {

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.graphicsLayer {
                                        renderEffect =
                                            RenderEffect
                                                .createBlurEffect(
                                                    32f,
                                                    32f,
                                                    Shader.TileMode.CLAMP
                                                )
                                                .asComposeRenderEffect()
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .background(
                                Color.Black.copy(alpha = 0.18f)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(20.dp)
                            )
                    )
                }

                /**
                 * Content
                 */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    /**
                     * Back
                     */
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    /**
                     * Home
                     */
                    IconButton(
                        onClick = onHomeClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    /**
                     * Title
                     */
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    /**
                     * Small Status Chips
                     */
                    CompactChip("1.5x")

                    Spacer(modifier = Modifier.width(6.dp))

                    CompactChip("Anime4K")

                    Spacer(modifier = Modifier.width(10.dp))

                    /**
                     * Time
                     */
                    Text(
                        text = "11:45",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        /**
         * 中间播放按钮
         */
        if (!isPlaying || effectiveShowControls) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (showLoading) {
                    LoadingIndicator()
                } else if (!isPlaying) {
                    FilledIconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                } else {
                    // Playing: small pause button when controls are visible
                    IconButton(onClick = onPlayClick) {
                        Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        /**
         * 锁定按钮
         */
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            FilledIconButton(
                onClick = onLockClick,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(42.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.45f)
                )
            ) {
                Icon(
                    imageVector = if (isLocked)
                        Icons.Outlined.Lock
                    else
                        Icons.Outlined.LockOpen,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        /**
         * 底部控制栏
         */
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 10.dp
                    )
            ) {

                /**
                 * Background
                 */
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(18.dp))
                ) {

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.graphicsLayer {
                                        renderEffect =
                                            RenderEffect
                                                .createBlurEffect(
                                                    32f,
                                                    32f,
                                                    Shader.TileMode.CLAMP
                                                )
                                                .asComposeRenderEffect()
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .background(
                                Color.Black.copy(alpha = 0.18f)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(18.dp)
                            )
                    )
                }

                /**
                 * Content
                 */
                Column(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
                ) {

                    /**
                     * Ultra Thin Slider
                     */
                    PlayerSlider(
                        value = progress,
                        buffered = bufferedProgress,
                        onValueChange = onProgressChange,
                        modifier = Modifier.height(12.dp)
                    )

                    /**
                     * Bottom Controls
                     */
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        /**
                         * Play
                         */
                        IconButton(
                            onClick = {},
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        /**
                         * Time
                         */
                        Text(
                            text = "$currentTime / $totalTime",
                            color = Color.White.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.labelSmall
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        /**
                         * Quality
                         */
                        Text(
                            text = "1080P",
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelSmall
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        /**
                         * Fullscreen
                         */
                        IconButton(
                            onClick = onFullscreenClick,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Fullscreen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        /**
         * Resume 按钮
         */
        AnimatedVisibility(
            visible = showResumeButton,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp)
        ) {
            ElevatedButton(
                onClick = {},
                shape = RoundedCornerShape(50),
            ) {
                Text("从头开始播放")
            }
        }

        /**
         * Retry
         */
        AnimatedVisibility(
            visible = showRetry,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                shape = RoundedCornerShape(28.dp)
            ) {

                Column(
                    modifier = Modifier.padding(
                        horizontal = 28.dp,
                        vertical = 24.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "视频加载失败",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FilledTonalButton(
                        onClick = {}
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text("点击重试")
                    }
                }
            }
        }

        /**
         * Timer
         */
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = 90.dp,
                    start = 12.dp
                )
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
            ) {
                Text(
                    text = "#9 尻\n9",
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    color = Color.White,
                    fontSize = 22.sp
                )
            }
        }
    }
}

@Composable
private fun CompactChip(
    text: String
) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Color.White.copy(alpha = 0.08f)
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(10.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun PlayerSlider(
    value: Float,
    buffered: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,

        /**
         * Thumb
         */
        thumb = {
            Box(
                modifier = Modifier
                    .size(14.dp),
                contentAlignment = Alignment.Center
            ) {

                /**
                 * Glow
                 */
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            Color.White.copy(alpha = 0.22f),
                            CircleShape
                        )
                )

                /**
                 * Real Thumb
                 */
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color.White,
                            CircleShape
                        )
                )
            }
        },

        /**
         * Track
         */
        track = {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                contentAlignment = Alignment.CenterStart
            ) {

                /**
                 * Background Track
                 */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(100))
                        .background(
                            Color.White.copy(alpha = 0.14f)
                        )
                )

                /**
                 * Buffered Track
                 */
                Box(
                    modifier = Modifier
                        .fillMaxWidth(buffered.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(100))
                        .background(
                            Color.White.copy(alpha = 0.32f)
                        )
                )

                /**
                 * Active Track
                 */
                Box(
                    modifier = Modifier
                        .fillMaxWidth(value.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(100))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                                )
                            )
                        )
                )
            }
        }
    )
}

enum class GestureIndicatorType {
    Brightness,
    Volume,
    Progress,
}

@Composable
fun GestureIndicatorOverlay(
    visible: Boolean,
    type: GestureIndicatorType,
    percent: Float,
    modifier: Modifier = Modifier,
    text: String = "${(percent * 100).toInt()}%"
) {

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(
            initialScale = 0.92f
        ),
        exit = fadeOut() + scaleOut(
            targetScale = 0.92f
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            /**
             * Glass Container
             */
            Box(
                modifier = Modifier
                    .size(
                        width = 170.dp,
                        height = 190.dp
                    )
                    .clip(RoundedCornerShape(36.dp))
            ) {

                /**
                 * Blur Layer
                 */
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.graphicsLayer {
                                    renderEffect =
                                        RenderEffect
                                            .createBlurEffect(
                                                55f,
                                                55f,
                                                Shader.TileMode.CLAMP
                                            )
                                            .asComposeRenderEffect()
                                }
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            Color.Black.copy(alpha = 0.32f)
                        )
                )

                /**
                 * Glass Gradient
                 */
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.04f)
                                )
                            )
                        )
                )

                /**
                 * Border
                 */
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(36.dp)
                        )
                )

                /**
                 * Content
                 */
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 22.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Icon(
                        imageVector = when (type) {
                            GestureIndicatorType.Brightness -> Icons.Outlined.Brightness7
                            GestureIndicatorType.Volume -> Icons.AutoMirrored.Outlined.VolumeUp
                            GestureIndicatorType.Progress -> Icons.Outlined.FastForward
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = when (type) {
                            GestureIndicatorType.Brightness -> "亮度"
                            GestureIndicatorType.Volume -> "音量"
                            GestureIndicatorType.Progress -> "进度"
                        },
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    LinearProgressIndicator(
                        progress = { percent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100)),
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 960,
    heightDp = 540
)
@Composable
private fun VideoPlayerUiPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        VideoPlayerUi()
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 960,
    heightDp = 540
)
@Composable
private fun VideoPlayerUiLoadingPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        VideoPlayerUi(
            showLoading = true,
            isPlaying = true,
            showResumeButton = false,
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 960,
    heightDp = 540
)
@Composable
private fun VideoPlayerUiRetryPreview() {
    ComponentPreview {
        VideoPlayerUi(
            showRetry = true,
            showResumeButton = false,
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 960,
    heightDp = 540
)
@Composable
private fun GestureIndicatorOverlayPreview() {
    ComponentPreview {
        GestureIndicatorOverlay(
            visible = true,
            type = GestureIndicatorType.Brightness,
            percent = 0.5f,
        )
    }
}