package com.yenaly.han1meviewer.ui.preview

import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.logic.model.DownloadItemNode
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.model.HomePage
import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
import com.yenaly.han1meviewer.logic.model.VideoComments
import com.yenaly.han1meviewer.ui.screen.home.homepage.HomeCategory
import kotlinx.datetime.LocalDate


/**
 * Compose预览用数据源
 */
val fakeArtists = listOf(
    SubscriptionItem("初音未来", "null"),
    SubscriptionItem("绫波丽", "null"),
    SubscriptionItem("阿库娅", "null"),
    SubscriptionItem("初音未来", "null"),
    SubscriptionItem("绫波丽", "null"),
    SubscriptionItem("阿库娅", "null"),
    SubscriptionItem("初音未来", "null"),
    SubscriptionItem("绫波丽", "null"),
    SubscriptionItem("阿库娅", "null"),
)

val fakeVideos = listOf(
    SubscriptionVideosItem(
        title = "小恶魔的补习计划",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
        videoCode = "101573",
        duration = "04:34",
        views = "44.9万次",
        reviews = "100%",
        uploadTime = "2010-12-10",
    ),
    SubscriptionVideosItem(
        title = "姐姐的秘密训练",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101574l.jpg",
        videoCode = "101574",
        duration = "23:15",
        views = "22.1万次",
        reviews = "95%",
        uploadTime = "2010-12-10",
    ),
    SubscriptionVideosItem(
        title = "放学后的约定",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101575l.jpg",
        videoCode = "101575",
        duration = "18:02",
        views = "58.3万次",
        reviews = "97%",
        uploadTime = "2010-12-10",
    ),
    SubscriptionVideosItem(
        title = "班长的福利日",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101576l.jpg",
        videoCode = "101576",
        duration = "12:47",
        views = "30.0万次",
        reviews = "92%",
        uploadTime = "2010-12-10",
    ),
    SubscriptionVideosItem(
        title = "图书馆的秘密角落",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101577l.jpg",
        videoCode = "101577",
        duration = "15:20",
        views = "61.7万次",
        reviews = "99%",
        uploadTime = "2010-12-10",
    ),
)

val fakeVideosItem = SubscriptionVideosItem(
    title = "小恶魔的补习计划",
    coverUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
    videoCode = "101573",
    duration = "04:34",
    views = "44.9万次",
    reviews = "100%",
    uploadTime = "2020-12-12",
)

val fakePlaylists = listOf(
    Playlists.Playlist(
        listCode = "code1",
        title = "浪漫喜剧精选合集",
        total = 24,
        coverUrl = "https://picsum.photos/300/200?random=1",
    ),
    Playlists.Playlist(
        listCode = "code2",
        title = "动作大片必看榜单",
        total = 18,
        coverUrl = "https://picsum.photos/300/200?random=2",
    ),
    Playlists.Playlist(
        listCode = "code3",
        title = "温暖治愈的日常剧推荐",
        total = 32,
        coverUrl = "https://picsum.photos/300/200?random=3",
    ),
    Playlists.Playlist(
        listCode = "code4",
        title = "悬疑推理高分作品",
        total = 12,
        coverUrl = "https://picsum.photos/300/200?random=4",
    ),
    Playlists.Playlist(
        listCode = "code5",
        title = "经典动画短片集锦",
        total = 45,
        coverUrl = "https://picsum.photos/300/200?random=5",
    ),
)

val fakeBanner = HomePage.Banner(
    title = "【新作】小悪魔の補習計画 - 第1話",
    description = "クラスで一番真面目な委員長が、放課後に秘密の補習を…",
    picUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
    videoCode = "101573",
)

val fakeAnnouncements = listOf(
    Announcement(
        title = "服务器维护通知",
        content = "将于明日凌晨2:00-4:00进行服务器维护，届时可能无法正常访问。",
        priority = 0,
        isActive = true,
    ),
    Announcement(
        title = "新功能上线：AI字幕生成",
        content = "现已支持AI自动生成中文字幕，请在播放器设置中开启体验。",
        priority = 1,
        isActive = true,
    ),
    Announcement(
        title = "社区规范更新",
        content = "为营造更好的社区氛围，我们更新了评论社区规范，请各位用户遵守。",
        priority = 2,
        isActive = true,
    ),
)

