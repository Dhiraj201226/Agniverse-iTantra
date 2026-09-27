package com.example.myapplication.core.security

import java.security.MessageDigest

/**
 * Computes cryptographic SHA-256 hashes at source and destination
 * to verify end-to-end packet integrity and detect tampering over mesh.
 */
object MessageHasher {

    fun computeSha256(input: String): String {
        if (input.isEmpty()) return "0000000000000000"
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02X".format(it) }.take(16) // 16-char Hex Hash
        } catch (e: Exception) {
            "HASH-ERR-" + input.hashCode().toString(16).uppercase()
        }
    }

    fun computeFullSha256(input: String): String {
        if (input.isEmpty()) return "00000000000000000000000000000000"
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            "HASH-ERROR"
        }
    }
}
