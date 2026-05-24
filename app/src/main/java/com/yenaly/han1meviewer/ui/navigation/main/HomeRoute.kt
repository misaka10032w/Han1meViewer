package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.CheckInType
import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.AnnouncementDialog
import com.yenaly.han1meviewer.ui.component.TripleButtonDialog
import com.yenaly.han1meviewer.ui.screen.home.HomePageScreen
import com.yenaly.han1meviewer.ui.screen.home.homepage.LocalSearchHistoryQuery
import com.yenaly.han1meviewer.ui.viewmodel.CheckInCalendarViewModel
import com.yenaly.yenaly_libs.utils.copyTextToClipboard
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeRouteScreen(
    activity: MainActivity,
    isDrawerOpen: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToSearchAdvanced: (Map<String, String>) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel = activity.viewModel
    val checkInViewModel: CheckInCalendarViewModel = viewModel()
    val confirmToExit = stringResource(R.string.confirm_to_exit)
    val finishedMasturbating = stringResource(R.string.finished_masturbating)
    val doMore = stringResource(R.string.do_more)
    val checkoutExit = stringResource(R.string.checkout_exit)
    val exit = stringResource(R.string.exit)
    var showExitDialog by remember { mutableStateOf(false) }
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    CompositionLocalProvider(
        LocalSearchHistoryQuery provides { keyword: String ->
            DatabaseRepo.SearchHistory.loadAll(keyword).first().map { it.query }
        }
    ) {
        HomePageScreen(
            viewModel = viewModel,
            isDrawerOpen = isDrawerOpen,
            onOpenDrawer = onOpenDrawer,
            onNavigateToPreview = onNavigateToPreview,
            onNavigateToSearch = { query -> onNavigateToSearch(query) },
            onOpenSearchPage = { onNavigateToSearch(null) },
            onNavigateToSearchAdvanced = onNavigateToSearchAdvanced,
            onOpenVideo = onNavigateToVideo,
            onLongPressVideoCopy = { code, title ->
                copyTextToClipboard(getHanimeShareText(title, code))
                showShortToast(R.string.copy_to_clipboard)
            },
            onShowExitDialog = { showExitDialog = true },
            onShowAnnouncementDialog = { announcement = it },
        )
    }

    if (showExitDialog) {
        TripleButtonDialog(
            visible = true,
            title = confirmToExit,
            message = finishedMasturbating,
            negativeText = doMore,
            neutralText = checkoutExit,
            positiveText = exit,
            onNegative = { showExitDialog = false },
            onNeutral = {
                checkInViewModel.addRecord(
                    LocalDate.now(),
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                    CheckInType.MASTURBATION.storeName, "", "",
                )
                activity.finish()
            },
            onPositive = { activity.finish() },
            onDismiss = { showExitDialog = false },
        )
    }

    announcement?.let { data ->
        AnnouncementDialog(
            announcementData = data,
            onDismiss = { announcement = null },
        )
    }
}
