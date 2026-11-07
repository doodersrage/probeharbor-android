package dev.thermaltrace.android.data.api

import android.util.Log
import dev.thermaltrace.android.BuildConfig
import dev.thermaltrace.android.data.auth.SessionStore
import dev.thermaltrace.android.data.auth.SessionTokens
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.net.URLEncoder

/**
 * Mirrors the web dashboard session.
 * - Supabase (until the site moves): `sb-access-token` + `sb-refresh-token`
 *   cookies; the site refreshes them through Set-Cookie, saved here.
 *   `sb-mfa-required=0` is required after YubiKey OTP (AAL stays aal1).
 * - Better Auth (after the move): one signed session value, sent as the
 *   cookie the sign-in hand-off named (`sessionCookieName`). It doesn't
 *   rotate; the site extends it while it's used.
 */
class SessionCookieJar(
    private val baseHost: String,
    private val sessionStore: SessionStore,
) : CookieJar {
    @Volatile
    private var memoryCookies: List<Cookie> = emptyList()

    @Volatile
    private var cachedTokens: SessionTokens? = null

    fun syncFromSession() {
        val tokens = runBlocking { sessionStore.current() } ?: run {
            cachedTokens = null
            memoryCookies = emptyList()
            return
        }
        cachedTokens = tokens
        applyTokens(tokens)
    }

    fun clear() {
        cachedTokens = null
        memoryCookies = emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!url.host.endsWith(baseHost)) return
        // Better Auth session: nothing to pick up (sign-out clears it explicitly).
        if ((cachedTokens ?: runBlocking { sessionStore.current() })?.sessionCookieName != null) return
        val kept = memoryCookies.associateBy { it.name }.toMutableMap()
        for (cookie in cookies) {
            if (cookie.name in SESSION_COOKIE_NAMES) {
                kept[cookie.name] = cookie
            }
        }
        memoryCookies = kept.values.toList()
        val access = kept[ACCESS_COOKIE]?.value
        val refresh = kept[REFRESH_COOKIE]?.value
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) return

        val mfaValue = kept[MFA_COOKIE]?.value
        val existing = cachedTokens ?: runBlocking { sessionStore.current() }
        val mfaSatisfied = when (mfaValue) {
            MFA_SATISFIED_VALUE -> true
            MFA_REQUIRED_VALUE -> false
            else -> existing?.mfaSatisfied == true
        }
        val tokens = SessionTokens(
            accessToken = access,
            refreshToken = refresh,
            mfaSatisfied = mfaSatisfied,
        )
        cachedTokens = tokens
        runBlocking { sessionStore.save(tokens) }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!url.host.endsWith(baseHost)) return emptyList()
        if (memoryCookies.isEmpty()) {
            val tokens = cachedTokens ?: runBlocking { sessionStore.current() }
            if (tokens != null) {
                cachedTokens = tokens
                applyTokens(tokens)
            }
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Attaching ${memoryCookies.size} session cookies to ${url.encodedPath}")
        }
        return memoryCookies
    }

    private fun applyTokens(tokens: SessionTokens) {
        val betterAuthCookie = tokens.sessionCookieName
        if (betterAuthCookie != null) {
            // Signed value has + / = : send it encoded, as browsers do.
            memoryCookies = listOf(buildCookie(betterAuthCookie, URLEncoder.encode(tokens.accessToken, "UTF-8")))
            return
        }
        val cookies = mutableListOf(
            buildCookie(ACCESS_COOKIE, tokens.accessToken),
            buildCookie(REFRESH_COOKIE, tokens.refreshToken),
        )
        if (tokens.mfaSatisfied) {
            cookies += buildCookie(MFA_COOKIE, MFA_SATISFIED_VALUE)
        }
        memoryCookies = cookies
    }

    private fun buildCookie(name: String, value: String): Cookie =
        Cookie.Builder()
            .name(name)
            .value(value)
            .domain(baseHost)
            .path("/")
            .secure()
            .httpOnly()
            .build()

    companion object {
        private const val TAG = "SessionCookieJar"
        const val ACCESS_COOKIE = "sb-access-token"
        const val REFRESH_COOKIE = "sb-refresh-token"
        const val MFA_COOKIE = "sb-mfa-required"
        const val MFA_SATISFIED_VALUE = "0"
        const val MFA_REQUIRED_VALUE = "1"
        private val SESSION_COOKIE_NAMES = setOf(ACCESS_COOKIE, REFRESH_COOKIE, MFA_COOKIE)
    }
}
