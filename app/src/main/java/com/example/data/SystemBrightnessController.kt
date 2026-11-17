package com.example.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.domain.VoiceBrightnessParser

data class SystemBrightnessSnapshot(
    val canWriteSettings: Boolean,
    val percentage: Int,
    val systemValue255: Int,
    val isManualMode: Boolean
)

data class BrightnessApplyResult(
    val appliedPercentage: Int,
    val appliedSystem255: Int,
    val wroteToSystemSettings: Boolean,
    val isManualMode: Boolean,
    val canWriteSettings: Boolean,
    val errorMessage: String? = null
)

class SystemBrightnessController(private val context: Context) {

    /**
     * Checks if the app has permission to modify system settings (Settings.System.canWrite).
     */
    fun canWriteSystemSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Creates an Intent to open Settings.ACTION_MANAGE_WRITE_SETTINGS for this package.
     */
    fun createManageWriteSettingsIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Reads the current system brightness (0–255) and brightness mode (Manual vs Adaptive).
     */
    fun readCurrentState(): SystemBrightnessSnapshot {
        val canWrite = canWriteSystemSettings()
        val resolver = context.contentResolver

        val rawBrightness = try {
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Settings.SettingNotFoundException) {
            128
        }.coerceIn(0, 255)

        val mode = try {
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
        } catch (_: Settings.SettingNotFoundException) {
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        }

        val percentage = VoiceBrightnessParser.systemBrightnessToPercentage(rawBrightness)
        val isManual = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL

        return SystemBrightnessSnapshot(
            canWriteSettings = canWrite,
            percentage = percentage,
            systemValue255 = rawBrightness,
            isManualMode = isManual
        )
    }

    /**
     * Applies a 0–100% brightness value:
     * 1. Converts percentage to Android's 0–255 integer scale.
     * 2. Automatically disables adaptive brightness (sets SCREEN_BRIGHTNESS_MODE_MANUAL)
     *    and writes SCREEN_BRIGHTNESS when Settings.System.canWrite(context) is true.
     * 3. Also applies window-level screenBrightness on the active Activity window for immediate visual feedback.
     */
    fun applyBrightnessPercentage(
        percentage: Int,
        activity: Activity? = null
    ): BrightnessApplyResult {
        val clampedPercent = percentage.coerceIn(0, 100)
        val raw255 = VoiceBrightnessParser.percentageToSystemBrightness(clampedPercent)
        val canWrite = canWriteSystemSettings()

        var wroteSystem = false
        var manualModeActive = false
        var errorMsg: String? = null

        if (canWrite) {
            try {
                val resolver = context.contentResolver
                // Automatically disable adaptive brightness
                Settings.System.putInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                // Write the 0-255 integer brightness value
                wroteSystem = Settings.System.putInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    raw255
                )
                manualModeActive = true
            } catch (e: SecurityException) {
                errorMsg = e.localizedMessage ?: "SecurityException writing system settings"
            } catch (e: Exception) {
                errorMsg = e.localizedMessage ?: "Failed to update system brightness"
            }
        }

        // Also update the current Activity window attribute so the display immediately reflects the brightness
        activity?.window?.let { window ->
            val layoutParams = window.attributes
            // Keep a minimum 0.01f on the window layout param when 0% so the user's screen doesn't turn pitch-black
            // while still writing exact 0 to Settings.System.SCREEN_BRIGHTNESS.
            layoutParams.screenBrightness = (raw255 / 255f).coerceIn(0.01f, 1.0f)
            window.attributes = layoutParams
        }

        return BrightnessApplyResult(
            appliedPercentage = clampedPercent,
            appliedSystem255 = raw255,
            wroteToSystemSettings = wroteSystem,
            isManualMode = if (canWrite) manualModeActive else readCurrentState().isManualMode,
            canWriteSettings = canWrite,
            errorMessage = errorMsg
        )
    }
}
