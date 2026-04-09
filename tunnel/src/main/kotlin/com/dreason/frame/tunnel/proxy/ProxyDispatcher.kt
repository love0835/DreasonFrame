package com.dreason.frame.tunnel.proxy

import android.net.VpnService
import com.dreason.frame.core.model.ProxyServer
import com.dreason.frame.core.model.Route
import com.dreason.frame.tunnel.dns.FakeIpPool
import com.dreason.frame.tunnel.routing.RuleEngine
import com.dreason.frame.tunnel.stats.TrafficMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket

/**
 * Local SOCKS5 server that accepts connections from tun2socks and routes them
 * to the appropriate upstream proxy based on the rule engine.
 *
 * Flow:
 * 1. tun2socks connects with a SOCKS5 CONNECT request containing the original destination
 * 2. ProxyDispatcher looks up the FakeIP to recover the real domain
 * 3. Rule engine determines the route (China proxy / Taiwan proxy / Direct)
 * 4. Connection is forwarded to the appropriate upstream proxy client
 * 5. Data is relayed bidirectionally
 */
class ProxyDispatcher(
    private val port: Int,
    private val ruleEngine: RuleEngine,
    private val fakeIpPool: FakeIpPool,
    private val chinaServer: ProxyServer?,
    private val taiwanServer: ProxyServer?,
    private val vpnService: VpnService,
) {

    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val trafficMonitor = TrafficMonitor()

    private val directClient = DirectClient(vpnService)
    private val chinaClient: ProxyClient? = chinaServer?.let { createClient(it) }
    private val taiwanClient: ProxyClient? = taiwanServer?.let { createClient(it) }

    fun start() {
        serverSocket = ServerSocket(port, 128, InetAddress.getByName("127.0.0.1"))

        job = scope.launch {
            while (isActive) {
                try {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch { handleConnection(clientSocket.getInputStream(), clientSocket.getOutputStream(), clientSocket) }
                } catch (e: Exception) {
                    if (isActive) continue else break
                }
            }
        }
    }

    private suspend fun handleConnection(
        input: InputStream,
        output: OutputStream,
        clientSocket: java.net.Socket,
    ) {
        try {
            // Read SOCKS5 greeting from tun2socks
            val greeting = ByteArray(3)
            input.read(greeting)

            // Respond: no auth required
            output.write(byteArrayOf(0x05, 0x00))
            output.flush()

            // Read CONNECT request
            val header = ByteArray(4)
            input.read(header)

            val addressType = header[3].toInt()
            val destAddress: String
            val destIp: String

            when (addressType) {
                0x01 -> { // IPv4
                    val addr = ByteArray(4)
                    input.read(addr)
                    val inetAddr = InetAddress.getByAddress(addr)
                    destIp = inetAddr.hostAddress ?: "0.0.0.0"
                    destAddress = destIp
                }
                0x03 -> { // Domain name
                    val domainLen = input.read()
                    val domainBytes = ByteArray(domainLen)
                    input.read(domainBytes)
                    destAddress = String(domainBytes)
                    destIp = destAddress
                }
                0x04 -> { // IPv6
                    val addr = ByteArray(16)
                    input.read(addr)
                    val inetAddr = InetAddress.getByAddress(addr)
                    destIp = inetAddr.hostAddress ?: "::"
                    destAddress = destIp
                }
                else -> {
                    clientSocket.close()
                    return
                }
            }

            val portHigh = input.read()
            val portLow = input.read()
            val destPort = (portHigh shl 8) or portLow

            // Resolve FakeIP to domain
            val fakeIpAddr = try { InetAddress.getByName(destIp) } catch (e: Exception) { null }
            val domain = fakeIpAddr?.let { fakeIpPool.reverseLookup(it) }
            val realIp = fakeIpAddr?.let { fakeIpPool.lookup(it)?.realIp?.hostAddress } ?: destIp

            // Determine route via rule engine
            val decision = ruleEngine.resolve(
                packageName = null, // TODO: integrate UidResolver
                domain = domain,
                destIp = realIp,
            )

            // Select proxy client based on route
            val proxyClient = when (decision.route) {
                Route.CHINA_PROXY -> chinaClient ?: directClient
                Route.TAIWAN_PROXY -> taiwanClient ?: directClient
                Route.DIRECT -> directClient
                Route.REJECT -> {
                    // Send SOCKS5 connection refused reply
                    output.write(byteArrayOf(0x05, 0x05, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                    output.flush()
                    clientSocket.close()
                    return
                }
            }

            // Connect to upstream
            val realDest = InetSocketAddress(
                fakeIpAddr?.let { fakeIpPool.lookup(it)?.realIp } ?: InetAddress.getByName(realIp),
                destPort
            )
            val upstream = proxyClient.connect(realDest, domain)

            // Send SOCKS5 success reply
            output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            output.flush()

            // Relay data bidirectionally
            val job1 = scope.launch { relay(input, upstream.outputStream, decision.route) }
            val job2 = scope.launch { relay(upstream.inputStream, output, decision.route) }

            job1.join()
            job2.join()

            upstream.close()
        } catch (e: Exception) {
            // Connection error — silently close
        } finally {
            try { clientSocket.close() } catch (_: Exception) {}
        }
    }

    private fun relay(from: InputStream, to: OutputStream, route: Route) {
        val buffer = ByteArray(8192)
        try {
            var bytesRead = from.read(buffer)
            while (bytesRead > 0) {
                to.write(buffer, 0, bytesRead)
                to.flush()
                trafficMonitor.addBytes(route, bytesRead.toLong())
                bytesRead = from.read(buffer)
            }
        } catch (_: Exception) {
            // Connection closed
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        serverSocket?.close()
        serverSocket = null
        chinaClient?.close()
        taiwanClient?.close()
    }

    fun getTrafficMonitor(): TrafficMonitor = trafficMonitor

    private fun createClient(server: ProxyServer): ProxyClient = when (server.protocol) {
        com.dreason.frame.core.model.ProxyProtocol.SOCKS5 -> Socks5Client(server, vpnService)
        com.dreason.frame.core.model.ProxyProtocol.HTTP -> HttpProxyClient(server, vpnService)
        com.dreason.frame.core.model.ProxyProtocol.SHADOWSOCKS -> {
            val client = ShadowsocksClient(server, vpnService)
            client.startLocalProxy()
            client
        }
    }
}
