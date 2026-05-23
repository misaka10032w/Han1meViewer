package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.network.DohConfig
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.ui.component.ChoiceDialog
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

data class NetworkSettingsUiState(
    val domainName: String,
    val domainDisplay: String,
    val proxySummary: String,
    val useBuiltInHosts: Boolean,
    val useDoH: Boolean,
    val dohSummary: String,
    val delaySummary: String,
)

data class DelayResultUi(
    val ip: String,
    val delay: Int,
)

data class DohTestResultUi(
    val host: String,
    val ips: List<String>,
    val delay: Int,
    val message: String,
)

enum class ProxyTypeOption(val value: Int) {
    Direct(HProxySelector.TYPE_DIRECT),
    System(HProxySelector.TYPE_SYSTEM),
    Http(HProxySelector.TYPE_HTTP),
    Socks(HProxySelector.TYPE_SOCKS),
}

@Composable
fun NetworkSettingsScreen(
    state: NetworkSettingsUiState,
    domainOptions: List<Pair<String, String>>,
    currentHost: String,
    delayResults: List<DelayResultUi>,
    isDelayTesting: Boolean,
    proxyType: Int,
    proxyIp: String,
    proxyPort: Int,
    dohEnabled: Boolean,
    dohPreset: String,
    dohCustomUrl: String,
    dohBootstrapIps: String,
    dohTimeoutSeconds: Int,
    dohTestResults: List<DohTestResultUi>,
    isDohTesting: Boolean,
    onDomainChange: (String) -> Unit,
    onUseBuiltInHostsChange: (Boolean) -> Unit,
    onSaveDohSettings: (Boolean, String, String, String, Int) -> Unit,
    onOpenDelayTest: () -> Unit,
    onOpenDohTest: () -> Unit,
    onDismissDelayTest: () -> Unit,
    onDismissDohTest: () -> Unit,
    onApplyProxy: (Int, String, Int) -> Unit,
) {
    var showDomainDialog by rememberSaveable { mutableStateOf(false) }
    var showProxyDialog by rememberSaveable { mutableStateOf(false) }
    var showDohDialog by rememberSaveable { mutableStateOf(false) }

    if (showDomainDialog) {
        NetworkChoiceDialog(
            title = stringResource(R.string.domain_name),
            selectedValue = state.domainName,
            options = domainOptions,
            onDismiss = { showDomainDialog = false },
            onSelect = {
                showDomainDialog = false
                onDomainChange(it)
            },
        )
    }

    if (showProxyDialog) {
        ProxyDialog(
            initialType = proxyType,
            initialIp = proxyIp,
            initialPort = proxyPort,
            onDismiss = { showProxyDialog = false },
            onConfirm = { type, ip, port ->
                showProxyDialog = false
                onApplyProxy(type, ip, port)
            },
        )
    }

    if (showDohDialog) {
        DohDialog(
            enabled = dohEnabled,
            preset = dohPreset,
            customUrl = dohCustomUrl,
            bootstrapIps = dohBootstrapIps,
            timeoutSeconds = dohTimeoutSeconds,
            onDismiss = { showDohDialog = false },
            onConfirm = { enabled, preset, url, bootstrapIps, timeout ->
                showDohDialog = false
                onSaveDohSettings(enabled, preset, url, bootstrapIps, timeout)
            },
        )
    }

    if (isDelayTesting) {
        DelayTestDialog(
            currentHost = currentHost,
            results = delayResults,
            onDismiss = onDismissDelayTest,
        )
    }

    if (isDohTesting) {
        DohTestDialog(
            currentHost = currentHost,
            results = dohTestResults,
            onDismiss = onDismissDohTest,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            SettingNavigationItem(
                title = stringResource(R.string.domain_name),
                valueText = state.domainDisplay,
                iconRes = R.drawable.baseline_domain_24,
                onClick = { showDomainDialog = true },
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.proxy),
                summary = state.proxySummary,
                iconRes = R.drawable.baseline_vpn_24,
                onClick = { showProxyDialog = true },
            )
        }

        item { NetworkGroupTitle(stringResource(R.string.builtin_dns)) }

        item {
            SettingSwitchItem(
                title = stringResource(R.string.use_built_in_hosts),
                summary = stringResource(R.string.use_built_in_hosts_summary),
                checked = state.useBuiltInHosts,
                iconRes = R.drawable.baseline_hosts_24,
                onCheckedChange = onUseBuiltInHostsChange,
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.use_doh),
                summary = state.dohSummary,
                iconRes = R.drawable.baseline_doh_24,
                onClick = { showDohDialog = true },
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.view_node_latency),
                summary = state.delaySummary,
                iconRes = R.drawable.baseline_delay_24,
                onClick = onOpenDelayTest,
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.test_doh),
                summary = stringResource(R.string.test_doh_summary),
                iconRes = R.drawable.baseline_doh_24,
                onClick = onOpenDohTest,
            )
        }
    }
}

