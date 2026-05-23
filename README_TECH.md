# Han1meViewer 技术文档

本文档面向维护者和贡献者，描述当前项目的技术架构、关键模块、数据流和开发约定。用户侧介绍请看 [README.md](README.md)。

## 1. 技术概览

Han1meViewer 当前是以 Kotlin + Jetpack Compose 为主的 Android 应用，整体采用 MVVM 风格组织。

核心技术：

- Kotlin 2.3.x，Java 21。
- Android Gradle Plugin 9.2.x。
- Jetpack Compose + Material 3。
- Navigation Compose typed routes。
- ViewModel + StateFlow + SharedFlow。
- Retrofit 3 + OkHttp + kotlinx.serialization converter。
- Jsoup 解析 HTML DOM。
- Room + KSP。
- WorkManager 后台下载和更新任务。
- Coil 3 Compose 图片加载。
- Media3 ExoPlayer、JZVD、MPV Android 播放链路。
- Firebase Analytics、Crashlytics、Performance、Remote Config、Realtime Database。

当前代码仍有少量历史 View/XML 组件，例如 JZVD 自定义 View、部分设置/播放器相关布局和 `yenaly_libs` 基础库组件，但主业务页面已经以 Compose 为主。

## 2. 模块结构

Gradle 模块：

- `:app`：主应用模块，包含 UI、业务状态、网络解析、本地数据库、播放器、下载、更新和资源。
- `:yenaly_libs`：项目内公共库，提供基础 Activity、Fragment、ViewModel、Preference、工具类和部分 View 封装。
- `buildSrc`：构建辅助逻辑，维护版本号、构建来源、提交 SHA 等 Gradle 侧配置。

主应用关键目录：

```text
app/src/main/java/com/yenaly/han1meviewer/
├── logic/                 数据、网络、解析、数据库、状态
│   ├── network/           Retrofit Service、OkHttp、DNS、Cookie、更新服务
│   ├── model/             业务模型和解析后的页面模型
│   ├── dao/               Room DAO 和 Database
│   ├── entity/            Room 实体和本地实体
│   ├── state/             WebsiteState、PageLoadingState、VideoLoadingState
│   ├── NetworkRepo.kt     网络仓库入口
│   ├── DatabaseRepo.kt    本地仓库入口
│   └── Parser.kt          HTML/JSON 解析入口
├── ui/
│   ├── activity/          MainActivity、登录、Cloudflare、手动 Cookie 页面
│   ├── navigation/        主导航、设置导航、路由和安全跳转
│   ├── screen/            Compose 页面
│   ├── component/         Compose 组件
│   ├── viewmodel/         页面 ViewModel
│   ├── view/              自定义 View 和播放器 View
│   └── theme/             Compose 主题、颜色、尺寸
├── util/                  文件、网络、权限、主题、Cookie、Toast 等工具
├── worker/                下载 Worker、更新 Worker、下载管理器
└── HanimeApplication.kt   应用入口
```

## 3. 分层职责

### UI 层

UI 层主要由 Compose 页面和组件组成。

关键职责：

- 只消费 ViewModel 暴露的状态。
- 通过回调触发导航和业务动作。
- 不直接访问 Retrofit Service 或 Room DAO。
- `LazyColumn`、`LazyRow`、`LazyVerticalGrid` 使用稳定 key 时，数据源必须提前保证唯一。

典型文件：

- `ui/screen/home/HomePageScreen.kt`
- `ui/screen/search/SearchScreen.kt`
- `ui/screen/video/VideoIntroductionScreen.kt`
- `ui/screen/video/VideoRouteHostScreen.kt`
- `ui/screen/home/download/DownloadScreen.kt`
- `ui/screen/home/myplaylist/MyPlayListScreen.kt`
- `ui/component/VideoCardItem.kt`
- `ui/component/lazy/AnimatedLazy.kt`

### ViewModel 层

ViewModel 是页面状态和业务动作的入口。

约定：

