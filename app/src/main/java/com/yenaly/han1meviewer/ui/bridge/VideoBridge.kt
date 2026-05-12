package com.yenaly.han1meviewer.ui.bridge

fun videoBridgeTag(videoCode: String, localUri: String?): String =
    "compose_video_${videoCode}_${localUri.orEmpty()}"