val fakeHomePageVideos = listOf(
    HanimeInfo(
        title = "小恶魔的补习计划",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
        videoCode = "101573",
        duration = "04:34",
        views = "44.9万次",
        reviews = "100%",
        currentArtist = "製作社A",
        uploadTime = "2010-12-10",
        itemType = HanimeInfo.NORMAL,
    ),
    HanimeInfo(
        title = "姐姐的秘密训练",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101574l.jpg",
        videoCode = "101574",
        duration = "23:15",
        views = "22.1万次",
        reviews = "95%",
        currentArtist = "製作社B",
        uploadTime = "2010-12-10",
        itemType = HanimeInfo.NORMAL,
    ),
    HanimeInfo(
        title = "放学后的约定",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101575l.jpg",
        videoCode = "101575",
        duration = "18:02",
        views = "58.3万次",
        reviews = "97%",
        currentArtist = "製作社C",
        uploadTime = "2010-12-10",
        itemType = HanimeInfo.NORMAL,
    ),
    HanimeInfo(
        title = "班长的福利日",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101576l.jpg",
        videoCode = "101576",
        duration = "12:47",
        views = "30.0万次",
        reviews = "92%",
        currentArtist = "製作社D",
        uploadTime = "2010-12-10",
        itemType = HanimeInfo.NORMAL,
    ),
    HanimeInfo(
        title = "图书馆的秘密角落",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101577l.jpg",
        videoCode = "101577",
        duration = "15:20",
        views = "61.7万次",
        reviews = "99%",
        currentArtist = "製作社E",
        uploadTime = "2010-12-10",
        itemType = HanimeInfo.NORMAL,
    ),
    HanimeInfo(
        title = "体育仓库的约定",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101588l.jpg",
        videoCode = "101588",
        duration = "22:10",
        views = "35.2万次",
        reviews = "94%",
        currentArtist = "製作社F",
        uploadTime = "2011-01-15",
        itemType = HanimeInfo.NORMAL,
    ),
)

val fakeCategories = listOf(
    HomeCategory(
        titleRes = R.string.latest_hanime,
        genre = "裏番",
        videos = fakeHomePageVideos,
    ),
    HomeCategory(
        titleRes = R.string.latest_release,
        sort = "最新上市",
        videos = fakeHomePageVideos.shuffled().take(4),
    ),
    HomeCategory(
        titleRes = R.string.they_watched,
        sort = "他們在看",
        videos = fakeHomePageVideos.shuffled().take(5),
    ),
)

val fakeTagList1 = listOf("新番", "预告", "校园", "妹妹", "姐系", "正太", "萝莉")
val fakeTagList2 = listOf(
    "新番",
    "预告",
    "校园",
    "妹妹",
    "姐系",
    "正太",
    "萝莉",
    "伪娘",
    "NTR",
    "SM",
    "暴力",
    "GURO",
    "血腥",
    "人妻"
)

val fakeNewHanimeInfo = listOf(
    HanimePreview.PreviewInfo(
        title = "日文标题",
        videoTitle = "中文标题 第1话",
        coverUrl = fakeHomePageVideos.first().coverUrl,
        introduction = "这是用于预览的简介内容，用来确认 Compose 版布局是否正常。",
        brand = "发行商 A",
        releaseDate = "2024-01-01",
        videoCode = fakeHomePageVideos.first().videoCode,
        tags = fakeTagList1,
        relatedPicsUrl = listOf(
            fakeHomePageVideos[1].coverUrl,
            fakeHomePageVideos[2].coverUrl
        ),
    ),
    HanimePreview.PreviewInfo(
        title = "日文标题2",
        videoTitle = "中文标题 第2话",
        coverUrl = fakeHomePageVideos.first().coverUrl,
        introduction = "这是用于预览的简介内容，用来确认 Compose 版布局是否正常。",
        brand = "发行商 B",
        releaseDate = "2022-05-02",
        videoCode = fakeHomePageVideos.first().videoCode,
        tags = fakeTagList2,
        relatedPicsUrl = listOf(
            fakeHomePageVideos[1].coverUrl,
            fakeHomePageVideos[2].coverUrl
        ),
    )
)

