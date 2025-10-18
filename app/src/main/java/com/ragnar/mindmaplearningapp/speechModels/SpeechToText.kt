package com.ragnar.mindmaplearningapp.speechModels

import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechToText: ViewModel() {
    private val TAG: String = "SpeechToText"
    private val handler = Handler(Looper.getMainLooper())

    // Permission request code
    companion object {
        const val RECORD_AUDIO_PERMISSION_REQUEST = 100
    }


    data class STTState(
        val isInitialized: Boolean = false,
        val isSpeaking: Boolean = false,
        val selectedLanguage: String = "en-IN",
        val statusMessage: String = "",
        var resultText: String = "",
        val hasPermission: Boolean = false
    )

    private val _state = MutableStateFlow(STTState())
    val state: StateFlow<STTState> = _state.asStateFlow()


    // STT Component
    private var speechRecognizer : SpeechRecognizer? = null
    private var context: Context? = null


    fun initialize(context: Context) {
        if (_state.value.isInitialized) return

        this.context = context

        Log.d(TAG, "Initializing Speech to Text Engine....")
        val hasPermission = checkAudioPermission()
        _state.value = _state.value.copy(hasPermission = hasPermission)

        if (hasPermission) {
            initializeSpeechRecognizer()
        }

    }
    private fun checkAudioPermission(): Boolean {
        return context?.let { ctx ->
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } ?: false
    }

    private fun initializeSpeechRecognizer() {
        context?.let { ctx ->
            if (SpeechRecognizer.isRecognitionAvailable(ctx)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
                speechRecognizer?.setRecognitionListener(speechRecognitionListener)
                _state.value = _state.value.copy(
                    isInitialized = true,
                    statusMessage = "Speech recognizer initialized"
                )
            } else {
                _state.value = _state.value.copy(
                    statusMessage = "Speech recognition not available on this device"
                )
            }
        }
    }

    fun startListening() {
        if (!_state.value.hasPermission){
            _state.value = _state.value.copy(
                statusMessage = "Audio permission required"
            )
            return
        }

        if (!_state.value.isInitialized) {
            _state.value = _state.value.copy(
                statusMessage = "Speech recognizer not initialized"
            )
            return
        }
        if (_state.value.isSpeaking){
            stopListening()
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, _state.value.selectedLanguage)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, _state.value.selectedLanguage)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            // Clear previous result
            _state.value = _state.value.copy(resultText = "")

            speechRecognizer?.startListening(intent)
            _state.value = _state.value.copy(
                isSpeaking = true,
                statusMessage = "Listening...."
            )
            Log.d(TAG, "Listening..........")

        } catch (e: Exception){
            _state.value = _state.value.copy(
                statusMessage = "Error Starting speech recognizer....."
            )
            Log.e(TAG, "Error Starting speech recognizer", e)
        }
    }



    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = _state.value.copy(
            isSpeaking = false,
            statusMessage = "Listening stopped"
        )
        Log.d(TAG, "Stopped Listening...")
    }

    fun setLanguage(language: String) {
        _state.value = _state.value.copy(
            selectedLanguage = language
        )
        Log.d(TAG, "Selected language : $language")
    }

    private val speechRecognitionListener = object : RecognitionListener {
        override fun onBeginningOfSpeech() {
            _state.value = _state.value.copy(
                isSpeaking = true,
                statusMessage = "Speech detected..."
            )
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(
                isSpeaking = false,
                statusMessage = "Processing speech..."
            )
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error"
            }
            _state.value = _state.value.copy(
                isSpeaking = false,
                statusMessage = errorMessage
            )
            Log.e(TAG, errorMessage)
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }

        override fun onEvent(eventType: Int, params: Bundle?) { }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _state.value = _state.value.copy(
                    resultText = matches[0]
                )
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(
                isSpeaking = true,
                statusMessage = "Ready for speech...."
            )
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _state.value = _state.value.copy(
                    isSpeaking = false,
                    statusMessage = "Speech recognized successfully!",
                    resultText = matches[0]
                )
                Log.d(TAG, matches[0])
            }
        }

        override fun onRmsChanged(rmsdB: Float) { }

    }

    fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Audio permission required", Toast.LENGTH_LONG).show()
                }
                _state.value = _state.value.copy(
                    hasPermission = false,
                    statusMessage = "Audio permission denied"
                )
            }
        }
    }

    private fun onPermissionGranted() {
        _state.value = _state.value.copy(hasPermission = true)
        if (!_state.value.isInitialized) {
            initializeSpeechRecognizer()
        }
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        context = null
        _state.value = _state.value.copy(
            isInitialized = false,
            isSpeaking = false
        )
    }
}