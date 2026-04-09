package com.dreason.frame.core.model

sealed class VpnState {
    data object Disconnected : VpnState()
    data object Connecting : VpnState()
    data class Connected(val startTime: Long = System.currentTimeMillis()) : VpnState()
    data class Error(val message: String) : VpnState()
}
