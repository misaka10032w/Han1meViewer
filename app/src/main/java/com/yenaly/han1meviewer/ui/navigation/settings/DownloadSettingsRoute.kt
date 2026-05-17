package com.yenaly.han1meviewer.ui.navigation.settings

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.network.interceptor.SpeedLimitInterceptor
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.TripleButtonDialog
import com.yenaly.han1meviewer.ui.screen.settings.DownloadSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.DownloadSettingsUiState
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.SafFileManager.KEY_TREE_URI
import com.yenaly.han1meviewer.util.showToast
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2

private const val DOWNLOAD_COUNT_LIMIT = "download_count_limit"
private const val DOWNLOAD_SPEED_LIMIT = "download_speed_limit"
private const val DOWNLOAD_USE_PRIVATE_STORAGE = "use_private_storage"

@Composable
fun DownloadSettingsRouteScreen(
    activity: MainActivity,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var showDownloadPathDialog by remember { mutableStateOf(false) }
    var showRestoreDefaultConfirm by remember { mutableStateOf(false) }
    val dao = remember { DownloadDatabase.instance.hanimeDownloadDao }
    val uiState = remember(refreshKey, context) { buildDownloadSettingsUiState(context) }

    val openDirectoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            SafFileManager.persistUriPermission(context, result.data)
            Preferences.preferenceSp.edit { putBoolean(DOWNLOAD_USE_PRIVATE_STORAGE, false) }
            context.showToast(R.string.directory_saved, result.data.toString())
            refreshKey++
        } else {
            context.showToast(R.string.no_directory_selected)
        }
    }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) return@rememberLauncherForActivityResult
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (activity.shouldShowRequestPermissionRationale(permission)) {
            context.showToast(R.string.storage_permission_denied_toast)
        } else {
            AlertDialog.Builder(context)
                .setTitle(R.string.permission_permanently_denied_title)
                .setMessage(R.string.storage_permission_settings_message)
                .setPositiveButton(R.string.go_to_settings) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermission.launch(permission)
            }
        }
    }

    DownloadSettingsScreen(
        state = uiState,
        maxDownloadCountLimit = 10,
        maxDownloadSpeedLimitIndex = SpeedLimitInterceptor.SPEED_BYTES.lastIndex,
        onOpenDownloadPath = { showDownloadPathDialog = true },
        onRestoreDefaultPath = { },
        onImportDownloadedFiles = {
            importDownloadedFiles(context, activity, dao, onCompleted = { refreshKey++ })
        },
        onDownloadCountLimitChange = { value ->
            Preferences.preferenceSp.edit { putInt(DOWNLOAD_COUNT_LIMIT, value) }
            HanimeDownloadManagerV2.maxConcurrentDownloadCount = value
            refreshKey++
        },
        onDownloadSpeedLimitChange = { value ->
            Preferences.preferenceSp.edit { putInt(DOWNLOAD_SPEED_LIMIT, value) }
            refreshKey++
        },
    )

    if (!Preferences.isUsePrivateStorage) {
        TripleButtonDialog(
            visible = showDownloadPathDialog,
            title = stringResource(R.string.select_download_folder),
            message = stringResource(R.string.select_folder_message),
            negativeText = stringResource(R.string.cancel),
            neutralText = stringResource(R.string.restore_default_path),
            positiveText = stringResource(R.string.ok),
            onNegative = { showDownloadPathDialog = false },
            onNeutral = {
                showDownloadPathDialog = false
                showRestoreDefaultConfirm = true
            },
            onPositive = {
                showDownloadPathDialog = false
                openDirectoryPicker.launch(SafFileManager.buildOpenDirectoryIntent())
            },
            onDismiss = { showDownloadPathDialog = false },
        )
    } else {
        ConfirmDialog(
            visible = showDownloadPathDialog,
            title = stringResource(R.string.select_download_folder),
            message = stringResource(R.string.select_folder_message),
            confirmText = stringResource(R.string.ok),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                showDownloadPathDialog = false
                openDirectoryPicker.launch(SafFileManager.buildOpenDirectoryIntent())
            },
            onDismiss = { showDownloadPathDialog = false },
        )
    }

    ConfirmDialog(
        visible = showRestoreDefaultConfirm,
        title = stringResource(R.string.restore_default_path),
        message = stringResource(R.string.restore_default_message),
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            Preferences.preferenceSp.edit {
                putBoolean(DOWNLOAD_USE_PRIVATE_STORAGE, true)
                remove(KEY_TREE_URI)
            }
            refreshKey++
            showRestoreDefaultConfirm = false
            context.showToast(R.string.default_path_restored)
        },
        onDismiss = { showRestoreDefaultConfirm = false },
    )
}

private fun buildDownloadSettingsUiState(context: Context): DownloadSettingsUiState {
    val uri = SafFileManager.getSavedUri()
    val pathSummary = if (Preferences.isUsePrivateStorage) {
        context.getExternalFilesDir(null)?.absolutePath.orEmpty()
    } else {
        DocumentFile.fromTreeUri(
            context,
            uri ?: return DownloadSettingsUiState(
                downloadPathSummary = context.getString(R.string.unknown_error),
                downloadCountLimit = Preferences.downloadCountLimit,
                downloadCountLimitSummary = toDownloadCountLimitPrettyString(
                    context,
                    Preferences.downloadCountLimit
                ),
                downloadSpeedLimitIndex = Preferences.preferenceSp.getInt(
                    DOWNLOAD_SPEED_LIMIT,
                    SpeedLimitInterceptor.NO_LIMIT_INDEX,
                ),
                downloadSpeedLimitSummary = SpeedLimitInterceptor.SPEED_BYTES[
                    Preferences.preferenceSp.getInt(
                        DOWNLOAD_SPEED_LIMIT,
                        SpeedLimitInterceptor.NO_LIMIT_INDEX,
                    )
                ].toDownloadSpeedPrettyString(context),
            )
        )?.name ?: uri.toString()
    }
    val speedIndex = Preferences.preferenceSp.getInt(
        DOWNLOAD_SPEED_LIMIT,
        SpeedLimitInterceptor.NO_LIMIT_INDEX,
    )
    return DownloadSettingsUiState(
        downloadPathSummary = pathSummary,
        downloadCountLimit = Preferences.downloadCountLimit,
        downloadCountLimitSummary = toDownloadCountLimitPrettyString(
            context,
            Preferences.downloadCountLimit
        ),
        downloadSpeedLimitIndex = speedIndex,
        downloadSpeedLimitSummary = SpeedLimitInterceptor.SPEED_BYTES[speedIndex]
            .toDownloadSpeedPrettyString(context),
    )
}
