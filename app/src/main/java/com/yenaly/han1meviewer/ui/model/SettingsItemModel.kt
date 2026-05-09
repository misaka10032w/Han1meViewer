package com.yenaly.han1meviewer.ui.model

sealed interface SettingsItemModel {
    data class Switch(
        val key: String,
        val title: String,
        val summary: String? = null,
        val checked: Boolean,
    ) : SettingsItemModel

    data class Navigation(
        val key: String,
        val title: String,
        val summary: String? = null,
        val valueText: String? = null,
    ) : SettingsItemModel

    data class Action(
        val key: String,
        val title: String,
        val summary: String? = null,
    ) : SettingsItemModel
}
