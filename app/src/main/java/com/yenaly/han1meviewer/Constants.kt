package com.yenaly.han1meviewer

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char

/**
 * 我觉得空字符串写出来太逆天了，所以搞了个常量
 */
const val EMPTY_STRING = ""

const val APP_NAME = "Han1meViewer"

// 标准时间格式

/* yyyy-MM-dd */
@JvmField
val LOCAL_DATE_FORMAT = LocalDate.Formats.ISO

/* yyyy-MM-dd HH:mm */
@JvmField
val LOCAL_DATE_TIME_FORMAT = LocalDateTime.Format {
    date(LocalDate.Formats.ISO); char(' ')
    hour(); char(':'); minute()
}

// 网络基本设置

const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"

// 設置發佈日期年份，在搜索的tag裏

/**
 * 發佈日期年份開始於
 */
const val SEARCH_YEAR_RANGE_START = 1990

/**
 * 發佈日期年份結束於
 */
const val SEARCH_YEAR_RANGE_END = BuildConfig.SEARCH_YEAR_RANGE_END

// intent傳值用名稱

const val FROM_DOWNLOAD = "FROM_DOWNLOAD"

const val VIDEO_CODE = "VIDEO_CODE"

//@Deprecated("Use [ADVANCED_SEARCH_MAP] instead")
//const val FROM_VIDEO_TAG = "FROM_VIDEO_TAG"

/**
 * 接受类型 [AdvancedSearchMap] 或者 [String]
 */
const val ADVANCED_SEARCH_MAP = "ADVANCED_SEARCH_MAP"

const val COMMENT_ID = "COMMENT_ID"

const val COMMENT_TYPE = "COMMENT_TYPE"

const val DATE_CODE = "DATE_CODE"

//const val CSRF_TOKEN = "CSRF_TOKEN"

// Result Code

//const val LOGIN_TO_MAIN_ACTIVITY = 0

// 给rv传值，判断布局需要wrap_content还是match_parent，不填则为默认
// 设置布局为MATCH_PARENT可以使rv在GridLayoutManager下能居中，反之不能

const val VIDEO_LAYOUT_WRAP_CONTENT = 1

const val VIDEO_LAYOUT_MATCH_PARENT = 2

// 給CommentFragment傳值，判斷是影片評論區還是預覽評論區 [COMMENT_TYPE]

const val VIDEO_COMMENT_PREFIX = "video"

const val PREVIEW_COMMENT_PREFIX = "preview"

// base url

@JvmField
val HANIME_BASE_URL = Preferences.baseUrl

/**
 * 如果添加备选网址别忘了改[String.toVideoCode]的videoUrlRegex
 * 没想弄动态匹配，觉得古法修改挺好的。
 */
object HanimeConstants {
    val HANIME_HOSTNAME = arrayOf("hanime1.me","hanime1.com","hanimeone.me","javchu.com")
    val HANIME_URL = arrayOf("https://hanime1.me/","https://hanime1.com/","https://hanimeone.me/","https://javchu.com/")
    val ANIME_URL = arrayOf("https://hanime1.me/","https://hanime1.com/","https://hanimeone.me/")
}

@JvmField
val HANIME_LOGIN_URL = HANIME_BASE_URL + "login"

// github url

const val HA1_GITHUB_URL = "https://github.com/misaka10032w/Han1meViewer"

const val HA1_GITHUB_ISSUE_URL = "$HA1_GITHUB_URL/issues"

const val HA1_GITHUB_FORUM_URL = "$HA1_GITHUB_URL/discussions"

const val HA1_GITHUB_RELEASES_URL = "$HA1_GITHUB_URL/releases"

const val HA1_GITHUB_API_URL = "https://api.github.com/repos/misaka10032w/Han1meViewer/"
const val FIREBASE_REALTIME_DATABASE = "https://han1meviewer-86e5f-default-rtdb.asia-southeast1.firebasedatabase.app/"
// for Shared Preference

const val LOGIN_COOKIE = "cookie"
const val SAVED_USER_ID = "saved_user_id"

const val CLOUDFLARE_COOKIE = "cf_cookie"

const val ALREADY_LOGIN = "already_login"

// Notification

const val DOWNLOAD_NOTIFICATION_CHANNEL = "download_channel"

const val UPDATE_NOTIFICATION_CHANNEL = "update_channel"

// File

const val FILE_PROVIDER_AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileProvider"