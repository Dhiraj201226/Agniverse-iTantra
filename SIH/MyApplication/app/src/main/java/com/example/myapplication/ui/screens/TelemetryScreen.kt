package com.example.myapplication.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.MainViewModel

@Composable
fun TelemetryScreen(viewModel: MainViewModel) {
    val telemetry by viewModel.telemetryState.collectAsState()
    val activeNeighbors by viewModel.activeNeighbors.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Transparency Dashboard",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Real-Time Telemetry & Mesh Observability",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Live Discovered Mesh Neighbors Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Discovered Mesh Neighbors",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${activeNeighbors.size} Connected",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeNeighbors.isEmpty()) {
                    Text(
                        text = "Scanning mesh network for active neighbor nodes...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    activeNeighbors.forEach { neighbor ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${neighbor.callSign} (${neighbor.nodeId})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Role: ${neighbor.role} | Lang: ${neighbor.primaryLanguage}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = neighbor.getStatusLabel(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Battery: ${neighbor.batteryLevel}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Store-and-Forward Bounded Queue Status Card
        val pendingStoreForwardCount = viewModel.wifiPeerEngine.storeAndForwardQueue.getPendingCount()
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Store-and-Forward Queue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (pendingStoreForwardCount > 0) Color(0xFFD97706) else Color(0xFF16A34A)
                    ) {
                        Text(
                            text = "$pendingStoreForwardCount Retained",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Retains messages for offline/disconnected nodes and flushes upon reconnection.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Capacity: 100 Messages | Expiry TTL: 2 Hours | Priority Ordering: Active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 3. Security Hardening & Replay Protection Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Security & Replay Protection",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("Replays Blocked", "${telemetry.replaysBlocked}", isWarning = telemetry.replaysBlocked > 0)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("Epoch ID", "${viewModel.encryptionManager.getEpochId()}")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("Sliding Window", "300s")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("Cipher", "AES-256")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Monotonic sequence tracking & nonce deduplication active across all mesh nodes.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. Network Packets & CRC Integrity Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Network Packets & Integrity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("TX Packets", "${telemetry.packetsTx}")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("RX Packets", "${telemetry.packetsRx}")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("Dropped", "${telemetry.packetsDropped}", isWarning = telemetry.packetsDropped > 0)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("CRC Errors", "${telemetry.crcErrors}", isWarning = telemetry.crcErrors > 0)
                    }
                }
            }
        }

        // 5. 4-Tier Battery Priority Policy Card
        val isDark = isSystemInDarkTheme()
        val (battBgColor, battTextColor, battHeaderColor) = when (telemetry.batteryPercentage) {
            in 21..100 -> Triple(
                if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B),
                if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
            )
            in 10..20 -> Triple(
                if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7),
                if (isDark) Color(0xFFFFFBEB) else Color(0xFF78350F),
                if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
            )
            in 5..9 -> Triple(
                if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2),
                if (isDark) Color(0xFFFEF2F2) else Color(0xFF7F1D1D),
                if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
            )
            else -> Triple(
                if (isDark) Color(0xFF831843) else Color(0xFFFCE7F3),
                if (isDark) Color(0xFFFDF2F8) else Color(0xFF831843),
                if (isDark) Color(0xFFF472B6) else Color(0xFFBE185D)
            )
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = battBgColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = battHeaderColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "4-Tier Battery Priority Policy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = battTextColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Battery Level: ${telemetry.batteryPercentage}% | Tier: ${telemetry.batteryTier.displayName}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = battTextColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = telemetry.batteryTier.rangeDescription,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = battTextColor
                )
            }
        }

        // 6. Stage-by-Stage Pipeline Latency Instrumentation
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pipeline Latency Instrumentation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LatencyRow("Speech-to-Text (${telemetry.activeSTTProvider})", "${telemetry.sttLatencyMs} ms")
                LatencyRow("Exact Translation (${telemetry.activeTranslationProvider})", "${telemetry.translationLatencyMs} ms")
                LatencyRow("Codec Byte Compression (zlib/dict)", "${telemetry.codecLatencyMs} ms")
                LatencyRow("Mesh Transport Transit Time", "${telemetry.transportLatencyMs} ms")
                LatencyRow("AES-256-GCM Decryption", "${telemetry.decryptionLatencyMs} ms")
                LatencyRow("Cloned Voice TTS Synthesis (${telemetry.activeTTSProvider})", "${telemetry.ttsLatencyMs} ms")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                LatencyRow("TOTAL END-TO-END PIPELINE LATENCY", "${telemetry.endToEndLatencyMs} ms", isBold = true)
            }
        }

        // 7. System Hardware Metrics
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System Hardware Metrics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("App RAM", "${telemetry.ramUsageMb} MB")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("CPU Load", "${telemetry.cpuPercentage}%")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("RTT", "${telemetry.rttMs} ms")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricItem("Loss Rate", "${telemetry.lossPercentage}%")
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, isWarning: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = if (isWarning) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LatencyRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
