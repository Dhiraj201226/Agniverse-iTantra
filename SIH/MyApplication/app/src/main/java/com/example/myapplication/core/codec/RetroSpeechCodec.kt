package com.example.myapplication.core.codec

import java.util.Locale

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
        "EVACUATE" to "EV1"
    )

    private val reverseDictionaryMap = dictionaryMap.entries.associate { (k, v) -> v to k }

    fun encode(text: String): Pair<ByteArray, Float> {
        val originalBytes = text.toByteArray(Charsets.UTF_8)
        var compressedText = text.uppercase(Locale.ROOT)
        for ((word, token) in dictionaryMap) {
            compressedText = compressedText.replace(word, "{$token}")
        }
        val compressedBytes = compressedText.toByteArray(Charsets.UTF_8)
        val ratio = if (originalBytes.isNotEmpty()) {
            compressedBytes.size.toFloat() / originalBytes.size.toFloat()
        } else {
            1.0f
        }
        return Pair(compressedBytes, ratio.coerceIn(0.15f, 0.95f))
    }

    fun decode(compressedBytes: ByteArray): String {
        var text = String(compressedBytes, Charsets.UTF_8)
        for ((token, word) in reverseDictionaryMap) {
            text = text.replace("{$token}", word)
        }
        return text.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }
    }
}
