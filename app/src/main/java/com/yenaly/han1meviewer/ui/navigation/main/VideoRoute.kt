package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.video.VideoRouteHostScreen

@Composable
fun VideoRouteScreen(
    activity: MainActivity,
    route: VideoRoute,
) {
    VideoRouteHostScreen(activity = activity, route = route)
}
