package dev.thermaltrace.android.data.auth

import dev.thermaltrace.android.BuildConfig
import dev.thermaltrace.android.data.api.SessionCookieJar
import dev.thermaltrace.android.data.api.ThermalTraceApi
import dev.thermaltrace.android.data.push.PushRegistrar
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.mfa.AuthenticatorAssuranceLevel
import io.github.jan.supabase.auth.mfa.MfaLevel
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AuthRepository(
    supabaseUrl: String,
    supabaseAnonKey: String,
    private val sessionStore: SessionStore,
    private val cookieJar: SessionCookieJar,
    private val pushRegistrar: PushRegistrar,
    private val api: ThermalTraceApi,
) {
    private val client: SupabaseClient? = if (
        supabaseUrl.isBlank() || supabaseAnonKey.isBlank()
    ) {
        null
    } else {
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseAnonKey,
        ) {
            install(Auth)
        }
    }

    val isConfigured: Boolean get() = client != null

    val sessionFlow: Flow<SessionTokens?> = sessionStore.sessionFlow

    val isSignedIn: Flow<Boolean> = sessionFlow.map { it != null }

    suspend fun restoreSession() {
        val tokens = sessionStore.current() ?: return
        cookieJar.syncFromSession()
        val supabase = client ?: return
        runCatching {
            supabase.auth.importSession(
                io.github.jan.supabase.auth.user.UserSession(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = null,
                ),
            )
        }
        runCatching { pushRegistrar.registerWithServer() }
    }

    private suspend fun persistSession(
        accessToken: String,
        refreshToken: String,
        mfaSatisfied: Boolean,
    ) {
        sessionStore.save(
            SessionTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                mfaSatisfied = mfaSatisfied,
            ),
        )
        cookieJar.syncFromSession()
        if (mfaSatisfied) {
            runCatching { pushRegistrar.registerWithServer() }
        }
    }

    suspend fun signIn(email: String, password: String): Result<AuthOutcome> {
        val supabase = client
            ?: return Result.failure(
                IllegalStateException(
                    "Supabase is not configured. Set supabase.url and supabase.anonKey in local.properties.",
                ),
            )

        return runCatching {
            supabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val session = supabase.auth.currentSessionOrNull()
                ?: error("Sign-in succeeded but no session was returned")
            val needsMfa = needsMfaStepUp(supabase)
            persistSession(
                session.accessToken,
                session.refreshToken,
                mfaSatisfied = !needsMfa,
            )
            AuthOutcome(
                needsMfa = needsMfa,
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
            )
        }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase not configured"))
        return runCatching {
            supabase.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                persistSession(
                    session.accessToken,
                    session.refreshToken,
                    mfaSatisfied = !needsMfaStepUp(supabase),
                )
            }
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase not configured"))
        return runCatching {
            supabase.auth.resetPasswordForEmail(email.trim())
        }
    }

    fun oauthStartUrl(provider: String): String {
        val base = BuildConfig.THERMALTRACE_BASE_URL.trimEnd('/')
        // app= tells the server which package to hand the session back to.
        return "$base/api/auth/mobile/start?provider=$provider&app=${BuildConfig.APPLICATION_ID}"
    }

    suspend fun exchangeMobileOAuth(exchangeToken: String): Result<AuthOutcome> = runCatching {
        val body = buildJsonObject { put("exchange_token", exchangeToken) }
            .toString()
            .toRequestBody("application/json".toMediaType())
        val response = api.exchangeMobileOAuth(body)
        val payload = response.body()
        if (!response.isSuccessful || payload?.ok != true) {
            error(payload?.error ?: "OAuth exchange failed")
        }
        val access = payload.accessToken ?: error("Missing access token")
        val refresh = payload.refreshToken ?: error("Missing refresh token")
        client?.auth?.importSession(
            io.github.jan.supabase.auth.user.UserSession(
                accessToken = access,
                refreshToken = refresh,
                expiresIn = 3600,
                tokenType = "bearer",
                user = null,
            ),
        )
        val needsMfa = client?.let { needsMfaStepUp(it) } ?: false
        persistSession(access, refresh, mfaSatisfied = !needsMfa)
        AuthOutcome(
            needsMfa = needsMfa,
            accessToken = access,
            refreshToken = refresh,
        )
    }

    suspend fun verifyMfa(code: String, yubikeyOtp: String? = null): Result<Unit> = runCatching {
        val body = buildJsonObject {
            if (!yubikeyOtp.isNullOrBlank()) {
                put("yubikey_otp", yubikeyOtp.trim())
            } else {
                put("code", code)
            }
        }
            .toString()
            .toRequestBody("application/json".toMediaType())
        val response = api.verifyMfa(body)
        val payload = response.body()
        if (!response.isSuccessful || payload?.ok != true) {
            error(payload?.error ?: "Invalid code")
        }
        val access = payload.accessToken ?: error("Missing access token")
        val refresh = payload.refreshToken ?: error("Missing refresh token")
        persistSession(access, refresh, mfaSatisfied = true)
    }

    fun plansUrl(): String = "${BuildConfig.THERMALTRACE_BASE_URL.trimEnd('/')}/dashboard/plans"

    fun pricingUrl(): String = "${BuildConfig.THERMALTRACE_BASE_URL.trimEnd('/')}/pricing"

    suspend fun signOut() {
        runCatching { pushRegistrar.unregisterFromServer() }
        runCatching { client?.auth?.signOut() }
        sessionStore.clear()
        cookieJar.clear()
    }

    fun sessionStatus(): Flow<SessionStatus>? = client?.auth?.sessionStatus

    private fun needsMfaStepUp(supabase: SupabaseClient): Boolean {
        val level: MfaLevel? = runCatching {
            supabase.auth.mfa.getAuthenticatorAssuranceLevel()
        }.getOrNull()
        if (level == null) return hasYubiKeyOtpEnrolled(supabase)

        val supabaseStepUp =
            level.next == AuthenticatorAssuranceLevel.AAL2 &&
                level.current != AuthenticatorAssuranceLevel.AAL2
        if (supabaseStepUp) return true

        return level.current != AuthenticatorAssuranceLevel.AAL2 &&
            hasYubiKeyOtpEnrolled(supabase)
    }

    private fun hasYubiKeyOtpEnrolled(supabase: SupabaseClient): Boolean {
        val raw = supabase.auth.currentUserOrNull()
            ?.userMetadata
            ?.get("yubikey_otp_public_ids")
            ?: return false
        return raw is List<*> && raw.isNotEmpty()
    }
}

data class AuthOutcome(
    val needsMfa: Boolean,
    val accessToken: String,
    val refreshToken: String,
)
