package com.yenaly.han1meviewer.logic.network

import android.util.Log
import com.yenaly.han1meviewer.HanimeConstants.HANIME_HOSTNAME
import com.yenaly.han1meviewer.Preferences
import okhttp3.Dns
import java.net.InetAddress

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/10 010 17:01
 */
class HDns : Dns {

    private val dnsMap = mutableMapOf<String, List<InetAddress>>()

    private val useBuiltInHosts = Preferences.useBuiltInHosts

    init {
        if (useBuiltInHosts) {
            dnsMap[HANIME_HOSTNAME[0]] = listOf(
                "104.25.254.167", "172.67.75.184", "172.64.229.154",
                "2606:4700:8dd1::2a46:47f8"
            )
            dnsMap[HANIME_HOSTNAME[1]] = listOf(
                "104.25.254.167", "172.67.75.184", "172.64.229.154",
                "2606:4700:8dd1::2a46:47f8"
            )
        }
    }

    companion object {

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
        return if (useBuiltInHosts) dnsMap[hostname] ?: Dns.SYSTEM.lookup(hostname)
        else Dns.SYSTEM.lookup(hostname)
    }
    fun getCDNList(host: String): List<String> {
        if (useBuiltInHosts) {
            val builtInIps = dnsMap[host]?.mapNotNull { it.hostAddress }?.distinct()
            if (!builtInIps.isNullOrEmpty()) {
                return builtInIps
            }
        }

        return runCatching {
            Dns.SYSTEM.lookup(host).map { it.hostAddress }.distinct()
        }.getOrElse {
            it.printStackTrace()
            emptyList()
        }
    }

}