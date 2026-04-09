package com.dreason.frame.tunnel.routing

import com.dreason.frame.core.model.Route
import com.dreason.frame.core.model.RoutingRule
import java.net.Inet4Address
import java.net.InetAddress

/**
 * Radix trie (Patricia trie) for efficient CIDR range matching.
 *
 * Each bit of the IP address traverses the trie. The longest matching prefix
 * determines the route. Efficiently handles thousands of CIDR blocks
 * (China has ~8,000-12,000 CIDR blocks from APNIC).
 */
class CidrMatcher {

    private class Node {
        var children = arrayOfNulls<Node>(2) // [0] and [1]
        var route: Route? = null
    }

    private val ipv4Root = Node()
    private val ipv6Root = Node()

    fun addRule(rule: RoutingRule) {
        val parts = rule.pattern.split("/")
        if (parts.size != 2) return

        val ip = try { InetAddress.getByName(parts[0]) } catch (e: Exception) { return }
        val prefixLen = parts[1].toIntOrNull() ?: return

        addCidr(ip, prefixLen, rule.route)
    }

    fun addCidr(ip: InetAddress, prefixLen: Int, route: Route) {
        val bits = ip.address
        val root = if (ip is Inet4Address) ipv4Root else ipv6Root
        var node = root

        val totalBits = bits.size * 8
        val effectivePrefix = minOf(prefixLen, totalBits)

        for (i in 0 until effectivePrefix) {
            val byteIndex = i / 8
            val bitIndex = 7 - (i % 8)
            val bit = (bits[byteIndex].toInt() shr bitIndex) and 1

            if (node.children[bit] == null) {
                node.children[bit] = Node()
            }
            node = node.children[bit]!!
        }

        node.route = route
    }

    /**
     * Match an IP address against all CIDR rules.
     * Returns the route from the most specific (longest prefix) matching rule.
     */
    fun match(ip: InetAddress): Route? {
        val bits = ip.address
        val root = if (ip is Inet4Address) ipv4Root else ipv6Root
        var node = root
        var lastMatch: Route? = null

        val totalBits = bits.size * 8

        for (i in 0 until totalBits) {
            val byteIndex = i / 8
            val bitIndex = 7 - (i % 8)
            val bit = (bits[byteIndex].toInt() shr bitIndex) and 1

            val child = node.children[bit] ?: break
            node = child
            if (node.route != null) {
                lastMatch = node.route
            }
        }

        return lastMatch
    }

    /**
     * Match an IP address string.
     */
    fun match(ipString: String): Route? {
        val ip = try { InetAddress.getByName(ipString) } catch (e: Exception) { return null }
        return match(ip)
    }
}
