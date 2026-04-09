package com.dreason.frame.tunnel.dns

import android.net.VpnService
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Forwards DNS queries to a real upstream DNS server via a protected UDP socket
 * (bypasses the VPN tunnel to avoid infinite loops).
 */
class DnsResolver(
    private val upstreamDns: String = "223.5.5.5",
    private val vpnService: VpnService,
    private val timeoutMs: Int = 5000,
) {

    /**
     * Resolve a DNS query by forwarding it to the upstream DNS server.
     * @param queryData Raw DNS query packet
     * @return Raw DNS response packet, or null on failure
     */
    fun resolve(queryData: ByteArray): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            // Protect socket so it bypasses VPN tunnel
            vpnService.protect(socket)
            socket.soTimeout = timeoutMs

            val dnsAddress = InetAddress.getByName(upstreamDns)
            val sendPacket = DatagramPacket(queryData, queryData.size, dnsAddress, 53)
            socket.send(sendPacket)

            val buffer = ByteArray(4096)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)

            buffer.copyOfRange(0, receivePacket.length)
        } catch (e: Exception) {
            null
        } finally {
            socket?.close()
        }
    }
}
