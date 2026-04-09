package com.dreason.frame.core.repository

import com.dreason.frame.core.database.dao.LogDao
import com.dreason.frame.core.database.entity.LogEntity
import com.dreason.frame.core.model.ConnectionLog
import com.dreason.frame.core.model.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogDao,
) {

    fun getRecent(limit: Int = 200): Flow<List<ConnectionLog>> =
        logDao.getRecent(limit).map { entities -> entities.map { it.toDomain() } }

    fun getByApp(packageName: String): Flow<List<ConnectionLog>> =
        logDao.getByApp(packageName).map { entities -> entities.map { it.toDomain() } }

    fun getByRoute(route: Route): Flow<List<ConnectionLog>> =
        logDao.getByRoute(route.name).map { entities -> entities.map { it.toDomain() } }

    suspend fun insert(log: ConnectionLog): Long =
        logDao.insert(log.toEntity())

    suspend fun deleteOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        logDao.deleteOlderThan(cutoff)
    }

    suspend fun deleteAll() = logDao.deleteAll()

    suspend fun getTotalBytesByRoute(route: Route, since: Long): Long =
        logDao.getTotalBytesByRoute(route.name, since) ?: 0L

    private fun LogEntity.toDomain() = ConnectionLog(
        id = id,
        timestamp = timestamp,
        sourceApp = sourceApp,
        destinationDomain = destinationDomain,
        destinationIp = destinationIp,
        destinationPort = destinationPort,
        transportProtocol = transportProtocol,
        route = Route.valueOf(route),
        matchedRuleId = matchedRuleId,
        bytesSent = bytesSent,
        bytesReceived = bytesReceived,
        durationMs = durationMs,
    )

    private fun ConnectionLog.toEntity() = LogEntity(
        id = id,
        timestamp = timestamp,
        sourceApp = sourceApp,
        destinationDomain = destinationDomain,
        destinationIp = destinationIp,
        destinationPort = destinationPort,
        transportProtocol = transportProtocol,
        route = route.name,
        matchedRuleId = matchedRuleId,
        bytesSent = bytesSent,
        bytesReceived = bytesReceived,
        durationMs = durationMs,
    )
}
