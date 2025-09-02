package com.yenaly.han1meviewer.logic.model

import android.os.Parcelable
import com.yenaly.yenaly_libs.utils.LanguageHelper
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Suppress("EqualsOrHashCode")
@Serializable
@Parcelize
data class ReportReason(
    @SerialName("lang")
    val lang: Language? = null,
    @SerialName("reason_key")
    val reasonKey: String? = null
) : Parcelable {
    @Serializable
    @Parcelize
    data class Language(
        @SerialName("zh-rCN")
        val zhrCN: String? = null,
        @SerialName("zh-rTW")
        val zhrTW: String? = null,
        @SerialName("en")
        val en: String? = null,
    ) : Parcelable

    override fun hashCode(): Int = reasonKey?.hashCode() ?: 0

    val value: String
        get() {
            if (lang == null) return reasonKey.orEmpty()

            val pl = LanguageHelper.preferredLanguage
            return when (pl.language) {
                Locale.CHINESE.language -> when (pl.country) {
                    Locale.SIMPLIFIED_CHINESE.country -> lang.zhrCN
                    else -> lang.zhrTW
                }
                Locale.ENGLISH.language -> lang.en
                else -> lang.zhrTW
            } ?: lang.zhrTW.orEmpty()
        }
}
