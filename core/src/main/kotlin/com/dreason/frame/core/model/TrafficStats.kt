package com.dreason.frame.core.model

data class TrafficStats(
    val chinaBytes: Long = 0,
    val taiwanBytes: Long = 0,
    val directBytes: Long = 0,
    val activeConnections: Int = 0,
    val totalConnections: Long = 0,
)
