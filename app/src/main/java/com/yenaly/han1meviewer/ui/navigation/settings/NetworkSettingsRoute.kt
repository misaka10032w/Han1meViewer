package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.core.net.toUri
import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.network.DohConfig
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.HanimeNetwork
import com.yenaly.han1meviewer.logout
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.screen.settings.DelayResultUi
import com.yenaly.han1meviewer.ui.screen.settings.DohTestResultUi
import com.yenaly.han1meviewer.ui.screen.settings.NetworkSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.NetworkSettingsUiState
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.utils.applicationContext
import com.yenaly.yenaly_libs.utils.showShortToast
import java.net.InetAddress
import java.util.concurrent.Executors

private const val NETWORK_PROXY_TYPE = "proxy_type"
private const val NETWORK_PROXY_IP = "proxy_ip"
private const val NETWORK_PROXY_PORT = "proxy_port"
private const val NETWORK_DOMAIN_NAME = "domain_name"
private const val NETWORK_SELECTED_BASE_URL = "selectedBaseUrl"
private const val NETWORK_USE_BUILT_IN_HOSTS = "use_built_in_hosts"
private const val NETWORK_CUSTOM_HOSTS_DATA = "custom_hosts_data"
private const val NETWORK_USE_DOH = "use_doh"
private const val NETWORK_DOH_PRESET = "doh_preset"
private const val NETWORK_DOH_CUSTOM_URL = "doh_custom_url"
private const val NETWORK_DOH_BOOTSTRAP_IPS = "doh_bootstrap_ips"
private const val NETWORK_DOH_TIMEOUT_SECONDS = "doh_timeout_seconds"

private enum class DohConflictTarget {
    EnableDoH,
    EnableBuiltInHosts,
}

