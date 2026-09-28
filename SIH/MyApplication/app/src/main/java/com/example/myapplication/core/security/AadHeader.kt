package com.example.myapplication.core.security

import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class PayloadType(val code: Byte) {
    TEMPLATE(0x01),
    FREE_TEXT(0x02);

    companion object {
        fun fromCode(code: Byte): PayloadType = if (code == 0x01.toByte()) TEMPLATE else FREE_TEXT
    }
}

/**
 * 16-Byte AAD Binary Header Specification:
 * Magic(2B) | Ver(1B) | Type(1B) | Flags(1B: Bit0=ALERT) | Seq(4B) | Ts(6B) | Lang(1B)
 * Passed as Additional Authenticated Data (AAD) into AES-256-GCM.
 */
data class AadHeader(
    val magic: String = "iT",         // 2 Bytes
    val version: Byte = 0x01,         // 1 Byte
    val payloadType: PayloadType,    // 1 Byte (0x01=TEMPLATE, 0x02=FREE_TEXT)
    val isAlert: Boolean = false,     // 1 Byte (Flags Bit0)
    val sequenceNumber: Long,         // 4 Bytes (UINT32)
    val timestampMs: Long,            // 6 Bytes (UINT48)
    val languageCode: Byte = 0x01     // 1 Byte (0=En, 1=Hi, 2=Ta, 3=Te, 4=Mr, 5=Bn, 6=Gu, 7=Kn)
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(16)
        buffer.put(magic.toByteArray(Charsets.UTF_8).take(2).toByteArray())
        buffer.put(version)
        buffer.put(payloadType.code)
        buffer.put(if (isAlert) 0x01.toByte() else 0x00.toByte())
        buffer.putInt(sequenceNumber.toInt())

        // Write 6-byte timestamp
        val tsBytes = ByteBuffer.allocate(8).putLong(timestampMs).array()
        buffer.put(tsBytes, 2, 6)

        buffer.put(languageCode)
        return buffer.array()
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): AadHeader {
            val buffer = ByteBuffer.wrap(bytes)
            val magicStr = String(bytes, 0, 2, Charsets.UTF_8)
            val ver = bytes[2]
            val type = PayloadType.fromCode(bytes[3])
            val isAlert = (bytes[4].toInt() and 0x01) != 0
            val seq = buffer.getInt(5).toLong() and 0xFFFFFFFFL

            val tsBuffer = ByteBuffer.allocate(8)
            tsBuffer.put(0.toByte())
            tsBuffer.put(0.toByte())
            tsBuffer.put(bytes, 9, 6)
            tsBuffer.flip()
            val ts = tsBuffer.long

            val lang = bytes[15]

            return AadHeader(
                magic = magicStr,
                version = ver,
                payloadType = type,
                isAlert = isAlert,
                sequenceNumber = seq,
                timestampMs = ts,
                languageCode = lang
            )
        }
    }
}

/**
 * ECDH Session Key Pairing & Authenticated AES-256-GCM Encryption with AAD Header.
 */
class AadGcmCipher {

    private val gcmTagLength = 128
    private var localEcdhKeyPair: KeyPair? = null
    private var sharedSessionKey: SecretKey? = null
    private var ecdhShortCode: String = "iT-9842"

    init {
        generateEcdhKeys()
    }

    fun generateEcdhKeys(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("EC").apply {
            initialize(256, SecureRandom())
        }
        val pair = keyGen.generateKeyPair()
        localEcdhKeyPair = pair

        // Derive 6-character Short Code / QR Code from Public Key
        val pubBytes = pair.public.encoded
        val digest = MessageDigest.getInstance("SHA-256").digest(pubBytes)
        ecdhShortCode = digest.joinToString("") { "%02X".format(it) }.take(6)

        // Generate default 256-bit AES session key
        val defaultKeyBytes = digest.take(32).toByteArray()
        sharedSessionKey = SecretKeySpec(defaultKeyBytes, "AES")

        return pair
    }

    fun getEcdhShortCode(): String = ecdhShortCode

    fun getLocalPublicKeyBytes(): ByteArray {
        return localEcdhKeyPair?.public?.encoded ?: ByteArray(0)
    }

    fun deriveSharedSessionKey(peerPublicKeyBytes: ByteArray) {
        try {
            val keyFact = KeyFactory.getInstance("EC")
            val peerKey = keyFact.generatePublic(X509EncodedKeySpec(peerPublicKeyBytes))
            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(localEcdhKeyPair?.private)
            ka.doPhase(peerKey, true)
            val sharedSecret = ka.generateSecret()

            val sessionKeyBytes = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
            sharedSessionKey = SecretKeySpec(sessionKeyBytes, "AES")
        } catch (e: Exception) {
            // Fallback key
        }
    }

    /**
     * Derive 12-byte Nonce = SHA256(Seq || Direction)[:12]
     */
    private fun deriveNonce(sequenceNumber: Long, direction: String = "TX"): ByteArray {
        val input = "SEQ_${sequenceNumber}_DIR_${direction}".toByteArray(Charsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-256").digest(input)
        return hash.take(12).toByteArray()
    }

    fun encrypt(aadHeader: AadHeader, plaintext: ByteArray): ByteArray {
        val key = sharedSessionKey ?: SecretKeySpec(ByteArray(32), "AES")
        val nonce = deriveNonce(aadHeader.sequenceNumber, "TX")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(gcmTagLength, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        // Pass 16-Byte Header as Additional Authenticated Data (AAD)
        val headerBytes = aadHeader.toByteArray()
        cipher.updateAAD(headerBytes)

        val ciphertextWithTag = cipher.doFinal(plaintext)

        // Output Frame = [AAD Header: 16B] + [Ciphertext + Tag]
        val frame = ByteArray(16 + ciphertextWithTag.size)
        System.arraycopy(headerBytes, 0, frame, 0, 16)
        System.arraycopy(ciphertextWithTag, 0, frame, 16, ciphertextWithTag.size)

        return frame
    }

    fun decrypt(frame: ByteArray): Pair<AadHeader, ByteArray> {
        if (frame.size < 16) {
            throw IllegalArgumentException("Frame size too small (<16 bytes)")
        }

        // Extract 16-Byte AAD Header
        val headerBytes = frame.copyOfRange(0, 16)
        val aadHeader = AadHeader.fromByteArray(headerBytes)

        val ciphertextWithTag = frame.copyOfRange(16, frame.size)
        val key = sharedSessionKey ?: SecretKeySpec(ByteArray(32), "AES")
        val nonce = deriveNonce(aadHeader.sequenceNumber, "TX")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(gcmTagLength, nonce)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        // Verify AAD Header authentication tag
        cipher.updateAAD(headerBytes)

        val plaintext = cipher.doFinal(ciphertextWithTag)
        return Pair(aadHeader, plaintext)
    }
}
