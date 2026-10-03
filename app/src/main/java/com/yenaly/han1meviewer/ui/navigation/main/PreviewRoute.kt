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
import com.yenaly.han1meviewer.ui.adaptive.AdaptiveListDetail
import com.yenaly.han1meviewer.ui.adaptive.TabletEmptyDetail
import com.yenaly.han1meviewer.ui.adaptive.currentContentUsesListDetail
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
    val useListDetail = currentContentUsesListDetail()
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDateCode by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedDate != null && selectedDateCode != null

    fun clearSelection() {
        selectedDate = null
        selectedDateCode = null
    }

    BackHandler(enabled = selected, onBack = ::clearSelection)
    val listContent: @Composable () -> Unit = {
        PreviewScreen(
            onBack = {
                if (useListDetail && selected) clearSelection() else onBack()
            },
            onNavigateToGetchuPreview = onNavigateToGetchuPreview,
            onNavigateToPreviewComment = { date, dateCode ->
                if (useListDetail) {
                    selectedDate = date
                    selectedDateCode = dateCode
                } else {
                    onNavigateToPreviewComment(date, dateCode)
                }
            },
            onNavigateToVideo = onNavigateToVideo,
            previewViewModel = previewViewModel,
            commentViewModel = commentViewModel,
        )
    }
    AdaptiveListDetail(
        useListDetail = useListDetail,
        showDetail = selected,
        listWidth = 420.dp,
        list = listContent,
        detail = {
            PreviewCommentRouteScreen(
                activity = activity,
                route = PreviewCommentRoute(selectedDate.orEmpty(), selectedDateCode.orEmpty()),
                onBack = ::clearSelection,
            )
        },
        emptyDetail = {
            TabletEmptyDetail(stringResource(R.string.tablet_select_preview_comment))
        },
    )
}
