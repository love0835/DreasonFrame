package com.dreason.frame.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proxy_servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val routeGroup: String,
    val enabled: Boolean = true,
    val latencyMs: Int? = null,

    // Common auth
    val username: String? = null,
    val password: String? = null,

    // Xray
    val uuid: String? = null,
    val transport: String? = null,
    val wsPath: String? = null,
    val wsHost: String? = null,
    val grpcServiceName: String? = null,
    val security: String? = null,
    val sni: String? = null,
    val fingerprint: String? = null,
    val alpn: String? = null,
    val allowInsecure: Boolean = false,
    val realityPublicKey: String? = null,
    val realityShortId: String? = null,
    val vmessEncryption: String? = null,
    val alterId: Int = 0,
    val vlessFlow: String? = null,

    // Shadowsocks
    val encryptMethod: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
