package com.yenaly.han1meviewer.logic.entity

import androidx.annotation.StringRes
import com.yenaly.han1meviewer.R

enum class CheckInType(@param:StringRes val displayNameRes: Int, val storeName: String) {
    MASTURBATION(R.string.checkin_type_masturbation, "自慰"),
    WET_DREAM(R.string.checkin_type_wet_dream, "梦遗"),
    SEX(R.string.checkin_type_sex, "做爱"),
    ORAL(R.string.checkin_type_oral, "口交"),
    OTHER(R.string.checkin_type_other, "其它");

    companion object {
        fun fromDisplayName(name: String): CheckInType =
            entries.firstOrNull { it.storeName == name } ?: MASTURBATION
    }
}