package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SystemBrightnessController
import com.example.domain.VoiceBrightnessParser
import com.example.domain.VoiceCommandAction
import com.example.speech.OfflineVoiceController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommandLogEntry(
    val id: Long,
    val timestamp: String,
    val sourceLabel: String,
    val inputDescription: String,
    val targetPercent: Int,
    val targetSystem255: Int
)

data class BrightnessUiState(
    val brightnessPercent: Int = 50,
    val systemBrightness255: Int = 128,
    val isManualMode: Boolean = true,
    val canWriteSettings: Boolean = false,
    val hasRecordAudioPermission: Boolean = false,
    val numericInputText: String = "50",
    val numericInputError: String? = null,
    val isListening: Boolean = false,
    val rmsDb: Float = 0f,
    val partialTranscript: String = "",
    val voiceStatusMessage: String = "Tap microphone to speak a brightness command",
    val voiceErrorMessage: String? = null,
    val lastRecognizedPhrase: String = "",
    val lastMatchedRule: String = "",
    val lastSpokenConfirmation: String = "",
    val isTtsEnabled: Boolean = true,
    val isTtsReady: Boolean = false,
    val preferOfflineSpeech: Boolean = true,
    val commandHistory: List<CommandLogEntry> = emptyList()
)

class BrightnessViewModel(application: Application) : AndroidViewModel(application) {

    private val brightnessController = SystemBrightnessController(application.applicationContext)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val _uiState = MutableStateFlow(BrightnessUiState())
    val uiState: StateFlow<BrightnessUiState> = _uiState.asStateFlow()

    private var currentActivityProvider: (() -> Activity?)? = null

    private val voiceController = OfflineVoiceController(
        context = application.applicationContext,
        onSpeechResult = { candidates ->
            handleSpeechCandidates(candidates)
        }
    )

    init {
        refreshSystemState()
        viewModelScope.launch {
            voiceController.state.collect { voiceState ->
                _uiState.update { current ->
                    current.copy(
                        isListening = voiceState.isListening,
                        rmsDb = voiceState.rmsDb,
                        partialTranscript = voiceState.partialTranscript,
                        isTtsReady = voiceState.isTtsReady,
                        preferOfflineSpeech = voiceState.preferOfflineRecognition,
                        voiceStatusMessage = voiceState.statusMessage,
                        voiceErrorMessage = voiceState.errorMessage
                    )
                }
            }
        }
    }

    fun bindActivityProvider(provider: () -> Activity?) {
        currentActivityProvider = provider
    }

    fun unbindActivityProvider() {
        currentActivityProvider = null
    }

    fun refreshSystemState() {
        val snapshot = brightnessController.readCurrentState()
        _uiState.update { current ->
            current.copy(
                canWriteSettings = snapshot.canWriteSettings,
                brightnessPercent = snapshot.percentage,
                systemBrightness255 = snapshot.systemValue255,
                isManualMode = snapshot.isManualMode,
                numericInputText = snapshot.percentage.toString()
            )
        }
    }

    fun updateRecordAudioPermissionStatus(granted: Boolean) {
        _uiState.update { it.copy(hasRecordAudioPermission = granted) }
    }

    fun buildManageWriteSettingsIntent(): Intent {
        return brightnessController.createManageWriteSettingsIntent()
    }

    /**
     * Updates brightness from the 0–100% slider.
     */
    fun onSliderValueChanged(newPercentageFloat: Float, speakOnFinish: Boolean = false) {
        val percent = newPercentageFloat.toInt().coerceIn(0, 100)
        applyBrightnessInternal(
            percentage = percent,
            sourceLabel = "Slider",
            inputDescription = "Slider set to $percent%",
            speakConfirmation = speakOnFinish,
            logEntry = speakOnFinish
        )
    }

    /**
     * Updates the text in the numeric input field.
     */
    fun onNumericInputChanged(newText: String) {
        val filtered = newText.filter { it.isDigit() }.take(3)
        val parsed = filtered.toIntOrNull()
        val error = when {
            filtered.isEmpty() -> null
            parsed == null || parsed !in 0..100 -> "Enter 0 to 100"
            else -> null
        }
        _uiState.update {
            it.copy(
                numericInputText = filtered,
                numericInputError = error
            )
        }
    }

    /**
     * Validates and applies the numeric text field value (0–100%).
     */
    fun applyNumericInput() {
        val text = _uiState.value.numericInputText.trim()
        val parsed = text.toIntOrNull()
        if (parsed == null || parsed !in 0..100) {
            _uiState.update {
                it.copy(numericInputError = "Enter a number from 0 to 100")
            }
            return
        }
        _uiState.update { it.copy(numericInputError = null) }
        applyBrightnessInternal(
            percentage = parsed,
            sourceLabel = "Numeric Input",
            inputDescription = "Entered $parsed%",
            speakConfirmation = true,
            logEntry = true
        )
    }

    /**
     * Applies one of the quick presets (25%, 43%, 75%, 100%).
     */
    fun applyPreset(presetPercentage: Int) {
        applyBrightnessInternal(
            percentage = presetPercentage.coerceIn(0, 100),
            sourceLabel = "Preset ${presetPercentage}%",
            inputDescription = "Quick preset ${presetPercentage}%",
            speakConfirmation = true,
            logEntry = true
        )
    }

