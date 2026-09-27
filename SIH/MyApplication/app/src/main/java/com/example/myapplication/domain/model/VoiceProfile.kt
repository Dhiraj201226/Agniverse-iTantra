package com.example.myapplication.domain.model

/**
 * Encapsulates Mel-Spectrogram acoustic features and vocal characteristics
 * extracted from a speaker's audio. Transmitted over low-bandwidth mesh
 * to synthesize speech on receiver devices in the sender's cloned voice profile.
 */
data class VoiceProfile(
    val speakerCallSign: String = "Responder-1",
    val pitchHz: Float = 150f,            // Fundamental Pitch F0 (80Hz - 300Hz)
    val pitchRatio: Float = 1.0f,         // Normalized Android TTS Pitch (0.5f - 2.0f)
    val speechRate: Float = 1.0f,         // Speaking Speed (0.7f - 1.5f)
    val spectralCentroidHz: Float = 2200f,// Acoustic Brightness / Timbre (800Hz - 4500Hz)
    val melEnergyBands: List<Float> = listOf(0.15f, 0.45f, 0.85f, 0.65f, 0.40f, 0.25f, 0.15f, 0.08f) // 8-band Mel Spectrogram
) {
    fun getPitchLabel(): String {
        return when {
            pitchHz < 120f -> "Deep Male ($pitchHz Hz)"
            pitchHz in 120f..175f -> "Mid Male / Deep Female ($pitchHz Hz)"
            pitchHz in 175f..240f -> "Female / High Pitch ($pitchHz Hz)"
            else -> "High Pitch ($pitchHz Hz)"
        }
    }

    fun getTimbreLabel(): String {
        return when {
            spectralCentroidHz < 1500f -> "Warm & Muffled"
            spectralCentroidHz in 1500f..2800f -> "Clear Natural"
            else -> "Bright & Crisp"
        }
    }
}
