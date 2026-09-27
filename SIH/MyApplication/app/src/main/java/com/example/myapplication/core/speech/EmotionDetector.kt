package com.example.myapplication.core.speech

import com.example.myapplication.domain.model.EmotionLabel
import com.example.myapplication.domain.model.Priority
import java.util.Locale

data class EmotionAnalysisResult(
    val emotion: EmotionLabel,
    val confidence: Float,
    val isDangerDetected: Boolean,
    val recommendedPriority: Priority,
    val escalationReason: String? = null
)

class EmotionDetector {

    private val panicKeywords = listOf("panic", "terrified", "screaming", "can't breathe", "sinking")
    private val distressKeywords = listOf("distress", "help", "trapped", "injured", "bleeding", "collapse", "danger")
    private val fearKeywords = listOf("fear", "afraid", "scared", "trapped inside", "blackout")
    private val happyKeywords = listOf("safe", "rescued", "clear", "good news", "thanks")
    private val angryKeywords = listOf("angry", "furious", "delayed", "useless")

    fun analyzeText(text: String, currentPriority: Priority): EmotionAnalysisResult {
        val lowerText = text.lowercase(Locale.ROOT)

        val isPanic = panicKeywords.any { lowerText.contains(it) }
        val isDistress = distressKeywords.any { lowerText.contains(it) }
        val isFear = fearKeywords.any { lowerText.contains(it) }
        val isHappy = happyKeywords.any { lowerText.contains(it) }
        val isAngry = angryKeywords.any { lowerText.contains(it) }

        return when {
            isPanic -> EmotionAnalysisResult(
                emotion = EmotionLabel.PANIC,
                confidence = 0.94f,
                isDangerDetected = true,
                recommendedPriority = Priority.CRITICAL,
                escalationReason = "High-panic distress keywords detected in voice/transcript"
            )
            isDistress -> EmotionAnalysisResult(
                emotion = EmotionLabel.DISTRESS,
                confidence = 0.90f,
                isDangerDetected = true,
                recommendedPriority = Priority.CRITICAL,
                escalationReason = "Distress / hazard keywords detected in transcript"
            )
            isFear -> EmotionAnalysisResult(
                emotion = EmotionLabel.FEAR,
                confidence = 0.86f,
                isDangerDetected = true,
                recommendedPriority = Priority.CRITICAL,
                escalationReason = "Fear indicators detected in transcript"
            )
            isHappy -> EmotionAnalysisResult(
                emotion = EmotionLabel.HAPPY,
                confidence = 0.88f,
                isDangerDetected = false,
                recommendedPriority = Priority.LOW
            )
            isAngry -> EmotionAnalysisResult(
                emotion = EmotionLabel.ANGRY,
                confidence = 0.82f,
                isDangerDetected = false,
                recommendedPriority = Priority.NORMAL
            )
            else -> EmotionAnalysisResult(
                emotion = EmotionLabel.NEUTRAL,
                confidence = 0.95f,
                isDangerDetected = false,
                recommendedPriority = currentPriority
            )
        }
    }
}
