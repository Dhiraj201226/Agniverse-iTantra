package com.example.myapplication.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MultipleStop
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.core.speech.CommunicationMode
import com.example.myapplication.ui.MainViewModel

@Composable
fun WalkieTalkieScreen(viewModel: MainViewModel) {
    val isPttActive by viewModel.isPttTransmitting.collectAsState()
    val nodeProfile by viewModel.nodeProfile.collectAsState()
    val selectedDestination by viewModel.selectedVoipDestination.collectAsState()

    val sourceNodeId = nodeProfile.nodeId
    val destinationOptions = listOf(
        "BROADCAST" to "Broadcast (All Mesh Nodes)",
        "NODE-B" to "NODE-B (Relay-Alpha)",
        "NODE-C" to "NODE-C (Rescue-2)",
        sourceNodeId to "Self Node ($sourceNodeId - Invalid Test Target)"
    )

    // Validation: Source and Destination of radio voice should NOT be the same
    val isSameSourceAndDestination = sourceNodeId.equals(selectedDestination, ignoreCase = true) && !selectedDestination.equals("BROADCAST", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Source and Destination Routing Panel
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Voice Routing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duplex Toggle
                val commsMode by viewModel.communicationMode.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SegmentedControl(
                        items = listOf("Half-Duplex", "Full-Duplex"),
                        selectedIndex = if (commsMode == CommunicationMode.PUSH_TO_TALK) 0 else 1,
                        onItemSelection = { index ->
                            viewModel.setCommunicationMode(if (index == 0) CommunicationMode.PUSH_TO_TALK else CommunicationMode.CONTINUOUS)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Source Node Display
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("SOURCE RADIO NODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("${nodeProfile.callSign} (ID: $sourceNodeId)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Destination Node Selector
                var destExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { destExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSameSourceAndDestination) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MultipleStop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DESTINATION TARGET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    destinationOptions.find { it.first == selectedDestination }?.second ?: selectedDestination,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = destExpanded,
                        onDismissRequest = { destExpanded = false }
                    ) {
                        destinationOptions.forEach { (id, label) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(label, fontWeight = FontWeight.Medium)
                                        if (id == sourceNodeId) {
                                            Text("⚠️ Same as Source (Invalid Route)", fontSize = 10.sp, color = Color(0xFFD32F2F))
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.setVoipDestination(id)
                                    destExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Validation Status Card
                if (isSameSourceAndDestination) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE),
                        border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD32F2F))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INVALID ROUTE: Source and Destination of radio voice cannot be the same ($sourceNodeId). Please select a different target destination.",
                                color = Color(0xFFB71C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Valid Route: Source ($sourceNodeId) ➔ Destination ($selectedDestination)",
                                color = Color(0xFF1B5E20),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Middle PTT Visualizer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSameSourceAndDestination -> Color(0xFF9E9E9E)
                            isPttActive -> Color(0xFFD32F2F)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                    .pointerInput(isSameSourceAndDestination) {
                        if (!isSameSourceAndDestination) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.startVoipPttStreaming()
                                    tryAwaitRelease()
                                    viewModel.stopVoipPttStreaming()
                                }
                            )
                        }
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Push to Talk",
                        tint = if (isPttActive) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when {
                            isSameSourceAndDestination -> "SAME NODE ROUTE BLOCKED"
                            isPttActive -> "LIVE VOIP TRANSMITTING..."
                            else -> "HOLD TO TALK (VOIP)"
                        },
                        color = if (isPttActive) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isSameSourceAndDestination -> Color(0xFFFFEBEE)
                    isPttActive -> Color(0xFFFFEBEE)
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isSameSourceAndDestination -> "Transmission Disabled"
                            isPttActive -> "Streaming Voice ($sourceNodeId ➔ $selectedDestination)"
                            else -> "Standby Listener Active"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

    }
}

@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelection: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (index == selectedIndex) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onItemSelection(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    color = if (index == selectedIndex) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

