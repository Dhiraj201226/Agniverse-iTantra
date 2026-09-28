package com.example.myapplication.core.codec

import java.util.Locale
import kotlin.math.max

data class EmergencyTemplate(
    val id: Int,                     // 2-byte template ID (1..12)
    val englishText: String,
    val hindiText: String,
    val tamilText: String,
    val marathiText: String,
    val audioWavAsset: String,       // Cached WAV pre-rendered in assets
    val isAlert: Boolean = false
)

sealed class FuzzyMatchResult {
    data class MatchHit(val template: EmergencyTemplate, val similarityScore: Float) : FuzzyMatchResult()
    data class MatchMiss(val reason: String) : FuzzyMatchResult()
}

/**
 * M1 Codebook Engine:
 * Defines 12 pre-rendered emergency templates with strict fuzzy-matching logic.
 * Partial matches (e.g. "Send help to north gate" vs "Send help") FAIL match
 * and send as FREE_TEXT to preserve full critical meaning.
 */
class CodebookEngine {

    val templates = listOf(
        EmergencyTemplate(1, "Send immediate medical assistance", "तुरंत चिकित्सा सहायता भेजें", "உடனடி மருத்துவ உதவி அனுப்பவும்", "तातडीने वैद्यकीय मदत पाठवा", "wav_t01_medical.wav"),
        EmergencyTemplate(2, "TRAPPED! Flash flood rising near sector bridge", "फंसे हुए हैं! सेक्टर ब्रिज के पास बाढ़ बढ़ रही है", "சிக்கிக் கொண்டோம்! பாலத்திற்கு அருகில் வெள்ளம் உயர்கிறது", "अडकलो आहोत! पुलाजवळ महापूर वाढतोय", "wav_t02_trapped_flood.wav", isAlert = true),
        EmergencyTemplate(3, "Fire outbreak! Need immediate evacuation", "आग लग गई है! तुरंत खाली करने की जरूरत है", "தீ விபத்து! உடனடியாக வெளியேற்ற வேண்டும்", "आग लागली आहे! त्वरित बाहेर पडा", "wav_t03_fire.wav", isAlert = true),
        EmergencyTemplate(4, "Power blackout! Comms down in sector", "बिजली गुल! संचार बंद", "மின்சாரம் இல்லை! தொடர்பு முடங்கியது", "वीज नाही! संवाद बंद", "wav_t04_power_blackout.wav"),
        EmergencyTemplate(5, "Road blocked! Need heavy machinery clearance", "रास्ता बंद! भारी मशीनरी सफाई की जरूरत", "சாலை அடைப்பு! கனரக இயந்திரம் தேவை", "रस्ता बंद! अवजड यंत्रसामग्रीची गरज", "wav_t05_road_blocked.wav"),
        EmergencyTemplate(6, "Search and rescue team deployed to sector", "खोज और बचाव दल सेक्टर में तैनात", "தேடுதல் மற்றும் மீட்புக் குழு அனுப்பப்பட்டது", "शोध आणि बचाव पथक तैनात", "wav_t06_rescue_team.wav"),
        EmergencyTemplate(7, "Requesting food and clean water ration supply", "भोजन और स्वच्छ पानी के राशन की मांग", "உணவு மற்றும் நன்னீர் விநியோகம் தேவை", "अन्न आणि स्वच्छ पाण्याची मागणी", "wav_t07_food_water.wav"),
        EmergencyTemplate(8, "Casualties reported at sector bridge location", "सेक्टर ब्रिज स्थान पर हताहतों की सूचना", "பாலம் பகுதியில் உயிரிழப்புகள் பதிவாகியுள்ளன", "पुलावर जखमींची माहिती", "wav_t08_casualties.wav", isAlert = true),
        EmergencyTemplate(9, "Base station online on Wi-Fi mesh network", "बेस स्टेशन वाई-फाई मेश नेटवर्क पर ऑनलाइन", "பேஸ் ஸ்டேஷன் நெட்வொர்க்கில் ஆன்லைனில் உள்ளது", "बेस स्टेशन मेश नेटवर्कवर ऑनलाइन", "wav_t09_base_station.wav"),
        EmergencyTemplate(10, "All personnel safe and accounted for", "सभी कर्मी सुरक्षित और जवाबदेह हैं", "அனைத்து ஊழியர்களும் பாதுகாப்பாக உள்ளனர்", "सर्व कर्मचारी सुरक्षित आहेत", "wav_t10_personnel_safe.wav"),
        EmergencyTemplate(11, "Urgent blood and triage supplies needed", "तत्काल रक्त और उपचार सामग्री की आवश्यकता", "உடனடி இரத்தம் மற்றும் மருத்துவ பொருட்கள் தேவை", "तातडीने रक्त आणि वैद्यकीय पुरवठा हवा", "wav_t11_blood_supplies.wav", isAlert = true),
        EmergencyTemplate(12, "ALERT SOS EMERGENCY DISTRESS DISPATCH", "अलर्ट एसओएस इमरजेंसी संकट प्रेषण", "எச்சரிக்கை அவசர உதவி அனுப்புதல்", "अलर्ट एसओएस आपत्कालीन मदत प्रेषण", "wav_t12_sos_alert.wav", isAlert = true)
    )

    fun getTemplateById(id: Int): EmergencyTemplate? {
        return templates.find { it.id == id }
    }

    /**
     * Performs strict fuzzy-matching against emergency codebook templates.
     * Threshold: >90% similarity AND string length ratio >0.85.
     * Partial matches FAIL to preserve full text meaning.
     */
    fun matchText(inputText: String, activeLanguage: String = "English"): FuzzyMatchResult {
        val cleanInput = inputText.lowercase(Locale.ROOT).trim()
        if (cleanInput.length < 3) return FuzzyMatchResult.MatchMiss("Input too short")

        var bestMatch: EmergencyTemplate? = null
        var highestSimilarity = 0.0f

        for (template in templates) {
            val targetText = when (activeLanguage.lowercase(Locale.ROOT)) {
                "hindi" -> template.hindiText
                "tamil" -> template.tamilText
                "marathi" -> template.marathiText
                else -> template.englishText
            }.lowercase(Locale.ROOT).trim()

            val sim = calculateLevenshteinSimilarity(cleanInput, targetText)
            val lenRatio = cleanInput.length.toFloat() / max(1, targetText.length).toFloat()

            // CRITICAL: Fail partial matches if length ratio < 0.85
            if (sim > 0.90f && lenRatio >= 0.85f && sim > highestSimilarity) {
                highestSimilarity = sim
                bestMatch = template
            }
        }

        return if (bestMatch != null) {
            FuzzyMatchResult.MatchHit(bestMatch, highestSimilarity)
        } else {
            FuzzyMatchResult.MatchMiss("No match >90% similarity or partial match failed")
        }
    }

    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Float {
        val distance = levenshteinDistance(s1, s2)
        val maxLen = max(s1.length, s2.length)
        return if (maxLen == 0) 1.0f else (1.0f - (distance.toFloat() / maxLen.toFloat()))
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLen = lhs.length
        val rhsLen = rhs.length

        var cost = IntArray(rhsLen + 1) { it }
        var newCost = IntArray(rhsLen + 1)

        for (i in 1..lhsLen) {
            newCost[0] = i
            for (j in 1..rhsLen) {
                val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[rhsLen]
    }
}
