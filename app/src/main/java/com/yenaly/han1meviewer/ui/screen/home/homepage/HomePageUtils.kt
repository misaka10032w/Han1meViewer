package com.yenaly.han1meviewer.ui.screen.home.homepage

import com.yenaly.han1meviewer.R
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

internal fun HomeCategory.toAdvancedSearchParams(): Map<String, String> = buildMap {
    genre?.let { put("genre", it) }
    sort?.let { put("sort", it) }
    tags?.let { put("tags", it) }
}

internal fun Throwable.toHomePageErrorMessageRes(): Int {
    val rawMessage = message.orEmpty().lowercase()
    return when {
        this is UnknownHostException ||
                rawMessage.contains("unable to resolve host") ||
                rawMessage.contains("no address associated with hostname") -> {
            R.string.home_error_dns
        }

        this is SocketTimeoutException || rawMessage.contains("timeout") -> {
            R.string.home_error_timeout
        }

        this is SSLHandshakeException ||
                rawMessage.contains("ssl") ||
                rawMessage.contains("certificate") -> {
            R.string.home_error_ssl
        }

        this is ConnectException || rawMessage.contains("failed to connect") -> {
            R.string.home_error_connect
        }

        this is SocketException && rawMessage.contains("connection reset") -> {
            R.string.home_error_connection_interrupted
        }

        rawMessage.contains("connection reset") -> {
            R.string.home_error_connection_reset
        }

        rawMessage.contains("403") -> {
            R.string.home_error_forbidden
        }

        rawMessage.contains("404") -> {
            R.string.home_error_not_found
        }

        rawMessage.contains("500") || rawMessage.contains("502") ||
                rawMessage.contains("503") || rawMessage.contains("504") -> {
            R.string.home_error_server_unavailable
        }

        else -> {
            R.string.home_error_generic
        }
    }
}
