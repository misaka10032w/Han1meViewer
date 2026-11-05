package com.yenaly.han1meviewer.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.navigation.main.MainTopLevelDestination
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

/** 导航轨每个 item 的固定高度，避免展开/折叠时高度变化影响图标间距。 */
private val RailItemHeight = 64.dp

/** 折叠态 item 间默认的 4dp 间距，展开态用相同间距补齐。 */
private val RailItemSpacing = 4.dp

/**
 * 主界面外壳：手机使用可自动隐藏的底部导航，平板/折叠屏使用导航轨。
 *
 * 展开宽度下导航轨可通过第五个箭头按钮折叠/展开标签；详情页面通过
 * [showNavigationSuite] 控制导航组件显示或隐藏。
 */
@Composable
fun MainActivityScaffold(
    selectedDestination: MainTopLevelDestination?,
    showNavigationSuite: Boolean,
    snackbarHostState: SnackbarHostState,
    onDestinationSelected: (MainTopLevelDestination) -> Boolean,
    content: @Composable () -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val windowSizeClass = adaptiveInfo.windowSizeClass
    val isExpandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    )
    val isMediumWidth = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )
    val isBottomNavigation = !isMediumWidth
    var railExpanded by rememberSaveable { mutableStateOf(false) }
    var bottomBarVisible by rememberSaveable { mutableStateOf(true) }
    val shouldExpandRail = isExpandedWidth && railExpanded

    LaunchedEffect(showNavigationSuite, selectedDestination) {
        if (showNavigationSuite) {
            bottomBarVisible = true
        }
    }
    LaunchedEffect(isBottomNavigation) {
        bottomBarVisible = true
    }

    val navigationVisible = showNavigationSuite && (!isBottomNavigation || bottomBarVisible)
    val navigationSuiteState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(navigationVisible) {
        navigationSuiteState.snapTo(
            if (navigationVisible) {
                NavigationSuiteScaffoldValue.Visible
            } else {
                NavigationSuiteScaffoldValue.Hidden
            }
        )
    }

    val railState = rememberWideNavigationRailState(
        initialValue = if (shouldExpandRail) {
            WideNavigationRailValue.Expanded
        } else {
            WideNavigationRailValue.Collapsed
        }
    )
    LaunchedEffect(shouldExpandRail) {
        railState.snapTo(
            if (shouldExpandRail) {
                WideNavigationRailValue.Expanded
            } else {
                WideNavigationRailValue.Collapsed
            }
        )
    }

    val density = LocalDensity.current
    val hideThreshold = with(density) { 4.dp.toPx() }
    val currentShowNavigationSuite by rememberUpdatedState(showNavigationSuite)
    val currentBottomNavigation by rememberUpdatedState(isBottomNavigation)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput ||
                    !currentBottomNavigation ||
                    !currentShowNavigationSuite
                ) {
                    return Offset.Zero
                }
                when {
                    available.y < -hideThreshold -> bottomBarVisible = false
                    available.y > hideThreshold -> bottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val bottomNavigationType = if (
        windowSizeClass.isHeightAtLeastBreakpoint(
            WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
        )
    ) {
        NavigationSuiteType.ShortNavigationBarCompact
    } else {
        NavigationSuiteType.ShortNavigationBarMedium
    }

    // 始终使用同一个 NavigationSuiteScaffold 及其 content 调用点，只切换导航类型。
    // 如果按 isBottomNavigation 在外部写 if/else 两套布局，横竖屏切换时 content
    // 会被整体移除并重新创建，播放页的播放器也会随之重建导致播放中断。
    NavigationSuiteScaffold(
        navigationSuiteType = if (isBottomNavigation) {
            bottomNavigationType
        } else {
            NavigationSuiteType.None
        },
        state = navigationSuiteState,
        navigationItems = {
            MainTopLevelDestination.entries.forEach { destination ->
                NavigationSuiteItem(
                    selected = selectedDestination == destination,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.titleRes)) },
                    navigationSuiteType = bottomNavigationType,
                )
            }
        },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (!isBottomNavigation && showNavigationSuite) {
                WideNavigationRail(
                    state = railState,
                    modifier = Modifier.fillMaxHeight(),
                    // 折叠态内部默认会在 item 之间加 4dp，展开态用 arrangement 补齐，
                    // 让两种状态下四个图标的垂直间距保持一致。
                    arrangement = if (shouldExpandRail) {
                        Arrangement.spacedBy(RailItemSpacing)
                    } else {
                        Arrangement.spacedBy(0.dp)
                    },
                ) {
                    MainTopLevelDestination.entries.forEach { destination ->
                        WideNavigationRailItem(
                            railExpanded = shouldExpandRail,
                            selected = selectedDestination == destination,
                            onClick = { onDestinationSelected(destination) },
                            modifier = Modifier.height(RailItemHeight),
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = if (shouldExpandRail) {
                                        null
                                    } else {
                                        stringResource(destination.titleRes)
                                    },
                                )
                            },
                            label = if (shouldExpandRail) {
                                { Text(stringResource(destination.titleRes)) }
                            } else {
                                null
                            },
                        )
                    }
                    if (isExpandedWidth) {
                        val toggleLabel = if (railExpanded) {
                            stringResource(R.string.collapse_navigation)
                        } else {
                            stringResource(R.string.expand_navigation)
                        }
                        WideNavigationRailItem(
                            railExpanded = shouldExpandRail,
                            selected = false,
                            onClick = { railExpanded = !railExpanded },
                            modifier = Modifier.height(RailItemHeight),
                            icon = {
                                Icon(
                                    imageVector = if (railExpanded) {
                                        Icons.AutoMirrored.Filled.KeyboardArrowLeft
                                    } else {
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                                    },
                                    contentDescription = toggleLabel,
                                )
                            },
                            label = null,
                        )
                    }
                }
            }
            ScaffoldContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isBottomNavigation) {
                            Modifier.nestedScroll(nestedScrollConnection)
                        } else {
                            Modifier
                        }
                    ),
                snackbarHostState = snackbarHostState,
                content = content,
            )
        }
    }
}

@Composable
private fun ScaffoldContent(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun MainActivityScaffoldPhonePreview() {
    ComponentPreview {
        MainActivityScaffold(
            selectedDestination = MainTopLevelDestination.Home,
            showNavigationSuite = true,
            snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() },
            onDestinationSelected = { true },
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Preview(showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
private fun MainActivityScaffoldTabletPreview() {
    ComponentPreview {
        MainActivityScaffold(
            selectedDestination = MainTopLevelDestination.ContentLibrary,
            showNavigationSuite = true,
            snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() },
            onDestinationSelected = { true },
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
