package com.dreason.frame.tunnel.dns

import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * FakeIP pool that allocates synthetic IP addresses from the 198.18.0.0/15 range.
 * Maps fake IPs to real domains for domain-based routing decisions.
 *
 * When a DNS query is intercepted, instead of returning the real IP, we allocate
 * a fake IP and return that. When a connection is later made to the fake IP,
 * we can recover the original domain name for rule matching.
 */
class FakeIpPool {

    companion object {
        // 198.18.0.0/15 = 198.18.0.0 to 198.19.255.255 (131,072 addresses)
        private val BASE = byteArrayOf(198.toByte(), 18, 0, 0)
        private const val POOL_SIZE = 131072
    }

    data class DomainMapping(
        val domain: String,
        val realIp: InetAddress?,
        val fakeIp: InetAddress,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val counter = AtomicInteger(1) // skip .0.0
    private val fakeToMapping = ConcurrentHashMap<InetAddress, DomainMapping>()
    private val domainToFake = ConcurrentHashMap<String, InetAddress>()

    /**
     * Allocate a fake IP for the given domain. If the domain already has a fake IP, return it.
     */
    fun allocate(domain: String, realIp: InetAddress? = null): InetAddress {
        domainToFake[domain]?.let { return it }

        val index = counter.getAndIncrement() % POOL_SIZE
        val ip = indexToIp(index)
        val mapping = DomainMapping(
            domain = domain,
            realIp = realIp,
            fakeIp = ip,
        )

        // Clean up any old mapping at this IP (LRU eviction)
        fakeToMapping[ip]?.let { old ->
            domainToFake.remove(old.domain)
        }

        fakeToMapping[ip] = mapping
        domainToFake[domain] = ip
        return ip
    }

    /**
     * Look up the original domain and real IP for a fake IP.
     */
    fun lookup(fakeIp: InetAddress): DomainMapping? = fakeToMapping[fakeIp]

    /**
     * Reverse lookup: get the domain name for a given IP (fake or real).
     */
    fun reverseLookup(ip: InetAddress): String? = fakeToMapping[ip]?.domain

    /**
     * Check if an IP is in the fake IP range.
     */
    fun isFakeIp(ip: InetAddress): Boolean {
        val bytes = ip.address
        return bytes.size == 4 &&
            bytes[0] == 198.toByte() &&
            (bytes[1] == 18.toByte() || bytes[1] == 19.toByte())
    }

    fun clear() {
        fakeToMapping.clear()
        domainToFake.clear()
        counter.set(1)
    }

    private fun indexToIp(index: Int): InetAddress {
        val bytes = byteArrayOf(
            BASE[0],
            (BASE[1].toInt() + (index shr 16)).toByte(),
            (index shr 8 and 0xFF).toByte(),
            (index and 0xFF).toByte(),
        )
        return Inet4Address.getByAddress(bytes)
    }
}
