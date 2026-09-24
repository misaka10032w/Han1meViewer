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
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.adaptive.TabletEmptyDetail
import com.yenaly.han1meviewer.ui.adaptive.TabletListDetail
import com.yenaly.han1meviewer.ui.adaptive.shouldUseListDetail
import com.yenaly.han1meviewer.ui.screen.home.PreviewScreen
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewViewModel

@Composable
fun PreviewRouteScreen(
    activity: MainActivity,
    onBack: () -> Unit,
    onNavigateToGetchuPreview: () -> Unit,
    onNavigateToPreviewComment: (String, String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val previewViewModel: PreviewViewModel = viewModel()
    val commentViewModel: CommentViewModel = viewModel(viewModelStoreOwner = activity)
    val listDetail = shouldUseListDetail()
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDateCode by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedDate != null && selectedDateCode != null

    if (!listDetail) {
        PreviewScreen(
            onBack = onBack,
            onNavigateToGetchuPreview = onNavigateToGetchuPreview,
            onNavigateToPreviewComment = onNavigateToPreviewComment,
            onNavigateToVideo = onNavigateToVideo,
            previewViewModel = previewViewModel,
            commentViewModel = commentViewModel,
        )
        return
    }

    BackHandler(enabled = selected) {
        selectedDate = null
        selectedDateCode = null
    }
    TabletListDetail(
        showDetail = selected,
        listWidth = 420.dp,
        list = {
            PreviewScreen(
                onBack = {
                    if (selected) {
                        selectedDate = null
                        selectedDateCode = null
                    } else {
                        onBack()
                    }
                },
                onNavigateToGetchuPreview = onNavigateToGetchuPreview,
                onNavigateToPreviewComment = { date, dateCode ->
                    selectedDate = date
                    selectedDateCode = dateCode
                },
                onNavigateToVideo = onNavigateToVideo,
                previewViewModel = previewViewModel,
                commentViewModel = commentViewModel,
            )
        },
        detail = {
            PreviewCommentRouteScreen(
                activity = activity,
                route = PreviewCommentRoute(selectedDate.orEmpty(), selectedDateCode.orEmpty()),
                onBack = {
                    selectedDate = null
                    selectedDateCode = null
                },
            )
        },
        emptyDetail = {
            TabletEmptyDetail(stringResource(R.string.tablet_select_preview_comment))
        },
    )
}
