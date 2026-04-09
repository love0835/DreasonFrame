package com.dreason.frame.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val sourceApp: String? = null,
    val destinationDomain: String? = null,
    val destinationIp: String,
    val destinationPort: Int,
    val transportProtocol: String,
    val route: String,
    val matchedRuleId: Long? = null,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val durationMs: Long = 0,
)
