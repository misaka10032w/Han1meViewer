package com.yenaly.han1meviewer.ui.fragment.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.SeekBarPreference
import com.yenaly.han1meviewer.HFileManager
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.network.interceptor.SpeedLimitInterceptor
import com.yenaly.han1meviewer.ui.fragment.PermissionRequester
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.util.setSummaryConverter
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.yenaly_libs.base.preference.LongClickablePreference
import com.yenaly.yenaly_libs.base.settings.YenalySettingsFragment
import com.yenaly.yenaly_libs.utils.copyToClipboard
import com.yenaly.yenaly_libs.utils.formatBytesPerSecond
import com.yenaly.yenaly_libs.utils.showShortToast
import androidx.core.net.toUri

class DownloadSettingsFragment : YenalySettingsFragment(R.xml.settings_download){

    companion object {
        const val DOWNLOAD_PATH = "download_path"
        const val DOWNLOAD_COUNT_LIMIT = "download_count_limit"
        const val DOWNLOAD_SPEED_LIMIT = "download_speed_limit"
    }

    private val downloadPath
            by safePreference<LongClickablePreference>(DOWNLOAD_PATH)
    private val downloadCountLimit
            by safePreference<SeekBarPreference>(DOWNLOAD_COUNT_LIMIT)
    private val downloadSpeedLimit
            by safePreference<SeekBarPreference>(DOWNLOAD_SPEED_LIMIT)
    private val storagePermissionRequester: PermissionRequester?
        get() = activity as? PermissionRequester


    override fun onStart() {
        super.onStart()
 //       (activity as SettingsActivity).setupToolbar()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.download_settings),
            canNavigateBack = true
        )
        storagePermissionRequester?.requestStoragePermission(
            onGranted = {
                // 用户已授权，可以继续
                Toast.makeText(requireContext(), "可以下载了喵\uD83D\uDC7F", Toast.LENGTH_SHORT).show()
            },
            onDenied = {
                // 拒绝授权，返回上一层
                Toast.makeText(requireContext(), "拒绝？拒绝就不好办了喵👿", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            },
            onPermanentlyDenied = {
                // 用户选择“不再询问”，引导去设置页
                showGoToSettingsDialog()
            }
        )
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

    override fun onPreferencesCreated(savedInstanceState: Bundle?) {
        downloadPath.apply {
            val path = HFileManager.getAppDownloadFolder(context).path
            summary = path
            setOnPreferenceClickListener {
                requireContext().showAlertDialog {
                    setTitle(R.string.not_allow_to_change)
                    setMessage(
                        getString(R.string.detailed_path_s, path) + "\n"
                                + getString(R.string.long_press_pref_to_copy)
                    )
                    setPositiveButton(R.string.ok, null)
                }
                return@setOnPreferenceClickListener true
            }
            setOnPreferenceLongClickListener {
                path.copyToClipboard()
                showShortToast(R.string.copy_to_clipboard)
                return@setOnPreferenceLongClickListener true
            }
        }
        downloadCountLimit.apply {
            setSummaryConverter(
                defValue = HanimeDownloadManagerV2.MAX_CONCURRENT_DOWNLOAD_DEF,
                converter = ::toDownloadCountLimitPrettyString
            ) {
                // HanimeDownloadManager.maxConcurrentDownloadCount = it
                HanimeDownloadManagerV2.maxConcurrentDownloadCount = it
            }
        }
        downloadSpeedLimit.apply {
            min = 0
            max = SpeedLimitInterceptor.SPEED_BYTES.lastIndex
            setSummaryConverter(defValue = SpeedLimitInterceptor.NO_LIMIT_INDEX, converter = { i ->
                SpeedLimitInterceptor.SPEED_BYTES[i].toDownloadSpeedPrettyString()
            })
        }
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