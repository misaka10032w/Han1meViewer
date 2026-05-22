package com.yenaly.han1meviewer.logic.model

enum class CreatorSort(val value: String) {
    Latest("latest"),
    Popular("popular"),
    Oldest("oldest"),
}

enum class CreatorTab {
    Uploaded,
    Uploading,
}

data class CreatorUploadingItem(
    override val title: String,
    override val coverUrl: String,
    override val videoCode: String,
    override val duration: String? = null,
    override val views: String? = null,
    override val reviews: String? = null,
    override val currentArtist: String? = null,
    override val uploadTime: String? = null,
    val remoteVideoUrl: String,
    val reviewStatus: String,
) : VideoItemType
