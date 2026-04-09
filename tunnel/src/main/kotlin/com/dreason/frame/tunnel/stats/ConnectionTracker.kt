package com.dreason.frame.tunnel.stats

import com.dreason.frame.core.model.ConnectionLog
import com.dreason.frame.core.model.Route
import com.dreason.frame.core.repository.LogRepository
import com.dreason.frame.tunnel.routing.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Tracks connections and logs them to the database.
 */
class ConnectionTracker(
    private val logRepository: LogRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun logConnection(
        sourceApp: String?,
        destinationDomain: String?,
        destinationIp: String,
        destinationPort: Int,
        transportProtocol: String,
        decision: RuleEngine.RouteDecision,
    ) {
        scope.launch {
            logRepository.insert(
                ConnectionLog(
                    timestamp = System.currentTimeMillis(),
                    sourceApp = sourceApp,
                    destinationDomain = destinationDomain,
                    destinationIp = destinationIp,
                    destinationPort = destinationPort,
                    transportProtocol = transportProtocol,
                    route = decision.route,
                )
            )
        }
    }
}
