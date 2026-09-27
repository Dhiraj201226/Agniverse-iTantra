package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.codec.Packetizer
import com.example.myapplication.core.codec.RetroSpeechCodec
import com.example.myapplication.core.emergency.EmergencyController
import com.example.myapplication.core.security.EncryptionManager
import com.example.myapplication.core.speech.*
import com.example.myapplication.core.telemetry.TelemetryEngine
import com.example.myapplication.core.transport.MeshTransportEngine
import com.example.myapplication.core.transport.VoipPttEngine
import com.example.myapplication.core.transport.WifiPeerEngine
import com.example.myapplication.domain.model.*
import com.example.myapplication.domain.model.VoiceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Engines & Utilities
    val encryptionManager = EncryptionManager()
    val codec = RetroSpeechCodec()
    val packetizer = Packetizer()
    val emotionDetector = EmotionDetector()
    val transportEngine = MeshTransportEngine()
    val emergencyController = EmergencyController(application)
    val telemetryEngine = TelemetryEngine()
    val wifiPeerEngine = WifiPeerEngine(application)
    val liveSpeechRecognizer = AndroidLiveSpeechRecognizer(application)
    val nativeTts = AndroidNativeTTS(application)
    val voipPttEngine = VoipPttEngine()

    val exactIndicTranslator = ExactIndicTranslator()

    // 2. Active AI Providers
    var activeSpeechRecognizer: SpeechRecognizer = liveSpeechRecognizer
    var activeTranslator: Translator = exactIndicTranslator
    var activeSynthesizer: SpeechSynthesizer = OfflineTTS()

    // 3. UI State Flows
    private val _nodeProfile = MutableStateFlow(NodeProfile())
    val nodeProfile: StateFlow<NodeProfile> = _nodeProfile.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _showNodeSetupSheet = MutableStateFlow(true)
    val showNodeSetupSheet: StateFlow<Boolean> = _showNodeSetupSheet.asStateFlow()

    private val _simulatedBatteryPercentage = MutableStateFlow(85)
    val simulatedBatteryPercentage: StateFlow<Int> = _simulatedBatteryPercentage.asStateFlow()

    private val _activeTargetLanguage = MutableStateFlow("Hindi")
    val activeTargetLanguage: StateFlow<String> = _activeTargetLanguage.asStateFlow()

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _recordingStatusText = MutableStateFlow<String?>(null)
    val recordingStatusText: StateFlow<String?> = _recordingStatusText.asStateFlow()

    private val _isPttTransmitting = MutableStateFlow(false)
    val isPttTransmitting: StateFlow<Boolean> = _isPttTransmitting.asStateFlow()

    private val _selectedVoipDestination = MutableStateFlow("BROADCAST")
    val selectedVoipDestination: StateFlow<String> = _selectedVoipDestination.asStateFlow()

    val telemetryState: StateFlow<TelemetryData> = telemetryEngine.telemetryState

    val melSpecExtractor = MelSpecVoiceExtractor()

    init {
        // Load initial sample messages with Mel-Spec Voice Profiles
        seedInitialMessages()

        // Start listening for incoming Walkie-Talkie VoIP Audio Streams over local mesh
        voipPttEngine.startListening(_nodeProfile.value.nodeId)

        // Start UDP Socket Listener for 2-Device Wi-Fi Mesh Comms
        wifiPeerEngine.startListening(_nodeProfile.value.nodeId) { receivedMsg ->
            viewModelScope.launch {
                _messages.update { current -> current + receivedMsg }
                telemetryEngine.recordRxPacket(128)

                // Speak incoming message aloud automatically in sender's CLONED VOICE profile!
                val textToSpeak = receivedMsg.translatedText ?: receivedMsg.originalText
                nativeTts.speakTextInClonedVoice(textToSpeak, _activeTargetLanguage.value, receivedMsg.voiceProfile)

                if (receivedMsg.priority == Priority.CRITICAL) {
                    emergencyController.triggerHapticMorseSos()
                    emergencyController.startSiren()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        wifiPeerEngine.stop()
        voipPttEngine.stopAll()
        nativeTts.shutdown()
    }

    private fun seedInitialMessages() {
        val initialList = listOf(
            Message(
                id = "MSG-101",
                senderId = "NODE-B",
                senderCallSign = "Relay-Alpha",
                originalText = "Base station online on Wi-Fi mesh. All channels active.",
                translatedText = "बेस स्टेशन वाई-फाई मेश पर ऑनलाइन है। सभी चैनल सक्रिय हैं।",
                priority = Priority.NORMAL,
                emotion = EmotionLabel.NEUTRAL,
                timestamp = System.currentTimeMillis() - 300000,
                status = MessageStatus.DELIVERED,
                voiceProfile = VoiceProfile(
                    speakerCallSign = "Relay-Alpha",
                    pitchHz = 110f,
                    pitchRatio = 0.75f,
                    speechRate = 0.95f,
                    spectralCentroidHz = 1600f
                )
            ),
            Message(
                id = "MSG-102",
                senderId = "NODE-C",
                senderCallSign = "Rescue-2",
                originalText = "TRAPPED! Flash flood rising near sector 4 bridge. Need immediate help!",
                translatedText = "फंसे हुए हैं! सेक्टर 4 पुल के पास बाढ़ बढ़ रही है। तुरंत मदद चाहिए!",
                priority = Priority.CRITICAL,
                emotion = EmotionLabel.PANIC,
                isDangerEscalated = true,
                timestamp = System.currentTimeMillis() - 120000,
                status = MessageStatus.DELIVERED,
                voiceProfile = VoiceProfile(
                    speakerCallSign = "Rescue-2",
                    pitchHz = 220f,
                    pitchRatio = 1.45f,
                    speechRate = 1.15f,
                    spectralCentroidHz = 3200f
                )
            )
        )
        _messages.value = initialList
    }

    fun updateNodeProfile(callSign: String, language: String, role: String) {
        _nodeProfile.update {
            val updated = it.copy(
                callSign = callSign,
                primaryLanguage = language,
                role = role,
                isConfigured = true
            )
            voipPttEngine.updateLocalNodeId(updated.nodeId)
            updated
        }
        _showNodeSetupSheet.value = false
    }

    fun openNodeSetupSheet() {
        _showNodeSetupSheet.value = true
    }

    fun dismissNodeSetupSheet() {
        _showNodeSetupSheet.value = false
    }

    fun setTargetLanguage(language: String) {
        _activeTargetLanguage.value = language
    }

    fun updateBatteryPercentage(percentage: Int) {
        _simulatedBatteryPercentage.value = percentage
        val tier = transportEngine.evaluateBatteryTier(percentage)
        telemetryEngine.updateBattery(percentage, tier)
    }

    fun startLiveSpeechRecording(onTextTranscribed: (String) -> Unit) {
        _isRecordingVoice.value = true
        _recordingStatusText.value = "Listening to microphone..."

        liveSpeechRecognizer.startLiveListening(
            onResult = { recognizedText ->
                _isRecordingVoice.value = false
                _recordingStatusText.value = null
                onTextTranscribed(recognizedText)
            },
            onError = { errorMsg ->
                _isRecordingVoice.value = false
                _recordingStatusText.value = "Error: $errorMsg"
            }
        )
    }

    fun setVoipDestination(destination: String) {
        _selectedVoipDestination.value = destination
    }

    fun startVoipPttStreaming() {
        val sourceId = _nodeProfile.value.nodeId
        val destId = _selectedVoipDestination.value

        // Enforce: Source and Destination of radio voice should NOT be the same
        if (sourceId.equals(destId, ignoreCase = true) && !destId.equals("BROADCAST", ignoreCase = true)) {
            _recordingStatusText.value = "Error: Source and Destination cannot be the same node ($sourceId)!"
            return
        }

        _isPttTransmitting.value = true
        _recordingStatusText.value = "Streaming Radio Voice ($sourceId ➔ $destId)"
        voipPttEngine.startTransmitting(sourceId, destId)
    }

    fun stopVoipPttStreaming() {
        if (!_isPttTransmitting.value) return
        _isPttTransmitting.value = false
        _recordingStatusText.value = null
        voipPttEngine.stopTransmitting()
        sendMessage(inputText = "[Voice Walkie-Talkie PTT Transmission Ended: ${_nodeProfile.value.nodeId} ➔ ${_selectedVoipDestination.value}]")
    }

    fun updateCustomVoiceProfile(pitchHz: Float, speechRate: Float, spectralCentroidHz: Float) {
        val newVoiceProfile = melSpecExtractor.buildCustomVoiceProfile(
            speakerCallSign = _nodeProfile.value.callSign,
            pitchHz = pitchHz,
            speechRate = speechRate,
            spectralCentroidHz = spectralCentroidHz
        )
        _nodeProfile.update { it.copy(voiceProfile = newVoiceProfile) }
    }

    fun speakMessageAloud(text: String) {
        nativeTts.speakText(text, _activeTargetLanguage.value)
    }

    fun speakMessageAloudInClonedVoice(text: String, voiceProfile: VoiceProfile?) {
        nativeTts.speakTextInClonedVoice(text, _activeTargetLanguage.value, voiceProfile)
    }

    fun sendMessage(inputText: String, priorityOverride: Priority = Priority.NORMAL) {
        if (inputText.isBlank()) return

        viewModelScope.launch {
            val currentProfile = _nodeProfile.value
            val battery = _simulatedBatteryPercentage.value

            // 1. Emotion Analysis & Danger Escalation
            val emotionResult = emotionDetector.analyzeText(inputText, priorityOverride)
            val finalPriority = if (emotionResult.isDangerDetected) {
                Priority.CRITICAL
            } else {
                priorityOverride
            }

            // 2. Exact Translation & Quality Judgment
            val translationRes = exactIndicTranslator.translateWithJudgement(
                text = inputText,
                sourceLang = currentProfile.primaryLanguage,
                targetLang = _activeTargetLanguage.value
            )

            // 3. Codec Encoding & Compression
            val (compressedBytes, compressionRatio) = codec.encode(inputText)

            // 4. AES-256-GCM Encryption
            val (encryptedBytes, iv) = encryptionManager.encrypt(compressedBytes)

            // 5. Packetization with CRC32
            val messageId = "MSG-${UUID.randomUUID().toString().take(6).uppercase()}"
            val packets = packetizer.packetize(
                messageId = messageId,
                payload = encryptedBytes,
                priority = finalPriority
            )

            val msg = Message(
                id = messageId,
                senderId = currentProfile.nodeId,
                senderCallSign = currentProfile.callSign,
                originalText = inputText,
                translatedText = translationRes.translatedText,
                sourceLanguage = currentProfile.primaryLanguage,
                targetLanguage = _activeTargetLanguage.value,
                priority = finalPriority,
                emotion = emotionResult.emotion,
                emotionConfidence = emotionResult.confidence,
                isDangerEscalated = emotionResult.isDangerDetected,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING,
                codecCompressionRatio = compressionRatio,
                voiceProfile = currentProfile.voiceProfile,
                translationAccuracy = translationRes.accuracyPercentage,
                translationBleuScore = translationRes.bleuScore,
                translationQualityGrade = translationRes.qualityGrade
            )

            // 6. 4-Tier Battery Scheduling & Mesh Transmission Evaluation
            val decision = transportEngine.canTransmitMessage(msg, battery)

            if (decision.allowed) {
                _messages.update { current -> current + msg.copy(status = MessageStatus.SENT) }
                telemetryEngine.recordTxPacket(encryptedBytes.size)

                // Sender's device does NOT speak its own outgoing message aloud.
                // Only recipient nodes speak incoming messages aloud when received.

                // 7. REAL 2-DEVICE PEER BROADCAST OVER WI-FI / HOTSPOT
                wifiPeerEngine.broadcastMessage(msg)
            } else {
                _messages.update { current -> current + msg.copy(status = MessageStatus.FAILED) }
                telemetryEngine.recordDroppedPacket()
            }
        }
    }

    fun triggerSosEmergency(preset: EmergencyPreset? = null) {
        val text = preset?.defaultText ?: "SOS CRITICAL EMERGENCY DISTRESS! Immediate medical & rescue assistance required at current GPS location!"
        emergencyController.triggerHapticMorseSos()
        emergencyController.startSiren()
        sendMessage(inputText = text, priorityOverride = Priority.CRITICAL)
    }

    fun stopEmergencySiren() {
        emergencyController.stopSiren()
    }

    fun selectSpeechRecognizer(provider: SpeechRecognizer) {
        activeSpeechRecognizer = provider
        updateTelemetryProviders()
    }

    fun selectTranslator(provider: Translator) {
        activeTranslator = provider
        updateTelemetryProviders()
    }

    fun selectSynthesizer(provider: SpeechSynthesizer) {
        activeSynthesizer = provider
        updateTelemetryProviders()
    }

    private fun updateTelemetryProviders() {
        telemetryEngine.updateProviders(
            stt = activeSpeechRecognizer.providerName,
            translation = activeTranslator.providerName,
            tts = activeSynthesizer.providerName,
            emotion = "Speech & Text Distress Detector"
        )
    }
}
