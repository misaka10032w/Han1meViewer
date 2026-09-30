package com.yenaly.han1meviewer.ui.navigation.main

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.screen.home.DailyCheckInScreen
import com.yenaly.han1meviewer.ui.widget.CheckInWidgetProvider

@Composable
fun DailyCheckInRouteScreen(
    activity: MainActivity,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val widgetPinHint = stringResource(R.string.widget_pin_not_supported_manual_add)
    val widgetNotSupported = stringResource(R.string.widget_not_supported)
    DailyCheckInScreen(
        activity = activity,
        onBack = onBack,
        onAddWidget = {
            val mgr = AppWidgetManager.getInstance(activity)
            GlobalToasts.show(widgetPinHint, level = GlobalToasts.ToastLevel.INFO)
            if (mgr.isRequestPinAppWidgetSupported) {
                mgr.requestPinAppWidget(
                    ComponentName(activity, CheckInWidgetProvider::class.java),
                    null, null,
                )
            } else {
                GlobalToasts.show(widgetNotSupported, level = GlobalToasts.ToastLevel.WARNING)
            }
        },
        onNavigateToVideo = onNavigateToVideo,
    )
}
