package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.window.core.layout.WindowSizeClass
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.main.navigateSettingsCategoryDestination
import com.yenaly.han1meviewer.ui.screen.settings.SettingsCategoryItem
import com.yenaly.han1meviewer.util.findActivityOrNull
import com.yenaly.han1meviewer.util.logScreenViewEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsScaffold(
    navController: NavController,
    fallbackDestination: Any,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivityOrNull<MainActivity>()
        ?: error("No MainActivity found")
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = backStackEntry?.resolveSettingsScreenDestination()
        ?: SettingsScreenDestination.Home

    fun navigateBack() {
        if (!navController.popBackStack()) {
            navController.navigate(fallbackDestination)
        }
    }

    LaunchedEffect(currentScreen) {
        activity.logScreenViewEvent(currentScreen.screenClassName)
    }

    val useTwoPane = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    if (useTwoPane) {
        SettingsTwoPaneScaffold(
            navController = navController,
            currentScreen = currentScreen,
            actions = actions,
            onBack = ::navigateBack,
            content = content,
        )
    } else {
        SettingsSinglePaneScaffold(
            currentScreen = currentScreen,
            actions = actions,
            onBack = ::navigateBack,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSinglePaneScaffold(
    currentScreen: SettingsScreenDestination,
    actions: @Composable () -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentScreen.showToolbar) {
                TopAppBar(
                    title = { Text(stringResource(currentScreen.titleRes)) },
                    navigationIcon = {
                        FilledIconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    actions = { actions() },
                    modifier = Modifier.statusBarsPadding(),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsTwoPaneScaffold(
    navController: NavController,
    currentScreen: SettingsScreenDestination,
    actions: @Composable () -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    // 双栏设置始终同时展示列表与详情，避免路由切换时左栏被重新展开而产生滑动。
    val fixedTwoPaneValue = remember {
        ThreePaneScaffoldValue(
            primary = PaneAdaptedValue.Expanded,
            secondary = PaneAdaptedValue.Expanded,
            tertiary = PaneAdaptedValue.Hidden,
        )
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = fixedTwoPaneValue,
        listPane = {
            AnimatedPane {
                SettingsListPane(
                    selectedCategory = currentScreen.category,
                    onSelect = { category ->
                        navController.navigateSettingsCategoryDestination(category)
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                SettingsSinglePaneScaffold(
                    currentScreen = currentScreen,
                    actions = actions,
                    onBack = onBack,
                    content = content,
                )
            }
        },
    )
}

@Composable
private fun SettingsListPane(
    selectedCategory: SettingsCategory?,
    onSelect: (SettingsCategory) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                top = 20.dp,
                bottom = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.settings_pane_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                items = SettingsCategory.entries,
                key = { it.key },
            ) { category ->
                SettingsCategoryItem(
                    category = category,
                    selected = category == selectedCategory,
                    onClick = { onSelect(category) },
                )
            }
        }
    }
}
