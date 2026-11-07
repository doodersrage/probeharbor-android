package dev.thermaltrace.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PortfolioResponse(
    val properties: List<PortfolioProperty> = emptyList(),
    val error: String? = null,
)

@Serializable
data class PortfolioProperty(
    @SerialName("household_id") val householdId: String,
    val name: String,
    val role: String,
    @SerialName("min_temp_f") val minTempF: Double? = null,
    @SerialName("freeze_threshold_f") val freezeThresholdF: Double = 34.0,
    @SerialName("at_risk") val atRisk: Boolean = false,
    @SerialName("last_reading_at") val lastReadingAt: String? = null,
    @SerialName("device_count") val deviceCount: Int = 0,
)

@Serializable
data class HomeInsightsResponse(
    @SerialName("freeze_threshold_f") val freezeThresholdF: Double = 34.0,
    @SerialName("nights_at_risk") val nightsAtRisk: List<NightAtRisk> = emptyList(),
    @SerialName("nws_alerts") val nwsAlerts: List<NwsAlert> = emptyList(),
    @SerialName("outdoor_temp_f") val outdoorTempF: Double? = null,
    val house: HouseInsightDto? = null,
    @SerialName("regional_benchmark") val regionalBenchmark: RegionalBenchmarkDto? = null,
    val weather: InsightWeather? = null,
    @SerialName("time_to_freeze") val timeToFreeze: TimeToFreezeDto? = null,
)

@Serializable
data class TimeToFreezeDto(
    val hours: Double? = null,
    @SerialName("hits_at") val hitsAt: String? = null,
    @SerialName("hits_at_label") val hitsAtLabel: String? = null,
    val confidence: String? = null,
    val source: String? = null,
    val message: String? = null,
    @SerialName("rate_f_per_hour") val rateFPerHour: Double? = null,
    @SerialName("freeze_threshold_f") val freezeThresholdF: Double? = null,
    @SerialName("current_temp_f") val currentTempF: Double? = null,
)

@Serializable
data class HouseInsightDto(
    val source: String = "",
    @SerialName("ambient_temp_f") val ambientTempF: Double = 0.0,
    @SerialName("heat_setpoint_f") val heatSetpointF: Double? = null,
    @SerialName("hvac_mode") val hvacMode: String? = null,
    val detail: String? = null,
    @SerialName("reference_label") val referenceLabel: String? = null,
)

@Serializable
data class RegionalBenchmarkDto(
    @SerialName("city_label") val cityLabel: String,
    @SerialName("your_temp_f") val yourTempF: Double,
    @SerialName("city_avg_temp_f") val cityAvgTempF: Double? = null,
    @SerialName("delta_f") val deltaF: Double? = null,
    val message: String,
)

@Serializable
data class NightAtRisk(
    @SerialName("date_label") val dateLabel: String,
    @SerialName("min_temp_f") val minTempF: Double,
    @SerialName("at_risk") val atRisk: Boolean,
)

@Serializable
data class NwsAlert(
    val event: String,
    val headline: String,
    val severity: String,
    val expires: String? = null,
)

@Serializable
data class InsightWeather(
    val name: String? = null,
    @SerialName("temp_f") val tempF: Double? = null,
    val description: String? = null,
)

@Serializable
data class DoorEventsResponse(
    @SerialName("live_sessions") val liveSessions: List<DoorSession> = emptyList(),
    val history: List<DoorHistoryEvent> = emptyList(),
)

@Serializable
data class DoorSession(
    val label: String,
    @SerialName("opened_at") val openedAt: String,
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("still_open") val stillOpen: Boolean = false,
)

@Serializable
data class DoorHistoryEvent(
    val id: String,
    val label: String,
    @SerialName("opened_at") val openedAt: String,
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
)

@Serializable
data class ThermostatStatusResponse(
    @SerialName("can_use") val canUse: Boolean = false,
    @SerialName("can_connect") val canConnect: Boolean = false,
    val configured: ThermostatConfigured = ThermostatConfigured(),
    val connections: List<ThermostatConnectionStatus> = emptyList(),
    @SerialName("connect_urls") val connectUrls: ThermostatConnectUrls? = null,
)

@Serializable
data class ThermostatConfigured(
    val nest: Boolean = false,
    val ecobee: Boolean = false,
)

@Serializable
data class ThermostatConnectionStatus(
    val provider: String,
    @SerialName("connected_at") val connectedAt: String? = null,
    @SerialName("ambient_temp_f") val ambientTempF: Double? = null,
    @SerialName("heat_setpoint_f") val heatSetpointF: Double? = null,
    @SerialName("hvac_mode") val hvacMode: String? = null,
)

@Serializable
data class ThermostatConnectUrls(
    val nest: String? = null,
    val ecobee: String? = null,
)

@Serializable
data class ReferralResponse(
    val code: String,
    @SerialName("register_url") val registerUrl: String,
    @SerialName("signup_count") val signupCount: Int = 0,
    @SerialName("bonus_trial_days") val bonusTrialDays: Int = 7,
)

@Serializable
data class IngestStatusResponse(
    val waiting: Boolean = false,
    @SerialName("waiting_count") val waitingCount: Int = 0,
    @SerialName("seen_count") val seenCount: Int = 0,
    @SerialName("latest_count") val latestCount: Int = 0,
    val devices: List<WaitingDevice> = emptyList(),
    @SerialName("checked_at") val checkedAt: String? = null,
)

@Serializable
data class WaitingDevice(
    val id: String,
    val name: String,
    @SerialName("sensor_keys") val sensorKeys: List<String> = emptyList(),
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
)

@Serializable
data class MobileExchangeResponse(
    val ok: Boolean = false,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    /**
     * Set once the site signs in with Better Auth (after its move off
     * Supabase): the token is then a signed session, sent as this cookie.
     * Absent while the site still uses Supabase.
     */
    @SerialName("session_cookie_name") val sessionCookieName: String? = null,
    val error: String? = null,
)

@Serializable
data class MfaVerifyResponse(
    val ok: Boolean = false,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val aal: String? = null,
    val error: String? = null,
    @SerialName("factorId") val factorId: String? = null,
    @SerialName("challengeId") val challengeId: String? = null,
    @SerialName("ceremonyType") val ceremonyType: String? = null,
    @SerialName("publicKey") val publicKey: JsonObject? = null,
)

@Serializable
data class ClaimsEmailRequest(
    @SerialName("adjuster_email") val adjusterEmail: String,
    val from: String? = null,
    val to: String? = null,
)

@Serializable
data class ClaimsEmailResponse(
    val ok: Boolean = false,
    @SerialName("verify_url") val verifyUrl: String? = null,
    @SerialName("verification_code") val verificationCode: String? = null,
    val error: String? = null,
)

@Serializable
data class SnoozeJsonResponse(
    val ok: Boolean = false,
    val kind: String? = null,
    val message: String? = null,
    val error: String? = null,
)
