package com.example.domain

import kotlin.math.roundToInt

sealed class VoiceCommandAction {
    data class SetExact(val percentage: Int, val matchedRule: String) : VoiceCommandAction()
    data class AdjustRelative(val deltaPercentage: Int, val matchedRule: String) : VoiceCommandAction()
    data class Unrecognized(val rawTranscript: String) : VoiceCommandAction()
}

data class BrightnessCommandResolution(
    val action: VoiceCommandAction,
    val targetPercentage: Int,
    val targetSystem255: Int,
    val spokenConfirmation: String,
    val recognizedPhrase: String
)

object VoiceBrightnessParser {

    private val numberRegex = Regex("""\b(\d{1,3})\b""")

    private val singleWordNumbers = mapOf(
        "zero" to 0,
        "oh" to 0,
        "one" to 1,
        "two" to 2,
        "to" to -1, // handled contextually, never standalone "to"
        "three" to 3,
        "four" to 4,
        "for" to -1,
        "five" to 5,
        "six" to 6,
        "seven" to 7,
        "eight" to 8,
        "nine" to 9,
        "ten" to 10,
        "eleven" to 11,
        "twelve" to 12,
        "thirteen" to 13,
        "fourteen" to 14,
        "fifteen" to 15,
        "sixteen" to 16,
        "seventeen" to 17,
        "eighteen" to 18,
        "nineteen" to 19,
        "twenty" to 20,
        "thirty" to 30,
        "forty" to 40,
        "fourty" to 40,
        "fifty" to 50,
        "sixty" to 60,
        "seventy" to 70,
        "eighty" to 80,
        "ninety" to 90,
        "hundred" to 100
    )

    private val tensWords = mapOf(
        "twenty" to 20,
        "thirty" to 30,
        "forty" to 40,
        "fourty" to 40,
        "fifty" to 50,
        "sixty" to 60,
        "seventy" to 70,
        "eighty" to 80,
        "ninety" to 90
    )

    private val unitsWords = mapOf(
        "one" to 1,
        "two" to 2,
        "three" to 3,
        "four" to 4,
        "five" to 5,
        "six" to 6,
        "seven" to 7,
        "eight" to 8,
        "nine" to 9
    )

    private val increaseKeywords = listOf(
        "brighter",
        "increase",
        "more",
        "higher",
        "turn up",
        "raise",
        "lighter",
        "boost"
    )

    private val decreaseKeywords = listOf(
        "dimmer",
        "darker",
        "decrease",
        "less",
        "lower",
        "turn down",
        "reduce",
        "dim"
    )

    private val maxKeywords = listOf(
        "max",
        "maximum",
        "full"
    )

    private val minKeywords = listOf(
        "minimum",
        "min",
        "low",
        "lowest"
    )

    /**
     * Converts a 0–100 brightness percentage to Android's 0–255 SCREEN_BRIGHTNESS integer scale.
     */
    fun percentageToSystemBrightness(percentage: Int): Int {
        val clamped = percentage.coerceIn(0, 100)
        return ((clamped * 255f) / 100f).roundToInt().coerceIn(0, 255)
    }

    /**
     * Converts Android's 0–255 SCREEN_BRIGHTNESS integer scale to a 0–100 percentage.
     */
    fun systemBrightnessToPercentage(systemValue: Int): Int {
        val clamped = systemValue.coerceIn(0, 255)
        return ((clamped * 100f) / 255f).roundToInt().coerceIn(0, 100)
    }

    /**
     * Formats the brief spoken confirmation for TextToSpeech.
     */
    fun formatSpokenConfirmation(percentage: Int): String {
        val clamped = percentage.coerceIn(0, 100)
        return "Brightness set to $clamped percent"
    }

    /**
     * Evaluates a list of SpeechRecognizer candidate strings and resolves the first valid command.
     */
    fun resolveFromCandidates(
        candidates: List<String>,
        currentPercentage: Int
    ): BrightnessCommandResolution? {
        if (candidates.isEmpty()) return null
        for (candidate in candidates) {
            val action = parseSpokenPhrase(candidate)
            if (action !is VoiceCommandAction.Unrecognized) {
                return buildResolution(action, currentPercentage, candidate)
            }
        }
        val firstRaw = candidates.first().trim()
        val unrecognized = VoiceCommandAction.Unrecognized(firstRaw)
        return buildResolution(unrecognized, currentPercentage, firstRaw)
    }

    /**
     * Resolves a single spoken phrase against the current brightness percentage.
     */
    fun resolvePhrase(
        phrase: String,
        currentPercentage: Int
    ): BrightnessCommandResolution {
        val action = parseSpokenPhrase(phrase)
        return buildResolution(action, currentPercentage, phrase.trim())
    }

