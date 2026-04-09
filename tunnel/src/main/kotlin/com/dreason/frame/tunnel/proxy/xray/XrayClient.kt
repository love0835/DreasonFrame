package com.dreason.frame.tunnel.proxy.xray

import android.content.Context
import android.net.VpnService
import android.util.Log
import com.dreason.frame.core.model.ProxyServer
import com.dreason.frame.tunnel.proxy.ProxiedConnection
import com.dreason.frame.tunnel.proxy.ProxyClient
import com.dreason.frame.tunnel.proxy.Socks5Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress

/**
 * Xray proxy client that manages an Xray-core instance.
 *
 * Architecture:
 * 1. Generate Xray JSON config from ProxyServer model
 * 2. Start Xray-core process (or call via JNI/gomobile binding)
 * 3. Xray-core listens as a local SOCKS5 proxy on 127.0.0.1:localPort
 * 4. ProxyDispatcher connects to this local SOCKS5 port
 *
 * Integration options:
 * - Option A (recommended): Use AndroidLibXrayLite (Go mobile binding)
 *   - Import as AAR, call via Kotlin/JNI
 *   - No subprocess management needed
 *   - See: https://github.com/AnyGogin31/AndroidLibXrayLite
 *
 * - Option B: Bundle xray binary, run as subprocess
 *   - More complex lifecycle management
 *   - Need to bundle ARM64/ARM/x86 binaries
 *
 * Current implementation uses Option A pattern with a fallback.
 */
class XrayClient(
    private val server: ProxyServer,
    private val vpnService: VpnService,
    private val context: Context,
    private val localPort: Int,
) : ProxyClient {

    companion object {
        private const val TAG = "XrayClient"
    }

    private var process: Process? = null
    private var isStarted = false
    private lateinit var localSocks5Client: Socks5Client

    /**
     * Start the Xray-core instance.
     * Generates config, writes to temp file, starts xray-core.
     */
    fun start(): Boolean {
        if (isStarted) return true

        try {
            // Generate Xray config
            val configJson = XrayConfigGenerator.generate(server, localPort)

            // Write config to internal storage
            val configFile = File(context.filesDir, "xray_config_${server.id}.json")
            configFile.writeText(configJson)

            Log.d(TAG, "Xray config written to ${configFile.absolutePath}")
            Log.d(TAG, "Config: $configJson")

            // Start Xray-core
            // Option A: Via AndroidLibXrayLite (Go mobile binding)
            startViaLibXray(configFile.absolutePath)

            // Create local SOCKS5 client pointing to the Xray local listener
            val localServer = com.dreason.frame.core.model.ProxyServer(
                name = "xray-local-${server.id}",
                host = "127.0.0.1",
                port = localPort,
                protocol = com.dreason.frame.core.model.ProxyProtocol.SOCKS5,
                routeGroup = server.routeGroup,
            )
            localSocks5Client = Socks5Client(localServer, vpnService)

            isStarted = true
            Log.d(TAG, "Xray started on 127.0.0.1:$localPort for ${server.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Xray: ${e.message}", e)
            return false
        }
    }

    /**
     * Start Xray via AndroidLibXrayLite Go mobile binding.
     *
     * AndroidLibXrayLite provides:
     * - Libv2ray.initV2Env(filesDir)
     * - Libv2ray.startLoop(configJson)
     * - Libv2ray.stopLoop()
     *
     * This requires adding the dependency:
     * implementation("io.github.nicetofresh:libxray:VERSION")
     */
    private fun startViaLibXray(configPath: String) {
        // TODO: Integrate AndroidLibXrayLite
        // When the library is added as a dependency, uncomment:
        //
        // import libv2ray.Libv2ray
        //
        // Libv2ray.initV2Env(context.filesDir.absolutePath, context.filesDir.absolutePath)
        // val configContent = File(configPath).readText()
        // Libv2ray.startLoop(configContent)
        //
        // For now, try to start via binary if bundled:
        startViaBinary(configPath)
    }

    /**
     * Fallback: Start Xray as a subprocess.
     * Requires xray binary to be bundled in app assets or jniLibs.
     */
    private fun startViaBinary(configPath: String) {
        val xrayBinary = findXrayBinary() ?: run {
            Log.w(TAG, "Xray binary not found. Please integrate AndroidLibXrayLite or bundle xray binary.")
            return
        }

        // Make executable
        xrayBinary.setExecutable(true)

        val command = listOf(
            xrayBinary.absolutePath,
            "run",
            "-config", configPath,
        )

        process = ProcessBuilder(command)
            .directory(context.filesDir)
            .redirectErrorStream(true)
            .start()

        // Log output in background
        Thread {
            process?.inputStream?.bufferedReader()?.forEachLine { line ->
                Log.d(TAG, "xray: $line")
            }
        }.start()
    }

    private fun findXrayBinary(): File? {
        // Check in native libs directory
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val candidates = listOf(
            File(nativeLibDir, "libxray.so"),
            File(context.filesDir, "xray"),
            File(context.filesDir, "xray-core"),
        )
        return candidates.firstOrNull { it.exists() && it.canExecute() }
    }

    override suspend fun connect(destination: InetSocketAddress, domain: String?): ProxiedConnection {
        if (!isStarted) {
            throw Exception("Xray client not started")
        }
        return localSocks5Client.connect(destination, domain)
    }

    override fun close() {
        isStarted = false

        // Stop Xray-core
        // Via AndroidLibXrayLite:
        // Libv2ray.stopLoop()

        // Via binary:
        process?.destroy()
        process = null

        localSocks5Client.close()

        // Clean up config file
        File(context.filesDir, "xray_config_${server.id}.json").delete()
    }
}