val fakeCommentList = listOf(
    VideoComments.VideoComment(
        avatar = "https://picsum.photos/64/64",
        username = "preview_child_1",
        date = "10分鐘前",
        content = "這是一條子評論預覽內容，用來確認 Compose 版底部評論頁的列表與對話框入口樣式。",
        thumbUp = 8,
        isChildComment = true,
        hasMoreReplies = false,
        replyCount = 0,
        id = "1",
        post = VideoComments.VideoComment.POST(
            foreignId = "1",
            likeCommentStatus = false,
            unlikeCommentStatus = false,
        ),
        reportableId = "1",
        reportableType = "comment",
    ),
    VideoComments.VideoComment(
        avatar = "https://picsum.photos/64/65",
        username = "preview_child_2",
        date = "3分鐘前",
        content = "第二條子評論，帶一點更短的文案和不同的點贊狀態。",
        thumbUp = 15,
        isChildComment = true,
        hasMoreReplies = false,
        replyCount = 0,
        id = "2",
        post = VideoComments.VideoComment.POST(
            foreignId = "2",
            likeCommentStatus = true,
            unlikeCommentStatus = false,
        ),
        reportableId = "2",
        reportableType = "comment",
    )
)
val fakeDownloadedVideos = fakeHomePageVideos.take(3).mapIndexed { index, item ->
    VideoWithCategories(
        video = HanimeDownloadEntity(
            groupId = 1,
            coverUrl = item.coverUrl,
            coverUri = null,
            title = item.title,
            addDate = System.currentTimeMillis(),
            videoCode = item.videoCode,
            videoUri = "test$index.mp4",
            quality = "720P",
            videoUrl = "https://example.com/test$index.mp4",
            length = 100L * 1024 * 1024,
            downloadedLength = 100L * 1024 * 1024,
            state = com.yenaly.han1meviewer.logic.state.DownloadState.Finished,
            id = index + 1,
        ),
        categories = emptyList(),
    )
}
val fakeDownloadedGroups = listOf(DownloadGroupEntity(name = "未分组", orderIndex = 0, id = 1))
val fakeDownloadedNodes = listOf(
    DownloadHeaderNode(
        groupKey = "未分组",
        originalVideos = fakeDownloadedVideos,
        isExpanded = true
    ),
    DownloadItemNode(fakeDownloadedVideos[0], "未分组"),
    DownloadItemNode(fakeDownloadedVideos[1], "未分组"),
    DownloadHeaderNode(
        groupKey = "分组1",
        originalVideos = fakeDownloadedVideos,
        isExpanded = true
    ),
    DownloadItemNode(fakeDownloadedVideos[0], "分组1"),
)

const val longText =
    "这是一段用于预览的简介文本，包含一个链接 https://hanime1.me/watch?v=101573 ，用于验证展开和收" +
            "起功能是否正常。为了触发折叠，这里再补充一些额外内容。超长文本超长文本超长文本超长文本超长文本超长文本超长文本" +
            "超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本"


val fakeVideoIntroduction = HanimeVideo(
    title = "Shishunki no Obenkyou 2",
    coverUrl = fakeHomePageVideos.first().coverUrl,
    chineseTitle = "思春期的性学习 第2话",
    introduction = "思春期的性学习 2。为了拓展自己的知识，女主开始在图书馆进行一些不太适合公开讨论的研究。\nhttps://hanime1.me/watch?v=101573",
    uploadTime = LocalDate(2024, 5, 10),
    views = "137.6万次",
    videoUrls = com.yenaly.han1meviewer.HanimeResolution().apply {
        parseResolution("720P", "https://example.com/video.mp4", "video/mp4")
    }.toResolutionLinkMap(),
    tags = fakeTagList2,
    playlist = HanimeVideo.Playlist(
        playlistName = "思春期系列",
        video = fakeHomePageVideos.take(5).mapIndexed { index, item ->
            item.copy(isPlaying = index == 1)
        },
    ),
    relatedHanimes = fakeHomePageVideos,
    artist = HanimeVideo.Artist(
        name = "製作社A",
        avatarUrl = fakeHomePageVideos.first().coverUrl,
        genre = "3D",
        post = HanimeVideo.Artist.POST(
            userId = "1001",
            artistId = "2002",
            isSubscribed = true,
        ),
    ),
    isFav = true,
    currentUserId = "10086",
    originalComic = "https://example.com/comic",
)