package com.arbcn.admin.core

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LicenseSigner {
    private const val HMAC_SECRET = "ARBCN_2026_SECURE_LICENSE_SECRET_v1_X9K2M8N4P6"

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(HMAC_SECRET.toByteArray(), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun generateLicense(plan: String, deviceId: String, days: Int = 30): String {
        val cleanedDevice = deviceId.trim().uppercase()
        val planUpper = plan.trim().uppercase()

        val expiry = if (planUpper == "LIFETIME") {
            Long.MAX_VALUE
        } else {
            val now = System.currentTimeMillis()
            now + days.toLong() * 24 * 60 * 60 * 1000L
        }

        val payload = "$planUpper|$expiry|$cleanedDevice"
        val signature = sign(payload).take(24).uppercase()

        return "ARBCN-$planUpper-$expiry-$cleanedDevice-$signature"
    }

    fun generatePromo(promoName: String, days: Int = 7): String {
        val nameClean = promoName.trim().uppercase().replace(" ", "")
        val payload = "PROMO|$nameClean|$days"
        val signature = sign(payload).take(16).uppercase()
        return "PROMO-$nameClean-$days-$signature"
    }
}
