package com.dreason.frame.tunnel.proxy

import android.net.VpnService
import com.dreason.frame.core.model.ProxyServer
import java.net.InetSocketAddress

/**
 * Shadowsocks proxy client.
 *
 * Strategy A (MVP): Run shadowsocks-rust `sslocal` as a local SOCKS5 proxy.
 * Bundle the sslocal binary for each ABI, start it as a child process listening
 * on 127.0.0.1:LOCAL_SS_PORT, then use Socks5Client to connect to it.
 *
 * Strategy B (future): Link shadowsocks-rust as a shared library via JNI.
 */
class ShadowsocksClient(
    private val server: ProxyServer,
    private val vpnService: VpnService,
    private val localPort: Int = 1080,
) : ProxyClient {

    private var process: Process? = null
    private var localSocks5Client: Socks5Client? = null

    /**
     * Start the sslocal process and return a SOCKS5 client pointing to it.
     * Note: In production, sslocal binary would be bundled in assets or jniLibs.
     */
    fun startLocalProxy(): Boolean {
        // Create a local proxy server config that connects to the remote SS server
        val localServer = ProxyServer(
            name = "ss-local",
            host = "127.0.0.1",
            port = localPort,
            protocol = com.dreason.frame.core.model.ProxyProtocol.SOCKS5,
            routeGroup = server.routeGroup,
        )
        localSocks5Client = Socks5Client(localServer, vpnService)

        // TODO: Start sslocal binary process
        // val command = listOf(
        //     ssLocalPath,
        //     "-s", server.host,
        //     "-p", server.port.toString(),
        //     "-l", localPort.toString(),
        //     "-k", server.password ?: "",
        //     "-m", server.encryptMethod ?: "aes-256-gcm",
        // )
        // process = ProcessBuilder(command).start()

        return true
    }

    override suspend fun connect(destination: InetSocketAddress, domain: String?): ProxiedConnection {
        val client = localSocks5Client
            ?: throw Exception("Shadowsocks local proxy not started")
        return client.connect(destination, domain)
    }

    override fun close() {
        localSocks5Client?.close()
        process?.destroy()
        process = null
    }
}
