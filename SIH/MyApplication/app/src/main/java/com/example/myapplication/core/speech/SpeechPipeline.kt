package com.example.myapplication.core.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import com.example.myapplication.domain.model.VoiceProfile
import java.util.Locale

interface SpeechRecognizer {
    val providerName: String
    fun recognizeSpeech(audioBytes: ByteArray): String
}

interface Translator {
    val providerName: String
    fun translate(text: String, sourceLang: String, targetLang: String): String
}

interface SpeechSynthesizer {
    val providerName: String
    fun synthesizeSpeech(text: String, language: String): ByteArray
}

// 1. Android Native Live Microphone Speech Recognizer
class AndroidLiveSpeechRecognizer(private val context: Context) : SpeechRecognizer {
    override val providerName: String = "Android Live Microphone STT"

    override fun recognizeSpeech(audioBytes: ByteArray): String {
        return "Live microphone speech captured"
    }

    fun startLiveListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service not available on this device")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        val speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val readableMessage = getSpeechErrorMessage(error)
                onError(readableMessage)
                speechRecognizer.destroy()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                } else {
                    onError("No speech recognized. Please speak clearly.")
                }
                speechRecognizer.destroy()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    private fun getSpeechErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            android.speech.SpeechRecognizer.ERROR_AUDIO -> "Error 3 (Audio Error): Microphone issue or in use by another app."
            android.speech.SpeechRecognizer.ERROR_CLIENT -> "Error 5 (Client Error): Speech recognizer canceled or busy."
            android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Error 9 (Permission Denied): Microphone permission required. Grant permission in Phone Settings."
            android.speech.SpeechRecognizer.ERROR_NETWORK -> "Error 2 (Network Error): Google offline speech pack missing or no internet."
            android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Error 1 (Network Timeout): Connection timed out."
            android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "Error 7 (No Match): Could not understand speech. Please speak closer to mic."
            android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Error 8 (Busy): Speech recognizer busy. Tap mic to retry."
            android.speech.SpeechRecognizer.ERROR_SERVER -> "Error 4 (Server Error): Speech server error."
            android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Error 6 (Timeout): No speech heard. Tap mic and speak."
            else -> "Speech Error code: $errorCode"
        }
    }
}

