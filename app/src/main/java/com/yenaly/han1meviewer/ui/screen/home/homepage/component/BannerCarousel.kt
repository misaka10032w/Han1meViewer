package com.yenaly.han1meviewer.ui.screen.home.homepage.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.AutoScrollEffect
import com.yenaly.han1meviewer.ui.component.PageIndicator
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHeroItems
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.ui.screen.home.homepage.HomeHeroItem

/**
 * 显示首页 Banner 轮播图。
 *
 * 轮播项由官网运营位与下方分类行中的视频混排而成，见 `buildHomeHeroItems`。多于一项时会在
 * 无用户交互的情况下自动翻页，见 [AutoScrollEffect]。
 *
 * @param items 轮播数据，为空时不渲染内容。
 * @param onItemClick 点击某一项时调用，参数为视频编号，可能为空。
 * @param modifier 应用于轮播图根布局的修饰符。
 * @param size 指定的渲染尺寸。为空时占满可用宽度并按 16:9 计算高度；大屏下由调用方传入受限尺寸。
 * @param pagerState 由调用方持有以支持外部切换（如右侧待播队列），默认内部自持。
 */
@Composable
fun BannerCarousel(
    items: List<HomeHeroItem>,
    onItemClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
    size: DpSize? = null,
    pagerState: PagerState = rememberPagerState(pageCount = { items.size.coerceAtLeast(1) })
) {
    if (items.isEmpty()) return

    AutoScrollEffect(pagerState = pagerState, pageCount = items.size)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .then(
                    if (size != null) Modifier.size(size)
                    else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                )
                .clip(RoundedCornerShape(12.dp))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val item = items[page.coerceIn(items.indices)]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onItemClick(item.videoCode) }
                ) {
                    RetryableImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.h_chan_loading),
                        error = painterResource(R.drawable.h_chan_load_failed)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        item.subtitle?.let { desc ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            PageIndicator(
                pageCount = items.size,
                currentPage = pagerState.currentPage,
                selectedColor = Color.White,
                unselectedColor = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Banner 轮播")
@Composable
private fun BannerCarouselPreview() {
    ComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            BannerCarousel(
                items = fakeHeroItems,
                onItemClick = {}
            )
        }
    }
}