- 持久页面状态使用 `MutableStateFlow`。
- 一次性事件使用 `MutableSharedFlow` 或 UI 回调。
- 不让 Compose 页面直接处理分页合并、去重、登录态判断等业务细节。
- 网络列表分页追加时应按稳定业务键去重，例如 `videoCode`、`listCode`、`id`。
- 登录用户相关页面要先检查 `Preferences.isAlreadyLogin` 和 `Preferences.savedUserId`。

典型文件：

- `ui/viewmodel/MainViewModel.kt`
- `ui/viewmodel/SearchViewModel.kt`
- `ui/viewmodel/VideoViewModel.kt`
- `ui/viewmodel/CommentViewModel.kt`
- `ui/viewmodel/DownloadViewModel.kt`
- `ui/viewmodel/MyPlayListViewModelV2.kt`
- `ui/viewmodel/OnlineWatchHistoryViewModel.kt`
- `ui/viewmodel/CreatorCenterViewModel.kt`
- `ui/viewmodel/UserAccountViewModel.kt`

### 数据层

数据层由 `NetworkRepo`、`DatabaseRepo`、`Parser`、Retrofit Service 和 Room DAO 组成。

网络数据流：

```text
ViewModel -> NetworkRepo -> HanimeNetwork Service -> Parser -> WebsiteState/PageLoadingState/VideoLoadingState -> ViewModel
```

本地数据流：

```text
ViewModel/Worker -> DatabaseRepo -> Room DAO -> Flow / suspend result -> ViewModel/UI
```

关键文件：

- `logic/NetworkRepo.kt`
- `logic/DatabaseRepo.kt`
- `logic/Parser.kt`
- `logic/network/HanimeNetwork.kt`
- `logic/network/ServiceCreator.kt`
- `logic/network/service/HanimeBaseService.kt`
- `logic/network/service/HanimeMyListService.kt`
- `logic/network/service/HanimeCommentService.kt`
- `logic/dao/HistoryDatabase.kt`
- `logic/dao/DownloadDatabase.kt`
- `logic/dao/MiscellanyDatabase.kt`

## 4. 状态模型

项目使用三个主要状态包装类型：

- `WebsiteState<T>`：普通页面或操作状态，包含 Loading、Success、Error。
- `PageLoadingState<T>`：分页列表状态，包含 Loading、Success、NoMoreData、Error。
- `VideoLoadingState<T>`：视频详情状态，处理视频不存在、解析失败、加载中和成功状态。

使用建议：

- 首页、订阅、账号、修改操作等非分页数据使用 `WebsiteState`。
- 搜索、收藏、稍后观看、播放列表、在线历史、创作中心列表使用 `PageLoadingState`。
- 视频详情使用 `VideoLoadingState`。
- UI 层不要根据异常类型硬编码网络逻辑，异常映射应尽量在 `NetworkRepo` 或 Parser 侧完成。

## 5. 导航架构

主导航基于 Navigation Compose typed routes。

关键文件：

- `ui/navigation/main/MainRoutes.kt`
- `ui/navigation/main/MainNavHost.kt`
- `ui/navigation/main/MainNavigationActions.kt`
- `ui/navigation/NavControllerExt.kt`
- `ui/navigation/settings/SettingsRoutes.kt`
- `ui/navigation/settings/SettingsNavHost.kt`

路由示例：

```kotlin
@Serializable
data class SearchRoute(
    val query: String? = null,
    val advancedSearchJson: String? = null,
)

@Serializable
data class VideoRoute(
    val videoCode: String,
    val localUri: String? = null,
)
```

导航约定：

- 普通跳转使用 `navController.navigateSafely(...)`，避免快速点击重复入栈。
- 页面间只传递路由必要参数，不传大对象。
- 复杂搜索参数通过 JSON 字符串承载，进入 `SearchRouteScreen` 后再灌入 `SearchViewModel`。
- 视频本地文件播放使用 `VideoRoute(videoCode = "-1", localUri = uri)` 这一路径。

## 6. 搜索和分页列表

搜索入口：

- 首页搜索框进入 `SearchRoute(query = keyword)`。
- 标签点击进入 `SearchRoute(query = tag)`。
- 高级搜索进入 `SearchRoute(advancedSearchJson = json)`。
- 详情页作者区域会同时传入 `query = artist.name` 和高级搜索 JSON，以保证搜索框填充并自动搜索。

