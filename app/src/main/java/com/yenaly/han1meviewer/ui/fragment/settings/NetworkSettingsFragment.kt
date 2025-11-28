package com.yenaly.han1meviewer.ui.fragment.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.HanimeConstants.HANIME_HOSTNAME
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.HanimeNetwork
import com.yenaly.han1meviewer.logout
import com.yenaly.han1meviewer.ui.adapter.DelayAdapter
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.view.pref.MaterialDialogPreference
import com.yenaly.han1meviewer.util.createAlertDialog
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.util.showWithBlurEffect
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.base.preference.MaterialSwitchPreference
import com.yenaly.yenaly_libs.base.settings.YenalySettingsFragment
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.unsafeLazy
import java.net.InetAddress
import java.util.concurrent.Executors

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/10 010 18:30
 */
class NetworkSettingsFragment : YenalySettingsFragment(R.xml.settings_network) {

    companion object {
        const val PROXY = "proxy"
        const val PROXY_TYPE = "proxy_type"
        const val PROXY_IP = "proxy_ip"
        const val PROXY_PORT = "proxy_port"
        const val DOMAIN_NAME = "domain_name"
        const val USE_BUILT_IN_HOSTS = "use_built_in_hosts"
        const val DELAY_TEST = "delay_test"
    }

    private val proxy
            by safePreference<Preference>(PROXY)
    private val domainName
            by safePreference<MaterialDialogPreference>(DOMAIN_NAME)
    private val useBuiltInHosts
            by safePreference<MaterialSwitchPreference>(USE_BUILT_IN_HOSTS)
    private val delayTest
            by safePreference<Preference>(DELAY_TEST)
    private val proxyDialog by unsafeLazy {
        ProxyDialog(proxy, R.layout.dialog_proxy)
    }

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.network_settings),
            canNavigateBack = true
        )
    }

    override fun onPreferencesCreated(savedInstanceState: Bundle?) {
        proxy.apply {
            summary = generateProxySummary(
                Preferences.proxyType,
                Preferences.proxyIp,
                Preferences.proxyPort
            )
            setOnPreferenceClickListener {
                proxyDialog.show()
                return@setOnPreferenceClickListener true
            }
        }
        domainName.apply {
            entries = arrayOf(
                "${HANIME_HOSTNAME[0]} (${getString(R.string.default_)})",
                "${HANIME_HOSTNAME[1]} (${getString(R.string.alternative)})",
                "${HANIME_HOSTNAME[2]} (${getString(R.string.alternative)})"

            )
            entryValues = HANIME_URL
            if (value == null) setValueIndex(0)

            setOnPreferenceChangeListener { _, newValue ->
                val origin = Preferences.baseUrl
                if (newValue != origin) {
                    requireContext().showAlertDialog {
                        setCancelable(false)
                        setTitle(R.string.attention)
                        setMessage(getString(R.string.domain_change_tips).trimIndent())
                        setPositiveButton(R.string.confirm) { _, _ ->
                            logout()
                            ActivityManager.restart(killProcess = true)
                        }
                        setNegativeButton(R.string.cancel) { _, _ ->
                            domainName.value = origin
                        }
                    }
                }
                return@setOnPreferenceChangeListener true
            }
        }
        useBuiltInHosts.apply {
            setOnPreferenceChangeListener { _, _ ->
                requireContext().showAlertDialog {
                    setCancelable(false)
                    setTitle(R.string.attention)
                    setMessage(getString(R.string.restart_or_not_working, EMPTY_STRING))
                    setPositiveButton(R.string.confirm) { _, _ ->
                        ActivityManager.restart(killProcess = true)
                    }
                    setNegativeButton(R.string.cancel, null)
                }
                return@setOnPreferenceChangeListener true
            }
        }
        delayTest.apply {
            setOnPreferenceClickListener {
                val currentHost = Preferences.baseUrl.toUri().host ?: getString(R.string.unknow)
                Executors.newSingleThreadExecutor().execute {
                    val hdns = HDns()
                    val ipList = hdns.getCDNList(currentHost)
                    // 回到主线程更新 UI
                    Handler(Looper.getMainLooper()).post {
                        Log.i("delayTest", ipList.toString())
                        val dialog = DelayTestDialog(this@apply, ipList)
                        dialog.show()
                    }
                }
                true
            }
        }

    }

    private fun generateProxySummary(type: Int, ip: String, port: Int): CharSequence {
        return when (type) {
            HProxySelector.TYPE_DIRECT -> getString(R.string.direct)
            HProxySelector.TYPE_SYSTEM -> getString(R.string.system_proxy)
            HProxySelector.TYPE_HTTP -> getString(R.string.http_proxy, ip, port)
            HProxySelector.TYPE_SOCKS -> getString(R.string.socks_proxy, ip, port)
            else -> getString(R.string.direct)
        }
    }
    inner class DelayTestDialog(
        delayTestPref: Preference,
        private val ipList: List<String>
    ) {
        private val dialog: AlertDialog
        private var adapter: DelayAdapter
        private val handler = Handler(Looper.getMainLooper())
        private var isRunning = false

        init {
            val view = LayoutInflater.from(delayTestPref.context)
                .inflate(R.layout.delay_test_dialog, null)
            val tvCurrentHost = view.findViewById<TextView>(R.id.current_host)
            val currentHost = Preferences.baseUrl
            tvCurrentHost.text = currentHost
            val rv = view.findViewById<RecyclerView>(R.id.rv_delay_list)
            adapter = DelayAdapter(ipList)
            rv.layoutManager = LinearLayoutManager(delayTestPref.context)
            rv.adapter = adapter

            dialog = delayTestPref.context.createAlertDialog {
                setTitle(getString(R.string.current_node_latency))
                setCancelable(false)
                setView(view)
                setPositiveButton(R.string.confirm) { _, _ -> stop() }
            }
        }

        fun show() {
            dialog.showWithBlurEffect()
            start()
        }

        private fun start() {
            isRunning = true
            scheduleNextTest()
        }

        private fun stop() {
            isRunning = false
            handler.removeCallbacksAndMessages(null)
        }

        private fun testIp(ip: String) {
            if (!isRunning) return
            Executors.newSingleThreadExecutor().execute {
                val delay = measureDelay(ip)
                handler.post {
                    adapter.updateDelay(ip, delay)
                }
            }
        }
        private fun scheduleNextTest() {
            if (!isRunning) return
            ipList.forEach { ip ->
                testIp(ip)
            }
            handler.postDelayed({ scheduleNextTest() }, 2000)
        }

        private fun measureDelay(ip: String): Int {
            return try {
                val start = System.currentTimeMillis()
                val address = InetAddress.getByName(ip)
                val reachable = address.isReachable(1000)
                if (reachable) (System.currentTimeMillis() - start).toInt() else -1
            } catch (e: Exception) {
                -1
            }
        }
    }

    inner class ProxyDialog(proxyPref: Preference, @LayoutRes layoutRes: Int) {

        private val dialog: AlertDialog

        private val cgTypes: ChipGroup
        private val etIp: TextInputEditText
        private val etPort: TextInputEditText

        init {
            val view = View.inflate(context, layoutRes, null)
            cgTypes = view.findViewById(R.id.cg_types)
            etIp = view.findViewById(R.id.et_ip)
            etPort = view.findViewById(R.id.et_port)
            initView()
            dialog = proxyPref.context.createAlertDialog {
                setView(view)
                setCancelable(false)
                setTitle(R.string.proxy)
                setPositiveButton(R.string.confirm, null) // Set to null. We override the onclick.
                setNegativeButton(R.string.cancel, null)
            }
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val ip = etIp.text?.toString().orEmpty()
                    val port = etPort.text?.toString()?.toIntOrNull() ?: -1
                    val isValid = checkValid(ip, port)
                    if (isValid) {
                        val proxyType = proxyType
                        if (proxyType == HProxySelector.TYPE_SOCKS){
                            showSocksWarning()
                        }
                        Preferences.preferenceSp.edit(commit = true) {
                            putInt(PROXY_TYPE, proxyType)
                            putString(PROXY_IP, ip)
                            putInt(PROXY_PORT, port)
                        }
                        // 重建相关联的所有网络请求
                        HProxySelector.rebuildNetwork()
                        HanimeNetwork.rebuildNetwork()
                        proxyPref.summary = generateProxySummary(proxyType, ip, port)
                        dialog.dismiss()
                    } else {
                        showShortToast("Invalid IP(v4) or Port(0..65535)")
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n")
        private fun initView() {
            when (Preferences.proxyType) {
                HProxySelector.TYPE_DIRECT -> cgTypes.check(R.id.chip_direct)
                HProxySelector.TYPE_SYSTEM -> cgTypes.check(R.id.chip_system_proxy)
                HProxySelector.TYPE_HTTP -> cgTypes.check(R.id.chip_http)
                HProxySelector.TYPE_SOCKS -> cgTypes.check(R.id.chip_socks)
            }
            val prefIp = Preferences.proxyIp
            val prefPort = Preferences.proxyPort
            if (prefIp.isNotBlank() && prefPort != -1) {
                etIp.setText(prefIp)
                etPort.setText(prefPort.toString())
            }
            enableView(cgTypes.checkedChipId)
            cgTypes.setOnCheckedStateChangeListener { _, checkedIds ->
                enableView(checkedIds.first())
            }
        }

        private val proxyType: Int
            get() = when (cgTypes.checkedChipId) {
                R.id.chip_direct -> HProxySelector.TYPE_DIRECT
                R.id.chip_system_proxy -> HProxySelector.TYPE_SYSTEM
                R.id.chip_http -> HProxySelector.TYPE_HTTP
                R.id.chip_socks -> HProxySelector.TYPE_SOCKS
                else -> HProxySelector.TYPE_DIRECT
            }

        private fun checkValid(ip: String, port: Int): Boolean {
            return when (proxyType) {
                HProxySelector.TYPE_DIRECT, HProxySelector.TYPE_SYSTEM -> true
                HProxySelector.TYPE_HTTP, HProxySelector.TYPE_SOCKS -> {
                    HProxySelector.validateIp(ip) && HProxySelector.validatePort(port)
                }

                else -> false
            }
        }

        private fun enableView(@IdRes checkedId: Int) {
            when (checkedId) {
                R.id.chip_direct -> {
                    etIp.isEnabled = false
                    etPort.isEnabled = false
                    etIp.text = null
                    etPort.text = null
                }

                R.id.chip_system_proxy -> {
                    etIp.isEnabled = false
                    etPort.isEnabled = false
                    etIp.text = null
                    etPort.text = null
                }

                R.id.chip_http -> {
                    etIp.isEnabled = true
                    etPort.isEnabled = true
                }

                R.id.chip_socks -> {
                    etIp.isEnabled = true
                    etPort.isEnabled = true
                }
            }
        }

        fun show() {
            initView()
            dialog.showWithBlurEffect()
        }
    }
    private fun showSocksWarning() {
        val context = requireContext()
        context.createAlertDialog {
            setTitle(context.getString(R.string.warning))
            setMessage(context.getString(R.string.mpv_socks5_warning))
            setPositiveButton(R.string.confirm) { _, _ -> }
        }.show()
    }
}
