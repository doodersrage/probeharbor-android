package dev.thermaltrace.android.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class SessionTokens(
    val accessToken: String,
    val refreshToken: String,
    /** YubiKey OTP does not raise JWT AAL; the site uses sb-mfa-required=0 instead. */
    val mfaSatisfied: Boolean = false,
    /**
     * Better Auth session cookie name when the site gave one (then access and
     * refresh token are the same signed session value). Null: Supabase
     * tokens, sent as the sb-* cookies.
     */
    val sessionCookieName: String? = null,
)

class SessionStore(
    private val dataStore: DataStore<Preferences>,
) {
    private val accessKey = stringPreferencesKey("sb_access_token")
    private val refreshKey = stringPreferencesKey("sb_refresh_token")
    private val mfaSatisfiedKey = booleanPreferencesKey("sb_mfa_satisfied")
    private val cookieNameKey = stringPreferencesKey("session_cookie_name")

    val sessionFlow: Flow<SessionTokens?> = dataStore.data.map { prefs ->
        val access = prefs[accessKey]
        val refresh = prefs[refreshKey]
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
            null
        } else {
            SessionTokens(
                accessToken = access,
                refreshToken = refresh,
                mfaSatisfied = prefs[mfaSatisfiedKey] == true,
                sessionCookieName = prefs[cookieNameKey]?.takeIf { it.isNotBlank() },
            )
        }
    }

    suspend fun current(): SessionTokens? = sessionFlow.first()

    suspend fun save(tokens: SessionTokens) {
        dataStore.edit { prefs ->
            prefs[accessKey] = tokens.accessToken
            prefs[refreshKey] = tokens.refreshToken
            prefs[mfaSatisfiedKey] = tokens.mfaSatisfied
            val name = tokens.sessionCookieName
            if (name.isNullOrBlank()) prefs.remove(cookieNameKey) else prefs[cookieNameKey] = name
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(accessKey)
            prefs.remove(refreshKey)
            prefs.remove(mfaSatisfiedKey)
            prefs.remove(cookieNameKey)
        }
    }
}
