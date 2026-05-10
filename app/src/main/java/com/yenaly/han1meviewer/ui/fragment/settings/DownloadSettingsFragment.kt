package com.yenaly.han1meviewer.ui.fragment.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.dao.download.HanimeDownloadDao
import com.yenaly.han1meviewer.logic.network.interceptor.SpeedLimitInterceptor
import com.yenaly.han1meviewer.ui.fragment.PermissionRequester
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.settings.DownloadSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.DownloadSettingsUiState
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.SafFileManager.KEY_TREE_URI
import com.yenaly.han1meviewer.util.SafFileManager.checkSafPermissions
import com.yenaly.han1meviewer.util.SafFileManager.migratePrivateToSaf
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.yenaly_libs.utils.formatBytesPerSecond
import com.yenaly.yenaly_libs.utils.showLongToast

class DownloadSettingsFragment : androidx.fragment.app.Fragment() {

    companion object {
        const val DOWNLOAD_PATH = "download_path"
        const val DOWNLOAD_COUNT_LIMIT = "download_count_limit"
        const val DOWNLOAD_SPEED_LIMIT = "download_speed_limit"
        const val USE_PRIVATE_STORAGE = "use_private_storage"
        const val IMPORT_DOWNLOADED_FILE = "import_downloaded_file"
    }

    private val storagePermissionRequester: PermissionRequester?
        get() = activity as? PermissionRequester
    private var uiState by mutableStateOf<DownloadSettingsUiState?>(null)