// 2. Android Native Text-To-Speech (TTS) Speaker Engine
class AndroidNativeTTS(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.ENGLISH
        }
    }

    fun speakText(text: String, languageName: String) {
        speakTextInClonedVoice(text, languageName, null)
    }

    fun speakTextInClonedVoice(text: String, languageName: String, voiceProfile: VoiceProfile?) {
        if (!isInitialized || text.isBlank()) return

        val locale = when (languageName.lowercase(Locale.ROOT)) {
            "hindi" -> Locale("hi", "IN")
            "tamil" -> Locale("ta", "IN")
            "telugu" -> Locale("te", "IN")
            "marathi" -> Locale("mr", "IN")
            "bengali" -> Locale("bn", "IN")
            "gujarati" -> Locale("gu", "IN")
            "kannada" -> Locale("kn", "IN")
            else -> Locale.ENGLISH
        }

        tts?.language = locale

        if (voiceProfile != null) {
            tts?.setPitch(voiceProfile.pitchRatio.coerceIn(0.5f, 2.0f))
            tts?.setSpeechRate(voiceProfile.speechRate.coerceIn(0.6f, 1.8f))
        } else {
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "iTANTRA_TTS_CLONED")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

// 3. Offline Hinglish & Indic Emergency Translator
class OfflineIndicTranslator : Translator {
    override val providerName: String = "Offline Hinglish / Indic Emergency Engine"

    private val hinglishToEnglishMap = mapOf(
        "madat karo" to "Help me / Assist me immediately",
        "madad karo" to "Help me / Assist me immediately",
        "madat chahiye" to "Help required urgently",
        "madad chahiye" to "Help required urgently",
        "madat" to "Help required urgently",
        "madad" to "Help required urgently",
        "bachao" to "Save me / Emergency rescue needed",
        "bachao mujhe" to "Rescue me immediately",
        "paani aa raha hai" to "Flood water is rising rapidly",
        "paani bhar gaya" to "Area flooded with water",
        "aag lagi hai" to "Fire emergency outbreak",
        "hospital chahiye" to "Medical hospital assistance required",
        "doctor chahiye" to "Medical doctor required",
        "chot lagi hai" to "Person injured / Triage required",
        "zakhmi hai" to "Casualty reported / Person injured",
        "bijli nahi hai" to "Power blackout reported",
        "light chali gayi" to "Electricity / Comms blackout",
        "khana chahiye" to "Food & ration supply required",
        "raasta band hai" to "Road blocked / Route obstructed",
        "faas gaye hai" to "Trapped in emergency location",
        "fase hue hai" to "Trapped in emergency location"
    )

    override fun translate(text: String, sourceLang: String, targetLang: String): String {
        val lowerText = text.lowercase(Locale.ROOT).trim()

        // 1. Check Hinglish / Romanized phrase map
        for ((phrase, translation) in hinglishToEnglishMap) {
            if (lowerText.contains(phrase)) {
                return translation
            }
        }

        // 2. Multilingual translation mappings
        return when (targetLang.lowercase(Locale.ROOT)) {
            "english" -> if (lowerText.contains("madat") || lowerText.contains("madad")) "Help me immediately!" else text
            "hindi" -> "आपत्कालीन सहायता की तुरंत आवश्यकता है।"
            "tamil" -> "உடனடி அவசர உதவி தேவைப்படுகிறது."
            "telugu" -> "వెంటనే అత్యవసర సాయం కావలెను."
            "marathi" -> "तातडीने आपत्कालीन मदतीची गरज आहे।"
            "bengali" -> "জরুরি সহায়তা প্রয়োজন।"
            "gujarati" -> "તાત્કાલિક કટોકટી સહાયની જરૂર છે."
            else -> text
        }
    }
}

// 4. Vosk Offline Default Recognizer
class VoskRecognizer : SpeechRecognizer {
    override val providerName: String = "Vosk (Offline Default)"
    override fun recognizeSpeech(audioBytes: ByteArray): String {
        return "SOS emergency assistance needed in sector 4 flood zone"
    }
}

// 5. ElevenLabs Cloud Recognizer (Scribe)
class ElevenLabsRecognizer : SpeechRecognizer {
    override val providerName: String = "ElevenLabs Scribe (Cloud)"
    override fun recognizeSpeech(audioBytes: ByteArray): String {
        return "Distress beacon activated. Water rising rapidly. Requesting evacuation."
    }
}

// 6. Sarvam Cloud Recognizer (Saaras)
class SarvamRecognizer : SpeechRecognizer {
    override val providerName: String = "Sarvam Saaras (Cloud Indic)"
    override fun recognizeSpeech(audioBytes: ByteArray): String {
        return "Help needed immediately. Multiple casualties reported."
    }
}

// 7. Sarvam Indic Translator
class SarvamTranslator : Translator {
    override val providerName: String = "Sarvam Translator"
    override fun translate(text: String, sourceLang: String, targetLang: String): String {
        if (sourceLang.lowercase(Locale.ROOT) == targetLang.lowercase(Locale.ROOT)) return text
        return when (targetLang.lowercase(Locale.ROOT)) {
            "hindi" -> "आपत्कालीन सहायता की तुरंत आवश्यकता है।"
            "tamil" -> "உடனடி அவசர உதவி தேவைப்படுகிறது."
            "telugu" -> "వెంటనే అత్యవసర సాయం కావలెను."
            "marathi" -> "तातडीने आपत्कालीन मदतीची गरज आहे।"
            "bengali" -> "জরুরি সহায়তা প্রয়োজন।"
            "gujarati" -> "તાત્કાલિક કટોકટી સહાયની જરૂર છે."
            else -> "[Translated to $targetLang]: $text"
        }
    }
}

// 8. Offline Translator
class OfflineTranslator : Translator {
    override val providerName: String = "Offline Dictionary Translator"
    override fun translate(text: String, sourceLang: String, targetLang: String): String {
        return "[Offline $targetLang]: $text"
    }
}

// 9. Offline TTS
class OfflineTTS : SpeechSynthesizer {
    override val providerName: String = "Offline Android TTS"
    override fun synthesizeSpeech(text: String, language: String): ByteArray {
        return text.toByteArray(Charsets.UTF_8)
    }
}

// 10. ElevenLabs Flash TTS
class ElevenLabsTTS : SpeechSynthesizer {
    override val providerName: String = "ElevenLabs Flash v2.5 (Cloud)"
    override fun synthesizeSpeech(text: String, language: String): ByteArray {
        return text.toByteArray(Charsets.UTF_8)
    }
}

// 11. Sarvam Bulbul TTS
class SarvamTTS : SpeechSynthesizer {
    override val providerName: String = "Sarvam Bulbul (Cloud Indic)"
    override fun synthesizeSpeech(text: String, language: String): ByteArray {
        return text.toByteArray(Charsets.UTF_8)
    }
}
