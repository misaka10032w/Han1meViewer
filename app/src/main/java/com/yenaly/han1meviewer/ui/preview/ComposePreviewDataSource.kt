package com.yenaly.han1meviewer.ui.preview

import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HomePage
import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
import com.yenaly.han1meviewer.ui.fragment.home.HomeCategory
import java.time.LocalDate
import java.time.YearMonth

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

fun generateFakeCheckInRecords(
    yearMonth: YearMonth = YearMonth.now(),
    maxState: Int = 2,
): Map<LocalDate, Int> {
    val daysInMonth = yearMonth.lengthOfMonth()
    return (1..daysInMonth).associate { day ->
        val date = LocalDate.of(yearMonth.year, yearMonth.month, day)
        date to (0..maxState).random()
    }
}

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
        title = "最新裏番",
        genre = "裏番",
        videos = fakeHomePageVideos,
    ),
    HomeCategory(
        title = "最新上市",
        sort = "最新上市",
        videos = fakeHomePageVideos.shuffled().take(4),
    ),
    HomeCategory(
        title = "他們在看",
        sort = "他們在看",
        videos = fakeHomePageVideos.shuffled().take(5),
    ),
)
