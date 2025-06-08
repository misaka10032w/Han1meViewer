package com.yenaly.han1meviewer.ui.fragment

interface PermissionRequester {
    fun requestStoragePermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit
    )
}