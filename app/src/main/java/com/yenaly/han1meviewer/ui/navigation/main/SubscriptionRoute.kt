package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeSearchShareText
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.screen.home.SubscriptionScreen
import com.yenaly.han1meviewer.ui.viewmodel.MySubscriptionsViewModel
import com.yenaly.yenaly_libs.utils.copyTextToClipboard

@Composable
fun SubscriptionRouteScreen(
    onBack: () -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MySubscriptionsViewModel = viewModel()
    val copied = stringResource(R.string.copy_to_clipboard)
    SubscriptionScreen(
        navigateBack = onBack,
        viewModel = viewModel,
        onClickArtist = { onNavigateToSearch(it) },
        onLongClickArtist = { artistName ->
            copyTextToClipboard(getHanimeSearchShareText(artistName))
            GlobalToasts.show(copied, level = GlobalToasts.ToastLevel.INFO)
        },
        onClickVideosItem = onNavigateToVideo,
        onLongClickVideosItem = { videoCode, title ->
            copyTextToClipboard(getHanimeShareText(title, videoCode))
            GlobalToasts.show(copied, level = GlobalToasts.ToastLevel.INFO)
        },
    )
}
