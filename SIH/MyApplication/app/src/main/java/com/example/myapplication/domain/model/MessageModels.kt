package com.example.myapplication.domain.model

import com.example.myapplication.core.security.PayloadType

enum class EmotionLabel(val displayName: String, val isDanger: Boolean) {
    NEUTRAL("Neutral", false),
    DISTRESS("Distress", true),
    PANIC("Panic", true),
    FEAR("Fear", true),
    HAPPY("Happy", false),
    ANGRY("Angry", false)
}

enum class Priority(val level: Int, val displayName: String) {
    CRITICAL(3, "CRITICAL"),
    NORMAL(2, "NORMAL"),
    LOW(1, "LOW")
}

enum class MessageStatus {
    DRAFT,
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

enum class PacketType {
    START,
    DATA,
    END,
    ACK,
    NACK
}

data class Message(
    val id: String,
    val senderId: String,
    val senderCallSign: String,
    val recipientId: String = "BROADCAST",
    val originalText: String,
    val translatedText: String? = null,
    val sourceLanguage: String = "English",
    val targetLanguage: String? = null,
    val priority: Priority = Priority.NORMAL,
    val emotion: EmotionLabel = EmotionLabel.NEUTRAL,
    val emotionConfidence: Float = 0.85f,
    val isDangerEscalated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val payloadType: PayloadType = PayloadType.FREE_TEXT,
    val templateId: Int? = null,
    val codecCompressionRatio: Float = 0.42f,
    val originalByteSize: Int = 0,
    val compressedByteSize: Int = 0,
    val sourceHash: String = "",
    val destinationHash: String = "",
    val isTampered: Boolean = false,
    val isEncrypted: Boolean = true,
    val hopCount: Int = 1,
    val voiceProfile: VoiceProfile? = null,
    val translationAccuracy: Float = 98.0f,
    val translationBleuScore: Float = 0.95f,
    val translationQualityGrade: String = "EXACT (98%)",
    val sttMs: Long = 240,
    val ttsMs: Long = 310,
    val e2eMs: Long = 780,
    val rtfRatio: Float = 0.12f
)

data class Packet(
    val magic: String = "iTANTRA",
    val version: Int = 1,
    val epoch: Long = 1001L,
    val packetType: PacketType,
    val messageId: String,
    val sequenceNumber: Int,
    val totalPackets: Int,
    val priority: Priority,
    val payload: ByteArray,
    val crc32: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Packet
        return messageId == other.messageId && sequenceNumber == other.sequenceNumber
    }

    override fun hashCode(): Int {
        var result = messageId.hashCode()
        result = 31 * result + sequenceNumber
        return result
    }
}

data class EmergencyPreset(
    val id: String,
    val title: String,
    val iconName: String,
    val defaultText: String,
    val priority: Priority = Priority.CRITICAL
)
