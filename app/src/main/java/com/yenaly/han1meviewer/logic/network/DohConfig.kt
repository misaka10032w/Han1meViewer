package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.Preferences

data class DohPreset(
    val key: String,
    val title: String,
    val url: String,
    val bootstrapIps: List<String>,
)

object DohConfig {
    val presets = listOf(
        DohPreset(
            key = "alidns",
            title = "AliDNS",
            url = "https://dns.alidns.com/dns-query",
            bootstrapIps = listOf("223.5.5.5", "223.6.6.6"),
        ),
        DohPreset(
            key = "dnspod",
            title = "DNSPod",
            url = "https://doh.pub/dns-query",
            bootstrapIps = listOf("1.12.12.12", "120.53.53.53"),
        ),
        DohPreset(
            key = "cloudflare",
            title = "Cloudflare",
            url = "https://cloudflare-dns.com/dns-query",
            bootstrapIps = listOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"),
        ),
    )

    fun selectedPreset(): DohPreset = presets.firstOrNull { it.key == Preferences.dohPreset } ?: presets.first()

    fun customUrl(): String = Preferences.dohCustomUrl.trim()

    fun bootstrapIps(): List<String> {
        val customBootstrapIps = Preferences.dohBootstrapIps
            .split(',', '\n', ';', ' ')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (customBootstrapIps.isNotEmpty()) return customBootstrapIps
        if (Preferences.dohPreset == "custom") return emptyList()
        return selectedPreset().bootstrapIps
    }

    fun timeoutSeconds(): Int = Preferences.dohTimeoutSeconds.coerceIn(1, 60)

    fun resolveUrl(): String? {
        if (!Preferences.useDoH) return null
        return when (Preferences.dohPreset) {
            "custom" -> customUrl().takeIf { it.isNotBlank() }
            else -> selectedPreset().url
        }
    }
}
