package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.myapplication.domain.model.EmergencyPreset
import com.example.myapplication.ui.MainViewModel

@Composable
fun EmergencyScreen(viewModel: MainViewModel) {
    val emergencyController = viewModel.emergencyController
    var isSirenPlaying by remember { mutableStateOf(emergencyController.isEmergencyActive()) }
    var manualLocation by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Emergency SOS Banner Button
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFB71C1C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("DISTRESS SOS DISPATCH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Triggers Haptic Morse SOS, Siren, and Mesh-Wide CRITICAL Broadcast", color = Color(0xFFFFCDD2), fontSize = 12.sp)

                Spacer(modifier = Modifier.height(16.dp))

                // Manual Location Input
                OutlinedTextField(
                    value = manualLocation,
                    onValueChange = { manualLocation = it },
                    label = { Text("Manual Location (e.g., Trail 4 near River)", color = Color.White) },
                    placeholder = { Text("Enter location if battery is dying...", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFFFFCDD2),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            viewModel.triggerSosEmergency(location = manualLocation)
                            isSirenPlaying = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("TRIGGER FULL SOS", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                    }

                    if (isSirenPlaying) {
                        Button(
                            onClick = {
                                viewModel.stopEmergencySiren()
                                isSirenPlaying = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
                        ) {
                            Text("MUTE SIREN", color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Haptic SOS Tester
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Haptic Morse SOS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Vibrates ...---... (3 short, 3 long, 3 short)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                IconButton(
                    onClick = { emergencyController.triggerHapticMorseSos() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.Vibration, contentDescription = "Test Morse")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Zero-Typing Disaster Presets
        Text(
            text = "One-Touch Zero-Typing Presets",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(emergencyController.presets) { preset ->
                PresetCard(preset = preset) {
                    viewModel.triggerSosEmergency(preset = preset, location = manualLocation)
                    isSirenPlaying = true
                }
            }
        }
    }
}

@Composable
fun PresetCard(preset: EmergencyPreset, onSelect: () -> Unit) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = when (preset.iconName) {
                    "Warning" -> Icons.Default.Warning
                    "WaterDrop" -> Icons.Default.WaterDamage
                    "LocalHospital" -> Icons.Default.LocalHospital
                    else -> Icons.Default.SignalCellularConnectedNoInternet4Bar
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(preset.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.height(4.dp))
            Text(preset.defaultText, fontSize = 11.sp, maxLines = 2, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
        }
    }
}