`SearchScreen` 支持：

- `initialQuery` 自动填充。
- 初始关键词自动搜索。
- 高级搜索条件自动搜索。
- 搜索历史建议。
- 下拉刷新和加载更多。

分页列表去重规则：

- 视频列表按 `HanimeInfo.videoCode` 去重。
- 播放列表按 `Playlists.Playlist.listCode` 去重。
- 上传中/已上传列表按各自 `videoCode` 去重。
- 评论列表使用评论模型提供的 `stableKey`。

原因：Compose `Lazy*` 使用 `key = { ... }` 时，数据源中出现重复 key 会直接抛出：

```text
java.lang.IllegalArgumentException: Key "xxxx" was already used.
```

因此分页合并应优先在 ViewModel 层处理：

```kotlin
(previous + incoming).distinctBy(HanimeInfo::videoCode)
```

## 7. 视频页架构

视频页由路由、Host、播放器、Tab 内容和详情内容协作完成。

关键文件：

- `ui/navigation/main/VideoRoute.kt`
- `ui/screen/video/VideoRouteHostScreen.kt`
- `ui/screen/video/VideoRouteContent.kt`
- `ui/screen/video/VideoShellContent.kt`
- `ui/screen/video/VideoTabsContent.kt`
- `ui/screen/video/VideoIntroductionScreen.kt`
- `ui/screen/video/CommentScreen.kt`
- `ui/screen/video/ChildCommentScreen.kt`
- `ui/screen/video/VideoRouteActions.kt`
- `ui/viewmodel/VideoViewModel.kt`

主要状态：

- `hanimeVideoStateFlow`：视频加载状态。
- `hanimeVideoFlow`：当前视频数据。
- `videoHostUiStateFlow`：Tab、AppBar、PIP、播放器高度、评论角标等 UI 状态。
- `videoIntroUiStateMap`：按 `videoCode` 缓存详情页滚动位置、系列横向列表位置和已恢复数据。

视频详情加载：

```text
VideoRoute -> VideoViewModel.getHanimeVideo -> NetworkRepo.getHanimeVideo -> Parser.hanimeVideoVer2 -> HanimeVideo
```

本地播放：

```text
VideoRoute(videoCode = "-1", localUri) -> VideoViewModel.buildLocalPlayInfo -> HanimeVideo(videoUrls = localUri)
```

作者点击搜索：

- `VideoIntroductionScreen` 的 `ArtistSection` 触发 `onOpenArtist`。
- `VideoRouteActions.openArtistSearch` 构造 `SearchRoute`。
- 搜索页通过 `route.query` 自动填充作者名并发起搜索。

## 8. 播放器链路

项目保留了历史 JZVD 播放器封装，同时引入 Media3 和 MPV 能力。

关键文件：

- `ui/view/video/HJzvdStd.kt`
- `ui/view/video/HMediaKernel.kt`
- `ui/view/video/HanimeDataSource.kt`
- `ui/screen/video/VideoPlayerUi.kt`
- `util/Videos.kt`
- `HanimeResolution.kt`

播放相关约定：

- 画质信息由 `HanimeResolution` 解析后提供给播放器。
- 播放器 UI 状态不要直接写入网络模型，应通过 ViewModel 或播放器状态对象同步。
- PIP、屏幕方向、播放器高度、滚动禁用等与页面布局相关的状态归 `VideoViewModel.VideoHostUiState` 管理。
- MPV 相关设置归 `MpvPlayerSettingsRouteScreen` 和偏好设置管理。

## 9. 下载架构

下载由 UI、ViewModel、下载管理器、Worker 和 Room 共同完成。

关键文件：

- `ui/screen/home/download/DownloadScreen.kt`
- `ui/screen/home/download/DownloadingScreen.kt`
- `ui/screen/home/download/DownloadedScreen.kt`
- `ui/viewmodel/DownloadViewModel.kt`
- `worker/HanimeDownloadManagerV2.kt`
- `worker/HanimeDownloadWorker.kt`
- `logic/entity/download/HanimeDownloadEntity.kt`
- `logic/entity/download/DownloadGroupEntity.kt`
- `logic/dao/download/HanimeDownloadDao.kt`
- `logic/dao/DownloadDatabase.kt`