    private val openDirectoryPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            SafFileManager.persistUriPermission(requireContext(), result.data)
            Preferences.preferenceSp.edit { putBoolean(USE_PRIVATE_STORAGE, false) }
            showLongToast(getString(R.string.directory_saved, result.data))
            uiState = buildUiState()
        } else {
            showLongToast(getString(R.string.no_directory_selected))
        }
    }

    private val dao: HanimeDownloadDao
        get() = DownloadDatabase.instance.hanimeDownloadDao

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.download_settings),
            canNavigateBack = true,
        )
        storagePermissionRequester?.requestStoragePermission(
            onGranted = {},
            onDenied = {
                Toast.makeText(requireContext(), "拒绝？拒绝就不好办了喵👿", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            },
            onPermanentlyDenied = { showGoToSettingsDialog() },
        )
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        uiState = buildUiState()
        setContent {
            HanimeTheme {
                uiState?.let { state ->
                    DownloadSettingsScreen(
                        state = state,
                        maxDownloadCountLimit = 10,
                        maxDownloadSpeedLimitIndex = SpeedLimitInterceptor.SPEED_BYTES.lastIndex,
                        onOpenDownloadPath = ::showDownloadPathDialog,
                        onRestoreDefaultPath = ::restoreDefaultPath,
                        onImportDownloadedFiles = ::importDownloadedFiles,
                        onDownloadCountLimitChange = { value ->
                            Preferences.preferenceSp.edit { putInt(DOWNLOAD_COUNT_LIMIT, value) }
                            HanimeDownloadManagerV2.maxConcurrentDownloadCount = value
                            uiState = buildUiState()
                        },
                        onDownloadSpeedLimitChange = { value ->
                            Preferences.preferenceSp.edit { putInt(DOWNLOAD_SPEED_LIMIT, value) }
                            uiState = buildUiState()
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        uiState = buildUiState()
    }

    private fun buildUiState(): DownloadSettingsUiState {
        val uri = SafFileManager.getSavedUri()
        val pathSummary = if (Preferences.isUsePrivateStorage) {
            requireContext().getExternalFilesDir(null)?.absolutePath.orEmpty()
        } else {
            DocumentFile.fromTreeUri(requireContext(), uri ?: return DownloadSettingsUiState(
                downloadPathSummary = "null",
                downloadCountLimit = Preferences.downloadCountLimit,
                downloadCountLimitSummary = toDownloadCountLimitPrettyString(Preferences.downloadCountLimit),
                downloadSpeedLimitIndex = Preferences.preferenceSp.getInt(DOWNLOAD_SPEED_LIMIT, SpeedLimitInterceptor.NO_LIMIT_INDEX),
                downloadSpeedLimitSummary = SpeedLimitInterceptor.SPEED_BYTES[
                    Preferences.preferenceSp.getInt(DOWNLOAD_SPEED_LIMIT, SpeedLimitInterceptor.NO_LIMIT_INDEX)
                ].toDownloadSpeedPrettyString(),
            ))?.name ?: uri.toString()
        }

        val speedIndex = Preferences.preferenceSp.getInt(DOWNLOAD_SPEED_LIMIT, SpeedLimitInterceptor.NO_LIMIT_INDEX)
        return DownloadSettingsUiState(
            downloadPathSummary = pathSummary,
            downloadCountLimit = Preferences.downloadCountLimit,
            downloadCountLimitSummary = toDownloadCountLimitPrettyString(Preferences.downloadCountLimit),
            downloadSpeedLimitIndex = speedIndex,
            downloadSpeedLimitSummary = SpeedLimitInterceptor.SPEED_BYTES[speedIndex].toDownloadSpeedPrettyString(),
        )
    }

    private fun showDownloadPathDialog() {
        requireContext().showAlertDialog {
            setTitle(getString(R.string.select_download_folder))
            setMessage(getString(R.string.select_folder_message))
            setPositiveButton(R.string.ok) { _, _ ->
                openDirectoryPicker.launch(SafFileManager.buildOpenDirectoryIntent())
            }
            setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            if (!Preferences.isUsePrivateStorage) {
                setNeutralButton(getString(R.string.restore_default_path)) { _, _ ->
                    restoreDefaultPath()
                }
            }
        }
    }

    private fun restoreDefaultPath() {
        if (!Preferences.isUsePrivateStorage) {
            requireContext().showAlertDialog {
                setTitle(getString(R.string.restore_default_path))
                setMessage(getString(R.string.restore_default_message))
                setPositiveButton(R.string.ok) { _, _ ->
                    Preferences.preferenceSp.edit {
                        putBoolean(USE_PRIVATE_STORAGE, true)
                        remove(KEY_TREE_URI)
                    }
                    uiState = buildUiState()
                    showLongToast(getString(R.string.default_path_restored))
                }
                setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            }
        } else {
            requireContext().showAlertDialog {
                setTitle(getString(R.string.already_default_path))
                setMessage(getString(R.string.already_default_message))
                setPositiveButton(getString(R.string.understood)) { _, _ -> }
            }
        }
    }

    private fun importDownloadedFiles() {
        if (!Preferences.isUsePrivateStorage &&
            !Preferences.safDownloadPath.isNullOrBlank() &&
            checkSafPermissions(requireContext())
        ) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.confirm_import))
                .setMessage(getString(R.string.import_warning))
                .setPositiveButton(R.string.ok) { _, _ ->
                    val dialogView = layoutInflater.inflate(R.layout.layout_dialog_progress, null, false)
                    val titleTv = dialogView.findViewById<MaterialTextView>(R.id.progress_title)
                    val percentTv = dialogView.findViewById<MaterialTextView>(R.id.progress_value)
                    val progressBar = dialogView.findViewById<LinearProgressIndicator>(R.id.progress_bar)

                    val progressDialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.import_progress))
                        .setView(dialogView)
                        .setCancelable(false)
                        .create()
                    progressDialog.show()
                    migratePrivateToSaf(requireContext(), dao) { migrated, total ->
                        Log.i("migrate", "$migrated,$total")
                        when (total) {
                            0 -> {
                                progressDialog.dismiss()
                                showLongToast(getString(R.string.no_exportable_files))
                                return@migratePrivateToSaf
                            }

                            -1 -> {
                                progressDialog.dismiss()
                                showLongToast(getString(R.string.permission_error))
                                return@migratePrivateToSaf
                            }
                        }
                        val percent = migrated * 100 / total
                        titleTv.text = getString(R.string.importing)
                        progressBar.max = 100
                        progressBar.progress = percent
                        percentTv.text = getString(R.string.import_progress_format).format(migrated, total, percent)

                        if (migrated == total) {
                            progressDialog.dismiss()
                            showLongToast(getString(R.string.import_complete, total))
                        }
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            requireContext().showAlertDialog {
                setTitle(getString(R.string.specify_path_first))
                setMessage(getString(R.string.path_permission_message))
                setPositiveButton(R.string.understood) { _, _ -> }
            }
        }
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("权限被永久拒绝")
            .setMessage("请前往设置开启存储权限，以便保存下载内容。")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${requireContext().packageName}".toUri()
                }
                startActivity(intent)
            }
            .setNegativeButton("取消") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .show()
    }

    private fun Long.toDownloadSpeedPrettyString(): String {
        return if (this == 0L) {
            getString(R.string.no_limit)
        } else {
            this.formatBytesPerSecond()
        }
    }

    private fun toDownloadCountLimitPrettyString(value: Int): String {
        return if (value == 0) {
            getString(R.string.no_limit)
        } else {
            value.toString()
        }
    }
}
