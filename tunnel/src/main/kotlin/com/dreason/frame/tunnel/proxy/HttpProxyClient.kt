package com.dreason.frame.tunnel.proxy

import android.net.VpnService
import android.util.Base64
import com.dreason.frame.core.model.ProxyServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

/**
 * HTTP CONNECT proxy client implementation.
 *
 * Flow:
 * 1. Connect to HTTP proxy server
 * 2. Send CONNECT host:port HTTP/1.1
 * 3. Read 200 Connection established
 * 4. Socket is now tunneled
 */
class HttpProxyClient(
    private val server: ProxyServer,
    private val vpnService: VpnService,
) : ProxyClient {

    override suspend fun connect(destination: InetSocketAddress, domain: String?): ProxiedConnection =
        withContext(Dispatchers.IO) {
            val socket = Socket()
            vpnService.protect(socket)
            socket.connect(InetSocketAddress(server.host, server.port), 10_000)

            val output = socket.getOutputStream()
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            val host = domain ?: destination.address.hostAddress
            val port = destination.port

            // Build CONNECT request
            val sb = StringBuilder()
            sb.append("CONNECT $host:$port HTTP/1.1\r\n")
            sb.append("Host: $host:$port\r\n")

            // Add proxy authentication if configured
            if (server.username != null && server.password != null) {
                val credentials = "${server.username}:${server.password}"
                val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                sb.append("Proxy-Authorization: Basic $encoded\r\n")
            }

            sb.append("\r\n")

            output.write(sb.toString().toByteArray())
            output.flush()

            // Read response
            val statusLine = reader.readLine()
                ?: throw Exception("HTTP proxy: no response")

            if (!statusLine.contains("200")) {
                socket.close()
                throw Exception("HTTP proxy failed: $statusLine")
            }

            // Read remaining headers until empty line
            var line = reader.readLine()
            while (line != null && line.isNotEmpty()) {
                line = reader.readLine()
            }

            ProxiedConnection(socket, socket.getInputStream(), socket.getOutputStream())
        }

    override fun close() {}
}
