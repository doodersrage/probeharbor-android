package dev.thermaltrace.android.data.api

import dev.thermaltrace.android.data.model.HomeReadingsResponse

class ReadingsRepository(
    private val api: ThermalTraceApi,
) {
    suspend fun fetchHomeReadings(space: String? = null): Result<HomeReadingsResponse> =
        runCatching {
            val response = api.homeReadings(save = 0, space = space)
            if (response.code() == 401) {
                error("Unauthorized — sign in again")
            }
            if (!response.isSuccessful) {
                error("HTTP ${response.code()}: ${response.errorBody()?.string().orEmpty()}")
            }
            response.body() ?: error("Empty response from ProbeHarbor")
        }
}
