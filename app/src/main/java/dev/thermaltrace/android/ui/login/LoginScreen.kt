package dev.thermaltrace.android.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thermaltrace.android.ui.theme.brandTitle

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onSignedIn: () -> Unit,
    onNeedsMfa: () -> Unit,
    onOpenOAuthUrl: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = brandTitle(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (state.mode) {
                LoginMode.SignIn -> "Sign in to probeharbor.dev"
                LoginMode.Register -> "Create your account"
                LoginMode.ForgotPassword -> "Reset your password"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        if (!state.configured) {
            Text(
                text = "Add supabase.url and supabase.anonKey to local.properties, then rebuild.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            return
        }

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = if (state.mode == LoginMode.ForgotPassword) ImeAction.Done else ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (state.mode == LoginMode.ForgotPassword) {
                        viewModel.submit(onSignedIn, onNeedsMfa)
                    }
                },
            ),
        )

        if (state.mode != LoginMode.ForgotPassword) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.submit(onSignedIn, onNeedsMfa) },
                ),
            )
        }

        state.error?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        state.message?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.submit(onSignedIn, onNeedsMfa) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    when (state.mode) {
                        LoginMode.SignIn -> "Sign in"
                        LoginMode.Register -> "Create account"
                        LoginMode.ForgotPassword -> "Send reset email"
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state.mode) {
                LoginMode.SignIn -> {
                    TextButton(onClick = { viewModel.setMode(LoginMode.Register) }) {
                        Text("Register")
                    }
                    TextButton(onClick = { viewModel.setMode(LoginMode.ForgotPassword) }) {
                        Text("Forgot password?")
                    }
                }
                else -> {
                    TextButton(onClick = { viewModel.setMode(LoginMode.SignIn) }) {
                        Text("Back to sign in")
                    }
                }
            }
        }

        if (state.mode == LoginMode.SignIn) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                "Or continue with",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            listOf("google" to "Google", "github" to "GitHub", "discord" to "Discord").forEach { (id, label) ->
                OutlinedButton(
                    onClick = { onOpenOAuthUrl(viewModel.oauthUrl(id)) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(label)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
