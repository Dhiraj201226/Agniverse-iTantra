package com.example.myapplication.core.codec

import com.example.myapplication.domain.model.Packet
import com.example.myapplication.domain.model.PacketType
import com.example.myapplication.domain.model.Priority
import java.util.zip.CRC32

class Packetizer {

    fun computeCrc(bytes: ByteArray): Long {
        val crc = CRC32()
        crc.update(bytes)
        return crc.value
    }

    fun packetize(
        messageId: String,
        payload: ByteArray,
        priority: Priority,
        chunkSize: Int = 512
    ): List<Packet> {
        val packets = mutableListOf<Packet>()
        val totalChunks = if (payload.isEmpty()) 1 else (payload.size + chunkSize - 1) / chunkSize

        // 1. START Packet
        val startHeader = "START:$messageId:$totalChunks".toByteArray(Charsets.UTF_8)
        packets.add(
            Packet(
                packetType = PacketType.START,
                messageId = messageId,
                sequenceNumber = 0,
                totalPackets = totalChunks + 2,
                priority = priority,
                payload = startHeader,
                crc32 = computeCrc(startHeader)
            )
        )

        // 2. DATA Packets
        for (i in 0 until totalChunks) {
            val fromIndex = i * chunkSize
            val toIndex = (fromIndex + chunkSize).coerceAtMost(payload.size)
            val chunk = payload.copyOfRange(fromIndex, toIndex)
            packets.add(
                Packet(
                    packetType = PacketType.DATA,
                    messageId = messageId,
                    sequenceNumber = i + 1,
                    totalPackets = totalChunks + 2,
                    priority = priority,
                    payload = chunk,
                    crc32 = computeCrc(chunk)
                )
            )
        }

        // 3. END Packet
        val endFooter = "END:$messageId:$totalChunks".toByteArray(Charsets.UTF_8)
        packets.add(
            Packet(
                packetType = PacketType.END,
                messageId = messageId,
                sequenceNumber = totalChunks + 1,
                totalPackets = totalChunks + 2,
                priority = priority,
                payload = endFooter,
                crc32 = computeCrc(endFooter)
            )
        )

        return packets
    }

    fun verifyChecksum(packet: Packet): Boolean {
        return computeCrc(packet.payload) == packet.crc32
    }

    fun reassemble(packets: List<Packet>): ByteArray {
        val dataPackets = packets
            .filter { it.packetType == PacketType.DATA }
            .sortedBy { it.sequenceNumber }

        val totalSize = dataPackets.sumOf { it.payload.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (p in dataPackets) {
            System.arraycopy(p.payload, 0, result, offset, p.payload.size)
            offset += p.payload.size
        }
        return result
    }
}
