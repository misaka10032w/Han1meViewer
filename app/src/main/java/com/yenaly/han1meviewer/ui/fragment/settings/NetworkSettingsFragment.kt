package com.yenaly.han1meviewer.ui.fragment.settings

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import androidx.core.net.toUri
import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.HanimeConstants.HANIME_HOSTNAME
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.HanimeNetwork
import com.yenaly.han1meviewer.logout
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.settings.DelayResultUi
import com.yenaly.han1meviewer.ui.screen.settings.NetworkSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.NetworkSettingsUiState
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.utils.showShortToast
import java.net.InetAddress
import java.util.concurrent.Executors

class NetworkSettingsFragment : androidx.fragment.app.Fragment() {

    companion object {
        const val PROXY = "proxy"
        const val PROXY_TYPE = "proxy_type"
        const val PROXY_IP = "proxy_ip"
        const val PROXY_PORT = "proxy_port"
        const val DOMAIN_NAME = "domain_name"
        const val SELECTED_BASE_URL = "selectedBaseUrl"
        const val USE_BUILT_IN_HOSTS = "use_built_in_hosts"
        const val DELAY_TEST = "delay_test"
    }

    private var uiState by mutableStateOf<NetworkSettingsUiState?>(null)
    private var currentHost by mutableStateOf(Preferences.baseUrl)
    private var isDelayTesting by mutableStateOf(false)
    private val delayResults = mutableStateListOf<DelayResultUi>()
    private val delayHandler = Handler(Looper.getMainLooper())

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.network_settings),
            canNavigateBack = true,
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
                NetworkSettingsScreen(
                    state = state,
                    domainOptions = buildDomainOptions(),
                    currentHost = currentHost,
                    delayResults = delayResults,
                    isDelayTesting = isDelayTesting,
                    proxyType = Preferences.proxyType,
                    proxyIp = Preferences.proxyIp,
                    proxyPort = Preferences.proxyPort,
                    onDomainChange = ::onDomainChange,
                    onUseBuiltInHostsChange = ::onUseBuiltInHostsChange,
                    onOpenDelayTest = ::startDelayTest,
                        onDismissDelayTest = ::stopDelayTest,
                        onApplyProxy = ::applyProxy,
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stopDelayTest()
    }

    private fun buildUiState(): NetworkSettingsUiState {
        return NetworkSettingsUiState(
            domainName = Preferences.baseUrl,
            domainDisplay = buildDomainOptions().firstOrNull { it.second == Preferences.baseUrl }?.first ?: Preferences.baseUrl,
            proxySummary = generateProxySummary(
                Preferences.proxyType,
                Preferences.proxyIp,
                Preferences.proxyPort,
            ),
            useBuiltInHosts = Preferences.useBuiltInHosts,
            delaySummary = getString(R.string.node_latency_sum),
        )
    }

    private fun buildDomainOptions(): List<Pair<String, String>> = listOf(
        "${HANIME_HOSTNAME[0]} (${getString(R.string.default_)})" to HANIME_URL[0],
        "${HANIME_HOSTNAME[1]} (${getString(R.string.alternative)})" to HANIME_URL[1],
        "${HANIME_HOSTNAME[2]} (${getString(R.string.alternative)})" to HANIME_URL[2],
        "${HANIME_HOSTNAME[3]} (av)" to HANIME_URL[3],
    )

    private fun onDomainChange(newValue: String) {
        val origin = Preferences.baseUrl
        if (newValue != origin) {
            requireContext().showAlertDialog {
                setCancelable(false)
                setTitle(R.string.attention)
                setMessage(getString(R.string.domain_change_tips).trimIndent())
                setPositiveButton(R.string.confirm) { _, _ ->
                    Preferences.preferenceSp.edit(commit = true) {
                        putString(DOMAIN_NAME, newValue)
                        putString(SELECTED_BASE_URL, newValue)
                    }
                    logout()
                    ActivityManager.restart(killProcess = true)
                }
                setNegativeButton(R.string.cancel, null)
            }
        }
    }

    private fun onUseBuiltInHostsChange(value: Boolean) {
        Preferences.preferenceSp.edit { putBoolean(USE_BUILT_IN_HOSTS, value) }
        uiState = buildUiState()
        requireContext().showAlertDialog {
            setCancelable(false)
            setTitle(R.string.attention)
            setMessage(getString(R.string.restart_or_not_working, EMPTY_STRING))
            setPositiveButton(R.string.confirm) { _, _ ->
                ActivityManager.restart(killProcess = true)
            }
            setNegativeButton(R.string.cancel, null)
        }
    }

    private fun applyProxy(type: Int, ip: String, port: Int) {
        val valid = when (type) {
            HProxySelector.TYPE_DIRECT, HProxySelector.TYPE_SYSTEM -> true
            HProxySelector.TYPE_HTTP, HProxySelector.TYPE_SOCKS -> HProxySelector.validateIp(ip) && HProxySelector.validatePort(port)
            else -> false
        }
        if (!valid) {
            showShortToast("Invalid IP(v4) or Port(0..65535)")
            return
        }
        if (type == HProxySelector.TYPE_SOCKS) {
            showSocksWarning()
        }
        Preferences.preferenceSp.edit(commit = true) {
            putInt(PROXY_TYPE, type)
            putString(PROXY_IP, ip)
            putInt(PROXY_PORT, port)
        }
        HProxySelector.rebuildNetwork()
        HanimeNetwork.rebuildNetwork()
        uiState = buildUiState()
    }

    private fun startDelayTest() {
        val host = Preferences.baseUrl.toUri().host ?: getString(R.string.unknow)
        currentHost = Preferences.baseUrl
        delayResults.clear()
        isDelayTesting = true
        Executors.newSingleThreadExecutor().execute {
            val ipList = HDns().getCDNList(host)
            Handler(Looper.getMainLooper()).post {
                Log.i("delayTest", ipList.toString())
                delayResults.clear()
                delayResults.addAll(ipList.map { DelayResultUi(it, -1) })
                scheduleNextTest(ipList)
            }
        }
    }

    private fun stopDelayTest() {
        isDelayTesting = false
        delayHandler.removeCallbacksAndMessages(null)
    }

    private fun scheduleNextTest(ipList: List<String>) {
        if (!isDelayTesting) return
        ipList.forEach { ip -> testIp(ip) }
        delayHandler.postDelayed({ scheduleNextTest(ipList) }, 2000)
    }

    private fun testIp(ip: String) {
        if (!isDelayTesting) return
        Executors.newSingleThreadExecutor().execute {
            val delay = measureDelay(ip)
            delayHandler.post {
                val index = delayResults.indexOfFirst { it.ip == ip }
                if (index >= 0) {
                    delayResults[index] = DelayResultUi(ip, delay)
                }
            }
        }
    }

    private fun measureDelay(ip: String): Int {
        return try {
            val start = System.currentTimeMillis()
            val address = InetAddress.getByName(ip)
            val reachable = address.isReachable(2000)
            if (reachable) (System.currentTimeMillis() - start).toInt() else -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun generateProxySummary(type: Int, ip: String, port: Int): String {
        return when (type) {
            HProxySelector.TYPE_DIRECT -> getString(R.string.direct)
            HProxySelector.TYPE_SYSTEM -> getString(R.string.system_proxy)
            HProxySelector.TYPE_HTTP -> getString(R.string.http_proxy, ip, port)
            HProxySelector.TYPE_SOCKS -> getString(R.string.socks_proxy, ip, port)
            else -> getString(R.string.direct)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun showSocksWarning() {
        requireContext().showAlertDialog {
            setTitle(R.string.warning)
            setMessage(R.string.mpv_socks5_warning)
            setPositiveButton(R.string.confirm) { _, _ -> }
        }
    }
}