下载数据流：

```text
DownloadScreen -> DownloadViewModel -> HanimeDownloadManagerV2 -> WorkManager -> HanimeDownloadWorker -> DownloadDatabase -> Flow -> UI
```

设计要点：

- 长任务交给 WorkManager，避免 Activity 生命周期影响下载。
- 下载进度落库，UI 通过数据库 Flow 获得刷新。
- 前台服务通知由 Worker 维护。
- 下载目录通过 SAF 相关工具处理，避免直接依赖旧外部存储权限。
- 下载完成后可走本地播放路径进入视频页。

性能注意：

- 下载进度不应过于频繁落库，否则多个任务同时下载时会造成数据库和 UI 刷新压力。
- 更新间隔应在“进度实时性”和“数据库写入频率”之间折中。

## 10. 本地数据库

Room 主要用于：

- 播放历史。
- 搜索历史。
- 高级搜索历史。
- 下载任务和下载分组。
- 共享/自定义关键 H 帧。
- 健康打卡记录。

关键入口：

- `logic/DatabaseRepo.kt`
- `logic/dao/HistoryDatabase.kt`
- `logic/dao/DownloadDatabase.kt`
- `logic/dao/MiscellanyDatabase.kt`
- `logic/dao/CheckInRecordDatabase.kt`

约定：

- UI 不直接访问 DAO。
- 跨页面复用的数据库操作集中到 `DatabaseRepo`。
- 高频写入要评估 Flow 回调频率和 UI 刷新成本。

## 11. 网络和解析

网络层入口：

- `NetworkRepo`：业务方法入口，统一包装状态。
- `HanimeNetwork`：Retrofit Service 聚合。
- `ServiceCreator`：OkHttp/Retrofit 创建。
- `Parser`：HTML/JSON 转领域模型。

拦截器和辅助：

- `CloudflareInterceptor`
- `UserAgentInterceptor`
- `SpeedLimitInterceptor`
- `UrlLoggingInterceptor`
- `HCookieJar`
- `HDns`
- `GitHubDns`
- `HProxySelector`

异常类型：

- `CloudFlareBlockedException`
- `IPBlockedException`
- `HanimeNotFoundException`
- `LoginStateExpiredException`
- `ParseException`

开发约定：

- Parser 应尽量容错，目标网站 DOM 改动时返回明确错误。
- `NetworkRepo` 应保留 `CancellationException` 语义，不吞掉协程取消。
- 登录态过期、Cloudflare、IP blocked 等应映射成可被 UI 理解的异常或状态。
- 解析列表时优先保证 `videoCode`、`listCode` 等业务键非空和稳定。

## 12. 账号、Cookie 和 Cloudflare

相关页面：

- `ui/activity/LoginActivity.kt`
- `ui/activity/ManualInputCookiesActivity.kt`
- `ui/activity/CloudflareActivity.kt`
- `ui/screen/account/AccountScreen.kt`
- `ui/screen/account/AvatarCropScreen.kt`
- `ui/viewmodel/UserAccountViewModel.kt`

关键点：

- 登录态和 Cookie 由 `Preferences`、`HCookieJar` 和相关工具共同维护。
- 手动 Cookie 输入用于绕过无法自动登录或 WebView 登录失败的情况。
- Cloudflare 页面和拦截器用于处理访问保护导致的请求失败。
- 账号信息修改、密码修改、头像上传都通过 `HanimeMyListService` 和 `NetworkRepo` 包装。

## 13. 公告系统与管理端

公告系统分为应用侧展示和管理端写入两部分。

关键文件：

- `ui/viewmodel/MainViewModel.kt`
- `ui/component/AnnouncementDialog.kt`
- `ui/screen/home/HomePageScreen.kt`
- `logic/model/Announcement.kt`
- `HanimeAnnouncementManagerWebUI/HanimeAnnouncementManager.html`
- `HanimeAnnouncementManagerWebUI/PermitAdmin.py`

