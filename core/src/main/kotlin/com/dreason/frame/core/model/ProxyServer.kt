package com.dreason.frame.core.model

/**
 * Complete proxy server configuration supporting all protocols.
 *
 * For Xray protocols (VMess/VLESS/Trojan):
 * - uuid: Required. The user UUID
 * - transport: Transport type (TCP/WS/gRPC/H2/QUIC)
 * - security: TLS/Reality/None
 * - wsPath: WebSocket path (when transport = WS)
 * - wsHost: WebSocket host header
 * - grpcServiceName: gRPC service name (when transport = gRPC)
 * - sni: TLS Server Name Indication
 * - fingerprint: TLS client fingerprint (chrome/firefox/safari/random)
 * - alpn: ALPN protocols (e.g., "h2,http/1.1")
 * - realityPublicKey: Reality public key
 * - realityShortId: Reality short ID
 * - vmessEncryption: VMess encryption method
 * - vlessFlow: VLESS flow control
 *
 * For Shadowsocks:
 * - password: SS password
 * - encryptMethod: SS encryption method (e.g., "aes-256-gcm")
 *
 * For SOCKS5/HTTP:
 * - username/password: Basic auth
 */
data class ProxyServer(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val protocol: ProxyProtocol,
    val routeGroup: Route,
    val enabled: Boolean = true,
    val latencyMs: Int? = null,

    // Common auth
    val username: String? = null,
    val password: String? = null,

    // Xray: core
    val uuid: String? = null,

    // Xray: transport
    val transport: TransportType = TransportType.TCP,
    val wsPath: String? = null,
    val wsHost: String? = null,
    val grpcServiceName: String? = null,

    // Xray: security
    val security: SecurityType = SecurityType.NONE,
    val sni: String? = null,
    val fingerprint: String? = null,
    val alpn: String? = null,
    val allowInsecure: Boolean = false,

    // Xray: Reality
    val realityPublicKey: String? = null,
    val realityShortId: String? = null,

    // Xray: VMess specific
    val vmessEncryption: VMessEncryption = VMessEncryption.AUTO,
    val alterId: Int = 0,

    // Xray: VLESS specific
    val vlessFlow: VLessFlow = VLessFlow.NONE,

    // Shadowsocks specific
    val encryptMethod: String? = null,
)