@Composable
private fun NetworkChoiceDialog(
    title: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    ChoiceDialog(
        title = title,
        options = options,
        selectedValue = selectedValue,
        onDismiss = onDismiss,
        onSelect = onSelect,
    )
}

@Composable
private fun ProxyDialog(
    initialType: Int,
    initialIp: String,
    initialPort: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Int) -> Unit,
) {
    var selectedType by rememberSaveable(initialType) {
        mutableStateOf(
            ProxyTypeOption.entries.firstOrNull { it.value == initialType }
                ?: ProxyTypeOption.System
        )
    }
    var ip by rememberSaveable(initialIp) { mutableStateOf(initialIp) }
    var portText by rememberSaveable(initialPort) {
        mutableStateOf(initialPort.takeIf { it >= 0 }?.toString().orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.proxy)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProxyTypeOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedType == option,
                                onClick = { selectedType = option },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Checkbox(
                            checked = selectedType == option,
                            onCheckedChange = null,
                        )
                        Text(
                            when (option) {
                                ProxyTypeOption.Direct -> stringResource(R.string.direct)
                                ProxyTypeOption.System -> stringResource(R.string.system_proxy)
                                ProxyTypeOption.Http -> stringResource(R.string.http)
                                ProxyTypeOption.Socks -> stringResource(R.string.socks)
                            }
                        )
                    }
                }
                val editable =
                    selectedType == ProxyTypeOption.Http || selectedType == ProxyTypeOption.Socks
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    enabled = editable,
                    label = { Text(stringResource(R.string.host_or_ipv4)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    enabled = editable,
                    label = { Text(stringResource(R.string.port)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedType.value, ip, portText.toIntOrNull() ?: -1)
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun DelayTestDialog(
    currentHost: String,
    results: List<DelayResultUi>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.current_node_latency)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = currentHost,
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.ip }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(item.ip)
                            Text(
                                text = if (item.delay >= 0) "${item.delay} ms" else stringResource(R.string.network_timeout_text),
                                color = when (item.delay) {
                                    in 0 until 100 -> Color(0xFF4CAF50)
                                    in 100..500 -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun DohTestDialog(
    currentHost: String,
    results: List<DohTestResultUi>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.doh_test_result)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = currentHost,
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.host }) { item ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(item.host)
                            Text(
                                text = item.message.ifBlank {
                                    if (item.delay >= 0) {
                                        "${item.delay} ms"
                                    } else {
                                        stringResource(R.string.network_timeout_text)
                                    }
                                },
                                color = when (item.delay) {
                                    in 0 until 100 -> Color(0xFF4CAF50)
                                    in 100..500 -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                },
                            )
                            if (item.ips.isNotEmpty()) {
                                Text(
                                    text = item.ips.joinToString(),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun DohDialog(
    enabled: Boolean,
    preset: String,
    customUrl: String,
    bootstrapIps: String,
    timeoutSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String, String, String, Int) -> Unit,
) {
    var dohEnabled by rememberSaveable(enabled) { mutableStateOf(enabled) }
    var presetValue by rememberSaveable(preset) {
        mutableStateOf(preset.ifBlank { DohConfig.presets.first().key })
    }
    var customValue by rememberSaveable(customUrl) { mutableStateOf(customUrl) }
    var bootstrapValue by rememberSaveable(bootstrapIps) { mutableStateOf(bootstrapIps) }
    var timeoutValue by rememberSaveable(timeoutSeconds) { mutableStateOf(timeoutSeconds.toString()) }
    var showPresetDialog by rememberSaveable { mutableStateOf(false) }

    if (showPresetDialog) {
        NetworkChoiceDialog(
            title = stringResource(R.string.doh_preset),
            selectedValue = presetValue,
            options = DohConfig.presets.map { it.title to it.key } + (stringResource(R.string.custom) to "custom"),
            onDismiss = { showPresetDialog = false },
            onSelect = {
                presetValue = it
                showPresetDialog = false
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.doh_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = dohEnabled, onClick = { dohEnabled = !dohEnabled })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Checkbox(checked = dohEnabled, onCheckedChange = null)
                    Text(stringResource(R.string.use_doh))
                }

                SettingNavigationItem(
                    title = stringResource(R.string.doh_preset),
                    valueText = DohConfig.presets.firstOrNull { it.key == presetValue }?.title
                        ?: stringResource(R.string.custom),
                    iconRes = R.drawable.baseline_domain_24,
                    onClick = { showPresetDialog = true },
                )

                OutlinedTextField(
                    value = customValue,
                    onValueChange = { customValue = it },
                    label = { Text(stringResource(R.string.doh_custom_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = bootstrapValue,
                    onValueChange = { bootstrapValue = it },
                    label = { Text(stringResource(R.string.doh_bootstrap_ips)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.doh_bootstrap_ips_summary)) },
                )

                OutlinedTextField(
                    value = timeoutValue,
                    onValueChange = { timeoutValue = it.filter(Char::isDigit).take(2) },
                    label = { Text(stringResource(R.string.doh_timeout_seconds)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.doh_timeout_seconds_summary)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    dohEnabled,
                    presetValue,
                    customValue.trim(),
                    bootstrapValue.trim(),
                    timeoutValue.toIntOrNull()?.coerceIn(1, 60) ?: 10,
                )
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun NetworkGroupTitle(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        HorizontalDivider()
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun NetworkSettingsScreenPreview() {
    ComponentPreview {
        NetworkSettingsScreen(
            state = NetworkSettingsUiState(
                domainName = "https://hanime1.me/",
                domainDisplay = "hanime1.me (默认)",
                proxySummary = "系统代理",
                useBuiltInHosts = false,
                useDoH = false,
                dohSummary = "关闭",
                delaySummary = "启用内建Hosts后可侦测延迟状况\n不启用为实际解析位址",
            ),
            domainOptions = listOf(
                "hanime1.me (默认)" to "https://hanime1.me/",
                "hanime1.com (备用)" to "https://hanime1.com/",
            ),
            currentHost = "https://hanime1.me/",
            delayResults = listOf(
                DelayResultUi("1.1.1.1", 82),
                DelayResultUi("8.8.8.8", 164),
                DelayResultUi("9.9.9.9", -1),
            ),
            dohTestResults = listOf(
                DohTestResultUi("hanime1.me", listOf("1.1.1.1"), 82, ""),
            ),
            isDelayTesting = false,
            isDohTesting = false,
            proxyType = HProxySelector.TYPE_SYSTEM,
            proxyIp = "",
            proxyPort = -1,
            dohEnabled = false,
            dohPreset = "cloudflare",
            dohCustomUrl = "",
            dohBootstrapIps = "1.1.1.1, 8.8.8.8",
            dohTimeoutSeconds = 10,
            onDomainChange = {},
            onUseBuiltInHostsChange = {},
            onSaveDohSettings = { _, _, _, _, _ -> },
            onOpenDelayTest = {},
            onOpenDohTest = {},
            onDismissDelayTest = {},
            onDismissDohTest = {},
            onApplyProxy = { _, _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DelayTestDialogPreview() {
    ComponentPreview {
        DelayTestDialog(
            currentHost = "https://hanime1.me/",
            results = listOf(
                DelayResultUi("1.1.1.1", 82),
                DelayResultUi("8.8.8.8", 164),
                DelayResultUi("9.9.9.9", -1),
            ),
            onDismiss = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProxyDialogPreview() {
    ComponentPreview {
        ProxyDialog(
            initialType = 1,
            initialIp = "1.1.1.1",
            initialPort = 8080,
            onDismiss = { },
            onConfirm = { _, _, _ -> },
        )
    }
}
