package com.example

import com.example.domain.VoiceBrightnessParser
import com.example.domain.VoiceCommandAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `parse exact numeric and spoken number phrases`() {
        val exact43 = VoiceBrightnessParser.resolvePhrase("set brightness to 43 percent", currentPercentage = 50)
        assertTrue(exact43.action is VoiceCommandAction.SetExact)
        assertEquals(43, exact43.targetPercentage)
        assertEquals(110, exact43.targetSystem255)
        assertEquals("Brightness set to 43 percent", exact43.spokenConfirmation)

        val bareNumber = VoiceBrightnessParser.resolvePhrase("43", currentPercentage = 80)
        assertEquals(43, bareNumber.targetPercentage)

        val spokenWords = VoiceBrightnessParser.resolvePhrase("forty three percent", currentPercentage = 20)
        assertEquals(43, spokenWords.targetPercentage)
    }

    @Test
    fun `parse relative increase phrases by 15 percent`() {
        for (phrase in listOf("brighter", "increase", "more", "make it brighter")) {
            val result = VoiceBrightnessParser.resolvePhrase(phrase, currentPercentage = 40)
            assertEquals("Failed for phrase: $phrase", 55, result.targetPercentage)
            assertEquals("Brightness set to 55 percent", result.spokenConfirmation)
        }
    }

    @Test
    fun `parse relative decrease phrases by 15 percent`() {
        for (phrase in listOf("dimmer", "darker", "decrease", "less")) {
            val result = VoiceBrightnessParser.resolvePhrase(phrase, currentPercentage = 50)
            assertEquals("Failed for phrase: $phrase", 35, result.targetPercentage)
            assertEquals("Brightness set to 35 percent", result.spokenConfirmation)
        }
    }

    @Test
    fun `parse max full and minimum low phrases`() {
        for (phrase in listOf("max", "full", "set to max")) {
            val result = VoiceBrightnessParser.resolvePhrase(phrase, currentPercentage = 30)
            assertEquals(100, result.targetPercentage)
            assertEquals(255, result.targetSystem255)
        }

        for (phrase in listOf("minimum", "low")) {
            val result = VoiceBrightnessParser.resolvePhrase(phrase, currentPercentage = 75)
            assertEquals(10, result.targetPercentage)
            assertEquals(26, result.targetSystem255)
        }
    }

    @Test
    fun `percentage to 0-255 scale conversion bounds`() {
        assertEquals(0, VoiceBrightnessParser.percentageToSystemBrightness(0))
        assertEquals(64, VoiceBrightnessParser.percentageToSystemBrightness(25))
        assertEquals(110, VoiceBrightnessParser.percentageToSystemBrightness(43))
        assertEquals(191, VoiceBrightnessParser.percentageToSystemBrightness(75))
        assertEquals(255, VoiceBrightnessParser.percentageToSystemBrightness(100))
    }
}
