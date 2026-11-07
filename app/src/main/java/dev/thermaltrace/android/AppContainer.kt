package dev.thermaltrace.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dev.thermaltrace.android.data.api.AlertsInboxRepository
import dev.thermaltrace.android.data.api.ClaimsPackRepository
import dev.thermaltrace.android.data.api.DashboardExtrasRepository
import dev.thermaltrace.android.data.api.DevicesRepository
import dev.thermaltrace.android.data.api.HistoryRepository
import dev.thermaltrace.android.data.api.HouseholdRepository
import dev.thermaltrace.android.data.api.ReadingsRepository
import dev.thermaltrace.android.data.api.SessionCookieJar
import dev.thermaltrace.android.data.api.SettingsFormClient
import dev.thermaltrace.android.data.api.SettingsRepository
import dev.thermaltrace.android.data.api.ShareRepository
import dev.thermaltrace.android.data.api.ThermalTraceApi
import dev.thermaltrace.android.data.api.createThermalTraceApi
import dev.thermaltrace.android.data.auth.AuthRepository
import dev.thermaltrace.android.data.auth.SessionStore
import dev.thermaltrace.android.data.push.PushRegistrar
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "thermaltrace_session",
)

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    val sessionStore = SessionStore(appContext.sessionDataStore)

    val cookieJar = SessionCookieJar(
        baseHost = BuildConfig.THERMALTRACE_BASE_URL
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/'),
        sessionStore = sessionStore,
    )

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(loggingInterceptor)
        .build()

    private val baseUrl = BuildConfig.THERMALTRACE_BASE_URL.trimEnd('/') + "/"

    val api: ThermalTraceApi = createThermalTraceApi(
        baseUrl = baseUrl,
        client = okHttpClient,
        json = json,
    )

    val readingsRepository = ReadingsRepository(api)

    private val formClient = SettingsFormClient(
        baseUrl = BuildConfig.THERMALTRACE_BASE_URL,
        cookieJar = cookieJar,
        logging = loggingInterceptor,
    )

    val settingsRepository = SettingsRepository(
        api = api,
        formClient = formClient,
    )

    val devicesRepository = DevicesRepository(
        api = api,
        formClient = formClient,
    )

    val householdRepository = HouseholdRepository(
        api = api,
        formClient = formClient,
    )

    val alertsInboxRepository = AlertsInboxRepository(api)
    val historyRepository = HistoryRepository(api)
    val claimsPackRepository = ClaimsPackRepository(appContext, okHttpClient)
    val shareRepository = ShareRepository(api, formClient)
    val dashboardExtrasRepository = DashboardExtrasRepository(api, json)

    val pushRegistrar = PushRegistrar(
        context = appContext,
        okHttpClient = okHttpClient,
    )

    val authRepository = AuthRepository(
        sessionStore = sessionStore,
        cookieJar = cookieJar,
        pushRegistrar = pushRegistrar,
        api = api,
    )
}
