package com.dreason.frame.core.model

enum class ProxyProtocol {
    SOCKS5,
    SHADOWSOCKS,
    HTTP;

    val displayName: String
        get() = when (this) {
            SOCKS5 -> "SOCKS5"
            SHADOWSOCKS -> "Shadowsocks"
            HTTP -> "HTTP Proxy"
        }
}
