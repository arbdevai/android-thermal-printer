package com.thermalprinter.app.subscription

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val Context.subscriptionStore by preferencesDataStore("subscription_prefs")

/**
 * Subscription & Daily Quota Manager (100% offline, privacy-first)
 * Daily quota: 7 free receipts per calendar day
 * License key: HMAC-SHA256-signed payload containing plan, expiry and device fingerprint
 * Anti-cheat: clock rollback protection + unique device binding
 */
class SubscriptionManager(private val context: Context) {
    companion object {
        const val FREE_DAILY_LIMIT = 7
        private const val HMAC_SECRET = "ARBCN_2026_SECURE_LICENSE_SECRET_v1_X9K2M8N4P6"

        private val KEY_LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
        private val KEY_USAGE_COUNT = intPreferencesKey("usage_count")
        private val KEY_LAST_SEEN_TIME = longPreferencesKey("last_seen_time")
        private val KEY_LICENSE_KEY = stringPreferencesKey("license_key")
        private val KEY_LICENSE_EXPIRY = longPreferencesKey("license_expiry")
        private val KEY_LICENSE_PLAN = stringPreferencesKey("license_plan")
        private val KEY_APPLIED_PROMOS = stringPreferencesKey("applied_promos")
    }

    private val store = context.applicationContext.subscriptionStore

