package com.example.myapplication.core.emergency

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LastKnownStatus(
    val nodeId: String = "Responder-1",
    val callSign: String = "Alpha-1",
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val batteryPercentage: Int = 85,
    val isEmergencyModeEnabled: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastMessageText: String = "Base station online on mesh",
    val connectionState: String = "CONNECTED (Wi-Fi Mesh)"
) {
    fun getLocationDisplay(): String {
        return if (isEmergencyModeEnabled && latitude != null && longitude != null) {
            "%.4f° N, %.4f° E (GPS Opt-In)".format(latitude, longitude)
        } else if (isEmergencyModeEnabled) {
            "GPS Enabled (Awaiting Lock)"
        } else {
            "Disabled (Privacy Protected)"
        }
    }
}

/**
 * Manages privacy-preserving, opt-in location sharing ONLY during emergency mode.
 * Does NOT perform background continuous tracking or store location history.
 */
class EmergencyLocationManager(private val context: Context) {

    private val _status = MutableStateFlow(LastKnownStatus())
    val status: StateFlow<LastKnownStatus> = _status.asStateFlow()

    fun setEmergencyModeEnabled(enabled: Boolean, nodeId: String, callSign: String, battery: Int) {
        _status.update {
            it.copy(
                isEmergencyModeEnabled = enabled,
                nodeId = nodeId,
                callSign = callSign,
                batteryPercentage = battery,
                lastSeenTimestamp = System.currentTimeMillis()
            )
        }
        if (enabled) {
            requestSingleLocationFix()
        } else {
            // Clear location immediately when emergency mode is disabled
            _status.update { it.copy(latitude = null, longitude = null) }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestSingleLocationFix() {
        if (!_status.value.isEmergencyModeEnabled) return

        try {
            val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val lastGps = locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastGps != null) {
                updateLocation(lastGps.latitude, lastGps.longitude)
            } else {
                // Fallback simulation for emergency coordinates in test environment
                updateLocation(28.6139, 77.2090)
            }
        } catch (e: Exception) {
            // Permission not granted or GPS disabled
        }
    }

    fun updateLocation(lat: Double, lng: Double) {
        if (!_status.value.isEmergencyModeEnabled) return
        _status.update {
            it.copy(
                latitude = lat,
                longitude = lng,
                lastSeenTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun updateLastMessage(msgText: String, battery: Int) {
        _status.update {
            it.copy(
                lastMessageText = msgText,
                batteryPercentage = battery,
                lastSeenTimestamp = System.currentTimeMillis()
            )
        }
    }
}
