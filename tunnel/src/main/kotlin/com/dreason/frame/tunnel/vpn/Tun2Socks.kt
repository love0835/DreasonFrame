package com.dreason.frame.tunnel.vpn

/**
 * JNI bridge to hev-socks5-tunnel native library.
 *
 * hev-socks5-tunnel reads raw IP packets from TUN fd, implements a userspace TCP/IP stack,
 * and forwards connections as SOCKS5 to the local ProxyDispatcher.
 *
 * The native library needs to be built from source via NDK and included in jniLibs.
 * See: https://github.com/heiher/hev-socks5-tunnel
 */
object Tun2Socks {

    private var isRunning = false

    init {
        try {
            System.loadLibrary("hev-socks5-tunnel")
        } catch (e: UnsatisfiedLinkError) {
            // Native library not yet integrated — will use fallback in development
        }
    }

    /**
     * Start the tun2socks tunnel.
     * @param tunFd File descriptor of the TUN interface
     * @param localSocksPort Port of the local SOCKS5 dispatcher
     * @param mtu MTU of the TUN interface
     */
    fun start(tunFd: Int, localSocksPort: Int, mtu: Int) {
        if (isRunning) return
        isRunning = true
        nativeStart(tunFd, "127.0.0.1", localSocksPort, mtu)
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        nativeStop()
    }

    fun isRunning(): Boolean = isRunning

    // Native methods — implemented in hev-socks5-tunnel C library
    private external fun nativeStart(tunFd: Int, socksAddr: String, socksPort: Int, mtu: Int)
    private external fun nativeStop()
}
