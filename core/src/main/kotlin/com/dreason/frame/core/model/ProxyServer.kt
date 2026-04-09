package com.dreason.frame.core.model

data class ProxyServer(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val protocol: ProxyProtocol,
    val username: String? = null,
    val password: String? = null,
    val encryptMethod: String? = null,
    val routeGroup: Route,
    val enabled: Boolean = true,
    val latencyMs: Int? = null,
)
