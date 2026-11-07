package dev.thermaltrace.android.data.auth

import dev.thermaltrace.android.BuildConfig
import dev.thermaltrace.android.data.api.SessionCookieJar
import dev.thermaltrace.android.data.api.ThermalTraceApi
import dev.thermaltrace.android.data.push.PushRegistrar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Sign-in happens on the website, in the browser: email + password, Google,
 * GitHub or Discord, and any two-factor step (authenticator app, YubiKey,
 * passkey). The site then hands the app a one-time code through the
 * /app/oauth link, swapped here for the session (`/api/auth/mobile/exchange`).
 *
 * This works the same before and after the site moves from Supabase to
 * Better Auth; the exchange says which kind of session it is
 * (`session_cookie_name`). The app no longer talks to Supabase directly.
 */
class AuthRepository(
    private val sessionStore: SessionStore,
    private val cookieJar: SessionCookieJar,
    private val pushRegistrar: PushRegistrar,
    private val api: ThermalTraceApi,
) {
    val sessionFlow: Flow<SessionTokens?> = sessionStore.sessionFlow

    val isSignedIn: Flow<Boolean> = sessionFlow.map { it != null }

    private val base: String get() = BuildConfig.THERMALTRACE_BASE_URL.trimEnd('/')

    suspend fun restoreSession() {
        sessionStore.current() ?: return
        cookieJar.syncFromSession()
        runCatching { pushRegistrar.registerWithServer() }
    }

    /** Email and password (and two-factor) on the website; comes back through the app link. */
    fun emailSignInUrl(): String =
        "$base/api/auth/companion/start?client=android&app=${BuildConfig.APPLICATION_ID}"

    fun registerUrl(): String = "$base/register"

    fun forgotPasswordUrl(): String = "$base/forgot-password"

    fun oauthStartUrl(provider: String): String {
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
            error(payload?.error ?: "Sign-in failed")
        }
        val access = payload.accessToken ?: error("Missing access token")
        val refresh = payload.refreshToken ?: error("Missing refresh token")
        // The site only hands off after any two-factor step was passed in the browser.
        sessionStore.save(
            SessionTokens(
                accessToken = access,
                refreshToken = refresh,
                mfaSatisfied = true,
                sessionCookieName = payload.sessionCookieName?.takeIf { it.isNotBlank() },
            ),
        )
        cookieJar.syncFromSession()
        runCatching { pushRegistrar.registerWithServer() }
        AuthOutcome(needsMfa = false, accessToken = access, refreshToken = refresh)
    }

    /** Two-factor for older sessions opened at the "mfa" deep link (sign-in now does it in the browser). */
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
        val current = sessionStore.current() ?: error("Not signed in")
        sessionStore.save(
            current.copy(
                accessToken = payload.accessToken ?: current.accessToken,
                refreshToken = payload.refreshToken ?: current.refreshToken,
                mfaSatisfied = true,
            ),
        )
        cookieJar.syncFromSession()
    }

    fun plansUrl(): String = "$base/dashboard/plans"

    fun pricingUrl(): String = "$base/pricing"

    suspend fun signOut() {
        runCatching { pushRegistrar.unregisterFromServer() }
        // Ends the session on the site too (Better Auth deletes it; Supabase clears cookies).
        runCatching { api.signOut() }
        sessionStore.clear()
        cookieJar.clear()
    }
}

data class AuthOutcome(
    val needsMfa: Boolean,
    val accessToken: String,
    val refreshToken: String,
)
