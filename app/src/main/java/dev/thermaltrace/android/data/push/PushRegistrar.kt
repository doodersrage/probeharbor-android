package dev.thermaltrace.android.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import dev.thermaltrace.android.BuildConfig
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class PushRegistrar(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val hasManualConfig: Boolean
        get() = BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
            BuildConfig.FIREBASE_APPLICATION_ID.isNotBlank() &&
            BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_GCM_SENDER_ID.isNotBlank()

    val isConfigured: Boolean
        get() = hasManualConfig || FirebaseApp.getApps(context).isNotEmpty()

    @Volatile
    private var initialized = false

    fun ensureInitialized(): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            ensureAlertChannel()
            initialized = true
            return true
        }
        if (!hasManualConfig) return false
        synchronized(this) {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setGcmSenderId(BuildConfig.FIREBASE_GCM_SENDER_ID)
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            ensureAlertChannel()
            initialized = true
        }
        return true
    }

    private fun ensureAlertChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ProbeHarbor alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Freeze, humidity, outage, and other probe alerts"
        }
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun registerWithServer(): Result<String> = runCatching {
        check(ensureInitialized()) {
            "Firebase is not configured. Add app/google-services.json or firebase.* in local.properties."
        }
        val token = FirebaseMessaging.getInstance().token.await()
        postToken(token)
        token
    }

    suspend fun unregisterFromServer(): Result<Unit> = runCatching {
        if (!ensureInitialized()) return@runCatching
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
            ?: return@runCatching
        deleteToken(token)
    }

    suspend fun syncToken(token: String) {
        if (!ensureInitialized()) return
        runCatching { postToken(token) }
            .onFailure { Log.w(TAG, "FCM token sync failed", it) }
    }

    private fun postToken(token: String) {
        val body = JSONObject()
            .put("token", token)
            .put("platform", "android")
            .put("appId", BuildConfig.APPLICATION_ID)
            .toString()
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url("${BuildConfig.THERMALTRACE_BASE_URL.trimEnd('/')}/api/push/fcm")
            .post(body)
            .header("Content-Type", "application/json")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == 403) {
                error("Pro plan required for push")
            }
            if (!response.isSuccessful) {
                error("FCM register failed HTTP ${response.code}: ${response.body?.string().orEmpty()}")
            }
        }
    }

    private fun deleteToken(token: String) {
        val body = JSONObject()
            .put("token", token)
            .toString()
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url("${BuildConfig.THERMALTRACE_BASE_URL.trimEnd('/')}/api/push/fcm")
            .delete(body)
            .header("Content-Type", "application/json")
            .build()
        okHttpClient.newCall(request).execute().use { /* best-effort */ }
    }

    companion object {
        const val CHANNEL_ID = "thermaltrace_alerts"
        private const val TAG = "PushRegistrar"
        private val JSON = "application/json".toMediaType()
    }
}
