package com.yenaly.han1meviewer.util

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.videoUrlRegex

/**
 * 剪贴板视频链接检测。
 *
 * - 同一条剪贴板内容只处理一次，避免每次回到应用都重复提示；
 * - 忽略本应用自己复制出去的分享文本，避免刚复制完就被提示打开。
 */
object ClipboardVideoLinkGuard {

    private const val SELF_SHARE_SUFFIX = "- From Han1meViewer -"

    /**
     * 读取剪贴板并返回其中新出现的视频 code。
     *
     * 无论内容是否包含链接都会记录，这样用户之后重新复制同一条链接时仍能再次提示。
     *
     * @return 需要提示的视频 code，没有新内容或不应提示时返回 null
     */
    fun findNewVideoCode(): String? {
        val clipText = textFromClipboard?.toString()?.trim().orEmpty()
        if (clipText.isEmpty()) return null

        // 与上次读取到的内容相同，说明不是新复制的内容
        if (clipText == Preferences.lastHandledClipboard) return null
        Preferences.lastHandledClipboard = clipText

        // 本应用复制出去的分享文本，不再提示
        if (clipText.contains(SELF_SHARE_SUFFIX)) return null

        return videoUrlRegex.find(clipText)?.groupValues?.getOrNull(1)
    }
}
