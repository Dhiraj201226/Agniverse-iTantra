package com.example.myapplication.core.speech

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.Locale

enum class ModelLoadState {
    UNLOADED,
    LOADING,
    LOADED,
    ERROR
}

data class LanguageModelConfig(
    val languageName: String,
    val isoCode: String,
    val modelSizeBytes: Long,
    val isInstalled: Boolean = true,
    val loadState: ModelLoadState = ModelLoadState.UNLOADED,
    val errorMessage: String? = null
) {
    fun getFormattedSize(): String = "${(modelSizeBytes / (1024 * 1024))} MB"
}

/**
 * Manages offline STT/TTS language models for the required 10 languages:
 * Hindi, Gujarati, Marathi, Kannada, Malayalam, Tamil, Telugu, Odia, Bengali, English.
 * Only loads the selected active language model to prevent memory overflow on low-end devices.
 */
class LanguageModelManager {

    private val supportedLanguages = listOf(
        LanguageModelConfig("Hindi", "hi", 188 * 1024 * 1024L, isInstalled = true),
        LanguageModelConfig("Gujarati", "gu", 175 * 1024 * 1024L, isInstalled = true),
        LanguageModelConfig("Marathi", "mr", 182 * 1024 * 1024L, isInstalled = true),
        LanguageModelConfig("Kannada", "kn", 168 * 1024 * 1024L, isInstalled = true),
        LanguageModelConfig("Malayalam", "ml", 172 * 1024 * 1024L, isInstalled = true),
        LanguageModelConfig("Tamil", "ta", 188 * 1024 * 1024L, isInstalled = true),
        LanguageModelConfig("Telugu", "te", 170 * 1024 * 1024L, isInstalled = true),
        LanguageModelConfig("Odia", "or", 165 * 1024 * 1024L, isInstalled = true),
        LanguageModelConfig("Bengali", "bn", 180 * 1024 * 1024L, isInstalled = true),
        LanguageModelConfig("English", "en", 150 * 1024 * 1024L, isInstalled = true)
    )

    private val _modelsState = MutableStateFlow<List<LanguageModelConfig>>(supportedLanguages)
    val modelsState: StateFlow<List<LanguageModelConfig>> = _modelsState.asStateFlow()

    private val _activeLanguage = MutableStateFlow("Hindi")
    val activeLanguage: StateFlow<String> = _activeLanguage.asStateFlow()

    suspend fun loadLanguageModel(languageName: String): Boolean = withContext(Dispatchers.IO) {
        val target = supportedLanguages.find { it.languageName.equals(languageName, ignoreCase = true) }
            ?: return@withContext false

        // 1. Mark target model as LOADING
        _modelsState.update { list ->
            list.map {
                if (it.languageName.equals(languageName, ignoreCase = true)) {
                    it.copy(loadState = ModelLoadState.LOADING, errorMessage = null)
                } else if (it.loadState == ModelLoadState.LOADED) {
                    // Unload previous models to save RAM
                    it.copy(loadState = ModelLoadState.UNLOADED)
                } else {
                    it
                }
            }
        }

        try {
            delay(180) // Simulate ONNX model loading
            _activeLanguage.value = target.languageName

            _modelsState.update { list ->
                list.map {
                    if (it.languageName.equals(languageName, ignoreCase = true)) {
                        it.copy(loadState = ModelLoadState.LOADED)
                    } else {
                        it
                    }
                }
            }
            true
        } catch (e: Exception) {
            _modelsState.update { list ->
                list.map {
                    if (it.languageName.equals(languageName, ignoreCase = true)) {
                        it.copy(loadState = ModelLoadState.ERROR, errorMessage = e.localizedMessage)
                    } else {
                        it
                    }
                }
            }
            false
        }
    }

    suspend fun unloadLanguageModel(languageName: String) = withContext(Dispatchers.IO) {
        _modelsState.update { list ->
            list.map {
                if (it.languageName.equals(languageName, ignoreCase = true)) {
                    it.copy(loadState = ModelLoadState.UNLOADED)
                } else {
                    it
                }
            }
        }
    }

    fun getActiveModelConfig(): LanguageModelConfig? {
        val currentLang = _activeLanguage.value
        return _modelsState.value.find { it.languageName.equals(currentLang, ignoreCase = true) }
    }
}
