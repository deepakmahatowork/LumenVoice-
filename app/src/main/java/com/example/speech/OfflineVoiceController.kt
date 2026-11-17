package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceControllerState(
    val isSpeechAvailable: Boolean = true,
    val isListening: Boolean = false,
    val rmsDb: Float = 0f,
    val partialTranscript: String = "",
    val isTtsReady: Boolean = false,
    val preferOfflineRecognition: Boolean = true,
    val statusMessage: String = "Ready for offline voice command",
    val errorMessage: String? = null
)

class OfflineVoiceController(
    context: Context,
    private val onSpeechResult: (List<String>) -> Unit
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var retriedWithoutOfflineFlag = false

    private val _state = MutableStateFlow(
        VoiceControllerState(
            isSpeechAvailable = SpeechRecognizer.isRecognitionAvailable(appContext)
        )
    )
    val state: StateFlow<VoiceControllerState> = _state.asStateFlow()

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val tts = textToSpeech
                    val result = tts?.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.getDefault())
                    }
                    tts?.setSpeechRate(1.02f)
                    _state.value = _state.value.copy(isTtsReady = true)
                } else {
                    _state.value = _state.value.copy(isTtsReady = false)
                }
            }
        } catch (_: Exception) {
            _state.value = _state.value.copy(isTtsReady = false)
        }
    }

    fun setPreferOfflineRecognition(preferOffline: Boolean) {
        _state.value = _state.value.copy(preferOfflineRecognition = preferOffline)
    }

    /**
     * Speaks a confirmation phrase using Android's native TextToSpeech.
     */
    fun speakConfirmation(text: String, enabled: Boolean = true) {
        if (!enabled || text.isBlank()) return
        val tts = textToSpeech ?: return
        if (_state.value.isTtsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lumen_brightness_confirm")
        }
    }

    /**
     * Starts listening for a spoken brightness command using Android's native SpeechRecognizer.
     */
    fun startListening() {
        mainHandler.post {
            if (_state.value.isListening) {
                stopListening()
                return@post
            }
            retriedWithoutOfflineFlag = false
            startListeningInternal(useOfflineExtra = _state.value.preferOfflineRecognition)
        }
    }

    private fun startListeningInternal(useOfflineExtra: Boolean) {
        try {
            textToSpeech?.stop()
            ensureSpeechRecognizer()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                if (useOfflineExtra) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }

            _state.value = _state.value.copy(
                isListening = true,
                partialTranscript = "",
                errorMessage = null,
                statusMessage = if (useOfflineExtra) {
                    "Listening (On-Device Offline Mode)…"
                } else {
                    "Listening for voice command…"
                }
            )
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isListening = false,
                rmsDb = 0f,
                errorMessage = "Speech recognizer unavailable on this device: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            _state.value = _state.value.copy(
                isListening = false,
                rmsDb = 0f,
                statusMessage = "Ready for offline voice command"
            )
        }
    }

    private fun ensureSpeechRecognizer() {
        if (speechRecognizer != null) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = _state.value.copy(
                        isListening = true,
                        errorMessage = null,
                        statusMessage = "Say a percentage (0–100), \"Brighter\", \"Dimmer\", \"Max\", or \"Low\""
                    )
                }

                override fun onBeginningOfSpeech() {
                    _state.value = _state.value.copy(
                        isListening = true,
                        statusMessage = "Capturing voice…"
                    )
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = rmsdB.coerceIn(0f, 12f)
                    _state.value = _state.value.copy(rmsDb = normalized)
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _state.value = _state.value.copy(
                        isListening = false,
                        rmsDb = 0f,
                        statusMessage = "Processing command locally…"
                    )
                }

                override fun onError(error: Int) {
                    // If the device lacks downloaded offline speech packs and errors with offline flag set,
                    // automatically retry once without EXTRA_PREFER_OFFLINE so recognition still succeeds.
                    if (_state.value.preferOfflineRecognition &&
                        !retriedWithoutOfflineFlag &&
                        (error == SpeechRecognizer.ERROR_CLIENT ||
                            error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                            error == ERROR_LANGUAGE_UNAVAILABLE_COMPAT ||
                            error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
                    ) {
                        retriedWithoutOfflineFlag = true
                        startListeningInternal(useOfflineExtra = false)
                        return
                    }

                    val friendlyError = mapSpeechError(error)
                    _state.value = _state.value.copy(
                        isListening = false,
                        rmsDb = 0f,
                        partialTranscript = "",
                        statusMessage = "Tap microphone to try again",
                        errorMessage = friendlyError
                    )
                }

                override fun onResults(results: Bundle?) {
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.filter { it.isNotBlank() }
                        .orEmpty()

                    _state.value = _state.value.copy(
                        isListening = false,
                        rmsDb = 0f,
                        partialTranscript = matches.firstOrNull().orEmpty(),
                        errorMessage = null
                    )

                    if (matches.isNotEmpty()) {
                        onSpeechResult(matches)
                    } else {
                        _state.value = _state.value.copy(
                            errorMessage = "No spoken words detected. Try saying \"43 percent\" or \"Brighter\"."
                        )
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _state.value = _state.value.copy(partialTranscript = partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun mapSpeechError(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone availability."
            SpeechRecognizer.ERROR_CLIENT -> "Speech client error. Tap microphone to retry."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Offline speech pack not installed for this locale in system Voice Input settings."
            SpeechRecognizer.ERROR_NO_MATCH ->
                "Didn't catch that phrase. Try \"43\", \"Brighter\", \"Dimmer\", \"Max\", or \"Low\"."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please wait a moment."
            SpeechRecognizer.ERROR_SERVER -> "Speech recognition service error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap the microphone and speak clearly."
            else -> "Voice recognition could not complete (code $errorCode)."
        }
    }

    fun release() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {
            }
            try {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        // SDK 31+ ERROR_LANGUAGE_UNAVAILABLE constant value
        private const val ERROR_LANGUAGE_UNAVAILABLE_COMPAT = 13
    }
}
