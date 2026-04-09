package com.dreason.frame.tunnel.proxy.xray

import com.dreason.frame.core.model.ProxyProtocol
import com.dreason.frame.core.model.ProxyServer
import com.dreason.frame.core.model.SecurityType
import com.dreason.frame.core.model.TransportType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Generates Xray-core compatible JSON configuration from ProxyServer model.
 *
 * Xray-core configuration format:
 * {
 *   "inbounds": [{ local SOCKS5 listener }],
 *   "outbounds": [{ upstream proxy config }],
 * }
 *
 * The generated config runs Xray as a local SOCKS5 proxy on 127.0.0.1:localPort,
 * forwarding traffic to the remote server via the configured protocol.
 */
object XrayConfigGenerator {

    fun generate(server: ProxyServer, localPort: Int): String {
        val config = JSONObject()

        // Log
        config.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        // Inbound: local SOCKS5 listener
        config.put("inbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "socks-in")
                put("port", localPort)
                put("listen", "127.0.0.1")
                put("protocol", "socks")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http")
                        put("tls")
                    })
                })
            })
        })

        // Outbound: remote proxy server
        config.put("outbounds", JSONArray().apply {
            put(buildOutbound(server))
        })

        return config.toString(2)
    }

    private fun buildOutbound(server: ProxyServer): JSONObject {
        val outbound = JSONObject()
        outbound.put("tag", "proxy")

        when (server.protocol) {
            ProxyProtocol.VMESS -> buildVMessOutbound(outbound, server)
            ProxyProtocol.VLESS -> buildVLessOutbound(outbound, server)
            ProxyProtocol.TROJAN -> buildTrojanOutbound(outbound, server)
            else -> throw IllegalArgumentException("Not an Xray protocol: ${server.protocol}")
        }

        // Stream settings (transport + security)
        outbound.put("streamSettings", buildStreamSettings(server))

        return outbound
    }

    private fun buildVMessOutbound(outbound: JSONObject, server: ProxyServer) {
        outbound.put("protocol", "vmess")
        outbound.put("settings", JSONObject().apply {
            put("vnext", JSONArray().apply {
                put(JSONObject().apply {
                    put("address", server.host)
                    put("port", server.port)
                    put("users", JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", server.uuid)
                            put("alterId", server.alterId)
                            put("security", server.vmessEncryption.configValue)
                        })
                    })
                })
            })
        })
    }

    private fun buildVLessOutbound(outbound: JSONObject, server: ProxyServer) {
        outbound.put("protocol", "vless")
        outbound.put("settings", JSONObject().apply {
            put("vnext", JSONArray().apply {
                put(JSONObject().apply {
                    put("address", server.host)
                    put("port", server.port)
                    put("users", JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", server.uuid)
                            put("encryption", "none")
                            val flow = server.vlessFlow.configValue
                            if (flow.isNotEmpty()) {
                                put("flow", flow)
                            }
                        })
                    })
                })
            })
        })
    }

    private fun buildTrojanOutbound(outbound: JSONObject, server: ProxyServer) {
        outbound.put("protocol", "trojan")
        outbound.put("settings", JSONObject().apply {
            put("servers", JSONArray().apply {
                put(JSONObject().apply {
                    put("address", server.host)
                    put("port", server.port)
                    put("password", server.password ?: server.uuid ?: "")
                })
            })
        })
    }

    private fun buildStreamSettings(server: ProxyServer): JSONObject {
        val stream = JSONObject()

        // Transport
        when (server.transport) {
            TransportType.TCP -> {
                stream.put("network", "tcp")
            }
            TransportType.WEBSOCKET -> {
                stream.put("network", "ws")
                stream.put("wsSettings", JSONObject().apply {
                    if (!server.wsPath.isNullOrBlank()) put("path", server.wsPath)
                    if (!server.wsHost.isNullOrBlank()) {
                        put("headers", JSONObject().apply {
                            put("Host", server.wsHost)
                        })
                    }
                })
            }
            TransportType.GRPC -> {
                stream.put("network", "grpc")
                stream.put("grpcSettings", JSONObject().apply {
                    if (!server.grpcServiceName.isNullOrBlank()) {
                        put("serviceName", server.grpcServiceName)
                    }
                    put("multiMode", false)
                })
            }
            TransportType.HTTP2 -> {
                stream.put("network", "h2")
                stream.put("httpSettings", JSONObject().apply {
                    if (!server.wsHost.isNullOrBlank()) {
                        put("host", JSONArray().apply { put(server.wsHost) })
                    }
                    if (!server.wsPath.isNullOrBlank()) put("path", server.wsPath)
                })
            }
            TransportType.QUIC -> {
                stream.put("network", "quic")
                stream.put("quicSettings", JSONObject().apply {
                    put("security", "none")
                    put("key", "")
                    put("header", JSONObject().apply {
                        put("type", "none")
                    })
                })
            }
        }

        // Security
        when (server.security) {
            SecurityType.TLS -> {
                stream.put("security", "tls")
                stream.put("tlsSettings", JSONObject().apply {
                    if (!server.sni.isNullOrBlank()) put("serverName", server.sni)
                    if (!server.fingerprint.isNullOrBlank()) put("fingerprint", server.fingerprint)
                    val alpnValue = server.alpn
                    if (!alpnValue.isNullOrBlank()) {
                        put("alpn", JSONArray().apply {
                            alpnValue.split(",").map { it.trim() }.forEach { put(it) }
                        })
                    }
                    put("allowInsecure", server.allowInsecure)
                })
            }
            SecurityType.REALITY -> {
                stream.put("security", "reality")
                stream.put("realitySettings", JSONObject().apply {
                    if (!server.sni.isNullOrBlank()) put("serverName", server.sni)
                    if (!server.fingerprint.isNullOrBlank()) put("fingerprint", server.fingerprint)
                    else put("fingerprint", "chrome")
                    if (!server.realityPublicKey.isNullOrBlank()) put("publicKey", server.realityPublicKey)
                    if (!server.realityShortId.isNullOrBlank()) put("shortId", server.realityShortId)
                })
            }
            SecurityType.NONE -> {
                stream.put("security", "none")
            }
        }

        return stream
    }

    /**
     * Parse a standard vmess:// share link into a ProxyServer.
     * Format: vmess://base64(json)
     */
    fun parseVMessLink(link: String): ProxyServer? {
        if (!link.startsWith("vmess://")) return null
        return try {
            val encoded = link.removePrefix("vmess://")
            val json = JSONObject(String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)))

            ProxyServer(
                name = json.optString("ps", "VMess Server"),
                host = json.getString("add"),
                port = json.optString("port", "443").toInt(),
                protocol = ProxyProtocol.VMESS,
                uuid = json.getString("id"),
                alterId = json.optString("aid", "0").toInt(),
                transport = when (json.optString("net", "tcp")) {
                    "ws" -> TransportType.WEBSOCKET
                    "grpc" -> TransportType.GRPC
                    "h2" -> TransportType.HTTP2
                    "quic" -> TransportType.QUIC
                    else -> TransportType.TCP
                },
                security = when (json.optString("tls", "")) {
                    "tls" -> SecurityType.TLS
                    "reality" -> SecurityType.REALITY
                    else -> SecurityType.NONE
                },
                wsPath = json.optString("path", null),
                wsHost = json.optString("host", null),
                sni = json.optString("sni", null),
                routeGroup = com.dreason.frame.core.model.Route.DIRECT,
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse a standard vless:// share link into a ProxyServer.
     * Format: vless://uuid@host:port?type=ws&security=tls&...#name
     */
    fun parseVLessLink(link: String): ProxyServer? {
        if (!link.startsWith("vless://")) return null
        return try {
            val uri = java.net.URI(link)
            val uuid = uri.userInfo
            val host = uri.host
            val port = uri.port
            val name = java.net.URLDecoder.decode(uri.fragment ?: "VLESS Server", "UTF-8")

            val params = (uri.query ?: "").split("&").associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (if (parts.size > 1) java.net.URLDecoder.decode(parts[1], "UTF-8") else "")
            }

            ProxyServer(
                name = name,
                host = host,
                port = port,
                protocol = ProxyProtocol.VLESS,
                uuid = uuid,
                transport = when (params["type"]) {
                    "ws" -> TransportType.WEBSOCKET
                    "grpc" -> TransportType.GRPC
                    "h2" -> TransportType.HTTP2
                    "quic" -> TransportType.QUIC
                    else -> TransportType.TCP
                },
                security = when (params["security"]) {
                    "tls" -> SecurityType.TLS
                    "reality" -> SecurityType.REALITY
                    else -> SecurityType.NONE
                },
                sni = params["sni"],
                fingerprint = params["fp"],
                wsPath = params["path"],
                wsHost = params["host"],
                grpcServiceName = params["serviceName"],
                realityPublicKey = params["pbk"],
                realityShortId = params["sid"],
                vlessFlow = when (params["flow"]) {
                    "xtls-rprx-vision" -> com.dreason.frame.core.model.VLessFlow.XTLS_RPRX_VISION
                    else -> com.dreason.frame.core.model.VLessFlow.NONE
                },
                alpn = params["alpn"],
                routeGroup = com.dreason.frame.core.model.Route.DIRECT,
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse a trojan:// share link into a ProxyServer.
     * Format: trojan://password@host:port?...#name
     */
    fun parseTrojanLink(link: String): ProxyServer? {
        if (!link.startsWith("trojan://")) return null
        return try {
            val uri = java.net.URI(link)
            val password = uri.userInfo
            val host = uri.host
            val port = uri.port
            val name = java.net.URLDecoder.decode(uri.fragment ?: "Trojan Server", "UTF-8")

            val params = (uri.query ?: "").split("&").associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (if (parts.size > 1) java.net.URLDecoder.decode(parts[1], "UTF-8") else "")
            }

            ProxyServer(
                name = name,
                host = host,
                port = port,
                protocol = ProxyProtocol.TROJAN,
                password = password,
                transport = when (params["type"]) {
                    "ws" -> TransportType.WEBSOCKET
                    "grpc" -> TransportType.GRPC
                    else -> TransportType.TCP
                },
                security = when (params["security"]) {
                    "tls", "" -> SecurityType.TLS  // Trojan defaults to TLS
                    "reality" -> SecurityType.REALITY
                    else -> SecurityType.TLS
                },
                sni = params["sni"] ?: host,
                fingerprint = params["fp"],
                wsPath = params["path"],
                grpcServiceName = params["serviceName"],
                alpn = params["alpn"],
                routeGroup = com.dreason.frame.core.model.Route.DIRECT,
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse any supported share link.
     */
    fun parseShareLink(link: String): ProxyServer? = when {
        link.startsWith("vmess://") -> parseVMessLink(link)
        link.startsWith("vless://") -> parseVLessLink(link)
        link.startsWith("trojan://") -> parseTrojanLink(link)
        else -> null
    }
}
