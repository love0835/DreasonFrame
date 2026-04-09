package com.dreason.frame.tunnel.proxy

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Interface for all proxy client implementations.
 */
interface ProxyClient {
    /**
     * Establish a proxied connection to the target destination.
     * @param destination The target host:port to connect to
     * @param domain Optional domain name (for proxies that support remote DNS)
     * @return A ProxiedConnection ready for data relay
     */
    suspend fun connect(destination: InetSocketAddress, domain: String? = null): ProxiedConnection

    fun close()
}

/**
 * Represents an active proxied connection that can relay data.
 */
class ProxiedConnection(
    val socket: Socket,
    val inputStream: InputStream,
    val outputStream: OutputStream,
) {
    fun close() {
        try { inputStream.close() } catch (_: Exception) {}
        try { outputStream.close() } catch (_: Exception) {}
        try { socket.close() } catch (_: Exception) {}
    }
}
