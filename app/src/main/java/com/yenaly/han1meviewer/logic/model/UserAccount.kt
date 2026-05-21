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
