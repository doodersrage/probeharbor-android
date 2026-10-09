package dev.thermaltrace.android.ui.history

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thermaltrace.android.data.model.ChartPointDto
import dev.thermaltrace.android.ui.theme.BrandTrace
import dev.thermaltrace.android.ui.theme.brandTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.claimsFile) {
        val file = state.claimsFile ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Open claims pack")) }
        viewModel.clearClaimsFile()
    }

    if (state.emailDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEmailDialog,
            title = { Text("Email claims pack") },
            text = {
                Column {
                    Text(
                        "Send a printable claims pack to your adjuster.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.adjusterEmail,
                        onValueChange = viewModel::onAdjusterEmailChange,
                        label = { Text("Adjuster email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    state.emailMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::emailClaimsPack,
                    enabled = !state.emailBusy,
                ) {
                    if (state.emailBusy) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                    } else {
                        Text("Send")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEmailDialog) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(brandTitle()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
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
                    Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 7, 30).forEach { days ->
                            FilterChip(
                                selected = state.days == days,
                                onClick = { viewModel.refresh(days) },
                                label = {
                                    Text(
                                        when (days) {
                                            1 -> "24h"
                                            7 -> "7d"
                                            else -> "30d"
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
                state.error?.let {
                    item { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                item {
                    Text("Claims / insurance pack", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.canUseClaimsPack) {
                            "Printable HTML for the selected window (Pro). Open and Print → Save as PDF."
                        } else {
                            "Claims pack is a Pro feature. Upgrade on probeharbor.dev to download or email a printable pack."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::downloadClaimsPack,
                        enabled = !state.claimsBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.claimsBusy) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                        } else {
                            Text("Download claims pack")
                        }
                    }
                    state.claimsMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::openEmailDialog,
                        enabled = !state.emailBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Email claims pack to adjuster")
                    }
                    state.emailMessage?.takeIf { !state.emailDialogOpen }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                item {
                    Text("Temperature (°F)", style = MaterialTheme.typography.titleMedium)
                    if (state.points.isEmpty()) {
                        Text("No chart points in this window.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Sparkline(
                            garagePoints = state.points,
                            housePoints = state.housePoints.takeIf { it.size >= 2 }.orEmpty(),
                        )
                        val temps = state.points.map { it.tempf }
                        Text(
                            "Min ${temps.minOrNull()?.let { "%.1f".format(it) }}°F · Max ${temps.maxOrNull()?.let { "%.1f".format(it) }}°F",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.housePoints.size >= 2) {
                            val label = when (state.houseOverlaySource) {
                                "thermostat" -> "House thermostat"
                                "reference" -> "Indoor reference"
                                else -> "House"
                            }
                            Text(
                                "$label overlay shown as dashed line.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item { Text("Recent readings", style = MaterialTheme.typography.titleMedium) }
                if (state.readings.isEmpty()) {
                    item {
                        Text("No readings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(state.readings, key = { "${it.timestamp}-${it.probeKey}-${it.tempf}" }) { row ->
                    Column {
                        Text(
                            listOfNotNull(row.probeLabel, row.feedName).joinToString(" · ")
                                .ifBlank { "Reading" },
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "${"%.1f".format(row.tempf)}°F · ${row.humidity.toInt()}% RH · ${row.timestamp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun Sparkline(
    garagePoints: List<ChartPointDto>,
    housePoints: List<ChartPointDto> = emptyList(),
) {
    val garageColor = BrandTrace
    val houseColor = MaterialTheme.colorScheme.secondary
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(vertical = 8.dp),
    ) {
        if (garagePoints.size < 2) return@Canvas
        val allTemps = garagePoints.map { it.tempf } + housePoints.map { it.tempf }
        val min = allTemps.minOrNull() ?: return@Canvas
        val max = allTemps.maxOrNull() ?: return@Canvas
        val range = (max - min).takeIf { it > 0.01 } ?: 1.0

        fun yFor(temp: Double): Float =
            size.height * (1f - ((temp - min) / range).toFloat())

        val garagePath = Path()
        garagePoints.forEachIndexed { index, point ->
            val x = size.width * index / (garagePoints.size - 1).toFloat()
            val y = yFor(point.tempf)
            if (index == 0) garagePath.moveTo(x, y) else garagePath.lineTo(x, y)
        }
        drawPath(
            path = garagePath,
            color = garageColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )

        if (housePoints.size >= 2) {
            val housePath = Path()
            housePoints.forEachIndexed { index, point ->
                val x = size.width * index / (housePoints.size - 1).toFloat()
                val y = yFor(point.tempf)
                if (index == 0) housePath.moveTo(x, y) else housePath.lineTo(x, y)
            }
            drawPath(
                path = housePath,
                color = houseColor,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                ),
            )
        }

        val last = garagePoints.last()
        val lastY = yFor(last.tempf)
        drawCircle(color = garageColor, radius = 6f, center = Offset(size.width, lastY))
    }
}
