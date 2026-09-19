package com.yenaly.han1meviewer.ui.screen.video

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.yenaly.han1meviewer.HAdvancedSearch
import com.yenaly.han1meviewer.HCacheManager
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeVideoDownloadLink
import com.yenaly.han1meviewer.getHanimeVideoLink
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.main.SearchRoute
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.requestPostNotificationPermission
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.han1meviewer.worker.HanimeDownloadWorker
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.copyToClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.Serializable

class VideoRouteActions(
    private val context: Context,
    private val scope: CoroutineScope,
    private val viewModel: VideoViewModel,
    private val genres: List<SearchOption>,
    private val requestStoragePermission: (
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) -> Unit,
    private val onPendingDownloadPromptChange: (DownloadPromptState?) -> Unit,
    private val getCheckedQuality: () -> String?,
    private val setCheckedQuality: (String?) -> Unit,
    private val onStoragePermissionDenied: () -> Unit,
    private val onRequestUnsubscribeConfirm: (HanimeVideo.Artist) -> Unit,
    private val onRequestDownloadPermissionSettings: () -> Unit,
) {
    fun openArtistSearch(artist: HanimeVideo.Artist) {
        val searchKey = genres.firstOrNull { option ->
            option.lang?.let { lang ->
                artist.genre == lang.zhrCN ||
                        artist.genre == lang.zhrTW ||
                        artist.genre == lang.en
            } == true
        }?.searchKey ?: ""
        val map = buildMap<HAdvancedSearch, Serializable> {
            put(HAdvancedSearch.QUERY, artist.name)
            if (searchKey.isNotEmpty() && !Preferences.searchArtistIgnoreVideoType) {
                put(HAdvancedSearch.GENRE, searchKey)
            }
        }
        val bundleMap = HashMap<String, Serializable>().apply {
            map.forEach { (key, value) -> put(key.name, value) }
        }
        val routeMap = bundleMap.mapValues { it.value.toString() }
        (context as? MainActivity)?.navController?.navigateSafely(
            SearchRoute(query = artist.name, advancedSearchJson = Json.encodeToString(routeMap))
        )
    }

    fun openTagSearch(tag: String) {
        (context as? MainActivity)?.navController?.navigateSafely(SearchRoute(query = tag))
    }

    fun toggleArtistSubscription(artist: HanimeVideo.Artist) {
        val post = artist.post ?: return
        if (!Preferences.isAlreadyLogin) {
            GlobalToasts.show(context.getString(R.string.login_first), level = GlobalToasts.ToastLevel.WARNING)
            return
        }
        if (artist.isSubscribed) {
            onRequestUnsubscribeConfirm(artist)
        } else {
            viewModel.subscribeArtist(post.userId, post.artistId)
        }
    }

    fun confirmUnsubscribe(artist: HanimeVideo.Artist) {
        val post = artist.post ?: return
        viewModel.unsubscribeArtist(post.userId, post.artistId)
    }

    fun toggleFavorite(video: HanimeVideo) {
        if (!Preferences.isAlreadyLogin) {
            GlobalToasts.show(context.getString(R.string.login_first), level = GlobalToasts.ToastLevel.WARNING)
            return
        }
        if (video.isFav) {
            viewModel.removeFromFavVideo(viewModel.videoCode, video.currentUserId)
        } else {
            viewModel.addToFavVideo(viewModel.videoCode, video.currentUserId)
        }
    }

    fun rateVideo(video: HanimeVideo, isPositive: Boolean) {
        if (!Preferences.isAlreadyLogin) {
            GlobalToasts.show(context.getString(R.string.login_first), level = GlobalToasts.ToastLevel.WARNING)
            return
        }
        viewModel.rateVideo(video, isPositive)
    }

    fun updateMyListSelection(
        myList: HanimeVideo.MyList?,
        selectedStates: List<Boolean>,
    ) {
        if (!Preferences.isAlreadyLogin || myList == null || myList.myListInfo.isEmpty()) {
            GlobalToasts.show(context.getString(R.string.login_first), level = GlobalToasts.ToastLevel.WARNING)
            return
        }
        myList.myListInfo.forEachIndexed { index, info ->
            val newChecked = selectedStates.getOrNull(index) ?: return@forEachIndexed
            if (info.isSelected != newChecked) {
                viewModel.modifyMyList(
                    listCode = info.code,
                    videoCode = viewModel.videoCode,
                    isChecked = newChecked,
                    position = index,
                )
            }
        }
    }

    fun openIntroductionLink(link: String) {
        try {
            context.browse(link)
        } catch (_: Exception) {
            link.copyToClipboard()
            GlobalToasts.show(context.getString(R.string.copy_to_clipboard), level = GlobalToasts.ToastLevel.INFO)
        }
    }

    fun openOriginalComic(comicLink: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, comicLink.toUri()))
        } catch (_: Exception) {
            GlobalToasts.show(context.getString(R.string.fault_prompt), level = GlobalToasts.ToastLevel.ERROR)
        }
    }

    fun openVideoWebPage() {
        context.browse(getHanimeVideoLink(viewModel.videoCode))
    }

    fun openOfficialDownloadPage() {
        context.browse(getHanimeVideoDownloadLink(viewModel.videoCode))
    }

    fun startDownloadFlow(videoData: HanimeVideo) {
        if (videoData.videoUrls.isEmpty()) {
            GlobalToasts.show(context.getString(R.string.no_video_links_found), level = GlobalToasts.ToastLevel.WARNING)
            return
        }
        requestStoragePermission(
            {
                viewModel.findDownloadedHanime(viewModel.videoCode)
            },
            {
                GlobalToasts.show(context.getString(R.string.storage_permission_denied_toast), level = GlobalToasts.ToastLevel.WARNING)
                onStoragePermissionDenied()
            },
            { openDownloadPermissionSettings() },
        )
    }

    fun confirmPendingDownload(
        videoData: HanimeVideo,
        pendingDownloadPrompt: DownloadPromptState?,
        autoGroupName: String? = null,
    ) {
        val redownload = pendingDownloadPrompt?.oldQuality != null
        onPendingDownloadPromptChange(null)
        scope.launch {
            enqueueDownloadWork(
                videoData = videoData,
                redownload = redownload,
                autoGroupName = autoGroupName,
            )
        }
    }

    private suspend fun enqueueDownloadWork(
        videoData: HanimeVideo,
        redownload: Boolean = false,
        autoGroupName: String? = null,
    ) {
        context.requestPostNotificationPermission()
        val quality = getCheckedQuality()
        withContext(Dispatchers.IO) {
            HCacheManager.saveHanimeVideoInfo(context, viewModel.videoCode, videoData)
        }
        val autoGroup = autoGroupName?.let { resolveAutoGroup(videoData, it) }
        autoGroup?.createdName?.let { name ->
            GlobalToasts.show(
                context.getString(R.string.download_auto_group_created, name),
                level = GlobalToasts.ToastLevel.SUCCESS,
            )
        }
        HanimeDownloadManagerV2.addTask(
            HanimeDownloadWorker.Args(
                quality = quality,
                downloadUrl = videoData.videoUrls[quality]?.link,
                videoType = videoData.videoUrls[quality]?.suffix,
                hanimeName = videoData.title,
                videoCode = viewModel.videoCode,
                coverUrl = videoData.coverUrl,
                groupId = autoGroup?.id,
            ),
            redownload = redownload,
        )
        GlobalToasts.show(
            context.getString(R.string.added_to_download_queue),
            level = GlobalToasts.ToastLevel.SUCCESS,
        )
    }

    private data class ResolvedGroup(val id: Int, val createdName: String?)

    /**
     * 解析自动分组：已有同名分组则复用，否则新建。
     *
     * @return 目标分组；分组名为空白时返回 null（即不自动分组）
     */
    private suspend fun resolveAutoGroup(videoData: HanimeVideo, name: String): ResolvedGroup? =
        withContext(Dispatchers.IO) {
            val groupName = name.trim().takeIf { it.isNotEmpty() }
                ?: return@withContext null
            val existing = DatabaseRepo.HanimeDownload.findGroupByName(groupName)
            val groupId = existing?.id
                ?: DatabaseRepo.HanimeDownload.createNewGroup(groupName).toInt()
            moveUngroupedSeriesToGroup(videoData, groupId)
            ResolvedGroup(id = groupId, createdName = groupName.takeIf { existing == null })
        }

    /**
     * 把同系列中仍处于默认分组（未分组）的影片一并归入目标分组。
     * 用户手动分过组的影片保持原位。
     */
    private suspend fun moveUngroupedSeriesToGroup(videoData: HanimeVideo, groupId: Int) {
        val seriesCodes = videoData.playlist?.video?.map { it.videoCode }.orEmpty()
        seriesCodes.forEach { videoCode ->
            val entity = DatabaseRepo.HanimeDownload.find(videoCode) ?: return@forEach
            if (entity.groupId != DownloadGroupEntity.DEFAULT_GROUP_ID) return@forEach
            DatabaseRepo.HanimeDownload.updateVideoGroup(videoCode, groupId)
        }
    }

    fun openDownloadPermissionSettings() {
        onRequestDownloadPermissionSettings()
    }

    fun goToDownloadPermissionSettings() {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        })
    }
}
