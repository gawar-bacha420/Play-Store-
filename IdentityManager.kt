package com.ultimatecloner.safe.Spoofing

import android.content.Context
import java.security.SecureRandom
import java.util.UUID

/**
 * ✅ PLAY STORE SAFE - Runtime Identity Management
 * 
 * IMPORTANT: This does NOT modify system properties (IMEI, MAC, etc.).
 * That would violate Play Store's "Device and Network Abuse" policy.
 * 
 * Instead, we generate random identities and store them locally.
 * Our native layer (injector) uses these values at RUNTIME only,
 * within the app's own process space.
 */
data class ContainerIdentity(
    val containerId: String,
    val androidId: String,
    val advertisingId: String,
    val deviceModel: String,
    val fingerprint: String
)

class IdentityManager(private val context: Context) {
    
    private val secureRandom = SecureRandom()
    private val identities = mutableMapOf<String, ContainerIdentity>()
    
    companion object {
        private val DEVICE_MODELS = listOf(
            "SM-S918B", "Pixel 8 Pro", "OnePlus 12", 
            "Vivo X100", "Xiaomi 14", "Nothing Phone 2"
        )
        private val MANUFACTURERS = listOf(
            "Samsung", "Google", "OnePlus", "Xiaomi", "Nothing"
        )
    }
    
    /**
     * Generates a new identity for a container.
     * This identity is stored and used at RUNTIME only.
     * 
     * ✅ SAFE: System properties are NOT modified.
     */
    fun generateIdentity(containerId: String): ContainerIdentity {
        val identity = ContainerIdentity(
            containerId = containerId,
            androidId = randomHex(16),
            advertisingId = UUID.randomUUID().toString(),
            deviceModel = DEVICE_MODELS[secureRandom.nextInt(DEVICE_MODELS.size)],
            fingerprint = generateFingerprint()
        )
        
        identities[containerId] = identity
        
        // Save to local storage
        val prefs = context.getSharedPreferences("identities", Context.MODE_PRIVATE)
        prefs.edit().putString(containerId, 
            "${identity.androidId}|${identity.advertisingId}|${identity.deviceModel}|${identity.fingerprint}"
        ).apply()
        
        return identity
    }
    
    fun getIdentity(containerId: String): ContainerIdentity {
        return identities[containerId] ?: run {
            val prefs = context.getSharedPreferences("identities", Context.MODE_PRIVATE)
            val saved = prefs.getString(containerId, null)
            if (saved != null) {
                val parts = saved.split("|")
                ContainerIdentity(
                    containerId = containerId,
                    androidId = parts[0],
                    advertisingId = parts[1],
                    deviceModel = parts.getOrElse(2) { "Unknown" },
                    fingerprint = parts.getOrElse(3) { "Unknown" }
                )
            } else {
                generateIdentity(containerId)
            }
        }
    }
    
    fun regenerateIdentity(containerId: String): ContainerIdentity {
        identities.remove(containerId)
        return generateIdentity(containerId)
    }
    
    private fun randomHex(length: Int): String {
        val chars = "0123456789abcdef"
        return (1..length).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
    }
    
    private fun generateFingerprint(): String {
        val manufacturer = MANUFACTURERS[secureRandom.nextInt(MANUFACTURERS.size)]
        val model = DEVICE_MODELS[secureRandom.nextInt(DEVICE_MODELS.size)]
        return "$manufacturer/$model/${randomHex(8).uppercase()}"
    }
}
