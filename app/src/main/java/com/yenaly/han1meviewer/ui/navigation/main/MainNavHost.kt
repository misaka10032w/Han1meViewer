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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowSizeClass
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.SanitizedPredictiveBackOwner
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.popBackStackSafely
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

private fun NavBackStackEntry?.isSettingsRoute(): Boolean {
    return this?.destination?.hasRoute<SettingsCategoryRoute>() == true ||
            SettingsDestinationSpec.fromDestination(this?.destination) != null
}

@Composable
fun MainNavHost(
    activity: MainActivity,
    navController: NavHostController,
    isLoggedIn: Boolean,
    avatarUrl: String?,
    username: String?,
    currentSite: String,
    onRequireLogin: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destinationSpec = MainDestinationSpec.fromDestination(backStackEntry?.destination)
    var pendingAvatarCropResult by remember { mutableStateOf<String?>(null) }

    val onBack: () -> Unit = { navController.popBackStackSafely() }
    val onNavigateToVideo: (String) -> Unit = { code -> navController.navigateSafely(VideoRoute(code)) }
    val onNavigateToLocalVideo: (String, String?) -> Unit =
        { code, uri -> navController.navigateSafely(VideoRoute(code, uri)) }

    LaunchedEffect(destinationSpec) {
        destinationSpec?.let(onDestinationChanged)
        // 主导航页面的 screen_view 埋点（设置页在 SettingsNavHost 中另行记录）
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
    val suppressSettingsPaneTransition: (NavBackStackEntry, NavBackStackEntry) -> Boolean =
        { initial, target ->
            isExpandedWidth && initial.isSettingsRoute() && target.isSettingsRoute()
        }

    // 清洗系统返回手势 progress（部分 OEM ROM 会下发 NaN，导致 NavHost 内部
    // SeekableTransitionState.seekTo 抛 IllegalArgumentException 崩溃），
    // 再交给 NavHost 使用；对返回行为本身完全透明。
    SanitizedPredictiveBackOwner {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            // 新页面进入：从右侧滑入，同时伴随淡入，且带有回弹感
            enterTransition = {
                when {
                    targetState.isAutoPlayVideo() -> EnterTransition.None
                    // 双栏设置只切换右侧内容，左栏保持不动。
                    suppressSettingsPaneTransition(initialState, targetState) -> EnterTransition.None
                    else -> slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(450))
                }
            },
            // 旧页面退出：向左轻微偏移，同时缩小并淡出，营造被“压在下面”的感觉
            exitTransition = {
                when {
                    targetState.isAutoPlayVideo() -> ExitTransition.None
                    suppressSettingsPaneTransition(initialState, targetState) -> ExitTransition.None
                    else -> slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        targetOffset = { it / 3 }, // 只偏移 1/3 的宽度
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    ) + scaleOut(targetScale = 0.9f) + fadeOut(animationSpec = tween(300))
                }
            },
            // 弹出（返回）新页面进入：从左侧滑入，由 0.9 放大恢复，营造“浮上来”的感觉
            popEnterTransition = { popEnterSpec(suppressSettingsPaneTransition(initialState, targetState)) },
            // 弹出（返回）旧页面退出：向右侧滑出，同时淡出
            popExitTransition = { popExitSpec(suppressSettingsPaneTransition(initialState, targetState)) },
            // 预测性返回（Navigation 2.10 新增分支）：手势返回时 NavHost 改走这两个参数，
            // 默认值是 fadeIn + scaleOut(0.7f)（系统样式的“中心缩放”），会盖掉上面自定义的
            // pop 动画。这里显式指定为与 popEnter/popExit 完全一致，保持旧观感，
            // 且手势拖动过程中动画会跟随手指进度预览（seekable）。
            predictivePopEnterTransition = { _ ->
                popEnterSpec(suppressSettingsPaneTransition(initialState, targetState))
            },
            predictivePopExitTransition = { _ ->
                popExitSpec(suppressSettingsPaneTransition(initialState, targetState))
            },
        ) {
            composable<HomeRoute> {
                HomeRouteScreen(
                    activity = activity,
                    onNavigateToPreview = { navController.navigateSafely(PreviewRoute) },
                    onNavigateToSearch = { query -> navController.navigateSafely(SearchRoute(query = query)) },
                    onNavigateToSearchAdvanced = { params ->
                        navController.navigateSafely(
                            SearchRoute(advancedSearchJson = Json.encodeToString(params))
                        )
                    },
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            composable<ContentLibraryRoute> {
                ContentLibraryScreen(
                    isLoggedIn = isLoggedIn,
                    onRequireLogin = onRequireLogin,
                    onNavigateToVideo = onNavigateToVideo,
                    onNavigateToLocalVideo = onNavigateToLocalVideo,
                )
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    isLoggedIn = isLoggedIn,
                    avatarUrl = avatarUrl,
                    username = username,
                    currentSite = currentSite,
                    onAccountClick = {
                        if (isLoggedIn) {
                            navController.navigateSafely(AccountRoute)
                        } else {
                            onRequireLogin()
                        }
                    },
                    onAvatarLongClick = onLogoutClick,
                    onDailyCheckInClick = { navController.navigateSafely(DailyCheckInRoute) },
                    onCreatorCenterClick = {
                        if (isLoggedIn) {
                            navController.navigateSafely(CreatorCenterRoute)
                        } else {
                            onRequireLogin()
                        }
                    },
                    onSettingsClick = { navController.navigateSafely(HomeSettingsRoute) },
                    onSwitchSiteClick = onSwitchSiteClick,
                )
            }
            composable<WatchHistoryRoute> {
                WatchHistoryRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            composable<MyFavVideoRoute> {
                FavVideoRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            composable<MyWatchLaterRoute> {
                WatchLaterRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            composable<MyPlaylistRoute> {
                MyPlaylistRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            composable<SubscriptionRoute> {
                SubscriptionRouteScreen(
                    onBack = onBack,
                    onNavigateToSearch = { query -> navController.navigateSafely(SearchRoute(query = query)) },
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            composable<DailyCheckInRoute> {
                DailyCheckInRouteScreen(
                    activity = activity,
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            composable<DownloadRoute> {
                DownloadRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                    onNavigateToLocalVideo = onNavigateToLocalVideo,
                )
            }
            composable<CreatorCenterRoute> {
                val creatorViewModel: CreatorCenterViewModel = viewModel()
                CreatorCenterScreen(
                    viewModel = creatorViewModel,
                    onBack = onBack,
                    onOpenUploadedVideo = { item -> onNavigateToVideo(item.videoCode) },
                    onOpenUploadingVideo = { item -> onNavigateToLocalVideo("-1", item.remoteVideoUrl) },
                )
            }
            composable<AccountRoute> {
                val accountViewModel: UserAccountViewModel = viewModel()
                AccountScreen(
                    viewModel = accountViewModel,
                    onBack = onBack,
                    onOpenAvatarCrop = { sourceUri ->
                        navController.navigateSafely(AvatarCropRoute(sourceUri))
                    },
                    pendingAvatarCropResult = pendingAvatarCropResult,
                    onAvatarCropResultConsumed = { pendingAvatarCropResult = null },
                    onRefreshHome = { activity.viewModel.getHomePage() },
                    onLogout = { activity.requestLogout(closeCurrentPageOnConfirm = true) },
                )
            }
            composable<AvatarCropRoute> {
                val route = it.toRoute<AvatarCropRoute>()
                AvatarCropScreen(
                    sourceUri = route.sourceUri,
                    onBack = onBack,
                    onConfirm = { file ->
                        pendingAvatarCropResult = file.absolutePath
                        onBack()
                    },
                )
            }
            composable<AboutAppRoute> {
                SettingsScaffold(
                    navController = navController,
                    fallbackDestination = HomeSettingsRoute,
                ) {
                    AboutAppScreen()
                }
            }
            composable<HomeSettingsRoute> {
                SettingsScaffold(
                    navController = navController,
                    fallbackDestination = HomeRoute,
                ) {
                    SettingsHomeRouteScreen(
                        onOpenCategory = { category ->
                            navController.navigateSettingsCategoryDestination(category)
                        },
                    )
                }
            }
            composable<SettingsCategoryRoute> { entry ->
                val route = entry.toRoute<SettingsCategoryRoute>()
                val category = SettingsCategory.fromKey(route.categoryKey)
                    ?: SettingsCategory.Appearance
                SettingsScaffold(
                    navController = navController,
                    fallbackDestination = HomeSettingsRoute,
                ) {
                    SettingsCategoryRouteScreen(
                        category = category,
                        activity = activity,
                        onNavigateToPlayerSettings = {
                            navController.navigateSettingsDestination(PlayerSettingsRoute)
                        },
                        onNavigateToHKeyframes = {
                            navController.navigateSettingsDestination(HKeyframesRoute)
                        },
                        onNavigateToSharedHKeyframes = {
                            navController.navigateSettingsDestination(SharedHKeyframesRoute)
                        },
                        onNavigateToAboutPage = {
                            navController.navigateSettingsDestination(AboutAppRoute)
                        },
                    )
                }
            }
            composable<PlayerSettingsRoute> {
                SettingsScaffold(
                    navController = navController,
                    fallbackDestination = HomeSettingsRoute,
                ) {
                    PlayerSettingsRouteScreen(
                        onNavigateToMpvSettings = {
                            navController.navigateSettingsDestination(MpvPlayerSettingsRoute)
                        },
                    )
                }
            }
            composable<NetworkSettingsRoute> {
                SettingsScaffold(
                    navController = navController,
                    fallbackDestination = HomeSettingsRoute,
                ) {
                    NetworkSettingsRouteScreen()
                }
            }
            composable<DownloadSettingsRoute> {
                SettingsScaffold(
                    navController = navController,
                    fallbackDestination = HomeSettingsRoute,
                ) {
                    DownloadSettingsRouteScreen(activity = activity)
                }
            }
            composable<MpvPlayerSettingsRoute> {
                SettingsScaffold(
                    navController = navController,
                    fallbackDestination = PlayerSettingsRoute,
                ) {
                    MpvPlayerSettingsRouteScreen()
                }
            }
            composable<HKeyframesRoute> {
                var showImportDialog by remember { mutableStateOf(false) }
                SettingsScaffold(
                    navController = navController,
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
            composable<SharedHKeyframesRoute> {
                SettingsScaffold(
                    navController = navController,
                    fallbackDestination = HKeyframeSettingsRoute,
                ) {
                    SharedHKeyframesRouteScreen(
                        onOpenVideo = onNavigateToVideo,
                    )
                }
            }
            composable<HKeyframeSettingsRoute> {
                SettingsScaffold(
                    navController = navController,
                    fallbackDestination = HomeSettingsRoute,
                ) {
                    HKeyframeSettingsRouteScreen(
                        onNavigateToHKeyframes = {
                            navController.navigateSettingsDestination(HKeyframesRoute)
                        },
                        onNavigateToSharedHKeyframes = {
                            navController.navigateSettingsDestination(SharedHKeyframesRoute)
                        },
                    )
                }
            }
            composable<SearchRoute> {
                SearchRouteScreen(
                    route = it.toRoute(),
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            composable<PreviewRoute> {
                PreviewRouteScreen(
                    activity = activity,
                    onBack = onBack,
                    onNavigateToGetchuPreview = {
                        navController.navigateSafely(GetchuPreviewRoute)
                    },
                    onNavigateToPreviewComment = { date, dateCode ->
                        navController.navigateSafely(PreviewCommentRoute(date, dateCode))
                    },
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            composable<GetchuPreviewRoute> {
                GetchuPreviewRouteScreen(
                    onBack = onBack,
                    onNavigateToDetail = { id -> navController.navigateSafely(GetchuPreviewDetailRoute(id)) },
                )
            }
            composable<GetchuPreviewDetailRoute> {
                GetchuPreviewDetailRouteScreen(
                    route = it.toRoute(),
                    onBack = onBack,
                    onNavigateToDetail = { id -> navController.navigateSafely(GetchuPreviewDetailRoute(id)) },
                    onNavigateToVideoUrl = { url -> navController.navigateSafely(VideoRoute("-1", url)) },
                )
            }
            composable<PreviewCommentRoute> {
                PreviewCommentRouteScreen(
                    activity = activity,
                    route = it.toRoute(),
                    onBack = onBack,
                )
            }
            composable<VideoRoute> {
                VideoRouteScreen(
                    activity = activity,
                    route = it.toRoute(),
                )
            }
        }
    }
}

private fun NavBackStackEntry.isAutoPlayVideo(): Boolean =
    destination.hasRoute<VideoRoute>() && toRoute<VideoRoute>().autoPlay

/**
 * 返回（pop）时新页面进入动画：从左侧滑入（偏移 1/3 宽度）、由 0.9 放大恢复，
 * 营造“浮上来”的感觉。同时用于 [NavHost] 的 popEnterTransition 与
 * predictivePopEnterTransition，保证程序化返回与手势返回观感一致。
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterSpec(
    suppressed: Boolean,
): EnterTransition =
    if (suppressed) {
        EnterTransition.None
    } else {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            initialOffset = { it / 3 },
            animationSpec = tween(450, easing = FastOutSlowInEasing)
        ) + scaleIn(initialScale = 0.9f) + fadeIn(animationSpec = tween(450))
    }

/**
 * 返回（pop）时旧页面退出动画：向右侧滑出并淡出。
 * 同时用于 [NavHost] 的 popExitTransition 与 predictivePopExitTransition。
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitSpec(
    suppressed: Boolean,
): ExitTransition =
    if (suppressed) {
        ExitTransition.None
    } else {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(450, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(300))
    }
