package com.yenaly.han1meviewer.ui.screen.home.preview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.exception.HanimeNotFoundException
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.pienization
import com.yenaly.han1meviewer.ui.component.ComponentPreview
import com.yenaly.han1meviewer.ui.component.EmptyContent
import com.yenaly.han1meviewer.ui.component.ErrorContent
import com.yenaly.han1meviewer.ui.component.LoadingContent
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.han1meviewer.ui.view.CollapsibleTags

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    previewState: WebsiteState<HanimePreview>,
    commentCount: Int,
    onBack: () -> Unit,
    onLoadDate: (String) -> Unit,
    onOpenComment: (String, String) -> Unit,
    onOpenVideo: (String) -> Unit,
) {
    var currentDateCode by rememberSaveable { mutableStateOf(currentDateCode()) }
    var selectedIndex by rememberSaveable(currentDateCode) { mutableIntStateOf(0) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val currentDateLabel = remember(currentDateCode) { toNormalDateLabel(currentDateCode) }
    val prevDateCode = remember(currentDateCode) { shiftMonthCode(currentDateCode, -1) }
    val nextDateCode = remember(currentDateCode) { shiftMonthCode(currentDateCode, 1) }
    val prevDateLabel = remember(prevDateCode) { toNormalDateLabel(prevDateCode) }
    val nextDateLabel = remember(nextDateCode) { toNormalDateLabel(nextDateCode) }

    val success = previewState as? WebsiteState.Success
    val currentPreview = success?.info?.previewInfo?.getOrNull(selectedIndex)

    val canPrev = when (previewState) {
        is WebsiteState.Loading -> false
        is WebsiteState.Success -> previewState.info.hasPrevious
        is WebsiteState.Error -> true
    }
    val canNext = when (previewState) {
        is WebsiteState.Success -> previewState.info.hasNext
        else -> false
    }

    LaunchedEffect(currentDateCode) {
        onLoadDate(currentDateCode)
        selectedIndex = 0
    }

    if (previewImageUrl != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { previewImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { previewImageUrl = null },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = previewImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.latest_hanime_list_monthly, currentDateLabel)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            actions = {
                IconButton(onClick = { onOpenComment(currentDateLabel, currentDateCode) }) {
                    BadgedBox(
                        badge = {
                            if (commentCount > 0) {
                                Badge { Text(commentCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_comment_24),
                            contentDescription = stringResource(R.string.comment),
                        )
                    }
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PreviewHeaderSection(
                    headerImageUrl = success?.info?.headerPicUrl,
                    prevLabel = prevDateLabel,
                    nextLabel = nextDateLabel,
                    canPrev = canPrev,
                    canNext = canNext,
                    onPrev = { currentDateCode = prevDateCode },
                    onNext = { currentDateCode = nextDateCode },
                )
            }

            when (previewState) {
                is WebsiteState.Loading -> item {
                    LoadingContent(modifier = Modifier.padding(horizontal = 16.dp))
                }

                is WebsiteState.Error -> item {
                    ErrorContent(
                        title = stringResource(R.string.hanime_list),
                        message = if (previewState.throwable is HanimeNotFoundException) {
                            stringResource(R.string.preview_page_updating)
                        } else {
                            previewState.throwable.pienization.toString()
                        },
                        onRetry = { onLoadDate(currentDateCode) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                is WebsiteState.Success -> {
                    item {
                        PreviewTourRow(
                            latestHanime = previewState.info.latestHanime,
                            selectedIndex = selectedIndex,
                            onSelect = { selectedIndex = it },
                        )
                    }

                    item {
                        if (currentPreview == null) {
                            EmptyContent(
                                title = stringResource(R.string.empty_content),
                                description = stringResource(R.string.new_anime_trailers),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        } else {
                            PreviewInfoCard(
                                previewInfo = currentPreview,
                                onOpenVideo = { code -> code?.let(onOpenVideo) },
                                onOpenImage = { previewImageUrl = it },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewHeaderSection(
    headerImageUrl: String?,
    prevLabel: String,
    nextLabel: String,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        AsyncImage(
            model = headerImageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(onClick = onPrev, enabled = canPrev, modifier = Modifier.weight(1f)) {
                Text(prevLabel)
            }
            FilledTonalButton(onClick = onNext, enabled = canNext, modifier = Modifier.weight(1f)) {
                Text(nextLabel)
            }
        }
    }
}

@Composable
private fun PreviewTourRow(
    latestHanime: List<HanimeInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(
            latestHanime,
            key = { index, item -> item.videoCode.ifBlank { "tour-$index" } },
        ) { index, item ->
            OutlinedCard(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(18.dp),
            ) {
                Box {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .width(92.dp)
                            .height(132.dp),
                        contentScale = ContentScale.Crop,
                    )
                    if (index == selectedIndex) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewInfoCard(
    previewInfo: HanimePreview.PreviewInfo,
    onOpenVideo: (String?) -> Unit,
    onOpenImage: (String) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                AsyncImage(
                    model = previewInfo.coverUrl,
                    contentDescription = previewInfo.videoTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
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

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row {
                    Column (modifier = Modifier.weight(1f) ) {
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
                    Text(
                        text = previewInfo.introduction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (previewInfo.tags.isNotEmpty()) {
                    PreviewTagsView(tags = previewInfo.tags)
                }

                if (previewInfo.relatedPicsUrl.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(previewInfo.relatedPicsUrl, key = { index, _ -> "$index-${previewInfo.videoCode}" }) { _, url ->
                            ElevatedCard(onClick = { onOpenImage(url) }, shape = RoundedCornerShape(16.dp)) {
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

@Composable
private fun PreviewTagsView(tags: List<String>) {
    val ownerLifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            CollapsibleTags(context).apply {
                lifecycle = ownerLifecycle
                isCollapsedEnabled = true
            }
        },
        update = { view ->
            view.lifecycle = ownerLifecycle
            view.tags = tags
        },
    )
}

private fun currentDateCode(): String = currentCodeFrom(2024, 1).let {
    val now = java.time.LocalDate.now()
    currentCodeFrom(now.year, now.monthValue)
}

private fun currentCodeFrom(year: Int, month: Int): String = "%04d%02d".format(year, month)

private fun toNormalDateLabel(code: String): String {
    val year = code.substring(0, 4).toInt()
    val month = code.substring(4, 6).toInt()
    return "$year/$month"
}

private fun shiftMonthCode(code: String, delta: Int): String {
    var year = code.substring(0, 4).toInt()
    var month = code.substring(4, 6).toInt() + delta
    while (month < 1) {
        month += 12
        year -= 1
    }
    while (month > 12) {
        month -= 12
        year += 1
    }
    return currentCodeFrom(year, month)
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun PreviewScreenPreview() {
    val preview = HanimePreview(
        headerPicUrl = fakeHomePageVideos.first().coverUrl,
        hasPrevious = true,
        hasNext = true,
        latestHanime = fakeHomePageVideos.take(5),
        previewInfo = listOf(
            HanimePreview.PreviewInfo(
                title = "日文标题",
                videoTitle = "中文标题 第1话",
                coverUrl = fakeHomePageVideos.first().coverUrl,
                introduction = "这是用于预览的简介内容，用来确认 Compose 版布局是否正常。",
                brand = "发行商 A",
                releaseDate = "2024-01-01",
                videoCode = fakeHomePageVideos.first().videoCode,
                tags = listOf("新番", "预告", "校园"),
                relatedPicsUrl = listOf(fakeHomePageVideos[1].coverUrl, fakeHomePageVideos[2].coverUrl),
            )
        ),
    )
    ComponentPreview {
        PreviewScreen(
            previewState = WebsiteState.Success(preview),
            commentCount = 12,
            onBack = {},
            onLoadDate = {},
            onOpenComment = { _, _ -> },
            onOpenVideo = {},
        )
    }
}
