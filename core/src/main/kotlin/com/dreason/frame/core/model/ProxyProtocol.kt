package com.dreason.frame.core.model

enum class ProxyProtocol {
    VMESS,
    VLESS,
    TROJAN,
    SHADOWSOCKS,
    SOCKS5,
    HTTP;

    val displayName: String
        get() = when (this) {
            VMESS -> "VMess"
            VLESS -> "VLESS"
            TROJAN -> "Trojan"
            SHADOWSOCKS -> "Shadowsocks"
            SOCKS5 -> "SOCKS5"
            HTTP -> "HTTP Proxy"
        }

    val isXray: Boolean
        get() = this in listOf(VMESS, VLESS, TROJAN)
}
