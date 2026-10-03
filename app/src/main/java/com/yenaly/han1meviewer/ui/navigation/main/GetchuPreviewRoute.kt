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
import com.yenaly.han1meviewer.ui.adaptive.AdaptiveListDetail
import com.yenaly.han1meviewer.ui.adaptive.TabletEmptyDetail
import com.yenaly.han1meviewer.ui.adaptive.currentContentUsesListDetail
import com.yenaly.han1meviewer.ui.screen.home.preview.getchupreview.GetchuPreviewDetailScreen
import com.yenaly.han1meviewer.ui.screen.home.preview.getchupreview.GetchuPreviewScreen
import com.yenaly.han1meviewer.ui.screen.home.preview.getchupreview.GetchuPreviewViewModel

@Composable
fun GetchuPreviewRouteScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToVideoUrl: (String) -> Unit,
) {
    val viewModel: GetchuPreviewViewModel = viewModel()
    val useListDetail = currentContentUsesListDetail()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    fun clearSelection() {
        selectedId = null
    }

    BackHandler(enabled = selectedId != null, onBack = ::clearSelection)
    val listContent: @Composable () -> Unit = {
        GetchuPreviewScreen(
            onBack = {
                if (useListDetail && selectedId != null) clearSelection() else onBack()
            },
            onNavigateToDetail = { id ->
                if (useListDetail) selectedId = id else onNavigateToDetail(id)
            },
            viewModel = viewModel,
        )
    }
    AdaptiveListDetail(
        useListDetail = useListDetail,
        showDetail = selectedId != null,
        listWidth = 360.dp,
        list = listContent,
        detail = {
            GetchuPreviewDetailScreen(
                id = selectedId.orEmpty(),
                onBack = ::clearSelection,
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
    val useListDetail = currentContentUsesListDetail()
    var selectedId by rememberSaveable(route.id) { mutableStateOf(route.id) }

    fun navigateBackFromDetail() {
        if (selectedId != route.id) selectedId = route.id else onBack()
    }

    BackHandler(enabled = selectedId != route.id, onBack = ::navigateBackFromDetail)
    val detailContent: @Composable () -> Unit = {
        GetchuPreviewDetailScreen(
            id = selectedId,
            onBack = ::navigateBackFromDetail,
            onNavigateToDetail = { id ->
                if (useListDetail) selectedId = id else onNavigateToDetail(id)
            },
            onNavigateToVideoUrl = onNavigateToVideoUrl,
            viewModel = viewModel,
        )
    }
    AdaptiveListDetail(
        useListDetail = useListDetail,
        showDetail = true,
        listWidth = 360.dp,
        list = {
            GetchuPreviewScreen(
                onBack = onBack,
                onNavigateToDetail = { selectedId = it },
                viewModel = viewModel,
            )
        },
        detail = detailContent,
        emptyDetail = { TabletEmptyDetail(stringResource(R.string.tablet_select_getchu)) },
    )
}