@Composable
fun NetworkSettingsRouteScreen() {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var currentHost by remember { mutableStateOf(Preferences.baseUrl) }
    var isDelayTesting by remember { mutableStateOf(false) }
    var isDohTesting by remember { mutableStateOf(false) }
    var showDomainRestartConfirm by remember { mutableStateOf(false) }
    var showHostsRestartConfirm by remember { mutableStateOf(false) }
    var showCustomHostsValidationError by remember { mutableStateOf<List<String>?>(null) }
    var showDohConflictConfirm by remember { mutableStateOf(false) }
    var pendingDomainValue by remember { mutableStateOf("") }
    var pendingDohConflictTarget by remember { mutableStateOf(DohConflictTarget.EnableDoH) }
    var pendingDohEnabled by remember { mutableStateOf(Preferences.useDoH) }
    var pendingDohPreset by remember { mutableStateOf(Preferences.dohPreset) }
    var pendingDohCustomUrl by remember { mutableStateOf(Preferences.dohCustomUrl) }
    var pendingDohBootstrapIps by remember { mutableStateOf(Preferences.dohBootstrapIps) }
    var pendingDohTimeoutSeconds by remember { mutableIntStateOf(Preferences.dohTimeoutSeconds) }
    val delayResults = remember { mutableStateListOf<DelayResultUi>() }
    val dohTestResults = remember { mutableStateListOf<DohTestResultUi>() }
    val delayHandler = remember { Handler(Looper.getMainLooper()) }
    val dohHandler = remember { Handler(Looper.getMainLooper()) }
    val executor = remember { Executors.newCachedThreadPool() }
    val uiState = remember(refreshKey, context) { buildNetworkSettingsUiState(context) }
    val networkTimeoutText = stringResource(R.string.network_timeout_text)
    fun stopDelayTest() {
        isDelayTesting = false
        delayHandler.removeCallbacksAndMessages(null)
    }

    fun stopDohTest() {
        isDohTesting = false
        dohHandler.removeCallbacksAndMessages(null)
    }

    fun measureDelay(ip: String): Int {
        return try {
            val start = System.currentTimeMillis()
            val address = InetAddress.getByName(ip)
            val reachable = address.isReachable(2000)
            if (reachable) (System.currentTimeMillis() - start).toInt() else -1
        } catch (_: Exception) {
            -1
        }
    }

    fun testIp(ip: String) {
        if (!isDelayTesting) return
        executor.execute {
            val delay = measureDelay(ip)
            delayHandler.post {
                val index = delayResults.indexOfFirst { it.ip == ip }
                if (index >= 0) {
                    delayResults[index] = DelayResultUi(ip, delay)
                }
            }
        }
    }

    fun scheduleNextTest(ipList: List<String>) {
        if (!isDelayTesting) return
        ipList.forEach(::testIp)
        delayHandler.postDelayed({ scheduleNextTest(ipList) }, 2000)
    }

    fun runDohTest() {
        if (isDohTesting) return
        val host = Preferences.baseUrl.toUri().host ?: applicationContext.getString(R.string.unknow)
        currentHost = Preferences.baseUrl
        dohTestResults.clear()
        isDohTesting = true
        executor.execute {
            val start = System.currentTimeMillis()
            val result = runCatching { HDns().lookupByDoHOnly(host) }
            val delay = (System.currentTimeMillis() - start).toInt()
            dohHandler.post {
                dohTestResults.clear()
                result.onSuccess { list ->
                    dohTestResults.add(
                        DohTestResultUi(
                            host = host,
                            ips = list.mapNotNull { it.hostAddress }.distinct(),
                            delay = delay,
                            message = "",
                        )
                    )
                }.onFailure { throwable ->
                    Log.w("DOH_TEST", "lookup failed for $host: ${throwable.message}")
                    dohTestResults.add(
                        DohTestResultUi(
                            host = host,
                            ips = emptyList(),
                            delay = -1,
                            message = throwable.message?.ifBlank { networkTimeoutText }
                                ?: networkTimeoutText,
                        )
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopDelayTest()
            stopDohTest()
            executor.shutdownNow()
        }
    }

    NetworkSettingsScreen(
        state = uiState,
        domainOptions = buildDomainOptions(context),
        currentHost = currentHost,
        delayResults = delayResults,
        dohTestResults = dohTestResults,
        isDelayTesting = isDelayTesting,
        isDohTesting = isDohTesting,
        proxyType = Preferences.proxyType,
        proxyIp = Preferences.proxyIp,
        proxyPort = Preferences.proxyPort,
        dohEnabled = Preferences.useDoH,
        dohPreset = Preferences.dohPreset,
        dohCustomUrl = Preferences.dohCustomUrl,
        dohBootstrapIps = Preferences.dohBootstrapIps,
        dohTimeoutSeconds = Preferences.dohTimeoutSeconds,
        onDomainChange = { newValue ->
            val origin = Preferences.baseUrl
            if (newValue != origin) {
                pendingDomainValue = newValue
                showDomainRestartConfirm = true
            }
        },
        onUseBuiltInHostsChange = { value ->
            if (value && Preferences.useDoH) {
                showDohConflictConfirm = true
                pendingDohConflictTarget = DohConflictTarget.EnableBuiltInHosts
                return@NetworkSettingsScreen
            }
            Preferences.preferenceSp.edit { putBoolean(NETWORK_USE_BUILT_IN_HOSTS, value) }
            refreshKey++
            showHostsRestartConfirm = true
        },
        onSaveCustomHosts = { data ->
            val errors = HDns.validateCustomHosts(data)
            if (errors.isNotEmpty()) {
                showCustomHostsValidationError = errors
                return@NetworkSettingsScreen
            }
            Preferences.preferenceSp.edit(commit = true) {
                putString(NETWORK_CUSTOM_HOSTS_DATA, data)
            }
            refreshKey++
            if (Preferences.useBuiltInHosts) {
                HanimeNetwork.rebuildNetwork()
            }
        },
        customHostsData = Preferences.customHostsData,
        onSaveDohSettings = { enabled, preset, url, bootstrapIps, timeoutSeconds ->
            pendingDohEnabled = enabled
            pendingDohPreset = preset
            pendingDohCustomUrl = url
            pendingDohBootstrapIps = bootstrapIps
            pendingDohTimeoutSeconds = timeoutSeconds
            if (enabled && Preferences.useBuiltInHosts) {
                showDohConflictConfirm = true
                pendingDohConflictTarget = DohConflictTarget.EnableDoH
                return@NetworkSettingsScreen
            }
            Preferences.preferenceSp.edit(commit = true) {
                putBoolean(NETWORK_USE_DOH, enabled)
                putString(NETWORK_DOH_PRESET, preset)
                putString(NETWORK_DOH_CUSTOM_URL, url)
                putString(NETWORK_DOH_BOOTSTRAP_IPS, bootstrapIps)
                putInt(NETWORK_DOH_TIMEOUT_SECONDS, timeoutSeconds.coerceIn(1, 60))
            }
            currentHost = Preferences.baseUrl
            refreshKey++
            HanimeNetwork.rebuildNetwork()
        },
        onOpenDelayTest = {
            val host =
                Preferences.baseUrl.toUri().host ?: applicationContext.getString(R.string.unknow)
            currentHost = Preferences.baseUrl
            delayResults.clear()
            isDelayTesting = true
            executor.execute {
                val ipList = HDns().getCDNList(host)
                Handler(Looper.getMainLooper()).post {
                    Log.i("delayTest", ipList.toString())
                    delayResults.clear()
                    delayResults.addAll(ipList.map { DelayResultUi(it, -1) })
                    scheduleNextTest(ipList)
                }
            }
        },
        onOpenDohTest = { runDohTest() },
        onDismissDelayTest = { stopDelayTest() },
        onDismissDohTest = { stopDohTest() },
        onApplyProxy = { type, ip, port ->
            val valid = when (type) {
                HProxySelector.TYPE_DIRECT, HProxySelector.TYPE_SYSTEM -> true
                HProxySelector.TYPE_HTTP, HProxySelector.TYPE_SOCKS -> HProxySelector.validateIp(ip) && HProxySelector.validatePort(
                    port
                )

                else -> false
            }
            if (!valid) {
                showShortToast(R.string.invalid_ip_or_port)
                return@NetworkSettingsScreen
            }
            if (type == HProxySelector.TYPE_SOCKS) {
                context.showAlertDialog {
                    setTitle(R.string.warning)
                    setMessage(R.string.mpv_socks5_warning)
                    setPositiveButton(R.string.confirm) { _, _ -> }
                }
            }
            Preferences.preferenceSp.edit(commit = true) {
                putInt(NETWORK_PROXY_TYPE, type)
                putString(NETWORK_PROXY_IP, ip)
                putInt(NETWORK_PROXY_PORT, port)
            }
            HProxySelector.rebuildNetwork()
            HanimeNetwork.rebuildNetwork()
            refreshKey++
        },
    )

    ConfirmDialog(
        visible = showDomainRestartConfirm,
        title = stringResource(R.string.attention),
        message = stringResource(R.string.domain_change_tips).trimIndent(),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        cancelable = false,
        onConfirm = {
            Preferences.preferenceSp.edit(commit = true) {
                putString(NETWORK_DOMAIN_NAME, pendingDomainValue)
                putString(NETWORK_SELECTED_BASE_URL, pendingDomainValue)
            }
            logout()
            ActivityManager.restart(killProcess = true)
        },
        onDismiss = { showDomainRestartConfirm = false },
    )

    ConfirmDialog(
        visible = showHostsRestartConfirm,
        title = stringResource(R.string.attention),
        message = stringResource(R.string.restart_or_not_working, EMPTY_STRING),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        cancelable = false,
        onConfirm = { ActivityManager.restart(killProcess = true) },
        onDismiss = { showHostsRestartConfirm = false },
    )

    val validationErrors = showCustomHostsValidationError
    if (validationErrors != null) {
        AlertDialog(
            onDismissRequest = { showCustomHostsValidationError = null },
            title = { Text(stringResource(R.string.attention)) },
            text = { Text(validationErrors.joinToString("\n")) },
            confirmButton = {
                TextButton(onClick = { showCustomHostsValidationError = null }) {
                    Text(stringResource(R.string.confirm))
                }
            },
        )
    }

    ConfirmDialog(
        visible = showDohConflictConfirm,
        title = stringResource(R.string.attention),
        message = stringResource(R.string.doh_conflict_message),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        cancelable = false,
        onConfirm = {
            Preferences.preferenceSp.edit(commit = true) {
                when (pendingDohConflictTarget) {
                    DohConflictTarget.EnableDoH -> {
                        putBoolean(NETWORK_USE_BUILT_IN_HOSTS, false)
                        putBoolean(NETWORK_USE_DOH, pendingDohEnabled)
                        putString(NETWORK_DOH_PRESET, pendingDohPreset)
                        putString(NETWORK_DOH_CUSTOM_URL, pendingDohCustomUrl)
                        putString(NETWORK_DOH_BOOTSTRAP_IPS, pendingDohBootstrapIps)
                        putInt(NETWORK_DOH_TIMEOUT_SECONDS, pendingDohTimeoutSeconds.coerceIn(1, 60))
                    }

                    DohConflictTarget.EnableBuiltInHosts -> {
                        putBoolean(NETWORK_USE_DOH, false)
                        putBoolean(NETWORK_USE_BUILT_IN_HOSTS, true)
                    }
                }
            }
            showDohConflictConfirm = false
            refreshKey++
            HanimeNetwork.rebuildNetwork()
        },
        onDismiss = { showDohConflictConfirm = false },
    )
}

private fun buildNetworkSettingsUiState(context: Context): NetworkSettingsUiState {
    return NetworkSettingsUiState(
        domainName = Preferences.baseUrl,
        domainDisplay = buildDomainOptions(context).firstOrNull { it.second == Preferences.baseUrl }?.first
            ?: Preferences.baseUrl,
        proxySummary = when (Preferences.proxyType) {
            HProxySelector.TYPE_DIRECT -> context.getString(R.string.direct)
            HProxySelector.TYPE_SYSTEM -> context.getString(R.string.system_proxy)
            HProxySelector.TYPE_HTTP -> context.getString(
                R.string.http_proxy,
                Preferences.proxyIp,
                Preferences.proxyPort
            )

            HProxySelector.TYPE_SOCKS -> context.getString(
                R.string.socks_proxy,
                Preferences.proxyIp,
                Preferences.proxyPort
            )

            else -> context.getString(R.string.direct)
        },
        useBuiltInHosts = Preferences.useBuiltInHosts,
        useDoH = Preferences.useDoH,
        dohSummary = buildDohSummary(context),
        delaySummary = context.getString(R.string.node_latency_sum),
    )
}

private fun buildDohSummary(context: Context): String {
    if (!Preferences.useDoH) return context.getString(R.string.doh_disabled_summary)
    if (Preferences.useBuiltInHosts) return context.getString(R.string.doh_conflict_message)
    val core = if (Preferences.dohPreset == "custom") {
        Preferences.dohCustomUrl.ifBlank { context.getString(R.string.custom) }
    } else {
        DohConfig.selectedPreset().title
    }
    val bootstrap = DohConfig.bootstrapIps().takeIf { it.isNotEmpty() }?.joinToString()
    return if (bootstrap != null) "$core\nBootstrap: $bootstrap" else core
}
