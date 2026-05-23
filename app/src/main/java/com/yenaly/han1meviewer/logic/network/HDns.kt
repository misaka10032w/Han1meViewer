package com.yenaly.han1meviewer.logic.network

import android.util.Log
import com.yenaly.han1meviewer.HanimeConstants.HANIME_HOSTNAME
import com.yenaly.han1meviewer.Preferences
import okhttp3.Dns
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/10 010 17:01
 */
class HDns : Dns {

    private data class DohRuntimeConfig(
        val url: String,
        val bootstrapIps: List<String>,
        val timeoutSeconds: Int,
    )

    @Volatile
    private var cachedDohConfig: DohRuntimeConfig? = null

    @Volatile
    private var cachedDohDns: Dns? = null

    companion object {

        private val cloudFlareIps = listOf(
            "172.64.229.154", "104.25.254.167", "172.67.75.184", "104.21.7.20", "172.67.187.141",
            "2606:4700:8dd1::2a46:47f8", "2606:4700:3031::ac43:bb8d", "2606:4700:3030::6815:746"
        )

        /**
         * 添加DNS
         */
        private operator fun MutableMap<String, List<InetAddress>>.set(
            host: String, ips: List<String>,
        ) {
            this[host] = ips.map {
                InetAddress.getByAddress(host, InetAddress.getByName(it).address)
            }
        }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        if (Preferences.useBuiltInHosts && HANIME_HOSTNAME.contains(hostname)) {
            return cloudFlareIps.map {
                InetAddress.getByAddress(hostname, InetAddress.getByName(it).address)
            }
        }

        val dohUrl = DohConfig.resolveUrl()
        if (!dohUrl.isNullOrBlank()) {
            return runCatching { lookupByDoH(dohUrl, hostname) }
                .getOrElse {
                    Log.w("DOH", "lookup failed for $hostname: ${it.message}")
                    Dns.SYSTEM.lookup(hostname)
                }
        }

        return Dns.SYSTEM.lookup(hostname)
    }

    private fun lookupByDoH(dohUrl: String, hostname: String): List<InetAddress> {
        val config = DohRuntimeConfig(
            url = dohUrl,
            bootstrapIps = DohConfig.bootstrapIps(),
            timeoutSeconds = DohConfig.timeoutSeconds(),
        )
        val dns = getOrCreateDohDns(config)
        return dns.lookup(hostname).also {
            Log.i("DOH", it.toString())
        }
    }

    fun lookupByDoHOnly(hostname: String): List<InetAddress> {
        val dohUrl = DohConfig.resolveUrl() ?: error("DoH is disabled")
        return lookupByDoH(dohUrl, hostname)
    }

    private fun getOrCreateDohDns(config: DohRuntimeConfig): Dns {
        val currentDns = cachedDohDns
        if (currentDns != null && cachedDohConfig == config) return currentDns

        synchronized(this) {
            val dnsAgain = cachedDohDns
            if (dnsAgain != null && cachedDohConfig == config) return dnsAgain

            val client = OkHttpClient.Builder()
                .connectTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .readTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .build()
            val bootstrapHosts = config.bootstrapIps.mapNotNull { ip ->
                runCatching { InetAddress.getByName(ip) }.getOrNull()
            }
            val dnsBuilder = DnsOverHttps.Builder()
                .client(client)
                .url(config.url.toHttpUrl())
                .includeIPv6(true)
                .post(false)
                .resolvePrivateAddresses(true)
                .resolvePublicAddresses(true)
            if (bootstrapHosts.isNotEmpty()) {
                dnsBuilder.bootstrapDnsHosts(bootstrapHosts)
            }
            val dns = dnsBuilder.build()

            cachedDohConfig = config
            cachedDohDns = dns
            return dns
        }
    }

    fun getCDNList(host: String): List<String> {
        if (Preferences.useBuiltInHosts && HANIME_HOSTNAME.contains(host)) {
            return cloudFlareIps.distinct()
        }

        return runCatching {
            Dns.SYSTEM.lookup(host).map { it.hostAddress }.distinct()
        }.getOrElse {
            it.printStackTrace()
            emptyList()
        }
    }

}
