package com.example.myapplication.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
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
import com.example.myapplication.core.security.PayloadType
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
                        // Battery Indicator (Language picker removed as per user preference constraint)

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
    val isDark = isSystemInDarkTheme()

    val bubbleColor = when {
        msg.priority == Priority.CRITICAL -> if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2)
        isMe -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textContentColor = when {
        msg.priority == Priority.CRITICAL -> if (isDark) Color(0xFFFEE2E2) else Color(0xFF7F1D1D)
        isMe -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (msg.priority == Priority.CRITICAL) Color(0xFFDC2626) else Color.Transparent

    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isMe) {
        RoundedCornerShape(12.dp).copy(bottomEnd = CornerSize(0.dp))
    } else {
        RoundedCornerShape(12.dp).copy(bottomStart = CornerSize(0.dp))
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = bubbleColor,
            border = if (msg.priority == Priority.CRITICAL) BorderStroke(2.dp, borderColor) else null,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isMe) {
                            Text(
                                text = msg.senderCallSign,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (msg.priority == Priority.CRITICAL) textContentColor else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Emotion Badge
                        if (msg.emotion.isDanger) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = "DANGER",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Priority Tag
                        if (msg.priority != Priority.NORMAL) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (msg.priority) {
                                    Priority.CRITICAL -> Color(0xFF991B1B)
                                    else -> Color(0xFF15803D)
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
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = msg.translatedText ?: msg.originalText,
                    fontSize = 15.sp,
                    color = textContentColor
                )
                
                if (msg.translatedText != null && msg.translatedText != msg.originalText) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Original: ${msg.originalText}",
                        fontSize = 12.sp,
                        color = textContentColor.copy(alpha = 0.7f)
                    )
                }

                if (!msg.translatedText.isNull_or_blank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🎯 Accuracy: ${msg.translationQualityGrade}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Speaker Play Aloud Icon
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Read Aloud",
                        tint = textContentColor.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                val textToSpeak = msg.translatedText ?: msg.originalText
                                onSpeakAloud(textToSpeak, msg.voiceProfile)
                            }
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val timeString = java.text.SimpleDateFormat("HH:mm").format(java.util.Date(msg.timestamp))
                        Text(
                            text = timeString,
                            fontSize = 10.sp,
                            color = textContentColor.copy(alpha = 0.7f)
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val isDoubleTick = msg.status == MessageStatus.DELIVERED || msg.status == MessageStatus.READ
                            Icon(
                                imageVector = if (msg.status == MessageStatus.SENT) Icons.Default.Check else Icons.Default.DoneAll,
                                contentDescription = "Delivery Status",
                                tint = if (msg.status == MessageStatus.READ) Color(0xFF1E88E5) else textContentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
