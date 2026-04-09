package com.dreason.frame.core.repository

import com.dreason.frame.core.database.dao.ServerDao
import com.dreason.frame.core.database.entity.ServerEntity
import com.dreason.frame.core.model.ProxyProtocol
import com.dreason.frame.core.model.ProxyServer
import com.dreason.frame.core.model.Route
import com.dreason.frame.core.model.SecurityType
import com.dreason.frame.core.model.TransportType
import com.dreason.frame.core.model.VLessFlow
import com.dreason.frame.core.model.VMessEncryption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val serverDao: ServerDao,
) {

    fun getAll(): Flow<List<ProxyServer>> =
        serverDao.getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getById(id: Long): ProxyServer? =
        serverDao.getById(id)?.toDomain()

    suspend fun getActiveByRouteGroup(route: Route): ProxyServer? =
        serverDao.getActiveByRouteGroup(route.name)?.toDomain()

    suspend fun getAllEnabled(): List<ProxyServer> =
        serverDao.getAllEnabled().map { it.toDomain() }

    suspend fun insert(server: ProxyServer): Long =
        serverDao.insert(server.toEntity())

    suspend fun update(server: ProxyServer) =
        serverDao.update(server.toEntity().copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteById(id: Long) =
        serverDao.deleteById(id)

    private fun ServerEntity.toDomain() = ProxyServer(
        id = id,
        name = name,
        host = host,
        port = port,
        protocol = ProxyProtocol.valueOf(protocol),
        routeGroup = Route.valueOf(routeGroup),
        enabled = enabled,
        latencyMs = latencyMs,
        username = username,
        password = password,
        uuid = uuid,
        transport = transport?.let { TransportType.valueOf(it) } ?: TransportType.TCP,
        wsPath = wsPath,
        wsHost = wsHost,
        grpcServiceName = grpcServiceName,
        security = security?.let { SecurityType.valueOf(it) } ?: SecurityType.NONE,
        sni = sni,
        fingerprint = fingerprint,
        alpn = alpn,
        allowInsecure = allowInsecure,
        realityPublicKey = realityPublicKey,
        realityShortId = realityShortId,
        vmessEncryption = vmessEncryption?.let { VMessEncryption.valueOf(it) } ?: VMessEncryption.AUTO,
        alterId = alterId,
        vlessFlow = vlessFlow?.let { VLessFlow.valueOf(it) } ?: VLessFlow.NONE,
        encryptMethod = encryptMethod,
    )

    private fun ProxyServer.toEntity() = ServerEntity(
        id = id,
        name = name,
        host = host,
        port = port,
        protocol = protocol.name,
        routeGroup = routeGroup.name,
        enabled = enabled,
        latencyMs = latencyMs,
        username = username,
        password = password,
        uuid = uuid,
        transport = transport.name,
        wsPath = wsPath,
        wsHost = wsHost,
        grpcServiceName = grpcServiceName,
        security = security.name,
        sni = sni,
        fingerprint = fingerprint,
        alpn = alpn,
        allowInsecure = allowInsecure,
        realityPublicKey = realityPublicKey,
        realityShortId = realityShortId,
        vmessEncryption = vmessEncryption.name,
        alterId = alterId,
        vlessFlow = vlessFlow.name,
        encryptMethod = encryptMethod,
    )
}
