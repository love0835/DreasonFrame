package com.dreason.frame.tunnel.dns

import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Inet4Address

/**
 * Intercepts DNS queries from the TUN interface, resolves them via upstream DNS,
 * allocates fake IPs, and returns fake DNS responses.
 *
 * This enables domain-based routing: by mapping fake IPs back to domains,
 * the routing engine can make decisions based on domain names rather than just IPs.
 *
 * Note: In the full implementation, this would parse IP/UDP headers from TUN packets.
 * For the initial architecture, DNS interception is handled at the SOCKS5 dispatcher level
 * by examining the destination and consulting the FakeIP pool.
 */
class DnsInterceptor(
    private val tunFd: ParcelFileDescriptor,
    val fakeIpPool: FakeIpPool,
    upstreamDns: String,
    vpnService: VpnService,
) {

    private val resolver = DnsResolver(upstreamDns, vpnService)
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        job = scope.launch {
            // DNS interception loop
            // In production, this reads raw packets from TUN fd,
            // identifies UDP/53 packets, intercepts them,
            // resolves via upstream, maps to FakeIP, and writes response back.
            //
            // For the MVP with tun2socks, DNS is handled differently:
            // tun2socks converts DNS queries to SOCKS5 UDP ASSOCIATE requests,
            // and the ProxyDispatcher handles domain resolution.
            interceptLoop()
        }
    }

    private suspend fun CoroutineScope.interceptLoop() {
        // Placeholder for TUN-level DNS interception.
        // The actual implementation will:
        // 1. Read IP packets from TUN fd
        // 2. Check if UDP and destination port 53
        // 3. Parse DNS query to get domain name
        // 4. Forward to upstream DNS resolver
        // 5. Get real IP from response
        // 6. Allocate fake IP from FakeIpPool
        // 7. Build DNS response with fake IP
        // 8. Write response packet back to TUN fd
        while (isActive) {
            try {
                Thread.sleep(1000) // Placeholder — replaced by actual packet reading
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    /**
     * Process a DNS query and return a response with a fake IP.
     * Called by ProxyDispatcher when it receives a DNS-related SOCKS5 request.
     */
    fun processDnsQuery(queryData: ByteArray): ByteArray? {
        val query = DnsPacketParser.parseQuery(queryData) ?: return null

        // Only handle A records for now
        if (query.type != 1) {
            return resolver.resolve(queryData)
        }

        // Resolve real IP first
        val realResponse = resolver.resolve(queryData)
        val realIp = realResponse?.let { DnsPacketParser.parseResponse(it) }
            ?.addresses
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull()

        // Allocate fake IP
        val fakeIp = fakeIpPool.allocate(query.domain, realIp)

        // Build response with fake IP
        return DnsPacketParser.buildResponse(query, fakeIp)
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
