package com.example.myapplication.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptionManager {

    private val provider = "AndroidKeyStore"
    private val keyAlias = "iTANTRA_SESSION_KEY"
    private val transformation = "AES/GCM/NoPadding"
    private val gcmTagLength = 128

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(provider).apply { load(null) }
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, provider
                )
                val keySpec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).apply {
                    setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    setKeySize(256)
                }.build()

                keyGenerator.init(keySpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // Fallback for non-AndroidKeyStore environment (e.g. unit testing)
        }
    }

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(provider).apply { load(null) }
            keyStore.getKey(keyAlias, null) as? SecretKey ?: generateFallbackKey()
        } catch (e: Exception) {
            generateFallbackKey()
        }
    }

    private var fallbackKey: SecretKey? = null
    private fun generateFallbackKey(): SecretKey {
        return fallbackKey ?: KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().also { fallbackKey = it }
    }

    fun encrypt(plainBytes: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainBytes)
        return Pair(encrypted, iv)
    }

    fun decrypt(encryptedBytes: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        val spec = GCMParameterSpec(gcmTagLength, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(encryptedBytes)
    }

    fun getKeyAlgorithm(): String = "AES-256-GCM (Authenticated Encryption)"
    
    fun getEpochId(): Long = 1001L
}
