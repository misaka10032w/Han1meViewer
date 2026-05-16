package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ResolutionLinkMap
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.ui.component.BottomSheetHandler
import com.yenaly.han1meviewer.ui.component.ComponentPreview
import com.yenaly.han1meviewer.ui.component.EmptyContent
import com.yenaly.han1meviewer.ui.component.ErrorContent
import com.yenaly.han1meviewer.ui.component.ExpandableRichText
import com.yenaly.han1meviewer.ui.component.LoadingContent
import com.yenaly.han1meviewer.ui.component.TagChipGroup
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.component.lazy.LazyRow
import com.yenaly.han1meviewer.ui.preview.fakeVideoIntroduction
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format

private val previewSafeDateFormat = LocalDate.Formats.ISO

data class DownloadPromptState(
    val newQuality: String,
    val oldQuality: String? = null,
)

@Composable
fun VideoIntroductionScreen(
    video: HanimeVideo?,
    state: VideoLoadingState<HanimeVideo>,
    fromDownload: Boolean,
    hideRelatedInIntro: Boolean,
    shareText: String,
    playlistInitialIndex: Int,
    introFirstVisibleItemIndex: Int,
    introFirstVisibleItemScrollOffset: Int,
    downloadPrompt: DownloadPromptState?,
    onRetry: () -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    onOpenArtist: (HanimeVideo.Artist) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onToggleSubscribe: (HanimeVideo.Artist) -> Unit,
    onToggleFavorite: () -> Unit,
    onManageMyList: (List<String>, List<Boolean>) -> Unit,
    onQuickCheckIn: (CheckInRecordEntity) -> Unit,
    onPrepareDownload: (String) -> Unit,
    onDismissDownloadPrompt: () -> Unit,
    onConfirmDownloadPrompt: () -> Unit,
    onRequestOpenOfficialDownloadPage: () -> Unit,
    onRequestOpenDownloadPermissionSettings: () -> Unit,
    onShare: () -> Unit,
    onCopyShareText: () -> Unit,
    onOpenWebPage: () -> Unit,
    onOpenOriginalComic: (() -> Unit)?,
    onCopyText: (String) -> Unit,
    onShowAllPlaylist: (() -> Unit)?,
    onPlaylistScrollChange: (Int) -> Unit,
    onIntroductionScrollChange: (Int, Int) -> Unit,
    onIntroductionLinkClick: (String) -> Unit,
) {
    val maxScreenWidth = LocalWindowInfo.current.containerSize.width.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = maxScreenWidth)
    ) {
        val currentVideo = video ?: (state as? VideoLoadingState.Success)?.info
        when {
            currentVideo != null -> VideoIntroductionContent(
                video = currentVideo,
                fromDownload = fromDownload,
                hideRelatedInIntro = hideRelatedInIntro,
                shareText = shareText,
                playlistInitialIndex = playlistInitialIndex,
                introFirstVisibleItemIndex = introFirstVisibleItemIndex,
                introFirstVisibleItemScrollOffset = introFirstVisibleItemScrollOffset,
                downloadPrompt = downloadPrompt,
                onOpenVideo = onOpenVideo,
                onOpenArtist = onOpenArtist,
                onNavigateToSearch = onNavigateToSearch,
                onToggleSubscribe = onToggleSubscribe,
                onToggleFavorite = onToggleFavorite,
                onManageMyList = onManageMyList,
                onQuickCheckIn = onQuickCheckIn,
                onPrepareDownload = onPrepareDownload,
                onDismissDownloadPrompt = onDismissDownloadPrompt,
                onConfirmDownloadPrompt = onConfirmDownloadPrompt,
                onRequestOpenOfficialDownloadPage = onRequestOpenOfficialDownloadPage,
                onRequestOpenDownloadPermissionSettings = onRequestOpenDownloadPermissionSettings,
                onShare = onShare,
                onCopyShareText = onCopyShareText,
                onOpenWebPage = onOpenWebPage,
                onOpenOriginalComic = onOpenOriginalComic,
                onCopyText = onCopyText,
                onShowAllPlaylist = onShowAllPlaylist,
                onPlaylistScrollChange = onPlaylistScrollChange,
                onIntroductionScrollChange = onIntroductionScrollChange,
                onIntroductionLinkClick = onIntroductionLinkClick,
            )

            state is VideoLoadingState.Error -> ErrorContent(
                title = stringResource(R.string.load_failed_retry),
                message = state.throwable.message,
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )

            state is VideoLoadingState.NoContent -> EmptyContent(
                hint = stringResource(R.string.video_might_not_exist),
            )

            else -> LoadingContent(
                modifier = Modifier.align(Alignment.Center),
                message = stringResource(R.string.loading),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoIntroductionContent(
    video: HanimeVideo,
    fromDownload: Boolean,
    hideRelatedInIntro: Boolean,
    shareText: String,
    playlistInitialIndex: Int,
    introFirstVisibleItemIndex: Int,
    introFirstVisibleItemScrollOffset: Int,
    downloadPrompt: DownloadPromptState?,
    onOpenVideo: (HanimeInfo) -> Unit,
    onOpenArtist: (HanimeVideo.Artist) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onToggleSubscribe: (HanimeVideo.Artist) -> Unit,
    onToggleFavorite: () -> Unit,
    onManageMyList: (List<String>, List<Boolean>) -> Unit,
    onQuickCheckIn: (CheckInRecordEntity) -> Unit,
    onPrepareDownload: (String) -> Unit,
    onDismissDownloadPrompt: () -> Unit,
    onConfirmDownloadPrompt: () -> Unit,
    onRequestOpenOfficialDownloadPage: () -> Unit,
    onRequestOpenDownloadPermissionSettings: () -> Unit,
    onShare: () -> Unit,
    onCopyShareText: () -> Unit,
    onOpenWebPage: () -> Unit,
    onOpenOriginalComic: (() -> Unit)?,
    onCopyText: (String) -> Unit,
    onShowAllPlaylist: (() -> Unit)?,
    onPlaylistScrollChange: (Int) -> Unit,
    onIntroductionScrollChange: (Int, Int) -> Unit,
    onIntroductionLinkClick: (String) -> Unit,
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = introFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = introFirstVisibleItemScrollOffset,
    )
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showQuickCheckInDialog by remember { mutableStateOf(false) }
    var showMyListDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }

    if (showDownloadQualityDialog) {
        DownloadQualityDialog(
            videoUrls = video.videoUrls,
            onDismiss = { showDownloadQualityDialog = false },
            onOpenOfficial = {
                showDownloadQualityDialog = false
                onRequestOpenOfficialDownloadPage()
            },
            onSelectQuality = { quality ->
                showDownloadQualityDialog = false
                onPrepareDownload(quality)
            },
        )
    }

    if (downloadPrompt != null) {
        DownloadConfirmDialog(
            video = video,
            prompt = downloadPrompt,
            onDismiss = onDismissDownloadPrompt,
            onConfirm = onConfirmDownloadPrompt,
            onOpenOfficial = {
                onDismissDownloadPrompt()
                onRequestOpenOfficialDownloadPage()
            },
        )
    }

    if (showPermissionSettingsDialog) {
        PermissionSettingsDialog(
            onDismiss = { showPermissionSettingsDialog = false },
            onConfirm = {
                showPermissionSettingsDialog = false
                onRequestOpenDownloadPermissionSettings()
            },
        )
    }

    if (showPlaylistSheet && video.playlist != null) {
        PlaylistBottomSheet(
            playlist = video.playlist,
            onDismiss = { showPlaylistSheet = false },
            onOpenVideo = {
                showPlaylistSheet = false
                onOpenVideo(it)
            },
        )
    }

    if (showQuickCheckInDialog) {
        QuickCheckInDialog(
            video = video,
            onDismiss = { showQuickCheckInDialog = false },
            onConfirm = {
                onQuickCheckIn(it)
                showQuickCheckInDialog = false
            },
        )
    }

    if (showMyListDialog && video.myList != null) {
        MyListDialog(
            myList = video.myList,
            onDismiss = { showMyListDialog = false },
            onConfirm = { selectedStates ->
                onManageMyList(video.myList.titleArray.toList(), selectedStates)
                showMyListDialog = false
            },
        )
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.distinctUntilChanged().collect { (index, offset) ->
            onIntroductionScrollChange(index, offset)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 6.dp, top = 12.dp, end = 6.dp, bottom = 24.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        video.artist?.let { artist ->
            item(key = "artist") {
                ArtistSection(
                    artist = artist,
                    onOpenArtist = { onOpenArtist(artist) },
                    onToggleSubscribe = { onToggleSubscribe(artist) },
                )
            }
        }

        item(key = "title") {
            TitleSection(video = video, onCopyText = onCopyText)
        }

        item(key = "meta") {
            MetaSection(video = video, fromDownload = fromDownload)
        }

        item(key = "intro") {
            ExpandableIntroductionSection(
                introduction = video.introduction.orEmpty(),
                onIntroductionLinkClick = onIntroductionLinkClick,
            )
        }

        if (!fromDownload) {
            item(key = "actions") {
                ActionSection(
                    isFav = video.isFav,
                    hasOriginalComic = !video.originalComic.isNullOrBlank(),
                    onQuickCheckIn = { showQuickCheckInDialog = true },
                    onOpenOriginalComic = onOpenOriginalComic,
                    onToggleFavorite = onToggleFavorite,
                    onManageMyList = { showMyListDialog = true },
                    onDownload = { showDownloadQualityDialog = true },
                    onShare = onShare,
                    onCopyShareText = onCopyShareText,
                    onOpenWebPage = onOpenWebPage,
                )
            }
        }

        if (video.tags.isNotEmpty()) {
            item(key = "tags") {
                TagsSection(
                    tags = video.tags,
                    onTagClick = onNavigateToSearch,
                )
            }
        }

        if (!fromDownload && video.playlist != null && video.playlist.video.isNotEmpty()) {
            item(key = "playlist") {
                PlaylistSection(
                    playlist = video.playlist,
                    initialIndex = playlistInitialIndex,
                    onOpenVideo = onOpenVideo,
                    onShowAllPlaylist = if (onShowAllPlaylist != null) {
                        { showPlaylistSheet = true }
                    } else {
                        null
                    },
                    onPlaylistScrollChange = onPlaylistScrollChange,
                )
            }
        }

        if (!fromDownload && !hideRelatedInIntro && video.relatedHanimes.isNotEmpty()) {
            item(key = "related") {
                RelatedVideosSection(
                    videos = video.relatedHanimes,
                    onOpenVideo = onOpenVideo,
                )
            }
        }
    }
}

@Composable
private fun DownloadQualityDialog(
    videoUrls: ResolutionLinkMap,
    onDismiss: () -> Unit,
    onOpenOfficial: () -> Unit,
    onSelectQuality: (String) -> Unit,
) {
    val qualities = videoUrls.keys.toList()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                qualities.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = false,
                                onClick = {
                                    if (quality == com.yenaly.han1meviewer.HanimeResolution.RES_UNKNOWN) {
                                        onOpenOfficial()
                                    } else {
                                        onSelectQuality(quality)
                                    }
                                },
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(quality)
                        if (quality == com.yenaly.han1meviewer.HanimeResolution.RES_UNKNOWN) {
                            Text(
                                text = stringResource(R.string.go_to_official),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun DownloadConfirmDialog(
    video: HanimeVideo,
    prompt: DownloadPromptState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onOpenOfficial: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (prompt.oldQuality != null) R.string.sure_to_redownload else R.string.sure_to_download
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.download_video_detail_below))
                prompt.oldQuality?.let {
                    Text(stringResource(R.string.check_video_exists_in_download, it))
                }
                Text(stringResource(R.string.name_with_colon) + video.title)
                Text(
                    stringResource(R.string.quality_with_colon) + if (
                        prompt.oldQuality != null && prompt.oldQuality != prompt.newQuality
                    ) {
                        "${prompt.oldQuality} → ${prompt.newQuality}"
                    } else {
                        prompt.newQuality
                    }
                )
                Text(
                    text = stringResource(R.string.after_download_tips),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.sure))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onOpenOfficial) {
                    Text(stringResource(R.string.go_to_official))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.no))
                }
            }
        },
    )
}

