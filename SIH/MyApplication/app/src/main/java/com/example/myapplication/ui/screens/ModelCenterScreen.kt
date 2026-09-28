package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.core.speech.*
import com.example.myapplication.domain.model.Priority
import com.example.myapplication.ui.MainViewModel

@Composable
fun ModelCenterScreen(viewModel: MainViewModel) {
    val nodeProfile by viewModel.nodeProfile.collectAsState()
    val batteryPct by viewModel.simulatedBatteryPercentage.collectAsState()

    val speechProviders = listOf(VoskRecognizer(), ElevenLabsRecognizer(), SarvamRecognizer())
    val translators = listOf(SarvamTranslator(), OfflineTranslator())
    val synthesizers = listOf(OfflineTTS(), ElevenLabsTTS(), SarvamTTS())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Model Center & Local Node Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        // 1. Node Profile Config Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("LOCAL NODE IDENTIFIER", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Call Sign: ${nodeProfile.callSign}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Language: ${nodeProfile.primaryLanguage} | Role: ${nodeProfile.role}", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.openNodeSetupSheet() }) {
                    Text("Reconfigure Node Information")
                }
            }
        }

        // 1A. 10-Language Offline Model Manager Card
        val langModels by viewModel.languageModels.collectAsState()
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("10-LANGUAGE OFFLINE MODEL MANAGER", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loads only active language model to save RAM on low-end devices. Unloads inactive models automatically.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                langModels.forEach { modelCfg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${modelCfg.languageName} (${modelCfg.isoCode.uppercase()})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Size: ${modelCfg.getFormattedSize()} | ${if (modelCfg.isInstalled) "Installed ✓" else "Not Installed"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (modelCfg.loadState) {
                                    ModelLoadState.LOADED -> {
                                        AssistChip(
                                            onClick = { viewModel.loadLanguageModel(modelCfg.languageName) },
                                            label = { Text("LOADED 🟢", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                        )
                                    }
                                    ModelLoadState.LOADING -> {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    }
                                    else -> {
                                        OutlinedButton(
                                            onClick = { viewModel.loadLanguageModel(modelCfg.languageName) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Load Model", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1B. Mel-Spectrogram Vocal Profile & Voice Cloning Studio
        val currentVp = nodeProfile.voiceProfile
        var pitchHz by remember { mutableStateOf(currentVp.pitchHz) }
        var speechRate by remember { mutableStateOf(currentVp.speechRate) }
        var centroidHz by remember { mutableStateOf(currentVp.spectralCentroidHz) }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MEL-SPECTROGRAM VOICE CLONING STUDIO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Low-bandwidth mesh transmits Mel-Spectrogram properties (<1KB). Receiver synthesizes text in YOUR exact voice profile!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pitch F0 Slider
                Text("Fundamental Pitch F0: ${pitchHz.toInt()} Hz (${if (pitchHz < 150f) "Male / Low" else "Female / High"})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Slider(
                    value = pitchHz,
                    onValueChange = { pitchHz = it },
                    valueRange = 80f..300f
                )

                // Speech Speed Slider
                Text("Speaking Rate: ${String.format("%.2f", speechRate)}x Speed", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it },
                    valueRange = 0.7f..1.5f
                )

                // Acoustic Timbre Slider
                Text("Spectral Centroid (Timbre): ${centroidHz.toInt()} Hz", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Slider(
                    value = centroidHz,
                    onValueChange = { centroidHz = it },
                    valueRange = 1000f..4000f
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 8-Band Mel Spectrogram Visualizer Bar Chart
                Text("Live 8-Band Mel-Filterbank Energies:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val tempVp = viewModel.melSpecExtractor.buildCustomVoiceProfile(nodeProfile.callSign, pitchHz, speechRate, centroidHz)
                    tempVp.melEnergyBands.forEachIndexed { idx, energy ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .width(18.dp)
                                    .height((energy * 35).dp.coerceAtLeast(4.dp))
                            ) {}
                            Text("${idx + 1}", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val testProfile = viewModel.melSpecExtractor.buildCustomVoiceProfile(nodeProfile.callSign, pitchHz, speechRate, centroidHz)
                            viewModel.speakMessageAloudInClonedVoice("Testing voice profile synthesis on mesh node.", testProfile)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Voice", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.updateCustomVoiceProfile(pitchHz, speechRate, centroidHz)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Profile", fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Simulated Battery Slider Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SIMULATE BATTERY LEVEL FOR TIER TESTING", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Simulated Battery: ${batteryPct}%", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Slider(
                    value = batteryPct.toFloat(),
                    onValueChange = { viewModel.updateBatteryPercentage(it.toInt()) },
                    valueRange = 1f..100f
                )
            }
        }

        // 3. STT Provider Selection Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SPEECH RECOGNITION (STT) ENGINE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                speechProviders.forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = viewModel.activeSpeechRecognizer.providerName == provider.providerName,
                            onClick = { viewModel.selectSpeechRecognizer(provider) }
                        )
                        Text(provider.providerName, fontSize = 14.sp)
                    }
                }
            }
        }

        // 4. Translator Selection Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("TRANSLATION ENGINE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                translators.forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = viewModel.activeTranslator.providerName == provider.providerName,
                            onClick = { viewModel.selectTranslator(provider) }
                        )
                        Text(provider.providerName, fontSize = 14.sp)
                    }
                }
            }
        }

        // 5. Acoustic & Linguistic Emotion Classifier Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ACOUSTIC & LINGUISTIC DISTRESS EMOTION MODEL", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Analyzes vocal pitch F0, speech rate, and multi-lingual keywords to classify emergency distress probabilities.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                var testInput by remember { mutableStateOf("TRAPPED! Sinking in flash flood water near sector 4 bridge! Need help!") }
                OutlinedTextField(
                    value = testInput,
                    onValueChange = { testInput = it },
                    label = { Text("Sample Text / Voice Transcript to Classify") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))
                val emotionRes = viewModel.emotionDetector.analyzeText(testInput, Priority.NORMAL, nodeProfile.voiceProfile)

                Text("Detected State: ${emotionRes.emotion.displayName} (${(emotionRes.confidence * 100).toInt()}% confidence)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (emotionRes.isDangerDetected) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary)
                if (emotionRes.escalationReason != null) {
                    Text("Auto Escalation: ${emotionRes.escalationReason}", fontSize = 11.sp, color = Color(0xFFB71C1C))
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Multi-Class Probability Distribution:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                emotionRes.emotionProbabilities.forEach { (label, prob) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label.displayName, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (label.isDanger) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .fillMaxWidth(prob)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${(prob * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 6. Security Key Parameters
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("CRYPTOGRAPHIC SESSION KEYS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Key Cipher: ${viewModel.encryptionManager.getKeyAlgorithm()}", fontSize = 12.sp)
                Text("Epoch ID: ${viewModel.encryptionManager.getEpochId()}", fontSize = 12.sp)
                Text("Storage: Android Keystore Hardware-backed", fontSize = 12.sp)
            }
        }
    }
}
