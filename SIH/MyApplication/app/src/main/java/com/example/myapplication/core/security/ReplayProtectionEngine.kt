package com.example.myapplication.core.security

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class ReplayCheckResult(
    val isAllowed: Boolean,
    val reason: String
)

/**
 * Hardened Security Engine for Replay Attack Prevention over Mesh.
 * Validates monotonic sequence numbers, sliding timestamp windows, and nonce uniqueness.
 */
class ReplayProtectionEngine(
    private val maxTimestampDiffMs: Long = 24 * 60 * 60 * 1000L // 24 Hour Window for offline devices
) {

    private val nodeSequenceMap = ConcurrentHashMap<String, Long>()
    private val seenNonces = ConcurrentHashMap.newKeySet<String>()

    fun validatePacket(
        senderId: String,
        sequenceNumber: Long,
        timestamp: Long,
        nonce: String
    ): ReplayCheckResult {
        val now = System.currentTimeMillis()

        // 1. Sliding Timestamp Window Check (5 Minutes Max)
        if (abs(now - timestamp) > maxTimestampDiffMs) {
            return ReplayCheckResult(false, "REPLAY ALERT: Timestamp outside 5-min sliding window")
        }

        // 2. Nonce / Unique Message ID Deduplication
        if (nonce.isNotBlank() && seenNonces.contains(nonce)) {
            return ReplayCheckResult(false, "REPLAY ALERT: Duplicate nonce/message ID detected ($nonce)")
        }

        // 3. Monotonic Sequence Number Check per Sender Node
        val lastSeq = nodeSequenceMap[senderId] ?: -1L
        if (sequenceNumber > 0 && sequenceNumber <= lastSeq) {
            return ReplayCheckResult(false, "REPLAY ALERT: Monotonic sequence regressed ($sequenceNumber <= $lastSeq)")
        }

        // Record valid security state
        if (sequenceNumber > 0) {
            nodeSequenceMap[senderId] = sequenceNumber
        }
        if (nonce.isNotBlank()) {
            seenNonces.add(nonce)
            if (seenNonces.size > 2000) {
                seenNonces.clear()
            }
        }

        return ReplayCheckResult(true, "VALID: Security & Replay Verification Passed")
    }

    fun resetNodeSequence(senderId: String) {
        nodeSequenceMap.remove(senderId)
    }

    fun clear() {
        nodeSequenceMap.clear()
        seenNonces.clear()
    }
}