应用侧链路：

- `MainViewModel.loadAnnouncements()` 从 Firebase Realtime Database 的 `announcements` 节点读取公告。
- 只有 `isActive = true` 的公告会参与展示。
- 首页通过公告卡片展示公告列表。
- 点击公告后进入 `AnnouncementDialog`，可查看标题、正文、时间、图片和按钮文案。
- 用户关闭公告后，会记录 `last_dismiss_time`，24 小时内不会再次自动弹出。

管理端链路：

- `HanimeAnnouncementManager.html` 是基于 Firebase Web SDK 的管理页面。
- 页面通过 `firebaseConfig` 连接到项目对应的 Realtime Database。
- 页面使用 `ADMIN_EMAIL` 和 `ADMIN_PASSWORD` 登录 Firebase Auth。
- 登录成功后读取 `announcements` 节点并渲染为表格。
- 页面支持新增、编辑、启用、停用和删除公告。

管理员权限：

- `PermitAdmin.py` 使用 Firebase Admin SDK 给指定 UID 写入 `isAdmin: true` 自定义声明。
- 该脚本依赖本地 `serviceAccountKey.json`。
- 运行前需要安装 `firebase-admin`，并填写目标用户 UID。
- 该自定义声明用于管理端权限校验。

公告字段约定：

- `title`：公告标题。
- `content`：公告正文。
- `imageUrl`：公告图片地址，可选。
- `positiveText`：确认按钮文本，可选。
- `negativeText`：取消按钮文本，可选。
- `priority`：排序优先级。
- `timestamp`：更新时间戳，单位为秒。
- `isActive`：是否启用。

实现约定：

- 公告数据应兼容部分字段缺失的情况。
- 管理端更新公告时会刷新 `timestamp`，便于前端显示最新时间。
- 停用公告时只更新 `isActive`，删除则直接移除节点。
- 后续如果新增公告字段，需要同步更新 `Announcement` 模型、管理端表单和展示弹窗。

## 14. 设置体系

设置页采用 Compose + typed routes。

关键文件：

- `ui/navigation/settings/SettingsRoutes.kt`
- `ui/navigation/settings/SettingsNavHost.kt`
- `ui/navigation/settings/HomeSettingsRoute.kt`
- `ui/screen/settings/HomeSettingsScreen.kt`
- `ui/screen/settings/PlayerSettingsScreen.kt`
- `ui/screen/settings/MpvPlayerSettingsScreen.kt`
- `ui/screen/settings/DownloadSettingsScreen.kt`
- `ui/screen/settings/NetworkSettingsScreen.kt`
- `ui/screen/settings/HKeyframeSettingsScreen.kt`

偏好配置入口：

- `Preferences.kt`
- `SearchGridColumnsConfig.kt`
- `HorizontalCardCountConfig.kt`
- `VideoCoverSize.kt`

约定：

- 新设置项应集中放在 `Preferences` 或明确的配置类里。
- UI 设置页只负责展示和修改偏好，不直接散落业务逻辑。
- 与播放器、网络、下载有关的设置要同步检查对应模块读取点。

## 15. 共享关键 H 帧

共享关键 H 帧仍使用 assets 中的 JSON 文件作为输入。

关键文件：

- `app/src/main/assets/h_keyframes/`
- `app/src/main/assets/h_keyframes/README.md`
- `logic/entity/HKeyframeEntity.kt`
- `logic/dao/HKeyframeDao.kt`
- `logic/DatabaseRepo.kt`
- `ui/screen/settings/HKeyframesScreen.kt`
- `ui/screen/settings/SharedHKeyframesScreen.kt`

读取思路：

```text
assets/h_keyframes/*.json -> Json.decodeFromStream<HKeyframeEntity> -> sort/group/flatten -> UI
```

分组扁平化示意：

```kotlin
entities
    .sortedWith(compareBy<HKeyframeEntity> { it.group }.thenBy { it.episode })
    .groupBy { it.group ?: "???" }
    .flatMap { (group, entities) ->
        listOf(HKeyframeHeader(title = group, attached = entities)) + entities
    }
```