    val deviceId: String by lazy {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val deviceInfo = "${android.os.Build.MANUFACTURER}:${android.os.Build.MODEL}:$androidId"
        MessageDigest.getInstance("SHA-256").digest(deviceInfo.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
            .uppercase()
    }

    data class SubscriptionState(
        val isPro: Boolean = false,
        val plan: String = "FREE",
        val expiry: Long = 0L,
        val usedToday: Int = 0,
        val dailyLimit: Int = FREE_DAILY_LIMIT,
        val isClockTampered: Boolean = false,
        val remaining: Int = FREE_DAILY_LIMIT
    )

    val subscriptionState: Flow<SubscriptionState> = store.data.map { prefs ->
        val now = System.currentTimeMillis()
        val lastSeen = prefs[KEY_LAST_SEEN_TIME] ?: 0L
        val isClockTampered = now + 60000L < lastSeen // tolerate 60s clock drift
        val expiry = prefs[KEY_LICENSE_EXPIRY] ?: 0L
        val plan = prefs[KEY_LICENSE_PLAN] ?: "FREE"
        val licenseKey = prefs[KEY_LICENSE_KEY] ?: ""
        val isPro = !isClockTampered && licenseKey.isNotBlank() && (expiry == Long.MAX_VALUE || expiry > now)

        val today = todayDate(now)
        val lastReset = prefs[KEY_LAST_RESET_DATE] ?: ""
        val usedToday = if (today == lastReset) prefs[KEY_USAGE_COUNT] ?: 0 else 0

        SubscriptionState(
            isPro = isPro,
            plan = if (isPro) plan else "FREE",
            expiry = expiry,
            usedToday = usedToday,
            isClockTampered = isClockTampered,
            remaining = if (isPro) Int.MAX_VALUE else (FREE_DAILY_LIMIT - usedToday).coerceAtLeast(0)
        )
    }

    suspend fun canGenerateReceipt(): Boolean {
        refreshDay()
        val state = subscriptionState.first()
        return !state.isClockTampered && (state.isPro || state.remaining > 0)
    }

    suspend fun consumeReceipt(): Boolean {
        refreshDay()
        val state = subscriptionState.first()
        if (state.isClockTampered) return false
        if (state.isPro) return true
        if (state.remaining <= 0) return false

        store.edit { prefs ->
            prefs[KEY_USAGE_COUNT] = (prefs[KEY_USAGE_COUNT] ?: 0) + 1
            prefs[KEY_LAST_SEEN_TIME] = System.currentTimeMillis()
        }
        return true
    }

    suspend fun refreshDay() {
        store.edit { prefs ->
            val now = System.currentTimeMillis()
            val lastSeen = prefs[KEY_LAST_SEEN_TIME] ?: 0L
            // Block if clock rollback detected
            if (now + 60000L < lastSeen) return@edit

            val today = todayDate(now)
            if (prefs[KEY_LAST_RESET_DATE] != today) {
                prefs[KEY_LAST_RESET_DATE] = today
                prefs[KEY_USAGE_COUNT] = 0
            }
            prefs[KEY_LAST_SEEN_TIME] = now
        }
    }

    /**
     * License format: ARBCN-PLAN-EXPIRY-DEVICEID-SIGNATURE
     * PLAN: WEEKLY / MONTHLY / YEARLY / LIFETIME
     * EXPIRY: unix timestamp millis (Long.MAX_VALUE for lifetime)
     * SIGNATURE: first 24 chars of HMAC-SHA256 hex of "PLAN|EXPIRY|DEVICEID"
     */
    suspend fun activateLicense(key: String): Result<String> {
        return try {
            val cleaned = key.trim().uppercase()
            val parts = cleaned.split("-")
            if (parts.size != 5 || parts[0] != "ARBCN") {
                return Result.failure(IllegalArgumentException("Format kode lisensi tidak valid"))
            }

            val plan = parts[1]
            val expiry = parts[2].toLongOrNull() ?: return Result.failure(IllegalArgumentException("Tanggal lisensi tidak valid"))
            val targetDeviceId = parts[3]
            val signature = parts[4]

            if (targetDeviceId != deviceId) {
                return Result.failure(IllegalArgumentException("Kode ini bukan untuk perangkat Anda"))
            }
            if (expiry != Long.MAX_VALUE && expiry <= System.currentTimeMillis()) {
                return Result.failure(IllegalArgumentException("Lisensi sudah kedaluwarsa"))
            }

            val payload = "$plan|$expiry|$targetDeviceId"
            val expectedSignature = sign(payload).take(24).uppercase()
            if (!MessageDigest.isEqual(signature.toByteArray(), expectedSignature.toByteArray())) {
                return Result.failure(IllegalArgumentException("Kode lisensi tidak valid atau telah diubah"))
            }

            store.edit { prefs ->
                prefs[KEY_LICENSE_KEY] = cleaned
                prefs[KEY_LICENSE_EXPIRY] = expiry
                prefs[KEY_LICENSE_PLAN] = plan
                prefs[KEY_LAST_SEEN_TIME] = System.currentTimeMillis()
            }
            Result.success("Paket $plan berhasil diaktifkan")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Promo format: PROMO-CODE-DAYS-SIGNATURE
     * Gives extra Pro days (one-time per code per device)
     */
    suspend fun applyPromoCode(code: String): Result<String> {
        return try {
            val cleaned = code.trim().uppercase()
            val parts = cleaned.split("-")
            if (parts.size != 4 || parts[0] != "PROMO") {
                return Result.failure(IllegalArgumentException("Format kode promo tidak valid"))
            }
            val promoName = parts[1]
            val days = parts[2].toIntOrNull() ?: return Result.failure(IllegalArgumentException("Durasi promo tidak valid"))
            val signature = parts[3]
            val expectedSignature = sign("PROMO|$promoName|$days").take(16).uppercase()
            if (!MessageDigest.isEqual(signature.toByteArray(), expectedSignature.toByteArray())) {
                return Result.failure(IllegalArgumentException("Kode promo tidak valid"))
            }

            val prefs = store.data.first()
            val applied = (prefs[KEY_APPLIED_PROMOS] ?: "").split(",").filter { it.isNotBlank() }
            if (promoName in applied) {
                return Result.failure(IllegalArgumentException("Kode promo sudah pernah digunakan"))
            }

            val now = System.currentTimeMillis()
            val currentExpiry = prefs[KEY_LICENSE_EXPIRY] ?: 0L
            val newExpiry = maxOf(now, currentExpiry) + days * 24 * 60 * 60 * 1000L
            val promoLicense = "ARBCN-PROMO-$newExpiry-$deviceId-${sign("PROMO|$newExpiry|$deviceId").take(24).uppercase()}"

            store.edit { p ->
                p[KEY_APPLIED_PROMOS] = (applied + promoName).joinToString(",")
                p[KEY_LICENSE_KEY] = promoLicense
                p[KEY_LICENSE_PLAN] = "PROMO"
                p[KEY_LICENSE_EXPIRY] = newExpiry
                p[KEY_LAST_SEEN_TIME] = now
            }
            Result.success("Promo berhasil! Anda mendapatkan $days hari Pro gratis")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(HMAC_SECRET.toByteArray(), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun todayDate(time: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))
}
