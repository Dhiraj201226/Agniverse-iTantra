package com.example.myapplication.domain.model

import java.util.UUID

data class NodeProfile(
    val nodeId: String = UUID.randomUUID().toString().take(8).uppercase(),
    val callSign: String = "Responder-1",
    val primaryLanguage: String = "English",
    val role: String = "Search & Rescue",
    val transportMode: String = "Wi-Fi / Mesh",
    val isConfigured: Boolean = false,
    val voiceProfile: VoiceProfile = VoiceProfile(speakerCallSign = "Responder-1")
)