    private fun buildResolution(
        action: VoiceCommandAction,
        currentPercentage: Int,
        phrase: String
    ): BrightnessCommandResolution {
        val targetPercent = when (action) {
            is VoiceCommandAction.SetExact -> action.percentage.coerceIn(0, 100)
            is VoiceCommandAction.AdjustRelative -> (currentPercentage + action.deltaPercentage).coerceIn(0, 100)
            is VoiceCommandAction.Unrecognized -> currentPercentage.coerceIn(0, 100)
        }
        val target255 = percentageToSystemBrightness(targetPercent)
        val confirmation = when (action) {
            is VoiceCommandAction.Unrecognized ->
                "Could not recognize command. Try saying a number from 0 to 100, brighter, dimmer, max, or minimum."
            else -> formatSpokenConfirmation(targetPercent)
        }
        return BrightnessCommandResolution(
            action = action,
            targetPercentage = targetPercent,
            targetSystem255 = target255,
            spokenConfirmation = confirmation,
            recognizedPhrase = phrase
        )
    }

    /**
     * Parses a spoken phrase locally in Kotlin according to the specification:
     * 1. Any spoken number from 0 to 100 (e.g., "43", "set brightness to 43 percent") -> SetExact(number)
     * 2. "Max" or "full" -> SetExact(100); "minimum" or "low" -> SetExact(10)
     * 3. "brighter", "increase", or "more" -> AdjustRelative(+15)
     * 4. "dimmer", "darker", "decrease", or "less" -> AdjustRelative(-15)
     */
    fun parseSpokenPhrase(rawPhrase: String): VoiceCommandAction {
        val normalized = rawPhrase
            .lowercase()
            .replace("%", " percent ")
            .replace("-", " ")
            .replace(Regex("""[^\w\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (normalized.isEmpty()) {
            return VoiceCommandAction.Unrecognized(rawPhrase)
        }

        // 1. Check for numeric digits (0..100) or spoken English numbers (zero..one hundred)
        val extractedNumber = extractSpokenNumber0To100(normalized)
        if (extractedNumber != null) {
            return VoiceCommandAction.SetExact(
                percentage = extractedNumber,
                matchedRule = "Exact number ($extractedNumber%)"
            )
        }

        val words = normalized.split(" ")

        // 2. Check for "max" / "full" -> 100% and "minimum" / "low" -> 10%
        if (maxKeywords.any { keyword -> containsKeyword(words, normalized, keyword) }) {
            return VoiceCommandAction.SetExact(
                percentage = 100,
                matchedRule = "Preset Max / Full (100%)"
            )
        }

        if (minKeywords.any { keyword -> containsKeyword(words, normalized, keyword) }) {
            return VoiceCommandAction.SetExact(
                percentage = 10,
                matchedRule = "Preset Minimum / Low (10%)"
            )
        }

        // 3. Check for relative increase ("brighter", "increase", "more") -> +15%
        if (increaseKeywords.any { keyword -> containsKeyword(words, normalized, keyword) }) {
            return VoiceCommandAction.AdjustRelative(
                deltaPercentage = 15,
                matchedRule = "Increase (+15%)"
            )
        }

        // 4. Check for relative decrease ("dimmer", "darker", "decrease", "less") -> -15%
        if (decreaseKeywords.any { keyword -> containsKeyword(words, normalized, keyword) }) {
            return VoiceCommandAction.AdjustRelative(
                deltaPercentage = -15,
                matchedRule = "Decrease (-15%)"
            )
        }

        return VoiceCommandAction.Unrecognized(rawPhrase)
    }

    private fun containsKeyword(words: List<String>, fullNormalized: String, keyword: String): Boolean {
        return if (keyword.contains(" ")) {
            fullNormalized.contains(keyword)
        } else {
            words.contains(keyword)
        }
    }

    /**
     * Extracts a number between 0 and 100 from either digits ("43") or words ("forty three").
     */
    internal fun extractSpokenNumber0To100(normalized: String): Int? {
        // First check explicit digits
        val digitMatches = numberRegex.findAll(normalized).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        val validDigit = digitMatches.firstOrNull { it in 0..100 }
        if (validDigit != null) {
            return validDigit
        }

        // Next check spoken English words
        val tokens = normalized.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        // Check "one hundred" or "a hundred" or "hundred"
        for (i in tokens.indices) {
            if (tokens[i] == "hundred") {
                return 100
            }
            val tensVal = tensWords[tokens[i]]
            if (tensVal != null) {
                val nextUnit = if (i + 1 < tokens.size) unitsWords[tokens[i + 1]] else null
                return if (nextUnit != null) {
                    (tensVal + nextUnit).coerceIn(0, 100)
                } else {
                    tensVal
                }
            }
            val singleVal = singleWordNumbers[tokens[i]]
            if (singleVal != null && singleVal in 0..100) {
                return singleVal
            }
        }

        return null
    }
}
