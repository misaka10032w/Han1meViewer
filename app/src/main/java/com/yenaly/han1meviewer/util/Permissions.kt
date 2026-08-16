package com.yenaly.han1meviewer.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalDialogs
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.yenaly_libs.utils.awaitActivityResult
import com.yenaly.yenaly_libs.utils.requestPermission
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.core.net.toUri


/**
 * 请求选择图片或视频
 */
suspend fun Context.pickVisualMedia(type: ActivityResultContracts.PickVisualMedia.VisualMediaType): Uri? =
    awaitActivityResult(
        ActivityResultContracts.PickVisualMedia(),
        PickVisualMediaRequest.Builder().setMediaType(type).build()
    )

/**
 * 獲得發送通知權限
 */
suspend fun Context.requestPostNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = requestPermission(Manifest.permission.POST_NOTIFICATIONS)
        if (!granted) {
            val allow = showPostNotificationPermissionDialog()
            if (!allow) {
                GlobalToasts.show(getString(R.string.msg_deny_download_notification), level = GlobalToasts.ToastLevel.WARNING)
                return false
            }
            requestPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    return true
}

/**
 * 顯示發送通知權限對話框
 */
private suspend fun Context.showPostNotificationPermissionDialog(): Boolean =
    suspendCancellableCoroutine { cont ->
        GlobalDialogs.show(
            GlobalDialogs.ConfirmRequest(
                title = getString(R.string.allow_post_notification),
                message = getString(R.string.reason_for_download_notification),
                confirmText = getString(R.string.allow),
                dismissText = getString(R.string.deny),
                onConfirm = { cont.resume(true) },
                onCancel = { cont.resume(false) },
                onDismissRequest = { cont.resume(false) },
            )
        )
        cont.invokeOnCancellation { GlobalDialogs.dismiss() }
    }

/**
 * 请求安装权限
 */
suspend fun Context.requestInstallPermission(): Boolean {
    if (packageManager.canRequestPackageInstalls()) return true
    val granted = requestPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES)
    if (!granted) {
        val goToSettings = showInstallPermissionDialog()
        if (!goToSettings) return false
        awaitActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:$packageName".toUri(),
            ),
        )
        requestPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES)
    }
    return packageManager.canRequestPackageInstalls()
}

/**
 * 显示安装权限对话框
 */
private suspend fun Context.showInstallPermissionDialog(): Boolean =
    suspendCancellableCoroutine { cont ->
        GlobalDialogs.show(
            GlobalDialogs.ConfirmRequest(
                title = getString(R.string.allow_install_from_unknown_app_sources),
                message = getString(R.string.reason_for_allow_install_from_unknown_app_sources),
                confirmText = getString(R.string.go_to_settings),
                dismissText = getString(R.string.deny),
                onConfirm = { cont.resume(true) },
                onCancel = { cont.resume(false) },
                onDismissRequest = { cont.resume(false) },
            )
        )
        cont.invokeOnCancellation { GlobalDialogs.dismiss() }
    }
