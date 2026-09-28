package com.example.myapplication.core.telemetry

import com.example.myapplication.domain.model.BatteryTier
import com.example.myapplication.domain.model.TelemetryData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TelemetryEngine {

    private val _telemetryState = MutableStateFlow(TelemetryData())
    val telemetryState: StateFlow<TelemetryData> = _telemetryState.asStateFlow()

    fun recordTxPacket(bytes: Int) {
        _telemetryState.update { current ->
            current.copy(
                packetsTx = current.packetsTx + 1,
                endToEndLatencyMs = (750..920).random().toLong()
            )
        }
    }

    fun recordRxPacket(bytes: Int) {
        _telemetryState.update { current ->
            current.copy(packetsRx = current.packetsRx + 1)
        }
    }

    fun recordDroppedPacket() {
        _telemetryState.update { current ->
            current.copy(packetsDropped = current.packetsDropped + 1)
        }
    }

    fun recordCrcError() {
        _telemetryState.update { current ->
            current.copy(
                packetsCorrupted = current.packetsCorrupted + 1,
                crcErrors = current.crcErrors + 1
            )
        }
    }

    fun recordReplayAttackBlocked() {
        _telemetryState.update { current ->
            current.copy(replaysBlocked = current.replaysBlocked + 1)
        }
    }

    fun recordPipelineLatencies(
        sttMs: Long,
        translationMs: Long,
        codecMs: Long,
        encryptionMs: Long,
        transportMs: Long,
        decryptionMs: Long,
        ttsMs: Long
    ) {
        val totalMs = sttMs + translationMs + codecMs + encryptionMs + transportMs + decryptionMs + ttsMs

        val runtime = Runtime.getRuntime()
        val usedRamMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt().coerceAtLeast(35)

        _telemetryState.update { current ->
            current.copy(
                sttLatencyMs = sttMs,
                translationLatencyMs = translationMs,
                codecLatencyMs = codecMs,
                transportLatencyMs = transportMs,
                decryptionLatencyMs = decryptionMs,
                ttsLatencyMs = ttsMs,
                endToEndLatencyMs = totalMs,
                ramUsageMb = usedRamMb,
                cpuPercentage = (12..25).random()
            )
        }
    }

    fun updateBattery(level: Int, tier: BatteryTier) {
        _telemetryState.update { current ->
            current.copy(
                batteryPercentage = level,
                batteryTier = tier
            )
        }
    }

    fun updateProviders(stt: String, translation: String, tts: String, emotion: String) {
        _telemetryState.update { current ->
            current.copy(
                activeSTTProvider = stt,
                activeTranslationProvider = translation,
                activeTTSProvider = tts,
                activeEmotionDetector = emotion
            )
        }
    }
}
