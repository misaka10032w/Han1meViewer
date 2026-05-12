package com.yenaly.han1meviewer.ui.screen.main

fun videoBridgeTag(videoCode: String, localUri: String?): String =
    "compose_video_${videoCode}_${localUri.orEmpty()}"
