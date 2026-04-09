package com.dreason.frame.tunnel.stats

import com.dreason.frame.core.model.Route
import com.dreason.frame.core.model.TrafficStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Monitors traffic statistics per route.
 */
class TrafficMonitor {

    private val chinaBytes = AtomicLong(0)
    private val taiwanBytes = AtomicLong(0)
    private val directBytes = AtomicLong(0)
    private val activeConnections = AtomicInteger(0)
    private val totalConnections = AtomicLong(0)

    private val _stats = MutableStateFlow(TrafficStats())
    val stats: StateFlow<TrafficStats> = _stats.asStateFlow()

    fun addBytes(route: Route, bytes: Long) {
        when (route) {
            Route.CHINA_PROXY -> chinaBytes.addAndGet(bytes)
            Route.TAIWAN_PROXY -> taiwanBytes.addAndGet(bytes)
            Route.DIRECT -> directBytes.addAndGet(bytes)
            Route.REJECT -> { /* no traffic for rejected connections */ }
        }
        updateStats()
    }

    fun connectionOpened() {
        activeConnections.incrementAndGet()
        totalConnections.incrementAndGet()
        updateStats()
    }

    fun connectionClosed() {
        activeConnections.decrementAndGet()
        updateStats()
    }

    fun reset() {
        chinaBytes.set(0)
        taiwanBytes.set(0)
        directBytes.set(0)
        activeConnections.set(0)
        totalConnections.set(0)
        updateStats()
    }

    private fun updateStats() {
        _stats.value = TrafficStats(
            chinaBytes = chinaBytes.get(),
            taiwanBytes = taiwanBytes.get(),
            directBytes = directBytes.get(),
            activeConnections = activeConnections.get(),
            totalConnections = totalConnections.get(),
        )
    }
}
