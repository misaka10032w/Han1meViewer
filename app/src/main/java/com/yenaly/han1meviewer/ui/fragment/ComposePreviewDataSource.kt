package com.yenaly.han1meviewer.ui.fragment

import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
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
    SubscriptionItem("阿库娅", "null")
)
val fakeVideos = listOf(
    SubscriptionVideosItem(
        title = "小恶魔的补习计划",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
        videoCode = "101573",
        duration = "04:34",
        views = "44.9万次",
        reviews = "100%",
        upLoadTime = "2010-12-10"
    ),
    SubscriptionVideosItem(
        title = "姐姐的秘密训练",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101574l.jpg",
        videoCode = "101574",
        duration = "23:15",
        views = "22.1万次",
        reviews = "95%",
        upLoadTime = "2010-12-10"
    ),
    SubscriptionVideosItem(
        title = "放学后的约定",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101575l.jpg",
        videoCode = "101575",
        duration = "18:02",
        views = "58.3万次",
        reviews = "97%",
        upLoadTime = "2010-12-10"
    ),
    SubscriptionVideosItem(
        title = "班长的福利日",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101576l.jpg",
        videoCode = "101576",
        duration = "12:47",
        views = "30.0万次",
        reviews = "92%",
        upLoadTime = "2010-12-10"
    ),
    SubscriptionVideosItem(
        title = "图书馆的秘密角落",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101577l.jpg",
        videoCode = "101577",
        duration = "15:20",
        views = "61.7万次",
        reviews = "99%",
        upLoadTime = "2010-12-10"
    )
)
val fakeVideosItem =
    SubscriptionVideosItem(
        title = "小恶魔的补习计划",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
        videoCode = "101573",
        duration = "04:34",
        views = "44.9万次",
        reviews = "100%",
        upLoadTime = "2020-12-12"
    )

fun generateFakeCheckInRecords(
    yearMonth: YearMonth = YearMonth.now(),
    maxState: Int = 2
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
        coverUrl = "https://picsum.photos/300/200?random=1"
    ),
    Playlists.Playlist(
        listCode = "code2",
        title = "动作大片必看榜单",
        total = 18,
        coverUrl = "https://picsum.photos/300/200?random=2"
    ),
    Playlists.Playlist(
        listCode = "code3",
        title = "温暖治愈的日常剧推荐",
        total = 32,
        coverUrl = "https://picsum.photos/300/200?random=3"
    ),
    Playlists.Playlist(
        listCode = "code4",
        title = "悬疑推理高分作品",
        total = 12,
        coverUrl = "https://picsum.photos/300/200?random=4"
    ),
    Playlists.Playlist(
        listCode = "code5",
        title = "经典动画短片集锦",
        total = 45,
        coverUrl = "https://picsum.photos/300/200?random=5"
    )
)