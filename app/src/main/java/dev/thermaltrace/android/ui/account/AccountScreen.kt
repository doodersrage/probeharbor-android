package dev.thermaltrace.android.ui.account

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thermaltrace.android.ui.theme.brandTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onOpenHousehold: () -> Unit = {},
    onOpenShare: () -> Unit = {},
    onOpenPortfolio: () -> Unit = {},
    onSignedOut: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.enablePush()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(brandTitle()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = { viewModel.signOut(onSignedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
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

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    state.email ?: "Signed in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }

                state.referral?.let { referral ->
                    Text("Refer a friend", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Code: ${referral.code} · ${referral.signupCount} signup(s) · +${referral.bonusTrialDays} trial days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            val text = viewModel.referralShareText() ?: return@OutlinedButton
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    },
                                    "Share referral link",
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Share referral link")
                    }
                }
                state.referralError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text("Push alerts", style = MaterialTheme.typography.titleMedium)
                if (!state.pushConfigured) {
                    Text(
                        "Add app/google-services.json (or firebase.* in local.properties) to enable FCM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Registers this phone with probeharbor.dev. Requires Pro and the Push channel enabled under Alerts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    return@OutlinedButton
                                }
                            }
                            viewModel.enablePush()
                        },
                        enabled = !state.pushBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.pushBusy) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                        } else {
                            Text("Enable push on this device")
                        }
                    }
                }

                Text("Display", style = MaterialTheme.typography.titleMedium)
                SettingSwitch("Use Celsius", prefs.useCelsius) {
                    viewModel.update { it.copy(useCelsius = !it.useCelsius) }
                }
                SettingSwitch("Show probe temps on home", prefs.showGarageTemps) {
                    viewModel.update { it.copy(showGarageTemps = !it.showGarageTemps) }
                }
                SettingSwitch("Show weather", prefs.showWeather) {
                    viewModel.update { it.copy(showWeather = !it.showWeather) }
                }

                Text("Theme", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("dark", "light", "system").forEach { theme ->
                        FilterChip(
                            selected = prefs.theme == theme,
                            onClick = { viewModel.update { it.copy(theme = theme) } },
                            label = { Text(theme.replaceFirstChar { c -> c.uppercase() }) },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                    else Text("Save preferences")
                }

                Text(
                    "Household and billing extras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onOpenHousehold,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Household")
                }
                OutlinedButton(
                    onClick = onOpenShare,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Share links")
                }
                OutlinedButton(
                    onClick = onOpenPortfolio,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Portfolio")
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.plansUrl())),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Plans & billing")
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://probeharbor.dev/dashboard/temperature")),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Open devices on web")
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://probeharbor.dev/dashboard/settings")),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open settings on web")
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
