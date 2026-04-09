package com.dreason.frame.tunnel.proxy

import android.net.VpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Direct connection client — bypasses all proxies.
 * Uses VpnService.protect(socket) to route the socket through the real
 * network interface instead of back into the TUN, preventing a routing loop.
 */
class DirectClient(
    private val vpnService: VpnService,
) : ProxyClient {

    override suspend fun connect(destination: InetSocketAddress, domain: String?): ProxiedConnection =
        withContext(Dispatchers.IO) {
            val socket = Socket()
            vpnService.protect(socket)
            socket.connect(destination, 10_000)

            ProxiedConnection(socket, socket.getInputStream(), socket.getOutputStream())
        }

    override fun close() {}
}
