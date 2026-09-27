package com.example.myapplication.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.model.Message
import com.example.myapplication.domain.model.MessageStatus
import com.example.myapplication.domain.model.Priority
import com.example.myapplication.domain.model.VoiceProfile
import com.example.myapplication.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommsScreen(viewModel: MainViewModel) {
    val messages by viewModel.messages.collectAsState()
    val nodeProfile by viewModel.nodeProfile.collectAsState()
    val batteryPct by viewModel.simulatedBatteryPercentage.collectAsState()
    val targetLang by viewModel.activeTargetLanguage.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val recordingStatusText by viewModel.recordingStatusText.collectAsState()

    var textInput by remember { mutableStateOf("") }

    val languages = listOf("Hindi", "Tamil", "Telugu", "Marathi", "Bengali", "Gujarati", "Kannada", "English")

    // Android Native System Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val voiceText = matches[0]
                textInput = voiceText
                viewModel.sendMessage(voiceText)
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "iTANTRA Mesh Comms",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Node: ${nodeProfile.callSign} (${nodeProfile.role})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // SOS Quick Trigger Button
                        Button(
                            onClick = { viewModel.triggerSosEmergency() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "SOS", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SOS", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Translation Selector
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Translate to: ", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            var langExpanded by remember { mutableStateOf(false) }
                            Box {
                                AssistChip(
                                    onClick = { langExpanded = true },
                                    label = { Text(targetLang, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                DropdownMenu(
                                    expanded = langExpanded,
                                    onDismissRequest = { langExpanded = false }
                                ) {
                                    languages.forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text(lang) },
                                            onClick = {
                                                viewModel.setTargetLanguage(lang)
                                                langExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Battery Indicator
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                batteryPct > 20 -> Color(0xFF2E7D32)
                                batteryPct in 10..20 -> Color(0xFFF57C00)
                                batteryPct in 5..9 -> Color(0xFFD32F2F)
                                else -> Color(0xFF880E4F)
                            }
                        ) {
                            Text(
                                text = "Battery: $batteryPct%",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Status Banner if recording
            if (recordingStatusText != null) {
                Surface(
                    color = if (isRecordingVoice) Color(0xFFD32F2F) else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = recordingStatusText ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(
                        msg = msg,
                        isMe = msg.senderId == nodeProfile.nodeId,
                        onSpeakAloud = { textToSpeak, vp ->
                            viewModel.speakMessageAloudInClonedVoice(textToSpeak, vp)
                        }
                    )
                }
            }

            // Input Bar
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Speech Live STT Button
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak into microphone for iTANTRA Comms...")
                                }
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                // Fallback to background listener
                                viewModel.startLiveSpeechRecording { recordedText ->
                                    viewModel.sendMessage(recordedText)
                                }
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isRecordingVoice) Color(0xFFD32F2F) else MaterialTheme.colorScheme.secondaryContainer
                            )
                    ) {
                        Icon(
                            imageVector = if (isRecordingVoice) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Live Microphone STT Recording",
                            tint = if (isRecordingVoice) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Type message or tap mic to record...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    msg: Message,
    isMe: Boolean,
    onSpeakAloud: (String, VoiceProfile?) -> Unit
) {
    val bubbleColor = when {
        msg.priority == Priority.CRITICAL -> Color(0xFFFFEBEE)
        isMe -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = if (msg.priority == Priority.CRITICAL) Color(0xFFD32F2F) else Color.Transparent

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bubbleColor,
        border = if (msg.priority == Priority.CRITICAL) BorderStroke(2.dp, borderColor) else null,
        modifier = Modifier.fillMaxWidth(0.9f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = msg.senderCallSign,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Speaker Play Aloud Icon
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Read Aloud in Cloned Voice",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                val textToSpeak = msg.translatedText ?: msg.originalText
                                onSpeakAloud(textToSpeak, msg.voiceProfile)
                            }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Emotion Badge
                    if (msg.emotion.isDanger) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD32F2F)
                        ) {
                            Text(
                                text = "DANGER: ${msg.emotion.displayName}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Priority Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (msg.priority) {
                            Priority.CRITICAL -> Color(0xFFC62828)
                            Priority.NORMAL -> Color(0xFF1565C0)
                            Priority.LOW -> Color(0xFF558B2F)
                        }
                    ) {
                        Text(
                            text = msg.priority.displayName,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = msg.originalText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (!msg.translatedText.isNull_or_blank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSpeakAloud(msg.translatedText ?: msg.originalText, msg.voiceProfile)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌐 ${msg.translatedText}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speak Translation",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Translation Quality Judgment Metric Badge
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🎯 Accuracy: ${msg.translationQualityGrade} | BLEU Score: ${String.format("%.2f", msg.translationBleuScore)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Cloned Voice Mel-Spec Metadata Tag
            msg.voiceProfile?.let { vp ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Voice Clone: ${vp.getPitchLabel()} | Timbre: ${vp.getTimbreLabel()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Codec Comp: ${(msg.codecCompressionRatio * 100).toInt()}% | AES-GCM Encrypted",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (msg.status == MessageStatus.SENT) "✓ Sent" else "✓ Delivered",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
