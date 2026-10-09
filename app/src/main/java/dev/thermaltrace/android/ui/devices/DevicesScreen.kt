package dev.thermaltrace.android.ui.devices

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thermaltrace.android.data.model.DeviceSummary
import dev.thermaltrace.android.data.model.IngestStatusResponse
import dev.thermaltrace.android.data.model.ThermostatConnectionStatus
import dev.thermaltrace.android.ui.theme.brandTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun openConnectUrl(url: String?) {
        url?.takeIf { it.isNotBlank() }?.let {
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(it))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(brandTitle()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Devices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Rename or set space. Add/remove devices on the website.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.ingestStatus?.takeIf { it.waiting }?.let { ingest ->
                    item { IngestWaitingCard(ingest) }
                }

                state.thermostat?.takeIf { it.canUse || it.canConnect }?.let { thermo ->
                    item {
                        Text("Thermostats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (thermo.connections.isNotEmpty()) {
                        items(thermo.connections, key = { it.provider }) { connection ->
                            ThermostatSnapshotCard(connection)
                        }
                    }
                    if (thermo.canConnect) {
                        val nestConnected = thermo.connections.any { it.provider == "nest" }
                        val ecobeeConnected = thermo.connections.any { it.provider == "ecobee" }
                        val nestUrl = thermo.connectUrls?.nest?.takeIf { thermo.configured.nest }
                        val ecobeeUrl = thermo.connectUrls?.ecobee?.takeIf { thermo.configured.ecobee }
                        val showNest = !nestConnected && nestUrl != null
                        val showEcobee = !ecobeeConnected && ecobeeUrl != null
                        if (showNest || showEcobee) {
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (showNest) {
                                        OutlinedButton(
                                            onClick = { openConnectUrl(nestUrl) },
                                            modifier = Modifier.weight(1f),
                                        ) { Text("Connect Nest") }
                                    }
                                    if (showEcobee) {
                                        OutlinedButton(
                                            onClick = { openConnectUrl(ecobeeUrl) },
                                            modifier = Modifier.weight(1f),
                                        ) { Text("Connect Ecobee") }
                                    }
                                }
                            }
                        } else if (!nestConnected && !ecobeeConnected) {
                            item {
                                Text(
                                    "Thermostat OAuth is not configured on this deployment yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.tertiary) } }

                if (state.devices.isEmpty()) {
                    item {
                        Text(
                            "No devices yet. Create them on probeharbor.dev.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(state.devices, key = { it.id }) { device ->
                    if (state.editingId == device.id) {
                        EditDeviceCard(
                            name = state.draftName,
                            space = state.draftSpace,
                            saving = state.saving,
                            onName = viewModel::onDraftName,
                            onSpace = viewModel::onDraftSpace,
                            onSave = viewModel::saveEdit,
                            onCancel = viewModel::cancelEdit,
                        )
                    } else {
                        DeviceCard(device, onEdit = { viewModel.startEdit(device) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IngestWaitingCard(ingest: IngestStatusResponse) {
    Column(Modifier.fillMaxWidth()) {
        Text("Waiting for first POST", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
        Text(
            "${ingest.waitingCount} device(s) awaiting data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ingest.devices.forEach { device ->
            Text(
                "${device.name} (${device.sensorKeys.joinToString()})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThermostatSnapshotCard(connection: ThermostatConnectionStatus) {
    Column(Modifier.fillMaxWidth()) {
        Text(connection.provider.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Medium)
        Text(
            listOfNotNull(
                connection.ambientTempF?.let { "Ambient ${"%.1f".format(it)}°F" },
                connection.heatSetpointF?.let { "Heat set ${"%.1f".format(it)}°F" },
                connection.hvacMode,
                connection.connectedAt?.let { "Since $it" },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeviceCard(device: DeviceSummary, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                listOfNotNull(
                    device.space?.takeIf { it.isNotBlank() }?.let { "Space: $it" },
                    "${device.sensorCount} sensor(s)",
                    device.kinds.joinToString(", ").takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
    }
}

@Composable
private fun EditDeviceCard(
    name: String,
    space: String,
    saving: Boolean,
    onName: (String) -> Unit,
    onSpace: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = space,
            onValueChange = onSpace,
            label = { Text("Space (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = !saving) {
                if (saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                else Text("Save")
            }
            OutlinedButton(onClick = onCancel, enabled = !saving) { Text("Cancel") }
        }
        Spacer(Modifier.height(4.dp))
    }
}
