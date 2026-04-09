package com.dreason.frame.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreason.frame.core.model.ProxyServer
import com.dreason.frame.core.model.Route
import com.dreason.frame.core.preferences.AppPreferences
import com.dreason.frame.core.repository.ServerRepository
import com.dreason.frame.tunnel.proxy.xray.XrayConfigGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    fun addServer(server: ProxyServer) {
        viewModelScope.launch {
            val id = serverRepository.insert(server)
            autoAssign(id, server.routeGroup)
        }
    }

    /**
     * Import a server from a share link (vmess://, vless://, trojan://).
     * @param link The share link
     * @param routeGroup Which route group to assign this server to
     */
    fun importFromLink(link: String, routeGroup: Route) {
        viewModelScope.launch {
            val server = XrayConfigGenerator.parseShareLink(link.trim())
            if (server != null) {
                val id = serverRepository.insert(server.copy(routeGroup = routeGroup))
                autoAssign(id, routeGroup)
                _importError.value = null
            } else {
                _importError.value = "無法解析分享連結，請確認格式正確"
            }
        }
    }

    /**
     * Import multiple servers from clipboard text (one link per line).
     */
    fun importFromClipboard(text: String, routeGroup: Route) {
        viewModelScope.launch {
            val lines = text.trim().lines().filter { it.isNotBlank() }
            var imported = 0

            for (line in lines) {
                val server = XrayConfigGenerator.parseShareLink(line.trim())
                if (server != null) {
                    val id = serverRepository.insert(server.copy(routeGroup = routeGroup))
                    if (imported == 0) autoAssign(id, routeGroup)
                    imported++
                }
            }

            _importError.value = if (imported > 0) {
                null
            } else {
                "未找到可匯入的伺服器連結"
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

    fun setAsActive(server: ProxyServer) {
        viewModelScope.launch {
            autoAssign(server.id, server.routeGroup)
        }
    }

    fun clearImportError() {
        _importError.value = null
    }

    private suspend fun autoAssign(id: Long, routeGroup: Route) {
        when (routeGroup) {
            Route.CHINA_PROXY -> preferences.setChinaServerId(id)
            Route.TAIWAN_PROXY -> preferences.setTaiwanServerId(id)
            else -> {}
        }
    }
}
