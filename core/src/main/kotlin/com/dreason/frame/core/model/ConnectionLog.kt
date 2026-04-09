package com.dreason.frame.core.model

data class ConnectionLog(
    val id: Long = 0,
    val timestamp: Long,
    val sourceApp: String? = null,
    val destinationDomain: String? = null,
    val destinationIp: String,
    val destinationPort: Int,
    val transportProtocol: String,
    val route: Route,
    val matchedRuleId: Long? = null,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val durationMs: Long = 0,
)