    /**
     * Adjusts brightness relatively by delta (e.g., +15% or -15%).
     */
    fun adjustBrightnessBy(deltaPercentage: Int) {
        val target = (_uiState.value.brightnessPercent + deltaPercentage).coerceIn(0, 100)
        val sign = if (deltaPercentage >= 0) "+$deltaPercentage%" else "$deltaPercentage%"
        applyBrightnessInternal(
            percentage = target,
            sourceLabel = "Step $sign",
            inputDescription = "Adjusted $sign to $target%",
            speakConfirmation = true,
            logEntry = true
        )
    }

    /**
     * Starts or stops the native Android SpeechRecognizer.
     */
    fun toggleVoiceListening() {
        if (_uiState.value.isListening) {
            voiceController.stopListening()
        } else {
            voiceController.startListening()
        }
    }

    /**
     * Processes a spoken phrase directly through the local Kotlin parser and native TTS confirmation.
     */
    fun executeSpokenPhrase(phrase: String) {
        handleSpeechCandidates(listOf(phrase))
    }

    private fun handleSpeechCandidates(candidates: List<String>) {
        val currentPercent = _uiState.value.brightnessPercent
        val resolution = VoiceBrightnessParser.resolveFromCandidates(candidates, currentPercent) ?: return

        when (val action = resolution.action) {
            is VoiceCommandAction.SetExact -> {
                _uiState.update {
                    it.copy(
                        lastRecognizedPhrase = resolution.recognizedPhrase,
                        lastMatchedRule = action.matchedRule,
                        lastSpokenConfirmation = resolution.spokenConfirmation,
                        voiceErrorMessage = null
                    )
                }
                applyBrightnessInternal(
                    percentage = resolution.targetPercentage,
                    sourceLabel = "Voice Command",
                    inputDescription = "\"${resolution.recognizedPhrase}\" (${action.matchedRule})",
                    speakConfirmation = true,
                    logEntry = true
                )
            }

            is VoiceCommandAction.AdjustRelative -> {
                _uiState.update {
                    it.copy(
                        lastRecognizedPhrase = resolution.recognizedPhrase,
                        lastMatchedRule = action.matchedRule,
                        lastSpokenConfirmation = resolution.spokenConfirmation,
                        voiceErrorMessage = null
                    )
                }
                applyBrightnessInternal(
                    percentage = resolution.targetPercentage,
                    sourceLabel = "Voice Command",
                    inputDescription = "\"${resolution.recognizedPhrase}\" (${action.matchedRule})",
                    speakConfirmation = true,
                    logEntry = true
                )
            }

            is VoiceCommandAction.Unrecognized -> {
                val fallbackSpeech = "Command not recognized. Try saying 43 percent, brighter, dimmer, max, or low."
                _uiState.update {
                    it.copy(
                        lastRecognizedPhrase = resolution.recognizedPhrase,
                        lastMatchedRule = "Unrecognized phrase",
                        lastSpokenConfirmation = fallbackSpeech,
                        voiceErrorMessage = "Heard \"${resolution.recognizedPhrase}\" — try a number (0–100), \"brighter\", \"dimmer\", \"max\", or \"minimum\"."
                    )
                }
                voiceController.speakConfirmation(fallbackSpeech, enabled = _uiState.value.isTtsEnabled)
            }
        }
    }

    private fun applyBrightnessInternal(
        percentage: Int,
        sourceLabel: String,
        inputDescription: String,
        speakConfirmation: Boolean,
        logEntry: Boolean
    ) {
        val activity = currentActivityProvider?.invoke()
        val result = brightnessController.applyBrightnessPercentage(
            percentage = percentage,
            activity = activity
        )
        val confirmationText = VoiceBrightnessParser.formatSpokenConfirmation(result.appliedPercentage)

        _uiState.update { current ->
            val updatedHistory = if (logEntry) {
                val newEntry = CommandLogEntry(
                    id = System.currentTimeMillis(),
                    timestamp = timeFormatter.format(Date()),
                    sourceLabel = sourceLabel,
                    inputDescription = inputDescription,
                    targetPercent = result.appliedPercentage,
                    targetSystem255 = result.appliedSystem255
                )
                (listOf(newEntry) + current.commandHistory).take(6)
            } else {
                current.commandHistory
            }

            current.copy(
                brightnessPercent = result.appliedPercentage,
                systemBrightness255 = result.appliedSystem255,
                isManualMode = result.isManualMode,
                canWriteSettings = result.canWriteSettings,
                numericInputText = result.appliedPercentage.toString(),
                numericInputError = null,
                lastSpokenConfirmation = if (speakConfirmation) confirmationText else current.lastSpokenConfirmation,
                commandHistory = updatedHistory
            )
        }

        if (speakConfirmation) {
            voiceController.speakConfirmation(
                text = confirmationText,
                enabled = _uiState.value.isTtsEnabled
            )
        }
    }

    fun toggleTtsEnabled() {
        _uiState.update { current ->
            val next = !current.isTtsEnabled
            current.copy(isTtsEnabled = next)
        }
    }

    fun togglePreferOfflineSpeech() {
        val next = !_uiState.value.preferOfflineSpeech
        voiceController.setPreferOfflineRecognition(next)
    }

    override fun onCleared() {
        super.onCleared()
        voiceController.release()
    }
}
