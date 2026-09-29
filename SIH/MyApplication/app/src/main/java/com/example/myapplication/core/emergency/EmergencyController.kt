package com.example.myapplication.core.emergency

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.myapplication.domain.model.EmergencyPreset
import com.example.myapplication.domain.model.Priority

import android.hardware.camera2.CameraManager
import kotlinx.coroutines.*

class EmergencyController(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var flashlightJob: Job? = null

    private var activeRingtone: Ringtone? = null
    private var isEmergencyLatched: Boolean = false

    val presets = listOf(
        EmergencyPreset("P1", "Cyclone Alert", "Warning", "URGENT: Cyclone alert issued for this sector. Immediate evacuation / shelter required.", Priority.CRITICAL),
        EmergencyPreset("P2", "Flash Flood", "WaterDrop", "URGENT: Flash flood levels rising rapidly. Move to higher ground immediately.", Priority.CRITICAL),
        EmergencyPreset("P3", "Medical Emergency", "LocalHospital", "CRITICAL: Urgent medical assistance & field triage team required at GPS coordinates.", Priority.CRITICAL),
        EmergencyPreset("P4", "Comms Blackout", "SignalCellularConnectedNoInternet4Bar", "ALERT: Infrastructure comms blackout. Switching all local units to iTANTRA Mesh.", Priority.CRITICAL)
    )

    fun triggerHapticMorseSos() {
        try {
            val dot = 150L
            val dash = 450L
            val gap = 100L
            val letterGap = 300L

            val morsePattern = longArrayOf(
                0,
                dot, gap, dot, gap, dot, letterGap,  // S (...)
                dash, gap, dash, gap, dash, letterGap, // O (---)
                dot, gap, dot, gap, dot               // S (...)
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(morsePattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(morsePattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(morsePattern, -1)
                }
            }
        } catch (e: Exception) {
            // Context simulated or non-hardware
        }
        triggerFlashlightMorseSos()
    }

    private fun triggerFlashlightMorseSos() {
        flashlightJob?.cancel()
        flashlightJob = scope.launch {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return@launch

                val dot = 150L
                val dash = 450L
                val gap = 100L
                val letterGap = 300L

                val pattern = listOf(
                    dot, gap, dot, gap, dot, letterGap,  // S
                    dash, gap, dash, gap, dash, letterGap, // O
                    dot, gap, dot, gap, dot               // S
                )

                // Flash SOS 3 times
                repeat(3) {
                    for (i in pattern.indices) {
                        if (i % 2 == 0) {
                            cameraManager.setTorchMode(cameraId, true)
                            delay(pattern[i])
                            cameraManager.setTorchMode(cameraId, false)
                        } else {
                            delay(pattern[i])
                        }
                    }
                    delay(1000L)
                }
            } catch (e: Exception) {
                // Ignore flashlight errors
            }
        }
    }

    fun startSiren() {
        try {
            if (activeRingtone?.isPlaying == true) return
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
            activeRingtone = ringtone
            isEmergencyLatched = true
        } catch (e: Exception) {
            isEmergencyLatched = true
        }
    }

    fun stopSiren() {
        try {
            activeRingtone?.stop()
            activeRingtone = null
            isEmergencyLatched = false
            flashlightJob?.cancel()
        } catch (e: Exception) {
            isEmergencyLatched = false
        }
    }

    fun isEmergencyActive(): Boolean = isEmergencyLatched
}
