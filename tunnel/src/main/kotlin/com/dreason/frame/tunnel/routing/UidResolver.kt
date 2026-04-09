package com.dreason.frame.tunnel.routing

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetSocketAddress

/**
 * Resolves the UID (and thus package name) of the app that owns a given network socket.
 *
 * On API 29+, uses ConnectivityManager.getConnectionOwnerUid().
 * On older APIs, falls back to parsing /proc/net/tcp and /proc/net/tcp6.
 */
class UidResolver(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Cache UID -> packageName mapping
    private val uidToPackage = HashMap<Int, String>()

    /**
     * Resolve the package name of the app that owns a connection from the given local address.
     */
    fun resolve(localAddress: InetSocketAddress, remoteAddress: InetSocketAddress, protocol: Int = 6): String? {
        val uid = getUid(localAddress, remoteAddress, protocol) ?: return null
        return getPackageName(uid)
    }

    private fun getUid(local: InetSocketAddress, remote: InetSocketAddress, protocol: Int): Int? {
        // API 29+: Use ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return try {
                connectivityManager.getConnectionOwnerUid(
                    protocol, local, remote
                ).takeIf { it >= 0 }
            } catch (e: Exception) {
                getUidFromProc(local.port)
            }
        }
        return getUidFromProc(local.port)
    }

    /**
     * Parse /proc/net/tcp to find the UID for a given local port.
     */
    private fun getUidFromProc(localPort: Int): Int? {
        val hexPort = String.format("%04X", localPort)

        for (path in listOf("/proc/net/tcp", "/proc/net/tcp6")) {
            try {
                BufferedReader(FileReader(path)).use { reader ->
                    reader.readLine() // Skip header
                    var line = reader.readLine()
                    while (line != null) {
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size >= 8) {
                            val localAddr = parts[1]
                            val port = localAddr.substringAfter(":")
                            if (port.equals(hexPort, ignoreCase = true)) {
                                return parts[7].toIntOrNull()
                            }
                        }
                        line = reader.readLine()
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    fun getPackageName(uid: Int): String? {
        uidToPackage[uid]?.let { return it }

        val packages = packageManager.getPackagesForUid(uid)
        val name = packages?.firstOrNull()
        if (name != null) {
            uidToPackage[uid] = name
        }
        return name
    }

    fun clearCache() {
        uidToPackage.clear()
    }
}
