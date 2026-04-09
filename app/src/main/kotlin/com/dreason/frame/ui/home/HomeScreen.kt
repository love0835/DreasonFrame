package com.dreason.frame.ui.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dreason.frame.core.model.VpnState
import com.dreason.frame.ui.theme.ChinaRoute
import com.dreason.frame.ui.theme.DirectRoute
import com.dreason.frame.ui.theme.TaiwanRoute

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val vpnPrepare = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleVpn()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "DreasonFrame",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "地區流量自動分流",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // VPN Toggle Button
        VpnToggleButton(
            state = uiState.vpnState,
            onClick = {
                val prepareIntent = viewModel.prepareVpn()
                if (prepareIntent != null) {
                    vpnPrepare.launch(prepareIntent)
                } else {
                    viewModel.toggleVpn()
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status text
        Text(
            text = when (uiState.vpnState) {
                is VpnState.Disconnected -> "未連線"
                is VpnState.Connecting -> "連線中..."
                is VpnState.Connected -> "已連線"
                is VpnState.Error -> "錯誤: ${(uiState.vpnState as VpnState.Error).message}"
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Server status cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServerStatusCard(
                modifier = Modifier.weight(1f),
                label = "中國代理",
                serverName = uiState.chinaServerName ?: "未設定",
                subtitle = if (uiState.chinaServerName == null) "走手機網路" else null,
                color = ChinaRoute,
                isConfigured = uiState.chinaServerName != null,
            )
            ServerStatusCard(
                modifier = Modifier.weight(1f),
                label = "台灣代理",
                serverName = uiState.taiwanServerName ?: "未設定",
                subtitle = if (uiState.taiwanServerName == null) "走手機網路" else null,
                color = TaiwanRoute,
                isConfigured = uiState.taiwanServerName != null,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Traffic stats
        TrafficStatsCard(stats = uiState.trafficStats)
    }
}

@Composable
private fun VpnToggleButton(
    state: VpnState,
    onClick: () -> Unit,
) {
    val isConnected = state is VpnState.Connected
    val isConnecting = state is VpnState.Connecting

    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> MaterialTheme.colorScheme.primary
            isConnecting -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "buttonColor",
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isConnected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "iconColor",
    )

    val scale by animateFloatAsState(
        targetValue = if (isConnecting) 0.95f else 1f,
        label = "scale",
    )

    Surface(
        shape = CircleShape,
        color = buttonColor,
        modifier = Modifier
            .size(120.dp)
            .scale(scale),
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = "VPN 開關",
            tint = iconColor,
            modifier = Modifier
                .padding(32.dp)
                .size(56.dp),
        )
    }
}

@Composable
private fun ServerStatusCard(
    modifier: Modifier = Modifier,
    label: String,
    serverName: String,
    subtitle: String? = null,
    color: Color,
    isConfigured: Boolean = true,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isConfigured) color.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isConfigured) color else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = serverName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isConfigured) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = DirectRoute,
                )
            }
        }
    }
}

@Composable
private fun TrafficStatsCard(
    stats: com.dreason.frame.core.model.TrafficStats,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "流量統計",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem("中國", formatBytes(stats.chinaBytes), ChinaRoute)
                StatItem("台灣", formatBytes(stats.taiwanBytes), TaiwanRoute)
                StatItem("直連", formatBytes(stats.directBytes), DirectRoute)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "活躍連線: ${stats.activeConnections} | 總連線: ${stats.totalConnections}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${bytes / 1024}KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0 / 1024.0)}MB"
    else -> "${"%.2f".format(bytes / 1024.0 / 1024.0 / 1024.0)}GB"
}
