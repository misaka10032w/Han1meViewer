@file:Suppress("DEPRECATION")
package com.yenaly.han1meviewer.ui.fragment.settings

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.mikepenz.aboutlibraries.LibsBuilder
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.HA1_GITHUB_FORUM_URL
import com.yenaly.han1meviewer.HA1_GITHUB_ISSUE_URL
import com.yenaly.han1meviewer.HA1_GITHUB_RELEASES_URL
import com.yenaly.han1meviewer.HanimeApplication
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.SettingsRouter
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.view.pref.HPrivacyPreference
import com.yenaly.han1meviewer.ui.view.pref.MaterialDialogPreference
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.util.ThemeUtils
import com.yenaly.han1meviewer.util.setSummaryConverter
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.util.showUpdateDialog
import com.yenaly.han1meviewer.util.showWithBlurEffect
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.base.preference.MaterialSwitchPreference
import com.yenaly.yenaly_libs.base.settings.YenalySettingsFragment
import com.yenaly.yenaly_libs.utils.application
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.folderSize
import com.yenaly.yenaly_libs.utils.formatFileSizeV2
import com.yenaly.yenaly_libs.utils.showLongToast
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
import kotlin.time.ExperimentalTime

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/07/01 001 14:25
 */
class HomeSettingsFragment : YenalySettingsFragment(R.xml.settings_home) {

    companion object {
        const val VIDEO_LANGUAGE = "video_language"
        const val DEFAULT_VIDEO_QUALITY = "default_video_quality"

        const val ALLOW_PIP_MDOE = "allow_pip_mode"
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
        const val FAKE_LAUNCHER_ICON = "pref_fake_launcher_icon"
        const val USE_DARK_MODE = "use_dark_mode"
        const val USE_DYNAMIC_COLOR = "use_dynamic_color"
        const val ALLOW_RESUME_PLAYBACK = "allow_resume_playback"
    }

    private val videoLanguage
            by safePreference<MaterialDialogPreference>(VIDEO_LANGUAGE)
    private val videoQuality
            by safePreference<MaterialDialogPreference>(DEFAULT_VIDEO_QUALITY)
    private val playerSettings
            by safePreference<Preference>(PLAYER_SETTINGS)
    private val allowPipMode
            by safePreference<Preference>(ALLOW_PIP_MDOE)
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
    private val ossLicense
            by safePreference<Preference>("oss_license")
    private val fakeLauncherIcon
            by safePreference<Preference>(FAKE_LAUNCHER_ICON)
    private val useDarkMode
            by safePreference<MaterialDialogPreference>(USE_DARK_MODE)
    private val useDynamicColor
            by safePreference<MaterialSwitchPreference>(USE_DYNAMIC_COLOR)
    private val allowResumePlayback
            by safePreference<MaterialSwitchPreference>(ALLOW_RESUME_PLAYBACK)

    private var checkUpdateTimes = 0

