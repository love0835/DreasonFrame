package com.dreason.frame.ui.home

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dreason.frame.core.model.TrafficStats
import com.dreason.frame.core.model.VpnState
import com.dreason.frame.core.preferences.AppPreferences
import com.dreason.frame.core.repository.ServerRepository
import com.dreason.frame.tunnel.vpn.VpnController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val vpnState: VpnState = VpnState.Disconnected,
    val chinaServerName: String? = null,
    val taiwanServerName: String? = null,
    val trafficStats: TrafficStats = TrafficStats(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val serverRepository: ServerRepository,
    private val preferences: AppPreferences,
) : AndroidViewModel(application) {

    val vpnState: StateFlow<VpnState> = VpnController.state

    val uiState: StateFlow<HomeUiState> = combine(
        VpnController.state,
        serverRepository.getAll(),
    ) { state, servers ->
        HomeUiState(
            vpnState = state,
            chinaServerName = servers.firstOrNull {
                it.routeGroup == com.dreason.frame.core.model.Route.CHINA_PROXY && it.enabled
            }?.name,
            taiwanServerName = servers.firstOrNull {
                it.routeGroup == com.dreason.frame.core.model.Route.TAIWAN_PROXY && it.enabled
            }?.name,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun toggleVpn() {
        when (VpnController.state.value) {
            is VpnState.Connected -> VpnController.stop(application)
            is VpnState.Disconnected, is VpnState.Error -> VpnController.start(application)
            is VpnState.Connecting -> { /* ignore during connecting */ }
        }
    }

    fun prepareVpn(): Intent? = VpnService.prepare(application)
}
