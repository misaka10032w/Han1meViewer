package com.yenaly.han1meviewer.logic.model

data class UserAccount(
    val csrfToken: String?,
    val avatarUrl: String,
    val username: String,
    val email: String,
    val userId: String,
    val joinedLabel: String?,
    val subscriberCount: Int,
    val videoCount: Int,
    // 網站上 email 是否可編輯（2026-11 改版後 email 輸入框為 disabled 唯讀），預設 true 兼容舊版
    val emailEditable: Boolean = true,
)

enum class UserAccountAction {
    ProfileUpdated,
    PasswordUpdated,
    AvatarUpdated,
}

enum class UserAccountSubmittingState {
    Idle,
    UpdatingProfile,
    UpdatingPassword,
    UpdatingAvatar,
}

data class UserAccountActionEvent(
    val action: UserAccountAction,
    val state: com.yenaly.han1meviewer.logic.state.WebsiteState<Unit>,
)
