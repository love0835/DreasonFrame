package com.dreason.frame.tunnel.proxy

import android.net.VpnService
import com.dreason.frame.core.model.ProxyServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * SOCKS5 proxy client implementation (RFC 1928).
 *
 * Handshake flow:
 * 1. Client greeting (supported auth methods)
 * 2. Server chooses auth method
 * 3. Auth (if required)
 * 4. CONNECT request with destination
 * 5. Server reply
 */
class Socks5Client(
    private val server: ProxyServer,
    private val vpnService: VpnService,
) : ProxyClient {

    companion object {
        private const val SOCKS_VERSION: Byte = 0x05
        private const val AUTH_NONE: Byte = 0x00
        private const val AUTH_USER_PASS: Byte = 0x02
        private const val CMD_CONNECT: Byte = 0x01
        private const val ADDR_TYPE_DOMAIN: Byte = 0x03
        private const val ADDR_TYPE_IPV4: Byte = 0x01
    }

    override suspend fun connect(destination: InetSocketAddress, domain: String?): ProxiedConnection =
        withContext(Dispatchers.IO) {
            val socket = Socket()
            vpnService.protect(socket)
            socket.connect(InetSocketAddress(server.host, server.port), 10_000)

            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            // Step 1: Greeting
            val hasAuth = server.username != null && server.password != null
            if (hasAuth) {
                output.write(byteArrayOf(SOCKS_VERSION, 2, AUTH_NONE, AUTH_USER_PASS))
            } else {
                output.write(byteArrayOf(SOCKS_VERSION, 1, AUTH_NONE))
            }
            output.flush()

            // Step 2: Server method selection
            val ver = input.readByte()
            val method = input.readByte()

            // Step 3: Authentication
            if (method == AUTH_USER_PASS && hasAuth) {
                val username = server.username!!.toByteArray()
                val password = server.password!!.toByteArray()
                output.writeByte(0x01) // auth version
                output.writeByte(username.size)
                output.write(username)
                output.writeByte(password.size)
                output.write(password)
                output.flush()

                val authVer = input.readByte()
                val authStatus = input.readByte()
                if (authStatus != 0.toByte()) {
                    socket.close()
                    throw Exception("SOCKS5 authentication failed")
                }
            } else if (method != AUTH_NONE) {
                socket.close()
                throw Exception("SOCKS5 unsupported auth method: $method")
            }

            // Step 4: CONNECT request
            output.writeByte(SOCKS_VERSION.toInt())
            output.writeByte(CMD_CONNECT.toInt())
            output.writeByte(0x00) // reserved

            if (domain != null) {
                // Use domain name (let proxy do DNS resolution)
                val domainBytes = domain.toByteArray()
                output.writeByte(ADDR_TYPE_DOMAIN.toInt())
                output.writeByte(domainBytes.size)
                output.write(domainBytes)
            } else {
                // Use IP address
                output.writeByte(ADDR_TYPE_IPV4.toInt())
                output.write(destination.address.address)
            }

            output.writeShort(destination.port)
            output.flush()

            // Step 5: Read reply
            val replyVer = input.readByte()
            val replyStatus = input.readByte()
            val replyRsv = input.readByte()
            val replyAddrType = input.readByte()

            if (replyStatus != 0.toByte()) {
                socket.close()
                throw Exception("SOCKS5 connect failed: status=$replyStatus")
            }

            // Read bound address (required by protocol, but we discard it)
            when (replyAddrType.toInt()) {
                0x01 -> input.readNBytes(4)   // IPv4
                0x03 -> {                      // Domain
                    val len = input.readByte().toInt() and 0xFF
                    input.readNBytes(len)
                }
                0x04 -> input.readNBytes(16)  // IPv6
            }
            input.readShort() // bound port

            ProxiedConnection(socket, socket.getInputStream(), socket.getOutputStream())
        }

    override fun close() {
        // No persistent state to clean up
    }
}