@Composable
private fun PermissionSettingsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = { Text(stringResource(R.string.storage_permission_settings_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.go_to_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun QuickCheckInDialog(
    video: HanimeVideo,
    onDismiss: () -> Unit,
    onConfirm: (CheckInRecordEntity) -> Unit,
) {
    var feeling by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(video.title) },
        text = {
            OutlinedTextField(
                value = feeling,
                onValueChange = {
                    if (it.length <= 200) {
                        feeling = it
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.dialog_feeling_hint)) },
                minLines = 3,
                supportingText = {
                    Text("${feeling.length}/200")
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    val sep = "\u001E"
                    val record = CheckInRecordEntity(
                        date = java.time.LocalDate.now().toString(),
                        time = time,
                        type = com.yenaly.han1meviewer.logic.entity.CheckInType.MASTURBATION.storeName,
                        sideDishes = "${video.title}$sep${video.playlist?.video?.firstOrNull { it.isPlaying }?.videoCode ?: ""}".removeSuffix(sep),
                        feeling = feeling,
                    )
                    onConfirm(record)
                }
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun MyListDialog(
    myList: HanimeVideo.MyList,
    onDismiss: () -> Unit,
    onConfirm: (List<Boolean>) -> Unit,
) {
    var selectedStates by remember(myList.myListInfo) {
        mutableStateOf(myList.myListInfo.map { it.isSelected })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(myList.myListInfo.indices.toList()) { index ->
                    val info = myList.myListInfo[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = selectedStates[index],
                                onValueChange = { checked ->
                                    selectedStates = selectedStates.toMutableList().also { it[index] = checked }
                                },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Checkbox(
                            checked = selectedStates[index],
                            onCheckedChange = null,
                        )
                        Text(info.title)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedStates) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.back))
            }
        },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistBottomSheet(
    playlist: HanimeVideo.Playlist,
    onDismiss: () -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetHandler() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.series_video),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    playlist.playlistName?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.blank_brackets, playlist.video.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(playlist.video, key = { it.videoCode }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onOpenVideo(item) },
                                onLongClick = null,
                            )
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .width(100.dp)
                                .height(66.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = item.currentArtist.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistSection(
    artist: HanimeVideo.Artist,
    onOpenArtist: () -> Unit,
    onToggleSubscribe: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onOpenArtist, onLongClick = null)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = artist.avatarUrl,
                contentDescription = artist.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist.genre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            artist.post?.let {
                Button(onClick = onToggleSubscribe) {
                    Text(
                        text = if (artist.isSubscribed) {
                            stringResource(R.string.subscribed)
                        } else {
                            stringResource(R.string.subscribe)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleSection(video: HanimeVideo, onCopyText: (String) -> Unit) {
    val primaryTitle = video.chineseTitle?.takeIf { it.isNotBlank() } ?: video.title
    val secondaryTitle = video.title.takeIf { it != primaryTitle }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = primaryTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { onCopyText(primaryTitle) },
            )
        )
        secondaryTitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { onCopyText(it) },
                )
            )
        }
    }
}

@Composable
private fun MetaSection(video: HanimeVideo, fromDownload: Boolean) {
    val viewsText = if (fromDownload) {
        stringResource(R.string.s_view_times, "0721")
    } else {
        stringResource(R.string.s_view_times, video.views.toString())
    }
    val uploadTime = video.uploadTime?.format(previewSafeDateFormat).orEmpty()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = viewsText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 16.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = uploadTime,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpandableIntroductionSection(
    introduction: String,
    onIntroductionLinkClick: (String) -> Unit,
) {
    if (introduction.isBlank()) return
    ExpandableRichText(
        text = introduction,
        onLinkClick = onIntroductionLinkClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionSection(
    isFav: Boolean,
    hasOriginalComic: Boolean,
    onQuickCheckIn: () -> Unit,
    onOpenOriginalComic: (() -> Unit)?,
    onToggleFavorite: () -> Unit,
    onManageMyList: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onCopyShareText: () -> Unit,
    onOpenWebPage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VideoActionButton(
            iconRes = R.drawable.ic_baseline_check_circle_24,
            label = stringResource(R.string.quick_checkin),
            onClick = onQuickCheckIn,
        )
        if (hasOriginalComic && onOpenOriginalComic != null) {
            VideoActionButton(
                iconRes = R.drawable.ic_baseline_book,
                label = stringResource(R.string.original_comic),
                onClick = onOpenOriginalComic,
            )
        }
        VideoActionButton(
            iconRes = if (isFav) R.drawable.ic_baseline_favorite_24 else R.drawable.ic_baseline_favorite_border_24,
            label = if (isFav) stringResource(R.string.liked) else stringResource(R.string.add_to_fav),
            onClick = onToggleFavorite,
        )
        VideoActionButton(
            iconRes = R.drawable.baseline_format_list_bulleted_24,
            label = stringResource(R.string.add_to_playlist),
            onClick = onManageMyList,
        )
        VideoActionButton(
            iconRes = R.drawable.ic_baseline_download_24,
            label = stringResource(R.string.download),
            onClick = onDownload,
        )
        VideoActionButton(
            iconRes = R.drawable.ic_baseline_share_24,
            label = stringResource(R.string.share),
            onClick = onShare,
            onLongClick = onCopyShareText,
        )
        VideoActionButton(
            iconRes = R.drawable.ic_baseline_language_24,
            label = stringResource(R.string.jump_to_webpage),
            onClick = onOpenWebPage,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .widthIn(min = 76.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TagsSection(
    tags: List<String>,
    onTagClick: (String) -> Unit,
) {
    TagChipGroup(
        tags = tags,
        collapsible = true,
        collapsedMaxLines = 2,
        modifier = Modifier.fillMaxWidth(),
        onTagClick = onTagClick,
    )
}

@Composable
private fun PlaylistSection(
    playlist: HanimeVideo.Playlist,
    initialIndex: Int,
    onOpenVideo: (HanimeInfo) -> Unit,
    onShowAllPlaylist: (() -> Unit)?,
    onPlaylistScrollChange: (Int) -> Unit,
) {
    val listState = remember(playlist.video, initialIndex) {
        LazyListState(firstVisibleItemIndex = initialIndex)
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect(onPlaylistScrollChange)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = stringResource(R.string.series_video),
            subtitle = playlist.playlistName,
            actionText = if (onShowAllPlaylist != null) stringResource(R.string.more) else null,
            onActionClick = onShowAllPlaylist,
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(playlist.video, key = { it.videoCode }) { item ->
                VideoCardItem(
                    videoItem = item,
                    isHorizontalCard = item.itemType == HanimeInfo.NORMAL,
                    onClickVideosItem = { onOpenVideo(item) },
                    onLongClickVideosItem = { _, _ -> },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RelatedVideosSection(
    videos: List<HanimeInfo>,
    onOpenVideo: (HanimeInfo) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = stringResource(R.string.related_video))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = if (videos.firstOrNull()?.itemType == HanimeInfo.NORMAL) {
                dimensionResource(R.dimen.video_cover_width)
            } else {
                dimensionResource(R.dimen.video_cover_simplified_width)
            }
            val columns = maxOf(1, (maxWidth / cardWidth).toInt())
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = columns,
                horizontalArrangement = Arrangement.Start
            ) {
                videos.forEach { item ->
                    VideoCardItem(
                        videoItem = item,
                        isHorizontalCard = item.itemType == HanimeInfo.NORMAL,
                        onClickVideosItem = { onOpenVideo(item) },
                        onLongClickVideosItem = { _, _ -> },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!actionText.isNullOrBlank() && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Text(actionText)
                }
            }
        }
        HorizontalDivider()
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900, device = "id:pixel_tablet")
@Composable
private fun VideoIntroductionScreenPreview() {
    ComponentPreview {
        VideoIntroductionScreen(
            video = fakeVideoIntroduction,
            state = VideoLoadingState.Success(fakeVideoIntroduction),
            fromDownload = false,
            hideRelatedInIntro = false,
            shareText = "share text",
            playlistInitialIndex = 1,
            downloadPrompt = null,
            onRetry = {},
            onOpenVideo = {},
            onOpenArtist = {},
            onNavigateToSearch = {},
            onToggleSubscribe = {},
            onToggleFavorite = {},
            onManageMyList = { _, _ -> },
            onQuickCheckIn = {},
            onPrepareDownload = {},
            onDismissDownloadPrompt = {},
            onConfirmDownloadPrompt = {},
            onRequestOpenOfficialDownloadPage = {},
            onRequestOpenDownloadPermissionSettings = {},
            onShare = {},
            onCopyShareText = {},
            onOpenWebPage = {},
            onOpenOriginalComic = {},
            onCopyText = {},
            onShowAllPlaylist = {},
            onPlaylistScrollChange = {},
            introFirstVisibleItemIndex = 0,
            introFirstVisibleItemScrollOffset = 0,
            onIntroductionScrollChange = { _, _ -> },
            onIntroductionLinkClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun VideoIntroductionScreenLoadingPreview() {
    ComponentPreview {
        VideoIntroductionScreen(
            video = null,
            state = VideoLoadingState.Loading,
            fromDownload = false,
            hideRelatedInIntro = false,
            shareText = "",
            playlistInitialIndex = 0,
            downloadPrompt = null,
            onRetry = {},
            onOpenVideo = {},
            onOpenArtist = {},
            onNavigateToSearch = {},
            onToggleSubscribe = {},
            onToggleFavorite = {},
            onManageMyList = { _, _ -> },
            onQuickCheckIn = {},
            onPrepareDownload = {},
            onDismissDownloadPrompt = {},
            onConfirmDownloadPrompt = {},
            onRequestOpenOfficialDownloadPage = {},
            onRequestOpenDownloadPermissionSettings = {},
            onShare = {},
            onCopyShareText = {},
            onOpenWebPage = {},
            onOpenOriginalComic = null,
            onCopyText = {},
            onShowAllPlaylist = null,
            onPlaylistScrollChange = {},
            introFirstVisibleItemIndex = 0,
            introFirstVisibleItemScrollOffset = 0,
            onIntroductionScrollChange = { _, _ -> },
            onIntroductionLinkClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun VideoIntroductionScreenErrorPreview() {
    ComponentPreview {
        VideoIntroductionScreen(
            video = null,
            state = VideoLoadingState.Error(Throwable("network error")),
            fromDownload = false,
            hideRelatedInIntro = false,
            shareText = "",
            playlistInitialIndex = 0,
            downloadPrompt = null,
            onRetry = {},
            onOpenVideo = {},
            onOpenArtist = {},
            onNavigateToSearch = {},
            onToggleSubscribe = {},
            onToggleFavorite = {},
            onManageMyList = { _, _ -> },
            onQuickCheckIn = {},
            onPrepareDownload = {},
            onDismissDownloadPrompt = {},
            onConfirmDownloadPrompt = {},
            onRequestOpenOfficialDownloadPage = {},
            onRequestOpenDownloadPermissionSettings = {},
            onShare = {},
            onCopyShareText = {},
            onOpenWebPage = {},
            onOpenOriginalComic = null,
            onCopyText = {},
            onShowAllPlaylist = null,
            onPlaylistScrollChange = {},
            introFirstVisibleItemIndex = 0,
            introFirstVisibleItemScrollOffset = 0,
            onIntroductionScrollChange = { _, _ -> },
            onIntroductionLinkClick = {},
        )
    }
}
