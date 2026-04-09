package com.dreason.frame.tunnel.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.dreason.frame.core.model.Route
import com.dreason.frame.core.model.VpnState
import com.dreason.frame.core.preferences.AppPreferences
import com.dreason.frame.core.repository.RuleRepository
import com.dreason.frame.core.repository.ServerRepository
import com.dreason.frame.tunnel.dns.DnsInterceptor
import com.dreason.frame.tunnel.dns.FakeIpPool
import com.dreason.frame.tunnel.proxy.DirectClient
import com.dreason.frame.tunnel.proxy.HttpProxyClient
import com.dreason.frame.tunnel.proxy.ProxyDispatcher
import com.dreason.frame.tunnel.proxy.Socks5Client
import com.dreason.frame.tunnel.routing.AppMatcher
import com.dreason.frame.tunnel.routing.CidrMatcher
import com.dreason.frame.tunnel.routing.DomainMatcher
import com.dreason.frame.tunnel.routing.RuleEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DreasonVpnService : VpnService() {

    @Inject lateinit var serverRepository: ServerRepository
    @Inject lateinit var ruleRepository: RuleRepository
    @Inject lateinit var preferences: AppPreferences

    private var tunFd: ParcelFileDescriptor? = null
    private var proxyDispatcher: ProxyDispatcher? = null
    private var dnsInterceptor: DnsInterceptor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_START = "com.dreason.frame.START_VPN"
        const val ACTION_STOP = "com.dreason.frame.STOP_VPN"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "dreason_vpn_channel"
        private const val TUN_ADDRESS = "10.0.0.2"
        private const val TUN_DNS = "10.0.0.1"
        private const val TUN_MTU = 1500
        private const val LOCAL_DISPATCHER_PORT = 10808
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                serviceScope.launch { startVpn() }
            }
        }
        return START_STICKY
    }

    private suspend fun startVpn() {
        try {
            VpnController.updateState(VpnState.Connecting)

            // Build TUN interface
            val builder = Builder()
                .setSession("DreasonFrame")
                .addAddress(TUN_ADDRESS, 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(TUN_DNS)
                .setMtu(TUN_MTU)
                .setBlocking(false)

            // Exclude our own app to prevent routing loops
            builder.addDisallowedApplication(packageName)

            tunFd = builder.establish() ?: run {
                VpnController.updateState(VpnState.Error("無法建立 VPN 通道"))
                return
            }

            // Load rules and build matchers
            val rules = ruleRepository.getAllEnabled()
            val domainMatcher = DomainMatcher()
            val cidrMatcher = CidrMatcher()
            val appMatcher = AppMatcher()

            rules.forEach { rule ->
                when (rule.type) {
                    com.dreason.frame.core.model.RuleType.DOMAIN_EXACT,
                    com.dreason.frame.core.model.RuleType.DOMAIN_SUFFIX,
                    com.dreason.frame.core.model.RuleType.DOMAIN_KEYWORD ->
                        domainMatcher.addRule(rule)
                    com.dreason.frame.core.model.RuleType.IP_CIDR ->
                        cidrMatcher.addRule(rule)
                    com.dreason.frame.core.model.RuleType.APP ->
                        appMatcher.addRule(rule)
                }
            }

            val defaultRoute = preferences.defaultRoute.first()
            val ruleEngine = RuleEngine(appMatcher, domainMatcher, cidrMatcher, defaultRoute)

            // Set up FakeIP pool for DNS interception
            val fakeIpPool = FakeIpPool()

            // Set up DNS interceptor
            val dnsServerAddr = preferences.dnsServer.first()
            dnsInterceptor = DnsInterceptor(
                tunFd = tunFd!!,
                fakeIpPool = fakeIpPool,
                upstreamDns = dnsServerAddr,
                vpnService = this,
            )

            // Load proxy servers
            val chinaServer = preferences.chinaServerId.first()?.let { serverRepository.getById(it) }
            val taiwanServer = preferences.taiwanServerId.first()?.let { serverRepository.getById(it) }

            // Set up proxy dispatcher
            proxyDispatcher = ProxyDispatcher(
                port = LOCAL_DISPATCHER_PORT,
                ruleEngine = ruleEngine,
                fakeIpPool = fakeIpPool,
                chinaServer = chinaServer,
                taiwanServer = taiwanServer,
                vpnService = this,
            )

            // Start components
            dnsInterceptor?.start()
            proxyDispatcher?.start()

            // Start tun2socks (native layer connects TUN fd to local SOCKS5 dispatcher)
            Tun2Socks.start(
                tunFd = tunFd!!.fd,
                localSocksPort = LOCAL_DISPATCHER_PORT,
                mtu = TUN_MTU,
            )

            VpnController.updateState(VpnState.Connected())
        } catch (e: Exception) {
            VpnController.updateState(VpnState.Error(e.message ?: "VPN 啟動失敗"))
            stopVpn()
        }
    }

    private fun stopVpn() {
        Tun2Socks.stop()
        dnsInterceptor?.stop()
        proxyDispatcher?.stop()
        tunFd?.close()
        tunFd = null
        VpnController.updateState(VpnState.Disconnected)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DreasonFrame VPN",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "VPN 連線狀態通知"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, DreasonVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DreasonFrame")
            .setContentText("VPN 已連線 — 流量分流中")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "中斷連線",
                stopPendingIntent,
            )
            .build()
    }
}
