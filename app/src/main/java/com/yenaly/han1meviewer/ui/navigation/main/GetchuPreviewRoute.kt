package com.yenaly.han1meviewer.ui.navigation.main

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.adaptive.TabletEmptyDetail
import com.yenaly.han1meviewer.ui.adaptive.TabletListDetail
import com.yenaly.han1meviewer.ui.adaptive.shouldUseListDetail
import com.yenaly.han1meviewer.ui.screen.home.preview.getchupreview.GetchuPreviewDetailScreen
import com.yenaly.han1meviewer.ui.screen.home.preview.getchupreview.GetchuPreviewScreen
import com.yenaly.han1meviewer.ui.screen.home.preview.getchupreview.GetchuPreviewViewModel

@Composable
fun GetchuPreviewRouteScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToVideoUrl: (String) -> Unit = {},
) {
    val viewModel: GetchuPreviewViewModel = viewModel()
    if (!shouldUseListDetail()) {
        GetchuPreviewScreen(
            onBack = onBack,
            onNavigateToDetail = onNavigateToDetail,
            viewModel = viewModel,
        )
        return
    }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = selectedId != null) { selectedId = null }
    TabletListDetail(
        showDetail = selectedId != null,
        listWidth = 360.dp,
        list = {
            GetchuPreviewScreen(
                onBack = onBack,
                onNavigateToDetail = { selectedId = it },
                viewModel = viewModel,
            )
        },
        detail = {
            GetchuPreviewDetailScreen(
                id = selectedId.orEmpty(),
                onBack = { selectedId = null },
                onNavigateToDetail = { selectedId = it },
                onNavigateToVideoUrl = onNavigateToVideoUrl,
                viewModel = viewModel,
            )
        },
        emptyDetail = { TabletEmptyDetail(stringResource(R.string.tablet_select_getchu)) },
    )
}

@Composable
fun GetchuPreviewDetailRouteScreen(
    route: GetchuPreviewDetailRoute,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToVideoUrl: (String) -> Unit,
) {
    val viewModel: GetchuPreviewViewModel = viewModel()
    if (!shouldUseListDetail()) {
        GetchuPreviewDetailScreen(
            id = route.id,
            onBack = onBack,
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToVideoUrl = onNavigateToVideoUrl,
            viewModel = viewModel,
        )
        return
    }
    var selectedId by rememberSaveable { mutableStateOf(route.id) }
    BackHandler(enabled = selectedId != route.id) { selectedId = route.id }
    TabletListDetail(
        showDetail = true,
        listWidth = 360.dp,
        list = {
            GetchuPreviewScreen(
                onBack = onBack,
                onNavigateToDetail = { selectedId = it },
                viewModel = viewModel,
            )
        },
        detail = {
            GetchuPreviewDetailScreen(
                id = selectedId,
                onBack = { selectedId = route.id },
                onNavigateToDetail = { selectedId = it },
                onNavigateToVideoUrl = onNavigateToVideoUrl,
                viewModel = viewModel,
            )
        },
        emptyDetail = { TabletEmptyDetail(stringResource(R.string.tablet_select_getchu)) },
    )
}
