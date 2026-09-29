package com.example.myapplication.core.transport

import com.example.myapplication.domain.model.NeighborNode
import com.example.myapplication.domain.model.NodeProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Manages background dynamic node discovery over UDP mesh.
 * Periodically broadcasts ping heartbeats and maintains a live Neighbor Table.
 */
class NodeDiscoveryEngine {

    private val _neighbors = MutableStateFlow<List<NeighborNode>>(emptyList())
    val neighbors: StateFlow<List<NeighborNode>> = _neighbors.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var heartbeatJob: Job? = null

    fun startHeartbeatDiscovery(
        localProfile: () -> NodeProfile,
        localBattery: () -> Int,
        sendPingBroadcast: (String) -> Unit
    ) {
        if (heartbeatJob?.isActive == true) return

        heartbeatJob = scope.launch {
            while (isActive) {
                val profile = localProfile()
                val pingJson = JSONObject().apply {
                    put("type", "DISCOVERY_PING")
                    put("nodeId", profile.nodeId)
                    put("callSign", profile.callSign)
                    put("role", profile.role)
                    put("language", profile.primaryLanguage)
                    put("battery", localBattery())
                    put("timestamp", System.currentTimeMillis())
                }.toString()

                sendPingBroadcast(pingJson)

                // Clean up offline stale neighbors (> 45s no ping)
                pruneStaleNeighbors()

                delay(10000) // 10s heartbeat ping interval
            }
        }
    }

    fun handleDiscoveryPing(json: JSONObject, onRespondPong: (String) -> Unit): NeighborNode? {
        val nodeId = json.optString("nodeId", "")
        val callSign = json.optString("callSign", "Peer-Node")
        val role = json.optString("role", "Responder")
        val lang = json.optString("language", "English")
        val batt = json.optInt("battery", 85)

        if (nodeId.isBlank()) return null

        val discovered = NeighborNode(
            nodeId = nodeId,
            callSign = callSign,
            role = role,
            primaryLanguage = lang,
            batteryLevel = batt,
            lastSeenTimestamp = System.currentTimeMillis()
        )

        updateNeighbor(discovered)
        return discovered
    }

    fun updateNeighbor(discovered: NeighborNode) {
        _neighbors.update { current ->
            val list = current.filter { it.nodeId != discovered.nodeId }.toMutableList()
            list.add(discovered)
            list
        }
    }

    private fun pruneStaleNeighbors() {
        val now = System.currentTimeMillis()
        _neighbors.update { current ->
            current.filter { it.isOnline(now) }
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
    }
}
