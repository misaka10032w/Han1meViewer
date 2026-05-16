package com.yenaly.han1meviewer.ui.bridge

interface VideoPageHost {
    fun showCommentBadge(count: Int)
    fun shouldEnterPip(): Boolean
    fun enterPipMode()
    fun onPipModeChanged(isInPip: Boolean)
    fun togglePlayPause()
}
