package com.example.myapplication.core.emergency

import android.content.Context
import android.media.AudioManager
import com.example.myapplication.core.speech.OpenSourceIndicTTS
import com.example.myapplication.domain.model.Message
import com.example.myapplication.domain.model.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.PriorityBlockingQueue

/**
 * Priority-aware Audio & TTS Queue Controller.
 * Guarantees that CRITICAL emergency messages interrupt normal TTS playback,
 * play at maximum appropriate volume, and resume the normal queue afterwards.
 */
class EmergencyAudioController(
    private val context: Context,
    private val ttsEngine: OpenSourceIndicTTS
) {

    private val audioQueue = PriorityBlockingQueue<Message>(20) { m1, m2 ->
        m2.priority.level.compareTo(m1.priority.level) // Higher priority level first
    }

    private var currentPlayingMessage: Message? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun enqueueAndPlay(message: Message, targetLanguage: String) {
        if (message.priority == Priority.CRITICAL) {
            // CRITICAL Priority: Interrupt current normal TTS playback immediately!
            if (currentPlayingMessage?.priority != Priority.CRITICAL) {
                ttsEngine.shutdown() // Stop current non-critical playback
                playbackJob?.cancel()
                currentPlayingMessage = null
            }
        }

        audioQueue.add(message)
        processAudioQueue(targetLanguage)
    }

    private fun processAudioQueue(targetLanguage: String) {
        if (playbackJob?.isActive == true && currentPlayingMessage != null) return

        playbackJob = scope.launch {
            while (isActive && audioQueue.isNotEmpty()) {
                val nextMsg = audioQueue.poll() ?: break
                currentPlayingMessage = nextMsg

                if (nextMsg.priority == Priority.CRITICAL) {
                    maximizeEmergencyVolume()
                }

                val textToSpeak = nextMsg.translatedText ?: nextMsg.originalText
                ttsEngine.speakText(textToSpeak, targetLanguage, nextMsg.voiceProfile)

                // Estimate audio playback duration based on word count
                val durationMs = (textToSpeak.split(" ").size * 350L).coerceAtLeast(1800L)
                delay(durationMs)

                currentPlayingMessage = null
            }
        }
    }

    private fun maximizeEmergencyVolume() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 10
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
        } catch (e: Exception) {
            // System safety restriction or permissions
        }
    }

    fun isCriticalPlaying(): Boolean {
        return currentPlayingMessage?.priority == Priority.CRITICAL
    }
}
