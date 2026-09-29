package com.example.myapplication.core.transport

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class VoipPttEngine {

    private val port = 8889
    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val headerMagic = "VOIP"
    private val headerSize = 36 // 4 bytes magic + 16 bytes sourceId + 16 bytes destId

    private var isRecording = false
    private var recordJob: Job? = null
    private var receiveJob: Job? = null

    private var audioTrack: AudioTrack? = null
    private var receiveSocket: DatagramSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var activeLocalNodeId: String = ""

    @Volatile
    var isFullDuplex: Boolean = false

    fun updateLocalNodeId(id: String) {
        activeLocalNodeId = id.trim()
    }

    fun startListening(localNodeId: String = "") {
        if (localNodeId.isNotBlank()) {
            activeLocalNodeId = localNodeId.trim()
        }
        if (receiveJob?.isActive == true) return

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfigOut)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        receiveJob = scope.launch {
            try {
                receiveSocket = DatagramSocket(port).apply {
                    broadcast = true
                    reuseAddress = true
                }
                val buffer = ByteArray(2048)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    receiveSocket?.receive(packet)

                    // RULE 1: Never play audio when local device is actively transmitting in Half-Duplex mode
                    if (isRecording && !isFullDuplex) {
                        continue
                    }

                    if (packet.length > headerSize) {
                        val magic = String(packet.data, 0, 4, Charsets.UTF_8)
                        if (magic == headerMagic) {
                            val sourceId = String(packet.data, 4, 16, Charsets.UTF_8).trim()
                            val destId = String(packet.data, 20, 16, Charsets.UTF_8).trim()

                            val currentId = activeLocalNodeId.ifBlank { localNodeId.trim() }

                            // RULE 2: Discard self-broadcast loopback audio (Sender must not hear own voice)
                            if (currentId.isNotBlank() && sourceId.equals(currentId, ignoreCase = true)) {
                                continue
                            }

                            // RULE 3: Source and Destination of radio voice should NOT be the same
                            if (sourceId.equals(destId, ignoreCase = true)) {
                                continue
                            }

                            // RULE 4: If targeted to a specific node, ignore if not meant for local node or broadcast
                            if (!destId.equals("BROADCAST", ignoreCase = true) && currentId.isNotBlank() && !destId.equals(currentId, ignoreCase = true)) {
                                continue
                            }

                            val pcmLength = packet.length - headerSize
                            audioTrack?.write(packet.data, headerSize, pcmLength)
                        } else {
                            // Legacy raw packet - discard if transmitting
                        }
                    }
                }
            } catch (e: Exception) {
                // Socket closed or error
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTransmitting(senderNodeId: String = "LOCAL_NODE", destinationNodeId: String = "BROADCAST") {
        // Enforce: Source and Destination of radio voice MUST NOT be the same
        val cleanSender = senderNodeId.trim()
        val cleanDest = destinationNodeId.trim()
        if (cleanSender.equals(cleanDest, ignoreCase = true) && !cleanDest.equals("BROADCAST", ignoreCase = true)) {
            // Refuse transmission if source and destination are identical
            return
        }

        if (isRecording) return
        isRecording = true

        recordJob = scope.launch {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    minBufferSize * 4
                )

                recorder.startRecording()
                val sendSocket = DatagramSocket().apply { broadcast = true }
                val globalBroadcast = InetAddress.getByName("255.255.255.255")
                val subnetBroadcast = getSubnetBroadcastAddress()

                val audioBufferSize = 1024
                val packetBuffer = ByteArray(headerSize + audioBufferSize)

                // Build 36-byte Header: Magic (4B) + SourceId (16B) + DestId (16B)
                val magicBytes = headerMagic.toByteArray(Charsets.UTF_8)
                val sourceBytes = cleanSender.padEnd(16, ' ').take(16).toByteArray(Charsets.UTF_8)
                val destBytes = cleanDest.padEnd(16, ' ').take(16).toByteArray(Charsets.UTF_8)

                System.arraycopy(magicBytes, 0, packetBuffer, 0, 4)
                System.arraycopy(sourceBytes, 0, packetBuffer, 4, 16)
                System.arraycopy(destBytes, 0, packetBuffer, 20, 16)

                val audioReadBuffer = ByteArray(audioBufferSize)

                while (isRecording && isActive) {
                    val bytesRead = recorder.read(audioReadBuffer, 0, audioReadBuffer.size)
                    if (bytesRead > 0) {
                        System.arraycopy(audioReadBuffer, 0, packetBuffer, headerSize, bytesRead)
                        sendSocket.send(DatagramPacket(packetBuffer, headerSize + bytesRead, globalBroadcast, port))
                        if (subnetBroadcast != null && subnetBroadcast != globalBroadcast) {
                            sendSocket.send(DatagramPacket(packetBuffer, headerSize + bytesRead, subnetBroadcast, port))
                        }
                    }
                }

                recorder.stop()
                recorder.release()
                sendSocket.close()
            } catch (e: Exception) {
                isRecording = false
            }
        }
    }

    fun stopTransmitting() {
        isRecording = false
        recordJob?.cancel()
    }

    fun stopAll() {
        stopTransmitting()
        receiveJob?.cancel()
        receiveSocket?.close()
        audioTrack?.stop()
        audioTrack?.release()
    }

    private fun getSubnetBroadcastAddress(): InetAddress? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) {
                        return broadcast
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
        return null
    }
}
