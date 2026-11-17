# Firebase 埋点清单

所有埋点统一经 `com.yenaly.han1meviewer.util.HAnalytics` 上报。
参数值自动截断到 Firebase Analytics 的 100 字符上限；用户关闭数据统计时 SDK 静默丢弃，无需判断。

错误类事件除 Analytics 事件外，若判定为「真 bug 信号」（`ParseException`、`SSLHandshakeException` 等非预期异常）会同时上报 Crashlytics non-fatal 并附带来源 breadcrumb；预期内站点状态（IP 封禁 / Cloudflare 质询 / 影片不存在 / 登录过期）只记事件。debug 构建不上报 Crashlytics。

## 一、Bug 排查

### network_error（网络层集中上报）

`NetworkRepo` 的 `websiteIOFlow / pageIOFlow / videoIOFlow` 在 catch 中统一上报，`source` 标识具体请求来源。

| 参数 | 说明 |
| --- | --- |
| source | home_page, search, video, preview, subscriptions, my_list, my_playlist, online_watch_history, account_page, uploaded_videos, uploading_videos, update_profile, update_password, update_avatar, delete_watch_history, delete_my_list, playlists, fav_video, rate_video, create_playlist, add_to_list, modify_playlist, comments, comment_reply, post_comment, post_comment_reply, like_comment, report_comment, subscribe_artist, version_check, login |
| exception_class | 异常类名 |
| exception_msg | 异常消息 |
| http_code | 从 message 还原的 HTTP 状态码（可解析时） |

### player_error（播放器错误）

| 参数 | 说明 |
| --- | --- |
| source | player |
| kernel | 内核类型（MPV / SYSTEM） |
| quality | 出错时清晰度 |

另带 exception_class / exception_msg；同时上报 Crashlytics non-fatal。

### download_result（下载任务结果）

| 参数 | 说明 |
| --- | --- |
| result | finished / failure / retry / paused / queued / deleted |
| reason | 异常类名或 http_状态码（如 SocketTimeoutException、http_416） |
| video_code / quality | 影片代码与清晰度 |

### cloudflare_challenge / cloudflare_blocked

CF 质询（拦截器触发，param: source=被质询域名）与首页内容被屏蔽。

### Crashlytics 自定义键（还原崩溃环境）

原有：login_state、app_language、version_source、running_download_work_count
新增：current_site、use_custom_mirror、proxy_type(direct/system/http/socks)、doh_enabled、doh_preset、use_builtin_hosts、use_backup_cdn
启动时与网络设置变化后（`HanimeNetwork.rebuildNetwork`）自动同步。

## 二、用户行为

### 1. 页面浏览 screen_view

- 主导航各页（MainNavHost）：screen_name = MainDestinationSpec 名（Home / Video / Search / Download 等）
- 设置页（SettingsNavHost，沿用原格式 MainActivity-XXX）
- Login / CloudflareVerification / ManualInputCookies 独立 Activity

### 2. 搜索（GA4 标准事件）

- `search`：search_term、search_type(simple/advanced)、page、broad、genre、sort、duration、tag_count、brand_count
- `view_search_results`：search_term、page、result_count、total_pages

### 3. 播放

- `view_item`（GA4 标准）：item_id=videoCode、from_download、is_local
- `playback_start`：quality、kernel、method(start/retry)
- `video_quality_switch`：from_quality、quality、kernel
- `video_pip`：video_code

### 4. 互动

- `add_to_wishlist` / `remove_from_wishlist`（GA4 标准）：item_id=videoCode、result、reason
- `rate_video`：video_code、method(like/dislike)、result
- `subscribe_artist`：method=artistId、is_cancel、result
- `comment_post`：method(评论类型/reply)、result、reason
- `daily_checkin`：method=打卡类型（不含自由文本备注）

### 5. 下载

- `download_start`：video_code、quality、redownload

### 6. 账号与系统

- `login`（GA4 标准）：method(password / webview_cookie / qrcode_cookie)、result、reason
- `logout`
- `site_switch`：from_url、to_url
- `clipboard_link_open`：video_code
- `update_confirm`：method=目标版本号
