package com.dreason.frame.tunnel.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.dreason.frame.core.model.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnController {

    private val _state = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    fun updateState(newState: VpnState) {
        _state.value = newState
    }

    fun start(context: Context) {
        val intent = Intent(context, DreasonVpnService::class.java).apply {
            action = DreasonVpnService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, DreasonVpnService::class.java).apply {
            action = DreasonVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun prepareVpn(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    val isConnected: Boolean
        get() = _state.value is VpnState.Connected
}
