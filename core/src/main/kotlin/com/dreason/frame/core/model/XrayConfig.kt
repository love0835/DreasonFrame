package com.dreason.frame.core.model

/**
 * Xray transport layer configuration.
 */
enum class TransportType {
    TCP,
    WEBSOCKET,
    GRPC,
    HTTP2,
    QUIC;

    val displayName: String
        get() = when (this) {
            TCP -> "TCP"
            WEBSOCKET -> "WebSocket"
            GRPC -> "gRPC"
            HTTP2 -> "HTTP/2"
            QUIC -> "QUIC"
        }
}

/**
 * TLS/Security configuration for Xray protocols.
 */
enum class SecurityType {
    NONE,
    TLS,
    REALITY;

    val displayName: String
        get() = when (this) {
            NONE -> "無"
            TLS -> "TLS"
            REALITY -> "Reality"
        }
}

/**
 * VMess encryption methods.
 */
enum class VMessEncryption {
    AUTO,
    AES_128_GCM,
    CHACHA20_POLY1305,
    NONE,
    ZERO;

    val configValue: String
        get() = when (this) {
            AUTO -> "auto"
            AES_128_GCM -> "aes-128-gcm"
            CHACHA20_POLY1305 -> "chacha20-poly1305"
            NONE -> "none"
            ZERO -> "zero"
        }

    val displayName: String
        get() = when (this) {
            AUTO -> "自動"
            AES_128_GCM -> "AES-128-GCM"
            CHACHA20_POLY1305 -> "ChaCha20-Poly1305"
            NONE -> "無加密"
            ZERO -> "Zero"
        }
}

/**
 * VLESS flow control types (for XTLS).
 */
enum class VLessFlow {
    NONE,
    XTLS_RPRX_VISION;

    val configValue: String
        get() = when (this) {
            NONE -> ""
            XTLS_RPRX_VISION -> "xtls-rprx-vision"
        }

    val displayName: String
        get() = when (this) {
            NONE -> "無"
            XTLS_RPRX_VISION -> "xtls-rprx-vision"
        }
}
