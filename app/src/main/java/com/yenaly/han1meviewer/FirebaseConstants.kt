package com.yenaly.han1meviewer

object FirebaseConstants {
    // <editor-fold desc="Analytics 事件名">

    /**
     * 高级搜索使用统计
     */
    const val ADV_SEARCH_OPT = "advanced_search_options"

    /**
     * 关键H帧使用统计
     */
    const val H_KEYFRAMES = "h_keyframes"

    /**
     * 网络请求失败（网络层集中上报）
     */
    const val EVENT_NETWORK_ERROR = "network_error"

    /**
     * 播放器内核错误
     */
    const val EVENT_PLAYER_ERROR = "player_error"

    /**
     * 开始播放（含清晰度与内核类型）
     */
    const val EVENT_PLAYBACK_START = "playback_start"

    /**
     * 切换清晰度
     */
    const val EVENT_VIDEO_QUALITY_SWITCH = "video_quality_switch"

    /**
     * 进入/退出画中画
     */
    const val EVENT_VIDEO_PIP = "video_pip"

    /**
     * 发表评论（含回复）
     */
    const val EVENT_COMMENT_POST = "comment_post"

    /**
     * 每日签到打卡
     */
    const val EVENT_DAILY_CHECKIN = "daily_checkin"

    /**
     * 登出
     */
    const val EVENT_LOGOUT = "logout"

    /**
     * 切换站点（镜像）
     */
    const val EVENT_SITE_SWITCH = "site_switch"

    /**
     * 开始下载影片
     */
    const val EVENT_DOWNLOAD_START = "download_start"

    /**
     * 下载任务结束（成功/失败/重试/暂停）
     */
    const val EVENT_DOWNLOAD_RESULT = "download_result"

    /**
     * 遇到 Cloudflare 质询（拦截器触发）
     */
    const val EVENT_CLOUDFLARE_CHALLENGE = "cloudflare_challenge"

    /**
     * 页面内容被 Cloudflare/IP 屏蔽（返回错误页）
     */
    const val EVENT_CLOUDFLARE_BLOCKED = "cloudflare_blocked"

    /**
     * 通过剪贴板链接跳转影片
     */
    const val EVENT_CLIPBOARD_LINK_OPEN = "clipboard_link_open"

    /**
     * 用户确认下载应用更新
     */
    const val EVENT_UPDATE_CONFIRM = "update_confirm"

    /**
     * 关注/取关创作者
     */
    const val EVENT_SUBSCRIBE_ARTIST = "subscribe_artist"

    // </editor-fold>

    // <editor-fold desc="Analytics 参数名">

    /**
     * 请求来源/错误来源，如 home_page、search、video
     */
    const val PARAM_SOURCE = "source"

    /**
     * 异常类名
     */
    const val PARAM_EXCEPTION_CLASS = "exception_class"

    /**
     * 异常消息（已截断脱敏）
     */
    const val PARAM_EXCEPTION_MSG = "exception_msg"

    /**
     * HTTP 状态码（可解析时）
     */
    const val PARAM_HTTP_CODE = "http_code"

    /**
     * 影片代码
     */
    const val PARAM_VIDEO_CODE = "video_code"

    /**
     * 清晰度，如 1080p
     */
    const val PARAM_QUALITY = "quality"

    /**
     * 切换前清晰度（video_quality_switch 事件）
     */
    const val PARAM_FROM_QUALITY = "from_quality"

    /**
     * 方式/类型，如登录方式 password、评论类型 video
     */
    const val PARAM_METHOD = "method"

    /**
     * 行为结果：success / failure
     */
    const val PARAM_RESULT = "result"

    /**
     * 失败原因摘要
     */
    const val PARAM_REASON = "reason"

    /**
     * 搜索/分页页码
     */
    const val PARAM_PAGE = "page"

    /**
     * 搜索结果总页数
     */
    const val PARAM_TOTAL_PAGES = "total_pages"

    /**
     * 本页结果数量
     */
    const val PARAM_RESULT_COUNT = "result_count"

    /**
     * 标签筛选数量
     */
    const val PARAM_TAG_COUNT = "tag_count"

    /**
     * 社团/品牌筛选数量
     */
    const val PARAM_BRAND_COUNT = "brand_count"

    /**
     * 搜索类型：simple / advanced
     */
    const val PARAM_SEARCH_TYPE = "search_type"

    /**
     * 是否模糊匹配
     */
    const val PARAM_BROAD = "broad"

    /**
     * 排序方式
     */
    const val PARAM_SORT = "sort"

    /**
     * 类型（genre）筛选
     */
    const val PARAM_GENRE = "genre"

    /**
     * 时长筛选
     */
    const val PARAM_DURATION = "duration"

    /**
     * 是否从下载库播放
     */
    const val PARAM_FROM_DOWNLOAD = "from_download"

    /**
     * 是否本地文件播放
     */
    const val PARAM_IS_LOCAL = "is_local"

    /**
     * 播放器内核类型
     */
    const val PARAM_KERNEL = "kernel"

    /**
     * 是否为重下载
     */
    const val PARAM_REDOWNLOAD = "redownload"

    /**
     * 是否取消关注
     */
    const val PARAM_IS_CANCEL = "is_cancel"

    /**
     * 切换前 URL（site_switch 事件）
     */
    const val PARAM_FROM_URL = "from_url"

    /**
     * 切换后 URL（site_switch 事件）
     */
    const val PARAM_TO_URL = "to_url"

    // </editor-fold>

    // <editor-fold desc="Crashlytics">

    /**
     * 当前是否为登录状态
     */
    const val LOGIN_STATE = "login_state"

    /**
     * 当前APP内使用语言
     */
    const val APP_LANGUAGE = "app_language"

    /**
     * 当前APP来源，debug、release或ci
     */
    const val VERSION_SOURCE = "version_source"

    /**
     * 当前正在下载的任务数量
     */
    const val RUNNING_DOWNLOAD_WORK_COUNT = "running_download_work_count"

    /**
     * 当前使用的站点（镜像）URL
     */
    const val CURRENT_SITE = "current_site"

    /**
     * 是否启用自定义镜像站
     */
    const val USE_CUSTOM_MIRROR = "use_custom_mirror"

    /**
     * 代理类型：direct / system / http / socks
     */
    const val PROXY_TYPE = "proxy_type"

    /**
     * 是否启用 DoH
     */
    const val DOH_ENABLED = "doh_enabled"

    /**
     * DoH 预设名
     */
    const val DOH_PRESET = "doh_preset"

    /**
     * 是否启用内置 Hosts
     */
    const val USE_BUILTIN_HOSTS = "use_builtin_hosts"

    /**
     * 是否启用备用媒体 CDN
     */
    const val USE_BACKUP_CDN = "use_backup_cdn"

    // </editor-fold>

    // <editor-fold desc="Remote Config">

    /**
     * 是否启用 CI 更新。
     *
     * 由于当前使用了 Firebase，所以希望正式版更新时，减少 CI 更新的概率。
     * 方便更好地控制更新以及崩溃数据统计。
     */
    const val ENABLE_CI_UPDATE = "enable_ci_update"

    val remoteConfigDefaults: Map<String, Any> = mapOf(
        ENABLE_CI_UPDATE to true
    )

    // </editor-fold>
}
