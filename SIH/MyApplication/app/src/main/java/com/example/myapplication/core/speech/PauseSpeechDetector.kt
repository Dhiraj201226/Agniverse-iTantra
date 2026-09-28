package com.example.myapplication.core.speech

import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

enum class CommunicationMode {
    PUSH_TO_TALK,
    CONTINUOUS
}

enum class SystemSpeechState {
    IDLE,
    LISTENING,
    PROCESSING,
    SENDING,
    RECEIVING,
    SPEAKING
}

data class SpeechDetectionConfig(
    val speechStartRms: Float = 350.0f,
    val silenceDurationMs: Long = 700L,        // Configurable silence pause (300-2000ms)
    val minUtteranceMs: Long = 400L,          // Minimum speech duration
    val maxUtteranceMs: Long = 12000L         // Maximum utterance cap before auto-framing
)

/**
 * Detects speech activity, monitors pauses/stoppages, and finalizes complete sentences
 * for both Push-To-Talk (PTT) and Continuous Communication modes.
 */
class PauseSpeechDetector(
    private var config: SpeechDetectionConfig = SpeechDetectionConfig()
) {

    private var isSpeaking = false
    private var speechStartTimeMs = 0L
    private var lastSpeechTimeMs = 0L
    private val pcmBufferStream = ByteArrayOutputStream()

    fun updateConfig(newConfig: SpeechDetectionConfig) {
        config = newConfig
    }

    fun processAudioChunk(
        pcmChunk: ShortArray,
        sampleRate: Int = 16000,
        onSentenceFinalized: (ByteArray) -> Unit
    ) {
        if (pcmChunk.isEmpty()) return

        val now = System.currentTimeMillis()
        val rms = calculateRms(pcmChunk)
        val isCurrentChunkSpeech = rms > config.speechStartRms

        if (isCurrentChunkSpeech) {
            lastSpeechTimeMs = now
            if (!isSpeaking) {
                isSpeaking = true
                speechStartTimeMs = now
                pcmBufferStream.reset()
            }

            // Append PCM bytes (16-bit)
            for (sample in pcmChunk) {
                pcmBufferStream.write((sample.toInt() and 0xFF))
                pcmBufferStream.write((sample.toInt() shr 8 and 0xFF))
            }

            // Force finalization if max utterance duration reached
            if ((now - speechStartTimeMs) >= config.maxUtteranceMs) {
                finalizeUtterance(onSentenceFinalized)
            }
        } else if (isSpeaking) {
            // Append background silence padding
            for (sample in pcmChunk) {
                pcmBufferStream.write((sample.toInt() and 0xFF))
                pcmBufferStream.write((sample.toInt() shr 8 and 0xFF))
            }

            val silenceElapsed = now - lastSpeechTimeMs
            val totalSpeechDuration = lastSpeechTimeMs - speechStartTimeMs

            // Trigger sentence finalization after silence duration threshold
            if (silenceElapsed >= config.silenceDurationMs) {
                if (totalSpeechDuration >= config.minUtteranceMs) {
                    finalizeUtterance(onSentenceFinalized)
                } else {
                    // Discard noise bursts shorter than minUtteranceMs
                    resetState()
                }
            }
        }
    }

    private fun finalizeUtterance(onSentenceFinalized: (ByteArray) -> Unit) {
        val audioData = pcmBufferStream.toByteArray()
        resetState()
        if (audioData.isNotEmpty()) {
            onSentenceFinalized(audioData)
        }
    }

    fun forceFinalize(onSentenceFinalized: (ByteArray) -> Unit) {
        if (pcmBufferStream.size() > 0) {
            finalizeUtterance(onSentenceFinalized)
        } else {
            resetState()
        }
    }

    private fun resetState() {
        isSpeaking = false
        speechStartTimeMs = 0L
        lastSpeechTimeMs = 0L
        pcmBufferStream.reset()
    }

    private fun calculateRms(samples: ShortArray): Float {
        var sum = 0.0
        for (sample in samples) {
            sum += (sample * sample).toDouble()
        }
        return sqrt(sum / samples.size.coerceAtLeast(1)).toFloat()
    }

    fun isCurrentlySpeaking(): Boolean = isSpeaking
}
