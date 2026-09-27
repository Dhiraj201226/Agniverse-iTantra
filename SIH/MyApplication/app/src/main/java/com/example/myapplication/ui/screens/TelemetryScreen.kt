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
import com.example.myapplication.ui.MainViewModel

@Composable
fun TelemetryScreen(viewModel: MainViewModel) {
    val telemetry by viewModel.telemetryState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Real-Time Transparency Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        // 1. Packets Metrics Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("NETWORK PACKETS & CRC INTEGRITY", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem("TX Packets", "${telemetry.packetsTx}")
                    MetricItem("RX Packets", "${telemetry.packetsRx}")
                    MetricItem("Dropped", "${telemetry.packetsDropped}", isWarning = telemetry.packetsDropped > 0)
                    MetricItem("CRC Errors", "${telemetry.crcErrors}", isWarning = telemetry.crcErrors > 0)
                }
            }
        }

        // 2. Battery Tier & Policy Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (telemetry.batteryPercentage) {
                    in 21..100 -> Color(0xFFE8F5E9)
                    in 10..20 -> Color(0xFFFFF3E0)
                    in 5..9 -> Color(0xFFFFEBEE)
                    else -> Color(0xFFFCE4EC)
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("4-TIER BATTERY PRIORITY POLICY", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Current Battery: ${telemetry.batteryPercentage}%", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Active Policy Tier: ${telemetry.batteryTier.displayName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(telemetry.batteryTier.rangeDescription, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 3. AI & Latency Breakdown
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI PIPELINE LATENCY BREAKDOWN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                LatencyRow("STT Latency (${telemetry.activeSTTProvider})", "${telemetry.sttLatencyMs} ms")
                LatencyRow("Translation (${telemetry.activeTranslationProvider})", "${telemetry.translationLatencyMs} ms")
                LatencyRow("Codec Compression", "${telemetry.codecLatencyMs} ms")
                LatencyRow("AES-GCM Decryption", "${telemetry.decryptionLatencyMs} ms")
                LatencyRow("TTS Synthesis (${telemetry.activeTTSProvider})", "${telemetry.ttsLatencyMs} ms")
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                LatencyRow("End-to-End Latency", "${telemetry.endToEndLatencyMs} ms", isBold = true)
            }
        }

        // 4. Hardware System Stats
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SYSTEM HARDWARE METRICS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem("App RAM", "${telemetry.ramUsageMb} MB")
                    MetricItem("CPU Load", "${telemetry.cpuPercentage}%")
                    MetricItem("RTT", "${telemetry.rttMs} ms")
                    MetricItem("Loss %", "${telemetry.lossPercentage}%")
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, isWarning: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isWarning) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LatencyRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