这种结构避免了在 assets 里维护复杂目录索引，也便于贡献者直接提交单个 JSON 文件。

## 16. Compose 列表封装

项目提供了 `AnimatedLazy.kt` 封装：

- `LazyColumn`
- `LazyRow`
- `LazyVerticalGrid`
- `AnimatedLazyListScope`
- `AnimatedLazyGridScope`

目的：

- 尽量兼容原生 Lazy API 的常用调用方式。
- 为列表 item 提供轻量入场动画。
- 在全项目保持较一致的列表观感。

注意：

- 它不会替代 Compose 对 key 唯一性的要求。
- 使用 `items(list, key = { ... })` 时，调用方仍必须保证 key 唯一。
- 网络分页列表优先在 ViewModel 层去重，不建议在 UI 层临时过滤后再展示，除非数据源来自单次接口且不参与后续业务逻辑。

## 17. 构建和版本

关键文件：

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `buildSrc/src/main/java/Config.kt`

当前构建特征：

- `compileSdk=37`
- `minSdk=27`
- `targetSdk=37`
- Java 21
- Compose Compiler 插件随 Kotlin 版本
- Release 默认启用混淆和资源压缩
- Release 只打 `arm64-v8a` ABI split
- APK 文件名格式：`Han1meViewer-v{versionName}.apk`

本地常用验证：

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
```

提交前建议至少运行：

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

## 18. 开发约定

代码约定：

- 优先做最小正确修改。
- 新 UI 优先使用 Compose。
- 页面状态优先使用 `StateFlow`，一次性事件用 `SharedFlow` 或回调。
- 网络和数据库访问不要写进 Composable。
- 列表分页合并要去重。
- 文本文件工作区行尾使用 CRLF，文件末尾保留一个换行符。

排查问题时优先看：

- 页面是否重复触发网络请求。
- ViewModel 是否在 Loading 状态下清空了不该清空的列表。
- `Lazy*` key 是否唯一。
- Parser 是否因 DOM 改动解析为空。
- 登录态或 Cookie 是否过期。
- Cloudflare/IP blocked 是否被正确映射。

## 19. 常见改动入口

新增首页模块：

- `HomePageScreen.kt`
- `MainViewModel.kt`
- `Parser.homePageVer2`
- `HomePage` 相关 model

新增搜索过滤条件：

- `SearchScreen.kt`
- `AdvancedSearchSheet.kt`
- `SearchViewModel.kt`
- `HAdvancedSearch.kt`
- `NetworkRepo.getHanimeSearchResult`
- `HanimeBaseService.getHanimeSearchResult`

新增视频详情字段：

- `HanimeVideo.kt`
- `Parser.hanimeVideoVer2`
- `VideoIntroductionScreen.kt`
- `VideoRouteActions.kt`，如果字段涉及点击动作或外部跳转

新增我的页面列表：

- `MainRoutes.kt`
- `MainNavHost.kt`
- 对应 `RouteScreen`
- 对应 `ViewModel`
- `NetworkRepo` 和 `Parser`

新增下载相关能力：

- `DownloadViewModel.kt`
- `HanimeDownloadManagerV2.kt`
- `HanimeDownloadWorker.kt`
- `DownloadDatabase` / DAO / Entity
- 下载设置页和权限/SAF 工具

新增设置项：

- `Preferences.kt` 或对应配置类
- `HomeSettingsScreen.kt` / 子设置页面
- 实际业务读取点

## 20. 维护提示

- 目标网站 DOM 改版时，优先检查 `Parser.kt`。
- 出现网络请求异常时，优先检查拦截器、Cookie、Cloudflare 和备用域名逻辑。
- 出现列表崩溃 `Key "..." was already used` 时，优先检查 ViewModel 合并列表是否去重。
- 出现视频页状态错乱时，优先检查 `VideoViewModel.VideoHostUiState` 和 `videoIntroUiStateMap`。
- 出现下载进度异常时，优先检查 Worker、Room 实体和 DAO Flow。
- 更新依赖版本后，优先跑 `:app:compileDebugKotlin`，播放器、导航和 KSP/Room 是高风险区域。
