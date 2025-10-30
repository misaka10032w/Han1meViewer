package com.yenaly.han1meviewer.logic.model

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 */
interface HanimeInfoType : MultiItemEntity

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/06/08 008 22:56
 */
data class HanimeInfo(
    override val title: String,
    override val coverUrl: String,
    override val videoCode: String,
    override val duration: String? = null,
    val artist: String? = null,
    override val views: String? = null,
    val uploadTime: String? = null,
    val genre: String? = null,

    val isPlaying: Boolean = false, // for video playlist only.

    override var itemType: Int,
    override val reviews: String? = "",
    override val currentArtist: String? = "",
    val watched: Boolean ?= false,
): VideoItemType , HanimeInfoType {
    companion object {
        const val NORMAL = 0
        const val SIMPLIFIED = 1
    }
}