    data class LauncherItem(
        val name: String,
        @param:DrawableRes val iconRes: Int,
        val alias: String
    )

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.showToolbar()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.settings),
            canNavigateBack = true
        )

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onPreferencesCreated(savedInstanceState: Bundle?) {
        val lockSwitch = findPreference<SwitchPreferenceCompat>("use_lock_screen")
        val items = listOf(
            LauncherItem(getString(R.string.hanime_app_name),
                R.drawable.ic_launcher,
                "com.yenaly.han1meviewer.LauncherAliasDefault"),
            LauncherItem(getString(R.string.app_name_fake_calc),
                R.drawable.ic_launcher_calc,
                "com.yenaly.han1meviewer.LauncherFakeCalc"),
            LauncherItem(getString(R.string.app_name_fake_cornhub),
                R.drawable.ic_launcher_cornhub,
                "com.yenaly.han1meviewer.LauncherFakeCornhub"),
            LauncherItem(getString(R.string.app_name_fake_xxt),
                R.drawable.ic_launcher_xxt,
                "com.yenaly.han1meviewer.LauncherFakeXxt")
        )
        lockSwitch?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                if (!isDeviceSecureCompat(requireContext())) {
                    Toast.makeText(requireContext(),
                        getString(R.string.not_set_sys_lock), Toast.LENGTH_LONG).show()
                    return@setOnPreferenceChangeListener false
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    Toast.makeText(requireContext(),
                        getString(R.string.not_compact_lock_screen), Toast.LENGTH_LONG).show()
                    return@setOnPreferenceChangeListener false
                }
            }
            true
        }
        val langPref = findPreference<ListPreference>("app_language")
        langPref?.setOnPreferenceChangeListener { _, _ ->
            Handler(Looper.getMainLooper()).post {
                activity?.recreate()
            }
            true
        }

        videoLanguage.apply {
            // 從 xml 轉移至此
            entries = arrayOf(
                getString(R.string.traditional_chinese),
                getString(R.string.simplified_chinese)
            )
            entryValues = arrayOf("zht", "zhs")
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
        useDarkMode.apply {
            entries = arrayOf(
                getString(R.string.follow_system),
                getString(R.string.always_off),
                getString(R.string.always_on),
            )
            entryValues = arrayOf("follow_system", "always_off", "always_on")
            // 不能直接用 defaultValue 设置，没效果
            if (value == null) setValueIndex(0)

            setOnPreferenceChangeListener { _, newValue ->
                if (newValue != Preferences.useDarkMode) {
                    Preferences.preferenceSp.edit {
                        putString(USE_DARK_MODE,newValue.toString())
                    }
                    ThemeUtils.applyDarkModeFromPreferences(requireContext())
                    requireActivity().recreate()
                }
                return@setOnPreferenceChangeListener true
            }
        }
        useDynamicColor.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                isEnabled = false
                return@apply
            }
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue != Preferences.useDynamicColor) {
                    requireContext().showAlertDialog {
                        setCancelable(false)
                        setTitle(R.string.attention)
                        setMessage(
                            getString(
                                R.string.restart_or_not_working,
                                getString(R.string.dynamic_color_title)
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
        videoQuality.apply {
            entries = arrayOf(
                "480P",
                "720P",
                "1080P"
            )
            entryValues = arrayOf("480P", "720P","1080P")
            if (value == null) setValueIndex(2)

            setOnPreferenceChangeListener { _, newValue ->
                if (newValue != Preferences.videoQuality) {
                    Toast.makeText(application, "Success：$newValue", Toast.LENGTH_SHORT).show()
                }
                return@setOnPreferenceChangeListener true
            }
        }
        allowPipMode.apply {
            setOnPreferenceChangeListener{ preference: Preference, newValue ->
                val enabled = newValue as Boolean
                if (enabled && !isPipPermissionGranted(requireContext())) {
                    Toast.makeText(requireContext(),
                        getString(R.string.request_pip_alert), Toast.LENGTH_SHORT).show()
                    openPipPermissionSettings(requireContext())
                    Handler(Looper.getMainLooper()).post {
                        (preference as SwitchPreferenceCompat).isChecked = false
                    }
                    false
                } else {
                    true
                }
            }
        }
        allowResumePlayback.apply {
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue != Preferences.allowResumePlayback) {
                    //TODO 可能做点什么？
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
        @Suppress ("DEPRECATION")
        ossLicense.apply {
            setOnPreferenceClickListener {
                LibsBuilder()
                    .withShowLoadingProgress(true)
                    .withSearchEnabled(false)
                    .withActivityTitle(getString(R.string.open_source_license) )
                    .withAboutIconShown(true)
                    .withAboutVersionShown(true)
                    .start(requireContext())
                true
            }
        }

        fakeLauncherIcon.apply {
        //    summary = getString(R.string.select_fake_icon)
            val currentAlias = Preferences.fakeLauncherIcon
            val currentItem = items.find { it.alias == currentAlias } ?: items[0]
            fakeLauncherIcon.summary = currentItem.name
            setOnPreferenceClickListener {
                val adapter = object : ArrayAdapter<LauncherItem>(context, 0, items) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = convertView ?: LayoutInflater.from(context)
                            .inflate(R.layout.item_simple_icon_with_text, parent, false)
                        val item = getItem(position)!!
                        view.findViewById<ImageView>(R.id.icon).setImageResource(item.iconRes)
                        view.findViewById<TextView>(R.id.name).text = item.name
                        return view
                    }
                }
                MaterialAlertDialogBuilder(context)
                    .setTitle(getString(R.string.fake_app_icon))
                    .setAdapter(adapter) { _, which ->
                        val selected = items[which]
                        preferenceManager.sharedPreferences?.edit {
                            putString(FAKE_LAUNCHER_ICON, selected.alias)
                        }
                        val app = context.applicationContext as? HanimeApplication
                        app?.switchLauncher(selected.alias)
                        summary = selected.name
                        showLongToast(getString(R.string.fake_icon_hint))
                    }
                    .show()
                true
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

    @OptIn(ExperimentalTime::class)
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
    private fun isDeviceSecureCompat(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isDeviceSecure
    }

    fun isPipPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            true
        }
    }

    private fun openPipPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS",
                "package:${context.packageName}".toUri())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }
}