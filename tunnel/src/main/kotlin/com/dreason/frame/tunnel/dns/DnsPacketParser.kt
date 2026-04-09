package com.dreason.frame.tunnel.dns

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Minimal DNS packet parser/builder for intercepting DNS queries and constructing responses.
 */
object DnsPacketParser {

    data class DnsQuery(
        val id: Int,
        val domain: String,
        val type: Int,       // 1 = A, 28 = AAAA
        val queryClass: Int, // 1 = IN
        val rawPacket: ByteArray,
    )

    data class DnsResponse(
        val id: Int,
        val domain: String,
        val addresses: List<InetAddress>,
        val ttl: Int = 300,
    )

    /**
     * Parse a DNS query packet.
     */
    fun parseQuery(data: ByteArray): DnsQuery? {
        if (data.size < 12) return null

        val input = DataInputStream(ByteArrayInputStream(data))

        val id = input.readUnsignedShort()
        val flags = input.readUnsignedShort()

        // Check if this is a query (QR bit = 0)
        if (flags and 0x8000 != 0) return null

        val qdCount = input.readUnsignedShort()
        input.readUnsignedShort() // anCount
        input.readUnsignedShort() // nsCount
        input.readUnsignedShort() // arCount

        if (qdCount < 1) return null

        // Parse domain name
        val domain = readDomainName(data, 12) ?: return null
        val offset = 12 + domainNameLength(data, 12)

        if (data.size < offset + 4) return null

        val type = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        val queryClass = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)

        return DnsQuery(
            id = id,
            domain = domain,
            type = type,
            queryClass = queryClass,
            rawPacket = data,
        )
    }

    /**
     * Build a DNS response with the given IP address.
     */
    fun buildResponse(query: DnsQuery, address: InetAddress, ttl: Int = 60): ByteArray {
        val output = ByteArrayOutputStream()
        val writer = DataOutputStream(output)

        // Header
        writer.writeShort(query.id)
        writer.writeShort(0x8180) // Standard response, no error
        writer.writeShort(1)     // Question count
        writer.writeShort(1)     // Answer count
        writer.writeShort(0)     // Authority count
        writer.writeShort(0)     // Additional count

        // Question section (copy from query)
        val questionStart = 12
        val questionEnd = questionStart + domainNameLength(query.rawPacket, questionStart) + 4
        writer.write(query.rawPacket, questionStart, questionEnd - questionStart)

        // Answer section
        writer.writeShort(0xC00C) // Pointer to domain name in question
        writer.writeShort(if (address is Inet4Address) 1 else 28) // Type A or AAAA
        writer.writeShort(1)      // Class IN
        writer.writeInt(ttl)
        val addrBytes = address.address
        writer.writeShort(addrBytes.size)
        writer.write(addrBytes)

        return output.toByteArray()
    }

    /**
     * Parse a DNS response to extract resolved addresses.
     */
    fun parseResponse(data: ByteArray): DnsResponse? {
        if (data.size < 12) return null

        val buffer = ByteBuffer.wrap(data)
        val id = buffer.short.toInt() and 0xFFFF
        val flags = buffer.short.toInt() and 0xFFFF

        // Check if this is a response (QR bit = 1)
        if (flags and 0x8000 == 0) return null

        val qdCount = buffer.short.toInt() and 0xFFFF
        val anCount = buffer.short.toInt() and 0xFFFF

        buffer.short // nsCount
        buffer.short // arCount

        // Skip question section
        var offset = 12
        var domain = readDomainName(data, offset) ?: return null
        for (i in 0 until qdCount) {
            offset += domainNameLength(data, offset) + 4
        }

        // Parse answers
        val addresses = mutableListOf<InetAddress>()
        for (i in 0 until anCount) {
            if (offset >= data.size) break

            // Skip name (could be pointer)
            offset += domainNameLength(data, offset)

            if (offset + 10 > data.size) break

            val type = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            offset += 2 // type
            offset += 2 // class
            offset += 4 // ttl
            val rdLength = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            offset += 2

            if (type == 1 && rdLength == 4 && offset + 4 <= data.size) {
                val addr = ByteArray(4)
                System.arraycopy(data, offset, addr, 0, 4)
                addresses.add(InetAddress.getByAddress(addr))
            } else if (type == 28 && rdLength == 16 && offset + 16 <= data.size) {
                val addr = ByteArray(16)
                System.arraycopy(data, offset, addr, 0, 16)
                addresses.add(InetAddress.getByAddress(addr))
            }

            offset += rdLength
        }

        return DnsResponse(id = id, domain = domain, addresses = addresses)
    }

    private fun readDomainName(data: ByteArray, startOffset: Int): String? {
        val parts = mutableListOf<String>()
        var offset = startOffset
        var jumped = false
        var maxJumps = 10

        while (offset < data.size && maxJumps > 0) {
            val len = data[offset].toInt() and 0xFF

            if (len == 0) break

            // Pointer
            if (len and 0xC0 == 0xC0) {
                if (offset + 1 >= data.size) return null
                val pointer = ((len and 0x3F) shl 8) or (data[offset + 1].toInt() and 0xFF)
                offset = pointer
                jumped = true
                maxJumps--
                continue
            }

            offset++
            if (offset + len > data.size) return null

            parts.add(String(data, offset, len))
            offset += len
        }

        return if (parts.isNotEmpty()) parts.joinToString(".") else null
    }

    private fun domainNameLength(data: ByteArray, startOffset: Int): Int {
        var offset = startOffset
        while (offset < data.size) {
            val len = data[offset].toInt() and 0xFF
            if (len == 0) return offset - startOffset + 1
            if (len and 0xC0 == 0xC0) return offset - startOffset + 2 // pointer is 2 bytes
            offset += len + 1
        }
        return offset - startOffset
    }
}
