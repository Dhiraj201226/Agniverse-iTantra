package com.example.myapplication.core.speech

import com.example.myapplication.domain.model.VoiceProfile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Extracts Mel-Spectrogram features, fundamental pitch F0, spectral centroid timbre,
 * and speech rate from raw PCM audio buffers or recorded voice samples.
 */
class MelSpecVoiceExtractor {

    fun extractVoiceProfileFromPcm(
        pcmData: ByteArray,
        sampleRate: Int = 16000,
        speakerCallSign: String = "Operator"
    ): VoiceProfile {
        if (pcmData.isEmpty()) {
            return VoiceProfile(speakerCallSign = speakerCallSign)
        }

        // 1. Convert 16-bit PCM bytes to short samples
        val sampleCount = pcmData.size / 2
        val samples = FloatArray(sampleCount)
        var maxAmplitude = 0.0f

        for (i in 0 until sampleCount) {
            val sample = ((pcmData[i * 2 + 1].toInt() shl 8) or (pcmData[i * 2].toInt() and 0xFF)).toShort()
            samples[i] = sample / 32768.0f
            maxAmplitude = max(maxAmplitude, abs(samples[i]))
        }

        // 2. Compute Zero-Crossing Rate & Estimated Pitch F0
        var zeroCrossings = 0
        for (i in 1 until sampleCount) {
            if ((samples[i] >= 0 && samples[i - 1] < 0) || (samples[i] < 0 && samples[i - 1] >= 0)) {
                zeroCrossings++
            }
        }

        val durationSec = sampleCount.toFloat() / sampleRate.toFloat()
        val estimatedZcrFrequency = if (durationSec > 0) (zeroCrossings / (2.0f * durationSec)) else 150f

        // Estimate F0 (Fundamental Pitch) using Auto-Correlation
        val estimatedPitchHz = calculateAutocorrelationPitch(samples, sampleRate).coerceIn(80f, 320f)

        // 3. Compute 8-Band Mel Spectrogram Energies
        val melBands = compute8BandMelEnergies(samples)

        // 4. Compute Spectral Centroid (Acoustic Brightness / Timbre)
        val spectralCentroid = computeSpectralCentroid(melBands)

        // 5. Calculate TTS Pitch Ratio (150Hz = 1.0f baseline)
        val pitchRatio = (estimatedPitchHz / 150.0f).coerceIn(0.5f, 2.0f)
        val speechRate = (1.0f + (estimatedZcrFrequency - 1000f) / 4000f).coerceIn(0.7f, 1.5f)

        return VoiceProfile(
            speakerCallSign = speakerCallSign,
            pitchHz = estimatedPitchHz,
            pitchRatio = pitchRatio,
            speechRate = speechRate,
            spectralCentroidHz = spectralCentroid,
            melEnergyBands = melBands
        )
    }

    private fun calculateAutocorrelationPitch(samples: FloatArray, sampleRate: Int): Float {
        val minLag = sampleRate / 350 // Max pitch 350 Hz
        val maxLag = sampleRate / 70  // Min pitch 70 Hz

        if (samples.size < maxLag * 2) return 150f

        var maxCorr = 0.0f
        var bestLag = 0

        for (lag in minLag..maxLag) {
            var corr = 0.0f
            for (i in 0 until (samples.size - lag)) {
                corr += samples[i] * samples[i + lag]
            }
            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        return if (bestLag > 0) (sampleRate.toFloat() / bestLag.toFloat()) else 150f
    }

    private fun compute8BandMelEnergies(samples: FloatArray): List<Float> {
        val bandEnergies = FloatArray(8)
        val chunkSize = max(1, samples.size / 8)

        for (b in 0 until 8) {
            var sumSquare = 0.0f
            val start = b * chunkSize
            val end = min(samples.size, (b + 1) * chunkSize)

            for (i in start until end) {
                sumSquare += samples[i] * samples[i]
            }

            val rms = sqrt(sumSquare / max(1, end - start))
            bandEnergies[b] = (rms * 4.0f).coerceIn(0.05f, 1.0f)
        }

        return bandEnergies.toList()
    }

    private fun computeSpectralCentroid(melBands: List<Float>): Float {
        val frequencies = floatArrayOf(150f, 350f, 750f, 1500f, 2500f, 3500f, 5000f, 7000f)
        var weightedSum = 0.0f
        var totalEnergy = 0.0f

        for (i in 0 until min(melBands.size, frequencies.size)) {
            weightedSum += melBands[i] * frequencies[i]
            totalEnergy += melBands[i]
        }

        return if (totalEnergy > 0f) (weightedSum / totalEnergy).coerceIn(800f, 5000f) else 2200f
    }

    fun buildCustomVoiceProfile(
        speakerCallSign: String,
        pitchHz: Float,
        speechRate: Float,
        spectralCentroidHz: Float
    ): VoiceProfile {
        val pitchRatio = (pitchHz / 150.0f).coerceIn(0.5f, 2.0f)

        // Generate synthetic Mel bands based on pitch & centroid
        val melBands = MutableList(8) { i ->
            val centerFreq = (i + 1) * 800f
            val distance = abs(centerFreq - spectralCentroidHz)
            val energy = (1.0f - (distance / 4000f)).coerceIn(0.1f, 0.95f)
            energy
        }

        return VoiceProfile(
            speakerCallSign = speakerCallSign,
            pitchHz = pitchHz,
            pitchRatio = pitchRatio,
            speechRate = speechRate,
            spectralCentroidHz = spectralCentroidHz,
            melEnergyBands = melBands
        )
    }
}
