package com.yenaly.han1meviewer.ui.fragment.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.HA1_GITHUB_FORUM_URL
import com.yenaly.han1meviewer.HA1_GITHUB_ISSUE_URL
import com.yenaly.han1meviewer.HA1_GITHUB_RELEASES_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.SettingsActivity
import com.yenaly.han1meviewer.ui.activity.SettingsRouter
import com.yenaly.han1meviewer.ui.fragment.IToolbarFragment
import com.yenaly.han1meviewer.ui.view.pref.HPrivacyPreference
import com.yenaly.han1meviewer.ui.view.pref.MaterialDialogPreference
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.util.setSummaryConverter
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.util.showUpdateDialog
import com.yenaly.han1meviewer.util.showWithBlurEffect
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.base.settings.YenalySettingsFragment
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.folderSize
import com.yenaly.yenaly_libs.utils.formatFileSizeV2
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/07/01 001 14:25
 */
class HomeSettingsFragment : YenalySettingsFragment(R.xml.settings_home),
    IToolbarFragment<SettingsActivity> {

    companion object {
        const val VIDEO_LANGUAGE = "video_language"
        const val PLAYER_SETTINGS = "player_settings"
        const val H_KEYFRAME_SETTINGS = "h_keyframe_settings"
        const val UPDATE = "update"
        const val ABOUT = "about"
        const val CLEAR_CACHE = "clear_cache"
        const val SUBMIT_BUG = "submit_bug"
        const val FORUM = "forum"
        const val NETWORK_SETTINGS = "network_settings"
        const val APPLY_DEEP_LINKS = "apply_deep_links"
        const val DOWNLOAD_SETTINGS = "download_settings"

        const val LAST_UPDATE_POPUP_TIME = "last_update_popup_time"
        const val UPDATE_POPUP_INTERVAL_DAYS = "update_popup_interval_days"
        const val USE_CI_UPDATE_CHANNEL = "use_ci_update_channel"

        const val USE_ANALYTICS = "use_analytics"
    }

    private val videoLanguage
            by safePreference<MaterialDialogPreference>(VIDEO_LANGUAGE)
    private val playerSettings
            by safePreference<Preference>(PLAYER_SETTINGS)
    private val hKeyframeSettings
            by safePreference<Preference>(H_KEYFRAME_SETTINGS)
    private val downloadSettings
            by safePreference<Preference>(DOWNLOAD_SETTINGS)
    private val update
            by safePreference<Preference>(UPDATE)
    private val useCIUpdateChannel
            by safePreference<SwitchPreferenceCompat>(USE_CI_UPDATE_CHANNEL)
    private val updatePopupIntervalDays
            by safePreference<SeekBarPreference>(UPDATE_POPUP_INTERVAL_DAYS)
    private val about
            by safePreference<Preference>(ABOUT)
    private val clearCache
            by safePreference<Preference>(CLEAR_CACHE)
    private val submitBug
            by safePreference<Preference>(SUBMIT_BUG)
    private val forum
            by safePreference<Preference>(FORUM)
    private val networkSettings
            by safePreference<Preference>(NETWORK_SETTINGS)
    private val applyDeepLinks
            by safePreference<Preference>(APPLY_DEEP_LINKS)
    private val useAnalytics
            by safePreference<HPrivacyPreference>(USE_ANALYTICS)

    private var checkUpdateTimes = 0

    override fun onStart() {
        super.onStart()
        (activity as SettingsActivity).setupToolbar()
    }

    override fun onPreferencesCreated(savedInstanceState: Bundle?) {
        videoLanguage.apply {

            // 從 xml 轉移至此
            entries = arrayOf(
                getString(R.string.traditional_chinese),
                getString(R.string.simplified_chinese)
            )
            entryValues = arrayOf("zh-CHT", "zh-CHS")
            // 不能直接用 defaultValue 设置，没效果
            if (value == null) setValueIndex(0)

            setOnPreferenceChangeListener { _, newValue ->
                if (newValue != Preferences.videoLanguage) {
                    requireContext().showAlertDialog {
                        setCancelable(false)
                        setTitle(R.string.attention)
                        setMessage(
                            getString(
                                R.string.restart_or_not_working,
                                getString(R.string.video_language)
                            )
                        )
                        setPositiveButton(R.string.confirm) { _, _ ->
                            ActivityManager.restart(killProcess = true)
                        }
                        setNegativeButton(R.string.cancel, null)
                    }
                }
                return@setOnPreferenceChangeListener true
            }
        }
        playerSettings.setOnPreferenceClickListener {
            SettingsRouter.with(this).navigateWithinSettings(R.id.playerSettingsFragment)
            return@setOnPreferenceClickListener true
        }
        hKeyframeSettings.setOnPreferenceClickListener {
            SettingsRouter.with(this).navigateWithinSettings(R.id.hKeyframeSettingsFragment)
            return@setOnPreferenceClickListener true
        }
        downloadSettings.setOnPreferenceClickListener {
            SettingsRouter.with(this).navigateWithinSettings(R.id.downloadSettingsFragment)
            return@setOnPreferenceClickListener true
        }
        about.apply {
            title = buildString {
                append(getString(R.string.about))
                append(" ")
                append(getString(R.string.hanime_app_name))
            }
            summary = getString(R.string.current_version, "v${BuildConfig.VERSION_NAME}")
        }
        clearCache.apply {
            val cacheDir = context.cacheDir
            var folderSize = cacheDir?.folderSize ?: 0L
            summary = generateClearCacheSummary(folderSize)
            setOnPreferenceClickListener {
                if (folderSize != 0L) {
                    context.showAlertDialog {
                        setTitle(R.string.sure_to_clear)
                        setMessage(R.string.sure_to_clear_cache)
                        setPositiveButton(R.string.confirm) { _, _ ->
                            CoroutineScope(Dispatchers.IO).launch {
                                if (cacheDir?.deleteRecursively() == true) {
                                    folderSize = cacheDir.folderSize
                                    withContext(Dispatchers.Main) {
                                        showShortToast(R.string.clear_success)
                                        summary = generateClearCacheSummary(folderSize)
                                    }
                                } else {
                                    folderSize = cacheDir.folderSize
                                    withContext(Dispatchers.Main) {
                                        showShortToast(R.string.clear_failed)
                                        summary = generateClearCacheSummary(folderSize)
                                    }
                                }
                            }
                        }
                        setNegativeButton(R.string.cancel, null)
                    }
                } else showShortToast(R.string.cache_empty)
                return@setOnPreferenceClickListener true
            }
        }
        submitBug.apply {
            setOnPreferenceClickListener {
                browse(HA1_GITHUB_ISSUE_URL)
                return@setOnPreferenceClickListener true
            }
        }
        forum.apply {
            setOnPreferenceClickListener {
                browse(HA1_GITHUB_FORUM_URL)
                return@setOnPreferenceClickListener true
            }
        }
        networkSettings.apply {
            setOnPreferenceClickListener {
                SettingsRouter.with(this@HomeSettingsFragment)
                    .navigateWithinSettings(R.id.networkSettingsFragment)
                return@setOnPreferenceClickListener true
            }
        }
        applyDeepLinks.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                isVisible = true
                setOnPreferenceClickListener {
                    showApplyDeepLinksDialog(it.context)
                    return@setOnPreferenceClickListener true
                }
            } else {
                isVisible = false
            }
        }
        useCIUpdateChannel.apply {
            setOnPreferenceChangeListener { _, _ ->
                AppViewModel.getLatestVersion()
                return@setOnPreferenceChangeListener true
            }
        }
        updatePopupIntervalDays.apply {
            setSummaryConverter(defValue = 0, converter = ::toIntervalDaysPrettyString)
        }
        useAnalytics.apply {
            setOnPreferenceChangeListener { _, newValue ->
                Firebase.analytics.setAnalyticsCollectionEnabled(newValue as Boolean)
                return@setOnPreferenceChangeListener true
            }
            setOnPreferenceLongClickListener {
                privacyDialog.showWithBlurEffect()
                return@setOnPreferenceLongClickListener true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFlow()
    }

    private fun initFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppViewModel.versionFlow.collect { state ->
                    when (state) {
                        is WebsiteState.Error -> {
                            checkUpdateTimes++
                            update.setSummary(R.string.check_update_failed)
                            update.setOnPreferenceClickListener {
                                if (checkUpdateTimes > 2) {
                                    showUpdateFailedDialog(it.context)
                                } else {
                                    AppViewModel.getLatestVersion()
                                }
                                return@setOnPreferenceClickListener true
                            }
                        }

                        is WebsiteState.Loading -> {
                            update.setSummary(R.string.checking_update)
                            update.onPreferenceClickListener = null
                        }

                        is WebsiteState.Success -> {
                            if (state.info == null) {
                                update.setSummary(R.string.already_latest_update)
                                update.onPreferenceClickListener = null
                            } else {
                                update.summary =
                                    getString(R.string.check_update_success, state.info.version)
                                update.setOnPreferenceClickListener {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        it.context.showUpdateDialog(state.info)
                                    }
                                    return@setOnPreferenceClickListener true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // #issue-124: Support deep links.
    @RequiresApi(Build.VERSION_CODES.S)
    private fun showApplyDeepLinksDialog(context: Context) {
        context.showAlertDialog {
            setTitle(R.string.apply_deep_links)
            setView(R.layout.dialog_apply_deep_links)
            setPositiveButton(R.string.go_to_settings) { _, _ ->
                // #issue-197: 有些手机不支持直接从应用里跳转到深层链接界面
                // 这个权限是一个系统级权限，所以没办法，不支持的手机只能自己找地方开了。
                try {
                    val intent = Intent().apply {
                        action = Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                        addCategory(Intent.CATEGORY_DEFAULT)
                        data = "package:${context.packageName}".toUri()
                        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    }
                    requireActivity().startActivity(intent)
                } catch (e: Exception) {
                    // 竟然还有手机不支持打开的
                    showShortToast(R.string.action_app_open_by_default_settings_not_support)
                    e.printStackTrace()
                }
            }
            setNegativeButton(R.string.cancel, null)
        }
    }

    private fun showUpdateFailedDialog(context: Context) {
        context.showAlertDialog {
            setTitle(R.string.do_not_check_update_again)
            setMessage(getString(R.string.update_failed_tips).trimIndent())
            setPositiveButton(R.string.take_me_to_download) { _, _ ->
                browse(HA1_GITHUB_RELEASES_URL)
            }
            setNegativeButton(R.string.cancel, null)
        }
    }

    private fun generateClearCacheSummary(size: Long): CharSequence {
        return getString(R.string.cache_usage_summary, size.formatFileSizeV2()).parseAsHtml()
//        return spannable {
//            size.formatFileSizeV2().span {
//                style(Typeface.BOLD)
//            }
//            " ".text()
//            getString(R.string.cache_occupy).text()
//        }
    }

    private fun toIntervalDaysPrettyString(value: Int): String {
        val lastUpdatePopupTime = Preferences.lastUpdatePopupTime
        val msg = if (lastUpdatePopupTime == 0L) {
            getString(R.string.no_update_popup_yet)
        } else {
            getString(
                R.string.last_update_popup_check_time,
                Instant.fromEpochSeconds(Preferences.lastUpdatePopupTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .format(LocalDateTime.Formats.ISO)
            )
        }
        return when (value) {
            0 -> getString(R.string.at_any_time)
            else -> getString(R.string.which_days, value)
        } + "\n" + msg
    }

    override fun SettingsActivity.setupToolbar() {
        supportActionBar!!.setTitle(R.string.settings)
    }
}