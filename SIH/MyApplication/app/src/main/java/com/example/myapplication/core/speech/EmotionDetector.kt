package com.example.myapplication.core.speech

import com.example.myapplication.domain.model.EmotionLabel
import com.example.myapplication.domain.model.Priority
import com.example.myapplication.domain.model.VoiceProfile
import java.util.Locale

data class EmotionAnalysisResult(
    val emotion: EmotionLabel,
    val confidence: Float,
    val isDangerDetected: Boolean,
    val recommendedPriority: Priority,
    val escalationReason: String? = null,
    val emotionProbabilities: Map<EmotionLabel, Float> = mapOf(
        EmotionLabel.NEUTRAL to 0.85f,
        EmotionLabel.PANIC to 0.05f,
        EmotionLabel.DISTRESS to 0.05f,
        EmotionLabel.FEAR to 0.03f,
        EmotionLabel.HAPPY to 0.01f,
        EmotionLabel.ANGRY to 0.01f
    )
)

/**
 * Acoustic & Linguistic Acoustic Distress Emotion Classifier.
 * Analyzes audio pitch (F0), speech rate, and multi-lingual keywords (English + Indic)
 * to compute multi-class probability distributions for emergency distress states.
 */
class EmotionDetector {

    private val panicKeywords = listOf("panic", "terrified", "screaming", "can't breathe", "sinking", "trapped!", "bachao", "fase", "faas")
    private val distressKeywords = listOf("distress", "help", "trapped", "injured", "bleeding", "collapse", "danger", "madad", "madat", "emergency", "flood")
    private val fearKeywords = listOf("fear", "afraid", "scared", "trapped inside", "blackout", "darkness")
    private val happyKeywords = listOf("safe", "rescued", "clear", "good news", "thanks", "ok", "roger")
    private val angryKeywords = listOf("angry", "furious", "delayed", "useless")

    fun analyzeText(
        text: String,
        currentPriority: Priority,
        voiceProfile: VoiceProfile? = null
    ): EmotionAnalysisResult {
        val lowerText = text.lowercase(Locale.ROOT)

        // 1. Linguistic Keyword Score Computation
        var panicScore = if (panicKeywords.any { lowerText.contains(it) }) 0.75f else 0.05f
        var distressScore = if (distressKeywords.any { lowerText.contains(it) }) 0.70f else 0.05f
        val fearScore = if (fearKeywords.any { lowerText.contains(it) }) 0.65f else 0.03f
        val happyScore = if (happyKeywords.any { lowerText.contains(it) }) 0.80f else 0.01f
        val angryScore = if (angryKeywords.any { lowerText.contains(it) }) 0.70f else 0.01f
        var neutralScore = if (panicScore < 0.2f && distressScore < 0.2f && happyScore < 0.2f) 0.85f else 0.10f

        // 2. Acoustic Feature Score Boost (Pitch F0 & Speech Rate)
        if (voiceProfile != null) {
            // High Pitch (>210 Hz) or fast speech rate (>1.25x) indicates vocal stress/panic
            if (voiceProfile.pitchHz > 210f || voiceProfile.speechRate > 1.25f) {
                panicScore += 0.20f
                distressScore += 0.15f
            } else if (voiceProfile.pitchHz in 160f..210f) {
                distressScore += 0.15f
            } else {
                neutralScore += 0.10f
            }
        }

        // Normalize Scores to Probability Distribution
        val totalScore = (panicScore + distressScore + fearScore + happyScore + angryScore + neutralScore).coerceAtLeast(1.0f)
        val pPanic = (panicScore / totalScore).coerceIn(0.01f, 0.99f)
        val pDistress = (distressScore / totalScore).coerceIn(0.01f, 0.99f)
        val pFear = (fearScore / totalScore).coerceIn(0.01f, 0.99f)
        val pHappy = (happyScore / totalScore).coerceIn(0.01f, 0.99f)
        val pAngry = (angryScore / totalScore).coerceIn(0.01f, 0.99f)
        val pNeutral = (neutralScore / totalScore).coerceIn(0.01f, 0.99f)

        val probMap = mapOf(
            EmotionLabel.PANIC to pPanic,
            EmotionLabel.DISTRESS to pDistress,
            EmotionLabel.FEAR to pFear,
            EmotionLabel.HAPPY to pHappy,
            EmotionLabel.ANGRY to pAngry,
            EmotionLabel.NEUTRAL to pNeutral
        )

        val topEmotion = probMap.maxByOrNull { it.value }?.key ?: EmotionLabel.NEUTRAL
        val topConfidence = probMap[topEmotion] ?: 0.85f

        val isDanger = topEmotion == EmotionLabel.PANIC || topEmotion == EmotionLabel.DISTRESS || topEmotion == EmotionLabel.FEAR

        return EmotionAnalysisResult(
            emotion = topEmotion,
            confidence = topConfidence,
            isDangerDetected = isDanger,
            recommendedPriority = if (isDanger) Priority.CRITICAL else currentPriority,
            escalationReason = if (isDanger) "Acoustic/Linguistic Distress Model detected ${topEmotion.displayName} (${(topConfidence * 100).toInt()}% prob)" else null,
            emotionProbabilities = probMap
        )
    }
}
