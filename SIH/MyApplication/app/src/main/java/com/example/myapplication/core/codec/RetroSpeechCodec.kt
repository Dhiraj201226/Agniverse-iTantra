package com.example.myapplication.core.codec

import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.Deflater

class RetroSpeechCodec {

    private val dictionaryMap = mapOf(
        "EMERGENCY" to "E1",
        "MEDICAL" to "M1",
        "TRAPPED" to "T1",
        "FLOOD" to "F1",
        "CYCLONE" to "C1",
        "HELP" to "H1",
        "REQUIRED" to "R1",
        "LOCATION" to "L1",
        "RESPONDER" to "RS1",
        "BLACKOUT" to "B1",
        "EVACUATE" to "EV1",
        "ASSISTANCE" to "A1",
        "SECTOR" to "S1",
        "BRIDGE" to "BR1",
        "IMMEDIATE" to "I1",
        "WATER" to "W1",
        "RISING" to "R2"
    )

    private val reverseDictionaryMap = dictionaryMap.entries.associate { (k, v) -> v to k }

    /**
     * Performs actual byte-level compression using dictionary substitution + Deflate/zlib compression.
     * Returns Triple(compressedBytes, originalByteSize, compressedByteSize).
     */
    fun encode(text: String): Triple<ByteArray, Int, Int> {
        val originalBytes = text.toByteArray(Charsets.UTF_8)
        val originalSize = originalBytes.size

        if (originalSize == 0) {
            return Triple(ByteArray(0), 0, 0)
        }

        var compressedText = text.uppercase(Locale.ROOT)
        for ((word, token) in dictionaryMap) {
            compressedText = compressedText.replace(word, "{$token}")
        }
        val dictBytes = compressedText.toByteArray(Charsets.UTF_8)

        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(dictBytes)
        deflater.finish()

        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(256)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        deflater.end()

        val deflatedBytes = outputStream.toByteArray()
        val compressedBytes = if (deflatedBytes.isNotEmpty() && deflatedBytes.size < originalSize) {
            deflatedBytes
        } else {
            dictBytes
        }

        val compressedSize = compressedBytes.size
        return Triple(compressedBytes, originalSize, compressedSize)
    }

    fun decode(compressedBytes: ByteArray): String {
        var text = String(compressedBytes, Charsets.UTF_8)
        for ((token, word) in reverseDictionaryMap) {
            text = text.replace("{$token}", word)
        }
        return text.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }
    }
}
