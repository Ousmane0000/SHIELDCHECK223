package com.example.lockunlockdetection

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Gère l'identifiant unique de l'appareil (UUID)
 * Génère un UUID unique lors de la première utilisation et le persiste
 */
object DeviceUniqueIdentifier {
    private const val PREFS_NAME = "shield_check_prefs"
    private const val UUID_KEY = "device_unique_id"
    private lateinit var sharedPreferences: SharedPreferences

    /**
     * Initialise le gestionnaire avec le contexte de l'application
     * @param context Contexte Android
     */
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Récupère ou génère l'UUID unique de l'appareil
     * @return UUID unique de l'appareil
     */
    fun getDeviceId(): String {
        var deviceId = sharedPreferences.getString(UUID_KEY, null)
        
        if (deviceId == null) {
            // Générer un nouvel UUID
            deviceId = UUID.randomUUID().toString()
            // Le persister
            sharedPreferences.edit().putString(UUID_KEY, deviceId).apply()
        }
        
        return deviceId
    }

    /**
     * Réinitialise l'UUID (utile pour les tests ou la réinitialisation)
     */
    fun resetDeviceId() {
        sharedPreferences.edit().remove(UUID_KEY).apply()
    }
}
