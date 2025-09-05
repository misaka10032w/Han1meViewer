package com.yenaly.han1meviewer.logic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MySubscriptions(
    val subscriptions: List<SubscriptionItem>,
    val subscriptionsVideos: List<SubscriptionVideosItem>,
    val maxPage: Int
) : Parcelable

@Parcelize
data class SubscriptionItem(
    val artistName: String,
    val avatar: String
) : Parcelable

@Parcelize
data class SubscriptionVideosItem(
    val title: String,
    val coverUrl: String,
    val videoCode: String,
    val duration: String? = null,
    val views: String? = null,
    val reviews: String? = null,
    val currentArtist: String? = null
) : Parcelable
