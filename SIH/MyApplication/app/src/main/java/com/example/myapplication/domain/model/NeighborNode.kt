package com.example.myapplication.domain.model

/**
 * Represents a dynamically discovered peer node on the local Wi-Fi / UDP mesh network.
 */
data class NeighborNode(
    val nodeId: String,
    val callSign: String,
    val role: String = "Responder",
    val primaryLanguage: String = "English",
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val batteryLevel: Int = 85,
    val hopCount: Int = 1
) {
    fun getStatusLabel(currentTime: Long = System.currentTimeMillis()): String {
        val diffSec = (currentTime - lastSeenTimestamp) / 1000
        return when {
            diffSec < 15 -> "Active 🟢"
            diffSec in 15..45 -> "Degraded 🟡"
            else -> "Offline 🔴"
        }
    }

    fun isOnline(currentTime: Long = System.currentTimeMillis()): Boolean {
        return (currentTime - lastSeenTimestamp) < 45000 // 45 sec timeout
    }
}
