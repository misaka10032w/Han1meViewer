package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.screen.home.myplaylist.PlaylistScreen
import com.yenaly.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import com.yenaly.yenaly_libs.utils.copyTextToClipboard

@Composable
fun MyPlaylistRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyPlayListViewModelV2 = viewModel()
    val copied = stringResource(R.string.copy_to_clipboard)
    PlaylistScreen(
        viewModel = viewModel,
        navigateBack = onBack,
        onClickItem = onNavigateToVideo,
        onLongClickItem = { videoCode, title ->
            copyTextToClipboard(getHanimeShareText(title, videoCode))
            GlobalToasts.show(copied, level = GlobalToasts.ToastLevel.INFO)
        },
    )
}
