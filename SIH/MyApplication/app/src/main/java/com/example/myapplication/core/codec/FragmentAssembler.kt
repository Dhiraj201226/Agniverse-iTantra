package com.example.myapplication.core.codec

import com.example.myapplication.domain.model.Packet
import com.example.myapplication.domain.model.PacketType
import java.util.concurrent.ConcurrentHashMap

data class SelectiveNackRequest(
    val messageId: String,
    val missingSequenceNumbers: List<Int>
)

/**
 * Manages message fragment reassembly, detects missing sequence numbers,
 * and generates selective NACK retransmission requests.
 */
class FragmentAssembler {

    private val pendingMessageFragments = ConcurrentHashMap<String, ConcurrentHashMap<Int, Packet>>()
    private val expectedChunkCounts = ConcurrentHashMap<String, Int>()

    fun processFragment(packet: Packet): ByteArray? {
        val msgId = packet.messageId

        if (packet.packetType == PacketType.START) {
            val parts = String(packet.payload, Charsets.UTF_8).split(":")
            if (parts.size >= 3) {
                expectedChunkCounts[msgId] = parts[2].toIntOrNull() ?: 1
            }
            pendingMessageFragments.putIfAbsent(msgId, ConcurrentHashMap())
            return null
        }

        if (packet.packetType == PacketType.DATA) {
            val msgMap = pendingMessageFragments.computeIfAbsent(msgId) { ConcurrentHashMap() }
            msgMap[packet.sequenceNumber] = packet

            val totalExpected = expectedChunkCounts[msgId]
            if (totalExpected != null && msgMap.size >= totalExpected) {
                return reassembleCompleteMessage(msgId)
            }
            return null
        }

        if (packet.packetType == PacketType.END) {
            val msgMap = pendingMessageFragments[msgId]
            val totalExpected = expectedChunkCounts[msgId] ?: (packet.sequenceNumber - 1)
            if (msgMap != null && msgMap.size >= totalExpected) {
                return reassembleCompleteMessage(msgId)
            }
        }

        return null
    }

    /**
     * Checks if any fragments are missing and generates a Selective NACK request.
     */
    fun checkMissingFragments(messageId: String): SelectiveNackRequest? {
        val fragments = pendingMessageFragments[messageId] ?: return null
        val totalExpected = expectedChunkCounts[messageId] ?: return null

        val missingSeqs = mutableListOf<Int>()
        for (seq in 1..totalExpected) {
            if (!fragments.containsKey(seq)) {
                missingSeqs.add(seq)
            }
        }

        return if (missingSeqs.isNotEmpty()) {
            SelectiveNackRequest(messageId, missingSeqs)
        } else null
    }

    private fun reassembleCompleteMessage(messageId: String): ByteArray {
        val fragments = pendingMessageFragments.remove(messageId) ?: return ByteArray(0)
        expectedChunkCounts.remove(messageId)

        val sortedFragments = fragments.values.sortedBy { it.sequenceNumber }
        val totalBytes = sortedFragments.sumOf { it.payload.size }
        val result = ByteArray(totalBytes)

        var offset = 0
        for (f in sortedFragments) {
            System.arraycopy(f.payload, 0, result, offset, f.payload.size)
            offset += f.payload.size
        }
        return result
    }

    fun clear() {
        pendingMessageFragments.clear()
        expectedChunkCounts.clear()
    }
}
