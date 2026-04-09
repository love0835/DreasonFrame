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
    val username: String? = null,
    val password: String? = null,
    val encryptMethod: String? = null,
    val routeGroup: String,
    val enabled: Boolean = true,
    val latencyMs: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
