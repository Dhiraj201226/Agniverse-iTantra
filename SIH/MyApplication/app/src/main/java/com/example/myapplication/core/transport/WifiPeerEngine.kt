package com.example.myapplication.core.transport

import android.content.Context
import android.net.wifi.WifiManager
import com.example.myapplication.domain.model.EmotionLabel
import com.example.myapplication.domain.model.Message
import com.example.myapplication.domain.model.MessageStatus
import com.example.myapplication.domain.model.Priority
import com.example.myapplication.core.security.MessageHasher
import com.example.myapplication.domain.model.VoiceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WifiPeerEngine(private val context: Context) {

    private val port = 8888
    private var socket: DatagramSocket? = null
    private var listenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var multicastLock: WifiManager.MulticastLock? = null

    fun startListening(localNodeId: String, onMessageReceived: (Message) -> Unit) {
        if (listenJob != null && listenJob?.isActive == true) return

        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("iTANTRA_MESH_LOCK")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            // Hotspot or non-Wi-Fi lock environment
        }

        listenJob = scope.launch {
            try {
                socket = DatagramSocket(port).apply {
                    broadcast = true
                    reuseAddress = true
                }
                val buffer = ByteArray(4096)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    val rawData = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    if (rawData.startsWith("{") && rawData.endsWith("}")) {
                        try {
                            val json = JSONObject(rawData)
                            val senderId = json.optString("senderId", "")

                            // Ignore self-broadcasts
                            if (senderId != localNodeId && senderId.isNotEmpty()) {
                                // Extract Voice Profile Mel-Spec metadata
                                val voiceProfile = if (json.has("voicePitchHz")) {
                                    val melBandsList = mutableListOf<Float>()
                                    val melJsonArray = json.optJSONArray("voiceMelBands")
                                    if (melJsonArray != null) {
                                        for (i in 0 until melJsonArray.length()) {
                                            melBandsList.add(melJsonArray.optDouble(i, 0.5).toFloat())
                                        }
                                    }
                                    VoiceProfile(
                                        speakerCallSign = json.optString("senderCallSign", "Remote-Node"),
                                        pitchHz = json.optDouble("voicePitchHz", 150.0).toFloat(),
                                        pitchRatio = json.optDouble("voicePitchRatio", 1.0).toFloat(),
                                        speechRate = json.optDouble("voiceSpeechRate", 1.0).toFloat(),
                                        spectralCentroidHz = json.optDouble("voiceCentroidHz", 2200.0).toFloat(),
                                        melEnergyBands = if (melBandsList.isNotEmpty()) melBandsList else listOf(0.15f, 0.45f, 0.85f, 0.65f, 0.40f, 0.25f, 0.15f, 0.08f)
                                    )
                                } else null

                                val textContent = json.optString("text", "")
                                val sourceHash = json.optString("sourceHash", MessageHasher.computeSha256(textContent))
                                val destinationHash = MessageHasher.computeSha256(textContent)
                                val isTampered = sourceHash.isNotBlank() && !sourceHash.equals(destinationHash, ignoreCase = true)

                                val origSize = json.optInt("origSize", textContent.toByteArray(Charsets.UTF_8).size)
                                val compSize = json.optInt("compSize", (origSize * 0.45).toInt())

                                val msg = Message(
                                    id = json.optString("id", "MSG-NET"),
                                    senderId = senderId,
                                    senderCallSign = json.optString("senderCallSign", "Remote-Node"),
                                    originalText = textContent,
                                    translatedText = if (json.has("translatedText") && !json.isNull("translatedText")) json.getString("translatedText") else null,
                                    priority = Priority.valueOf(json.optString("priority", "NORMAL")),
                                    emotion = EmotionLabel.valueOf(json.optString("emotion", "NEUTRAL")),
                                    isDangerEscalated = json.optBoolean("isDangerEscalated", false),
                                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                                    status = MessageStatus.DELIVERED,
                                    hopCount = json.optInt("hopCount", 1) + 1,
                                    voiceProfile = voiceProfile,
                                    translationAccuracy = json.optDouble("transAccuracy", 98.0).toFloat(),
                                    translationBleuScore = json.optDouble("transBleu", 0.95).toFloat(),
                                    translationQualityGrade = json.optString("transGrade", "EXACT (98%)"),
                                    originalByteSize = origSize,
                                    compressedByteSize = compSize,
                                    sourceHash = sourceHash,
                                    destinationHash = destinationHash,
                                    isTampered = isTampered
                                )
                                onMessageReceived(msg)
                            }
                        } catch (e: Exception) {
                            // Malformed packet
                        }
                    }
                }
            } catch (e: Exception) {
                // Socket closed or network error
            }
        }
    }

    private fun getSubnetBroadcastAddress(): InetAddress? {
        return try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcp = wifi?.dhcpInfo ?: return null
            val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
            val quads = ByteArray(4)
            for (k in 0..3) {
                quads[k] = (broadcast shr (k * 8) and 0xFF).toByte()
            }
            InetAddress.getByAddress(quads)
        } catch (e: Exception) {
            null
        }
    }

    fun broadcastMessage(msg: Message) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("id", msg.id)
                    put("senderId", msg.senderId)
                    put("senderCallSign", msg.senderCallSign)
                    put("text", msg.originalText)
                    put("translatedText", msg.translatedText)
                    put("priority", msg.priority.name)
                    put("emotion", msg.emotion.name)
                    put("isDangerEscalated", msg.isDangerEscalated)
                    put("timestamp", msg.timestamp)
                    put("hopCount", msg.hopCount)
                    put("transAccuracy", msg.translationAccuracy)
                    put("transBleu", msg.translationBleuScore)
                    put("transGrade", msg.translationQualityGrade)
                    put("origSize", msg.originalByteSize)
                    put("compSize", msg.compressedByteSize)
                    put("sourceHash", msg.sourceHash)

                    // Attach Mel-Spectrogram Acoustic Voice Profile parameters
                    msg.voiceProfile?.let { vp ->
                        put("voicePitchHz", vp.pitchHz)
                        put("voicePitchRatio", vp.pitchRatio)
                        put("voiceSpeechRate", vp.speechRate)
                        put("voiceCentroidHz", vp.spectralCentroidHz)
                        put("voiceMelBands", JSONArray(vp.melEnergyBands))
                    }
                }

                val bytes = json.toString().toByteArray(Charsets.UTF_8)
                val tempSocket = DatagramSocket().apply { broadcast = true }

                // 1. Send to global broadcast address
                val globalBroadcast = InetAddress.getByName("255.255.255.255")
                tempSocket.send(DatagramPacket(bytes, bytes.size, globalBroadcast, port))

                // 2. Send to subnet specific broadcast address (e.g. 192.168.43.255)
                val subnetBroadcast = getSubnetBroadcastAddress()
                if (subnetBroadcast != null && subnetBroadcast != globalBroadcast) {
                    tempSocket.send(DatagramPacket(bytes, bytes.size, subnetBroadcast, port))
                }

                tempSocket.close()
            } catch (e: Exception) {
                // Network unavailable or packet error
            }
        }
    }

    fun stop() {
        listenJob?.cancel()
        socket?.close()
        multicastLock?.let {
            if (it.isHeld) it.release()
        }
    }
}
