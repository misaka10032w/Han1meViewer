package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.Navigator
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SanitizedPredictiveBackOwner
import com.yenaly.han1meviewer.ui.navigation.settings.AboutAppRoute
import com.yenaly.han1meviewer.ui.navigation.settings.DownloadSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.DownloadSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframeSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesTopBarActions
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategory
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategoryRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategoryRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsHomeRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsScaffold
import com.yenaly.han1meviewer.ui.navigation.settings.SharedHKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SharedHKeyframesRouteScreen
import com.yenaly.han1meviewer.ui.screen.account.AccountScreen
import com.yenaly.han1meviewer.ui.screen.account.AvatarCropScreen
import com.yenaly.han1meviewer.ui.screen.home.CreatorCenterScreen
import com.yenaly.han1meviewer.ui.screen.library.ContentLibraryScreen
import com.yenaly.han1meviewer.ui.screen.main.ProfileScreen
import com.yenaly.han1meviewer.ui.screen.settings.AboutAppScreen
import com.yenaly.han1meviewer.ui.viewmodel.CreatorCenterViewModel
import com.yenaly.han1meviewer.ui.viewmodel.UserAccountViewModel
import com.yenaly.han1meviewer.util.HAnalytics
import kotlinx.serialization.json.Json

@Composable
fun MainNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    isLoggedIn: Boolean,
    avatarUrl: String?,
    username: String?,
    currentSite: String,
    onRequireLogin: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
) {
    val currentKey = navigationState.currentKey
    val destinationSpec = MainDestinationSpec.fromKey(currentKey)
    var pendingAvatarCropResult by remember { mutableStateOf<String?>(null) }

    val onBack: () -> Unit = { navigator.goBack() }
    val onNavigateToVideo: (String) -> Unit = { code -> navigator.navigateSafely(VideoRoute(code)) }
    val onNavigateToLocalVideo: (String, String?) -> Unit =
        { code, uri -> navigator.navigateSafely(VideoRoute(code, uri)) }

    LaunchedEffect(destinationSpec) {
        destinationSpec?.let(onDestinationChanged)
        // 主导航页面的 screen_view 埋点（设置页在 SettingsScaffold 中另行记录）
        destinationSpec?.let {
            HAnalytics.logScreenView(
                screenName = it.name,
                screenClassName = it.routeClass.simpleName ?: it.name,
            )
        }
    }

    val isExpandedWidth = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    val entryProvider = entryProvider<NavKey> {
        screen<HomeRoute> {
            HomeRouteScreen(
                activity = activity,
                onNavigateToPreview = { navigator.navigateSafely(PreviewRoute) },
                onNavigateToSearch = { query -> navigator.navigateSafely(SearchRoute(query = query)) },
                onNavigateToSearchAdvanced = { params ->
                    navigator.navigateSafely(
                        SearchRoute(advancedSearchJson = Json.encodeToString(params))
                    )
                },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<ContentLibraryRoute> {
            ContentLibraryScreen(
                isLoggedIn = isLoggedIn,
                onRequireLogin = onRequireLogin,
                onNavigateToVideo = onNavigateToVideo,
                onNavigateToLocalVideo = onNavigateToLocalVideo,
            )
        }
        screen<ProfileRoute> {
            ProfileScreen(
                isLoggedIn = isLoggedIn,
                avatarUrl = avatarUrl,
                username = username,
                currentSite = currentSite,
                onAccountClick = {
                    if (isLoggedIn) {
                        navigator.navigateSafely(AccountRoute)
                    } else {
                        onRequireLogin()
                    }
                },
                onAvatarLongClick = onLogoutClick,
                onDailyCheckInClick = { navigator.navigateSafely(DailyCheckInRoute) },
                onCreatorCenterClick = {
                    if (isLoggedIn) {
                        navigator.navigateSafely(CreatorCenterRoute)
                    } else {
                        onRequireLogin()
                    }
                },
                onSettingsClick = { navigator.navigateSafely(HomeSettingsRoute) },
                onSwitchSiteClick = onSwitchSiteClick,
            )
        }
        screen<WatchHistoryRoute> {
            WatchHistoryRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<MyFavVideoRoute> {
            FavVideoRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<MyWatchLaterRoute> {
            WatchLaterRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<MyPlaylistRoute> {
            MyPlaylistRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<SubscriptionRoute> {
            SubscriptionRouteScreen(
                onBack = onBack,
                onNavigateToSearch = { query -> navigator.navigateSafely(SearchRoute(query = query)) },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<DailyCheckInRoute> {
            DailyCheckInRouteScreen(
                activity = activity,
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<DownloadRoute> {
            DownloadRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
                onNavigateToLocalVideo = onNavigateToLocalVideo,
            )
        }
        screen<CreatorCenterRoute> {
            val creatorViewModel: CreatorCenterViewModel = viewModel()
            CreatorCenterScreen(
                viewModel = creatorViewModel,
                onBack = onBack,
                onOpenUploadedVideo = { item -> onNavigateToVideo(item.videoCode) },
                onOpenUploadingVideo = { item -> onNavigateToLocalVideo("-1", item.remoteVideoUrl) },
            )
        }
        screen<AccountRoute> {
            val accountViewModel: UserAccountViewModel = viewModel()
            AccountScreen(
                viewModel = accountViewModel,
                onBack = onBack,
                onOpenAvatarCrop = { sourceUri ->
                    navigator.navigateSafely(AvatarCropRoute(sourceUri))
                },
                pendingAvatarCropResult = pendingAvatarCropResult,
                onAvatarCropResultConsumed = { pendingAvatarCropResult = null },
                onRefreshHome = { activity.viewModel.getHomePage() },
                onLogout = { activity.requestLogout(closeCurrentPageOnConfirm = true) },
            )
        }
        screen<AvatarCropRoute> { key ->
            AvatarCropScreen(
                sourceUri = key.sourceUri,
                onBack = onBack,
                onConfirm = { file ->
                    pendingAvatarCropResult = file.absolutePath
                    onBack()
                },
            )
        }
        screen<AboutAppRoute> {
            SettingsScaffold(
                navigator = navigator,
                fallbackDestination = HomeSettingsRoute,
            ) {
                AboutAppScreen()
            }
        }
        screen<HomeSettingsRoute> {
            SettingsScaffold(
                navigator = navigator,
                fallbackDestination = HomeRoute,
            ) {
                SettingsHomeRouteScreen(
                    onOpenCategory = { category ->
                        navigator.navigateSettingsCategoryDestination(category)
                    },
                )
            }
        }
        screen<SettingsCategoryRoute> { key ->
            val category = SettingsCategory.fromKey(key.categoryKey)
                ?: SettingsCategory.Appearance
            SettingsScaffold(
                navigator = navigator,
                fallbackDestination = HomeSettingsRoute,
            ) {
                SettingsCategoryRouteScreen(
                    category = category,
                    activity = activity,
                    onNavigateToPlayerSettings = {
                        navigator.navigateSettingsDestination(PlayerSettingsRoute)
                    },
                    onNavigateToHKeyframes = {
                        navigator.navigateSettingsDestination(HKeyframesRoute)
                    },
                    onNavigateToSharedHKeyframes = {
                        navigator.navigateSettingsDestination(SharedHKeyframesRoute)
                    },
                    onNavigateToAboutPage = {
                        navigator.navigateSettingsDestination(AboutAppRoute)
                    },
                )
            }
        }
        screen<PlayerSettingsRoute> {
            SettingsScaffold(
                navigator = navigator,
                fallbackDestination = HomeSettingsRoute,
            ) {
                PlayerSettingsRouteScreen(
                    onNavigateToMpvSettings = {
                        navigator.navigateSettingsDestination(MpvPlayerSettingsRoute)
                    },
                )
            }
        }
        screen<NetworkSettingsRoute> {
            SettingsScaffold(
                navigator = navigator,
                fallbackDestination = HomeSettingsRoute,
            ) {
                NetworkSettingsRouteScreen()
            }
        }
        screen<DownloadSettingsRoute> {
            SettingsScaffold(
                navigator = navigator,
                fallbackDestination = HomeSettingsRoute,
            ) {
                DownloadSettingsRouteScreen(activity = activity)
            }
        }
        screen<MpvPlayerSettingsRoute> {
            SettingsScaffold(
                navigator = navigator,
                fallbackDestination = PlayerSettingsRoute,
            ) {
                MpvPlayerSettingsRouteScreen()
            }
        }
        screen<HKeyframesRoute> {
            var showImportDialog by remember { mutableStateOf(false) }
            SettingsScaffold(
                navigator = navigator,
                fallbackDestination = HKeyframeSettingsRoute,
                actions = {
                    HKeyframesTopBarActions(onImportClick = { showImportDialog = true })
                },
            ) {
                HKeyframesRouteScreen(
                    onOpenVideo = onNavigateToVideo,
                    showImportDialog = showImportDialog,
                    onImportDialogDismiss = { showImportDialog = false },
                )
            }
        }
        screen<SharedHKeyframesRoute> {
            SettingsScaffold(
                navigator = navigator,
                fallbackDestination = HKeyframeSettingsRoute,
            ) {
                SharedHKeyframesRouteScreen(
                    onOpenVideo = onNavigateToVideo,
                )
            }
        }
        screen<HKeyframeSettingsRoute> {
            SettingsScaffold(
                navigator = navigator,
                fallbackDestination = HomeSettingsRoute,
            ) {
                HKeyframeSettingsRouteScreen(
                    onNavigateToHKeyframes = {
                        navigator.navigateSettingsDestination(HKeyframesRoute)
                    },
                    onNavigateToSharedHKeyframes = {
                        navigator.navigateSettingsDestination(SharedHKeyframesRoute)
                    },
                )
            }
        }
        screen<SearchRoute> { key ->
            SearchRouteScreen(
                route = key,
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<PreviewRoute> {
            PreviewRouteScreen(
                activity = activity,
                onBack = onBack,
                onNavigateToGetchuPreview = {
                    navigator.navigateSafely(GetchuPreviewRoute)
                },
                onNavigateToPreviewComment = { date, dateCode ->
                    navigator.navigateSafely(PreviewCommentRoute(date, dateCode))
                },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<GetchuPreviewRoute> {
            GetchuPreviewRouteScreen(
                onBack = onBack,
                onNavigateToDetail = { id -> navigator.navigateSafely(GetchuPreviewDetailRoute(id)) },
            )
        }
        screen<GetchuPreviewDetailRoute> { key ->
            GetchuPreviewDetailRouteScreen(
                route = key,
                onBack = onBack,
                onNavigateToDetail = { id -> navigator.navigateSafely(GetchuPreviewDetailRoute(id)) },
                onNavigateToVideoUrl = { url -> navigator.navigateSafely(VideoRoute("-1", url)) },
            )
        }
        screen<PreviewCommentRoute> { key ->
            PreviewCommentRouteScreen(
                activity = activity,
                route = key,
                onBack = onBack,
            )
        }
        screen<VideoRoute> { key ->
            VideoRouteScreen(
                activity = activity,
                route = key,
            )
        }
    }

    // 清洗系统返回手势 progress（部分 OEM ROM 会下发 NaN，导致 NavDisplay 内部
    // seekTo 抛 IllegalArgumentException 崩溃），再交给 NavDisplay 使用；
    // 对返回行为本身完全透明。
    SanitizedPredictiveBackOwner {
        NavDisplay(
            backStack = navigationState.entriesInUse,
            onBack = { navigator.goBack() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator<NavKey>(),
            ),
            // 新页面进入：从右侧滑入，同时伴随淡入
            // 旧页面退出：向左轻微偏移（1/3 宽度）并缩小、淡出，营造“压在下面”的感觉
            transitionSpec = {
                when {
                    targetState.topKey().isAutoPlayVideo() ->
                        EnterTransition.None togetherWith ExitTransition.None
                    // 双栏设置只切换右侧内容，左栏保持不动。
                    suppressSettingsPaneTransition(isExpandedWidth) ->
                        EnterTransition.None togetherWith ExitTransition.None
                    else -> slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(450)) togetherWith
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            targetOffset = { it / 3 },
                            animationSpec = tween(450, easing = FastOutSlowInEasing)
                        ) + scaleOut(targetScale = 0.9f) + fadeOut(animationSpec = tween(300))
                }
            },
            // 返回（pop/预测返回）：新页面从左侧滑入（1/3 宽度）并放大恢复，
            // 旧页面向右滑出淡出
            popTransitionSpec = { mainPopTransform(suppressSettingsPaneTransition(isExpandedWidth)) },
            predictivePopTransitionSpec = { _ ->
                mainPopTransform(suppressSettingsPaneTransition(isExpandedWidth))
            },
            entryProvider = entryProvider,
        )
    }
}

/** Scene 的栈顶 key（SinglePaneScene 下即当前展示的页面路由）。 */
private fun Scene<*>.topKey(): Any? =
    entries.lastOrNull()?.metadata?.get(NAV_KEY_METADATA)

/**
 * [entry] 的封装：在 metadata 中携带原始 key。
 * NavEntry.key 在 Navigation 3 中是私有的（对外只暴露 contentKey 字符串），
 * 转场方向判断（如自动连播、双栏设置切换）需要原始类型化 key，故经由此通道传递。
 */
private inline fun <reified K : NavKey> EntryProviderScope<NavKey>.screen(
    noinline content: @Composable (K) -> Unit,
) {
    entry<K>(metadata = { mapOf(NAV_KEY_METADATA to it) }, content = content)
}

private const val NAV_KEY_METADATA = "com.yenaly.han1meviewer.NAV_KEY"

private fun Any?.isAutoPlayVideo(): Boolean = (this as? VideoRoute)?.autoPlay == true

private fun Any?.isSettingsKey(): Boolean =
    SettingsDestinationSpec.fromKey(this) != null || this is SettingsCategoryRoute

private fun AnimatedContentTransitionScope<Scene<NavKey>>.suppressSettingsPaneTransition(
    isExpandedWidth: Boolean,
): Boolean =
    isExpandedWidth && initialState.topKey().isSettingsKey() && targetState.topKey().isSettingsKey()

/**
 * 返回（pop）转场：新页面从左侧滑入（偏移 1/3 宽度）、由 0.9 放大恢复，
 * 营造“浮上来”的感觉；旧页面向右滑出并淡出。
 * pop 与 predictivePop 共用，保证程序化返回与手势返回观感一致。
 */
private fun AnimatedContentTransitionScope<Scene<NavKey>>.mainPopTransform(
    suppressed: Boolean,
): androidx.compose.animation.ContentTransform =
    if (suppressed) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            initialOffset = { it / 3 },
            animationSpec = tween(450, easing = FastOutSlowInEasing)
        ) + scaleIn(initialScale = 0.9f) + fadeIn(animationSpec = tween(450)) togetherWith
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
    }
