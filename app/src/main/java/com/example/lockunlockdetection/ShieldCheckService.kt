package com.example.lockunlockdetection

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service de sécurité Shield Check
 * 
 * Responsabilités:
 * - Vérifier régulièrement si l'appareil est volé
 * - Verrouiller l'appareil si le vol est détecté
 * - Gérer la communication avec Supabase
 */
class ShieldCheckService : Service() {
    
    private val TAG = "ShieldCheckService"
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    private var devicePolicyManager: DevicePolicyManager? = null
    private var componentName: ComponentName? = null
    private var supabaseRepository: SupabaseRepository? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service créé")
        
        // Initialiser le gestionnaire de politiques d'appareil
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        
        // Initialiser l'identifiant unique de l'appareil
        DeviceUniqueIdentifier.initialize(this)
        
        // Initialiser le référentiel Supabase
        supabaseRepository = SupabaseRepository(this)
        
        // Effectuer la première vérification au démarrage du service
        checkForTheft()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service démarré")
        
        // Vérifier immédiatement si l'appareil est volé
        checkForTheft()
        
        // Retourner START_STICKY pour redémarrer le service s'il est tué
        return START_STICKY
    }

    /**
     * Vérifie si l'appareil est volé en interrogeant la table Supabase
     */
    private fun checkForTheft() {
        serviceScope.launch {
            try {
                val deviceId = DeviceUniqueIdentifier.getDeviceId()
                Log.d(TAG, "Vérification du vol pour l'appareil: $deviceId")
                
                // Interroger Supabase
                val isStolenResult = withContext(Dispatchers.IO) {
                    supabaseRepository?.isDeviceStolen(deviceId) ?: false
                }
                
                if (isStolenResult) {
                    Log.w(TAG, "Appareil détecté comme volé! Verrouillage en cours...")
                    lockDevice()
                } else {
                    Log.d(TAG, "Appareil non signalé comme volé")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la vérification du vol", e)
            }
        }
    }

    /**
     * Verrouille immédiatement l'écran de l'appareil
     */
    private fun lockDevice() {
        try {
            // Vérifier si l'app a les droits d'administrateur
            if (devicePolicyManager?.isAdminActive(componentName!!) == true) {
                devicePolicyManager?.lockNow()
                Log.i(TAG, "Appareil verrouillé avec succès")
                
                // Optionnel: Envoyer une notification ou un log
                notifyTheftDetection()
            } else {
                Log.e(TAG, "L'application n'a pas les droits d'administrateur")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du verrouillage de l'appareil", e)
        }
    }

    /**
     * Notifie de la détection du vol (peut être étendu pour les notifications)
     */
    private fun notifyTheftDetection() {
        // Ici, vous pouvez ajouter:
        // - Créer une notification
        // - Envoyer un rapport au serveur
        // - Déclencher une alarme
        Log.w(TAG, "ALERTE: Vol d'appareil détecté et verrouillé!")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Service détruit")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "ShieldCheckService"
    }
}
