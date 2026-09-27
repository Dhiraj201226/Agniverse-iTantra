package com.example.myapplication.domain.model

enum class BatteryTier(val displayName: String, val rangeDescription: String) {
    NORMAL("NORMAL (>20%)", "All traffic (CRITICAL, NORMAL, LOW) active"),
    LOW_POWER("LOW POWER (10-20%)", "LOW priority deferred; CRITICAL & NORMAL active"),
    CRITICAL_POWER("CRITICAL POWER (5-10%)", "NORMAL & LOW deferred; Only CRITICAL active"),
    HIGHLY_CRITICAL("HIGHLY CRITICAL (<5%)", "Ultra-low power; Only vital CRITICAL SOS active")
}

data class TelemetryData(
    val packetsTx: Long = 0,
    val packetsRx: Long = 0,
    val packetsDropped: Long = 0,
    val packetsCorrupted: Long = 0,
    val packetsRetried: Long = 0,
    val crcErrors: Long = 0,
    val ackTimeouts: Long = 0,
    val rttMs: Long = 120,
    val lossPercentage: Float = 2.5f,
    val currentTransport: String = "Wi-Fi Mesh",
    val currentHops: Int = 2,
    val ttlRemaining: Int = 8,
    val sttLatencyMs: Long = 280,
    val translationLatencyMs: Long = 65,
    val codecLatencyMs: Long = 12,
    val transportLatencyMs: Long = 110,
    val decryptionLatencyMs: Long = 18,
    val ttsLatencyMs: Long = 340,
    val endToEndLatencyMs: Long = 845,
    val ramUsageMb: Int = 142,
    val cpuPercentage: Int = 18,
    val batteryPercentage: Int = 85,
    val batteryTier: BatteryTier = BatteryTier.NORMAL,
    val activeSTTProvider: String = "Vosk (Offline Default)",
    val activeTranslationProvider: String = "Sarvam Translator",
    val activeTTSProvider: String = "Offline TTS",
    val activeEmotionDetector: String = "Rule-based Distress Engine"
)
