package com.example.myapplication.core.transport

import com.example.myapplication.domain.model.Message
import com.example.myapplication.domain.model.Priority
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Store-and-Forward Bounded Priority Queue.
 * Retains messages when destination peer nodes are offline or disconnected,
 * and automatically flushes queued messages upon node reconnection over mesh.
 */
class StoreAndForwardQueue(
    private val maxCapacity: Int = 100,
    private val expiryMillis: Long = 2 * 3600 * 1000L // 2 Hours TTL
) {

    private val queue = CopyOnWriteArrayList<Message>()
    private val processedIds = ConcurrentHashMap.newKeySet<String>()

    fun enqueue(message: Message): Boolean {
        if (processedIds.contains(message.id)) {
            return false // Prevent duplicate store
        }

        pruneExpired()

        // Capacity Enforcement: Evict oldest LOW/NORMAL message if capacity reached
        if (queue.size >= maxCapacity) {
            val evictCandidate = queue.filter { it.priority != Priority.CRITICAL }
                .minByOrNull { it.timestamp }
            if (evictCandidate != null) {
                queue.remove(evictCandidate)
            } else if (message.priority != Priority.CRITICAL) {
                return false // Drop non-critical if queue is full of CRITICAL items
            }
        }

        queue.add(message)
        processedIds.add(message.id)

        // Sort by Priority (CRITICAL first) then timestamp
        queue.sortWith(compareByDescending<Message> { it.priority.level }.thenBy { it.timestamp })
        return true
    }

    fun flushMessagesForReconnectedNode(
        reconnectedNodeId: String,
        dispatch: (Message) -> Unit
    ): Int {
        pruneExpired()
        val now = System.currentTimeMillis()

        val matchingMessages = queue.filter { msg ->
            msg.recipientId == "BROADCAST" ||
            msg.recipientId.equals(reconnectedNodeId, ignoreCase = true) ||
            msg.targetLanguage != null
        }

        var flushedCount = 0
        for (msg in matchingMessages) {
            dispatch(msg)
            queue.remove(msg)
            flushedCount++
        }

        return flushedCount
    }

    fun pruneExpired() {
        val now = System.currentTimeMillis()
        queue.removeAll { (now - it.timestamp) > expiryMillis }
    }

    fun getPendingCount(): Int {
        pruneExpired()
        return queue.size
    }

    fun getQueuedMessages(): List<Message> {
        pruneExpired()
        return queue.toList()
    }

    fun clear() {
        queue.clear()
        processedIds.clear()
    }
}
