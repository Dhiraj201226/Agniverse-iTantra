package com.example.myapplication.core.transport

import com.example.myapplication.domain.model.BatteryTier
import com.example.myapplication.domain.model.Message
import com.example.myapplication.domain.model.Packet
import com.example.myapplication.domain.model.Priority
import java.util.concurrent.ConcurrentHashMap

data class DispatchDecision(
    val allowed: Boolean,
    val reason: String,
    val activeTier: BatteryTier
)

class MeshTransportEngine {

    private val seenMessageIds = ConcurrentHashMap.newKeySet<String>()
    private val priorityQueue = mutableListOf<Message>()
    private val maxTtl = 8

    fun evaluateBatteryTier(batteryPercentage: Int): BatteryTier {
        return when {
            batteryPercentage > 20 -> BatteryTier.NORMAL
            batteryPercentage in 10..20 -> BatteryTier.LOW_POWER
            batteryPercentage in 5..9 -> BatteryTier.CRITICAL_POWER
            else -> BatteryTier.HIGHLY_CRITICAL
        }
    }

    fun canTransmitMessage(message: Message, batteryPercentage: Int): DispatchDecision {
        val tier = evaluateBatteryTier(batteryPercentage)

        return when (tier) {
            BatteryTier.NORMAL -> DispatchDecision(
                allowed = true,
                reason = "Battery > 20%: All traffic classes allowed",
                activeTier = tier
            )
            BatteryTier.LOW_POWER -> {
                if (message.priority == Priority.LOW) {
                    DispatchDecision(
                        allowed = false,
                        reason = "Battery 10-20%: LOW priority message deferred to save power",
                        activeTier = tier
                    )
                } else {
                    DispatchDecision(
                        allowed = true,
                        reason = "Battery 10-20%: CRITICAL/NORMAL allowed",
                        activeTier = tier
                    )
                }
            }
            BatteryTier.CRITICAL_POWER -> {
                if (message.priority == Priority.CRITICAL) {
                    DispatchDecision(
                        allowed = true,
                        reason = "Battery 5-10%: Only CRITICAL emergency traffic allowed",
                        activeTier = tier
                    )
                } else {
                    DispatchDecision(
                        allowed = false,
                        reason = "Battery 5-10%: Non-critical traffic deferred",
                        activeTier = tier
                    )
                }
            }
            BatteryTier.HIGHLY_CRITICAL -> {
                if (message.priority == Priority.CRITICAL) {
                    DispatchDecision(
                        allowed = true,
                        reason = "Battery < 5%: Highly Critical SOS beacon path active",
                        activeTier = tier
                    )
                } else {
                    DispatchDecision(
                        allowed = false,
                        reason = "Battery < 5%: Radio disabled for non-CRITICAL SOS",
                        activeTier = tier
                    )
                }
            }
        }
    }

    fun isDuplicateMessage(messageId: String): Boolean {
        if (seenMessageIds.contains(messageId)) {
            return true
        }
        seenMessageIds.add(messageId)
        if (seenMessageIds.size > 500) {
            seenMessageIds.clear()
        }
        return false
    }

    fun shouldForwardPacket(packet: Packet, currentHops: Int): Boolean {
        if (currentHops >= maxTtl) return false
        return !isDuplicateMessage("${packet.messageId}_${packet.sequenceNumber}")
    }

    fun enqueue(message: Message) {
        priorityQueue.add(message)
        priorityQueue.sortWith(compareByDescending<Message> { it.priority.level }.thenBy { it.timestamp })
    }

    fun dequeueNext(): Message? {
        return if (priorityQueue.isNotEmpty()) priorityQueue.removeAt(0) else null
    }

    fun getQueueSize(): Int = priorityQueue.size
}
