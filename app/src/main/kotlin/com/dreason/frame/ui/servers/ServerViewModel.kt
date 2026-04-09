package com.dreason.frame.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreason.frame.core.model.ProxyProtocol
import com.dreason.frame.core.model.ProxyServer
import com.dreason.frame.core.model.Route
import com.dreason.frame.core.preferences.AppPreferences
import com.dreason.frame.core.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    val servers: StateFlow<List<ProxyServer>> = serverRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun addServer(
        name: String,
        host: String,
        port: Int,
        protocol: ProxyProtocol,
        routeGroup: Route,
        username: String? = null,
        password: String? = null,
        encryptMethod: String? = null,
    ) {
        viewModelScope.launch {
            val id = serverRepository.insert(
                ProxyServer(
                    name = name,
                    host = host,
                    port = port,
                    protocol = protocol,
                    username = username,
                    password = password,
                    encryptMethod = encryptMethod,
                    routeGroup = routeGroup,
                )
            )
            // Auto-assign as active server for the route group
            when (routeGroup) {
                Route.CHINA_PROXY -> preferences.setChinaServerId(id)
                Route.TAIWAN_PROXY -> preferences.setTaiwanServerId(id)
                else -> {}
            }
        }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch {
            serverRepository.deleteById(id)
        }
    }

    fun toggleServer(server: ProxyServer) {
        viewModelScope.launch {
            serverRepository.update(server.copy(enabled = !server.enabled))
        }
    }
}
