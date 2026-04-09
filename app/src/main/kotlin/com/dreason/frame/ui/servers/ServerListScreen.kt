package com.dreason.frame.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dreason.frame.core.model.ProxyProtocol
import com.dreason.frame.core.model.ProxyServer
import com.dreason.frame.core.model.Route
import com.dreason.frame.core.model.SecurityType
import com.dreason.frame.core.model.TransportType
import com.dreason.frame.core.model.VMessEncryption
import com.dreason.frame.core.model.VLessFlow
import com.dreason.frame.ui.theme.ChinaRoute
import com.dreason.frame.ui.theme.TaiwanRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    viewModel: ServerViewModel = hiltViewModel(),
) {
    val servers by viewModel.servers.collectAsState()
    val importError by viewModel.importError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(importError) {
        importError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("代理伺服器") },
                actions = {
                    // Import from clipboard
                    IconButton(onClick = {
                        val text = clipboardManager.getText()?.text
                        if (text != null) {
                            showImportDialog = true
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "從剪貼簿匯入")
                    }
                },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Import via share link
                FloatingActionButton(
                    onClick = { showImportDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(Icons.Default.Link, contentDescription = "匯入連結")
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Manual add
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "手動新增")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (servers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "尚未設定代理伺服器",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "支援 VMess / VLESS / Trojan / Shadowsocks / SOCKS5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "可直接貼上 vmess:// vless:// trojan:// 連結匯入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        onToggle = { viewModel.toggleServer(server) },
                        onDelete = { viewModel.deleteServer(server.id) },
                        onSetActive = { viewModel.setAsActive(server) },
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        ImportServerDialog(
            onDismiss = { showImportDialog = false },
            onImport = { link, routeGroup ->
                viewModel.importFromLink(link, routeGroup)
                showImportDialog = false
            },
            onImportClipboard = { text, routeGroup ->
                viewModel.importFromClipboard(text, routeGroup)
                showImportDialog = false
            },
            clipboardText = clipboardManager.getText()?.text,
        )
    }

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { server ->
                viewModel.addServer(server)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun ServerCard(
    server: ProxyServer,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onSetActive: () -> Unit,
) {
    val routeColor = when (server.routeGroup) {
        Route.CHINA_PROXY -> ChinaRoute
        Route.TAIWAN_PROXY -> TaiwanRoute
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = routeColor.copy(alpha = 0.05f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${server.host}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    Text(
                        text = server.protocol.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (server.protocol.isXray) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "[${server.transport.displayName}+${server.security.displayName}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = server.routeGroup.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = routeColor,
                    )
                }
            }

            IconButton(onClick = onSetActive) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "設為使用中",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Switch(checked = server.enabled, onCheckedChange = { onToggle() })

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "刪除",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// --- Import dialog (share link) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportServerDialog(
    onDismiss: () -> Unit,
    onImport: (String, Route) -> Unit,
    onImportClipboard: (String, Route) -> Unit,
    clipboardText: String?,
) {
    var link by remember { mutableStateOf("") }
    var routeGroup by remember { mutableStateOf(Route.CHINA_PROXY) }
    var routeExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("匯入伺服器") },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("貼上連結") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("從剪貼簿") },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        OutlinedTextField(
                            value = link,
                            onValueChange = { link = it },
                            label = { Text("分享連結") },
                            placeholder = { Text("vmess:// / vless:// / trojan://") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                        )
                    }
                    1 -> {
                        Text(
                            text = if (clipboardText != null) {
                                val lineCount = clipboardText.lines().count { it.isNotBlank() }
                                "剪貼簿中有 $lineCount 行內容"
                            } else {
                                "剪貼簿為空"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Route group picker
                ExposedDropdownMenuBox(
                    expanded = routeExpanded,
                    onExpandedChange = { routeExpanded = it },
                ) {
                    OutlinedTextField(
                        value = routeGroup.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("指定路由分組") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = routeExpanded,
                        onDismissRequest = { routeExpanded = false },
                    ) {
                        listOf(Route.CHINA_PROXY, Route.TAIWAN_PROXY).forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.displayName) },
                                onClick = { routeGroup = r; routeExpanded = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (selectedTab) {
                        0 -> onImport(link, routeGroup)
                        1 -> clipboardText?.let { onImportClipboard(it, routeGroup) }
                    }
                },
                enabled = when (selectedTab) {
                    0 -> link.isNotBlank()
                    1 -> clipboardText != null
                    else -> false
                },
            ) {
                Text("匯入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

// --- Manual add dialog (full form) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (ProxyServer) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    var protocol by remember { mutableStateOf(ProxyProtocol.VLESS) }
    var routeGroup by remember { mutableStateOf(Route.CHINA_PROXY) }

    // Xray fields
    var uuid by remember { mutableStateOf("") }
    var transport by remember { mutableStateOf(TransportType.TCP) }
    var security by remember { mutableStateOf(SecurityType.TLS) }
    var sni by remember { mutableStateOf("") }
    var wsPath by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf("chrome") }
    var vlessFlow by remember { mutableStateOf(VLessFlow.NONE) }

    // Legacy fields
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var encryptMethod by remember { mutableStateOf("") }

    var protocolExpanded by remember { mutableStateOf(false) }
    var routeExpanded by remember { mutableStateOf(false) }
    var transportExpanded by remember { mutableStateOf(false) }
    var securityExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手動新增伺服器") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("名稱") }, modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = host, onValueChange = { host = it },
                            label = { Text("地址") }, modifier = Modifier.weight(2f),
                        )
                        OutlinedTextField(
                            value = port, onValueChange = { port = it },
                            label = { Text("連接埠") }, modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Protocol picker
                item {
                    ExposedDropdownMenuBox(
                        expanded = protocolExpanded,
                        onExpandedChange = { protocolExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = protocol.displayName, onValueChange = {}, readOnly = true,
                            label = { Text("協議") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(expanded = protocolExpanded, onDismissRequest = { protocolExpanded = false }) {
                            ProxyProtocol.entries.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.displayName) },
                                    onClick = { protocol = p; protocolExpanded = false },
                                )
                            }
                        }
                    }
                }

                // Route group
                item {
                    ExposedDropdownMenuBox(
                        expanded = routeExpanded,
                        onExpandedChange = { routeExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = routeGroup.displayName, onValueChange = {}, readOnly = true,
                            label = { Text("路由分組") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(expanded = routeExpanded, onDismissRequest = { routeExpanded = false }) {
                            listOf(Route.CHINA_PROXY, Route.TAIWAN_PROXY).forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.displayName) },
                                    onClick = { routeGroup = r; routeExpanded = false },
                                )
                            }
                        }
                    }
                }

                // Xray-specific fields
                if (protocol.isXray) {
                    item {
                        OutlinedTextField(
                            value = uuid, onValueChange = { uuid = it },
                            label = { Text("UUID") }, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        ExposedDropdownMenuBox(
                            expanded = transportExpanded,
                            onExpandedChange = { transportExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = transport.displayName, onValueChange = {}, readOnly = true,
                                label = { Text("傳輸方式") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transportExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            )
                            ExposedDropdownMenu(expanded = transportExpanded, onDismissRequest = { transportExpanded = false }) {
                                TransportType.entries.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.displayName) },
                                        onClick = { transport = t; transportExpanded = false },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        ExposedDropdownMenuBox(
                            expanded = securityExpanded,
                            onExpandedChange = { securityExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = security.displayName, onValueChange = {}, readOnly = true,
                                label = { Text("加密方式") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = securityExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            )
                            ExposedDropdownMenu(expanded = securityExpanded, onDismissRequest = { securityExpanded = false }) {
                                SecurityType.entries.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.displayName) },
                                        onClick = { security = s; securityExpanded = false },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = sni, onValueChange = { sni = it },
                            label = { Text("SNI (選填)") }, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (transport == TransportType.WEBSOCKET) {
                        item {
                            OutlinedTextField(
                                value = wsPath, onValueChange = { wsPath = it },
                                label = { Text("WebSocket Path") },
                                placeholder = { Text("/ws") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // SOCKS5/HTTP auth
                if (protocol == ProxyProtocol.SOCKS5 || protocol == ProxyProtocol.HTTP) {
                    item {
                        OutlinedTextField(
                            value = username, onValueChange = { username = it },
                            label = { Text("帳號 (選填)") }, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("密碼 (選填)") }, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Shadowsocks
                if (protocol == ProxyProtocol.SHADOWSOCKS) {
                    item {
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("密碼") }, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = encryptMethod, onValueChange = { encryptMethod = it },
                            label = { Text("加密方式") },
                            placeholder = { Text("aes-256-gcm") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Trojan password
                if (protocol == ProxyProtocol.TROJAN) {
                    item {
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("密碼") }, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val portNum = port.toIntOrNull() ?: return@TextButton
                    onConfirm(
                        ProxyServer(
                            name = name.ifBlank { "$host:$port" },
                            host = host, port = portNum,
                            protocol = protocol, routeGroup = routeGroup,
                            uuid = uuid.ifBlank { null },
                            transport = transport, security = security,
                            sni = sni.ifBlank { null },
                            fingerprint = fingerprint.ifBlank { null },
                            wsPath = wsPath.ifBlank { null },
                            vlessFlow = vlessFlow,
                            username = username.ifBlank { null },
                            password = password.ifBlank { null },
                            encryptMethod = encryptMethod.ifBlank { null },
                        )
                    )
                },
                enabled = host.isNotBlank() && port.toIntOrNull() != null,
            ) {
                Text("確認")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
