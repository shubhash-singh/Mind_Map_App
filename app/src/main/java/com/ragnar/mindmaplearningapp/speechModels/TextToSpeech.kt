package com.ragnar.mindmaplearningapp.speechModels


import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Core controller for Text-to-Speech with Lip Sync integration
 * Manages TTS engine, WebView communication, and UI state
 */
class TextToSpeech : ViewModel(), TextToSpeech.OnInitListener {

    // State data class
    data class TTSState(
        val isInitialized: Boolean = false,
        val isSpeaking: Boolean = false,
        val selectedLanguage: String = "en-IN",
        val detectedLanguage: String = "",
        val selectedCharacter: String = "boy",
        val speechRate: Float = 1.0f,
        val pitch: Float = 1.0f,
        val statusMessage: String = "",
        val debugMode: Boolean = false,
        val availableVoices: List<String> = emptyList(),
        val currentViseme: String = "rest"
    )

    private val _state = MutableStateFlow(TTSState())
    val state: StateFlow<TTSState> = _state.asStateFlow()

    // TTS components
    private var textToSpeech: TextToSpeech? = null
    private var webView: WebView? = null
    private var availableVoices: List<Voice> = emptyList()

    // Language detection patterns
    private val languagePatterns = mapOf(
        "hi-IN" to Regex("[\u0900-\u097F]+"), // Devanagari script (Hindi)
        "kn-IN" to Regex("[\u0C80-\u0CFF]+"), // Kannada script
        "ta-IN" to Regex("[\u0B80-\u0BFF]+"), // Tamil script
        "te-IN" to Regex("[\u0C00-\u0C7F]+")  // Telugu script
    )

    private val languageNames = mapOf(
        "en-IN" to "English",
        "hi-IN" to "हिंदी (Hindi)",
        "kn-IN" to "ಕನ್ನಡ (Kannada)",
        "ta-IN" to "தமிழ் (Tamil)",
        "te-IN" to "తెలుగు (Telugu)"
    )

    /**
     * Initialize the TTS controller
     */
    fun initialize(context: Context) {
        // Only skip if the engine instance already exists
        if (textToSpeech != null) return

        updateStatus("Initializing Text-to-Speech...")
        Log.d("TextToSpeech", "Initializing Text-to-Speech...")
        textToSpeech = TextToSpeech(context, this)
    }

    /**
     * Setup WebView for lip sync animation
     */
    fun setupWebView(webView: WebView) {
        this.webView = webView

        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // Initialize with current character
                    switchCharacter(_state.value.selectedCharacter)
                    injectCharacterImages(view?.context!!)
                    updateStatus("Lip sync ready")
                }
            }


            loadUrl("file:///android_asset/LipSync.html")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                // Get available voices
                availableVoices = tts.voices?.toList() ?: emptyList()

                // Set default language
                setLanguageInternal("en-IN")

                // Setup utterance progress listener
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = true,
                            statusMessage = "Speaking..."
                        )
                    }

                    override fun onDone(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            statusMessage = "Speech completed"
                        )
                        stopLipSync()
                    }

                    override fun onError(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            statusMessage = "Speech error occurred"
                        )
                        stopLipSync()
                    }
                })

                _state.value = _state.value.copy(
                    isInitialized = true,
                    statusMessage = "Text-to-Speech ready",
                    availableVoices = availableVoices.map { "${it.name} (${it.locale})" }
                )
            }
        } else {
            _state.value = _state.value.copy(
                statusMessage = "Error: TTS initialization failed"
            )
            Log.e("TTS", "Initialization failed with status: $status")
        }
    }

    /**
     * Speak the given text with lip sync animation
     */
    fun speak(text: String) {
        if (!_state.value.isInitialized || text.isBlank()) {
            updateStatus("Error: Cannot speak - TTS not ready or empty text")
            return
        }

        textToSpeech?.let { tts ->
            // Detect language
            val detectedLang = detectLanguage(text)
            if (detectedLang != _state.value.selectedLanguage) {
                setLanguageInternal(detectedLang)
            }

            // Set speech parameters
            tts.setSpeechRate(_state.value.speechRate)
            tts.setPitch(_state.value.pitch)

            // Start lip sync animation
            startLipSync(text)

            // Start speaking
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "tts_${System.currentTimeMillis()}"

            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        }
    }

    /**
     * Stop current speech and lip sync
     */
    fun stop() {
        textToSpeech?.stop()
        stopLipSync()
        _state.value = _state.value.copy(
            isSpeaking = false,
            statusMessage = "Speech stopped"
        )
    }

    /**
     * Update text and detect language
     */
    fun updateText(text: String) {
        val detectedLangCode = detectLanguage(text)
        val langName = languageNames[detectedLangCode] ?: detectedLangCode

        _state.value = _state.value.copy(
            detectedLanguage = langName,
            statusMessage = "Detected language: $langName"
        )
    }

    /**
     * Set speech language
     */
    fun setLanguage(languageCode: String) {
        setLanguageInternal(languageCode)
        val langName = languageNames[languageCode] ?: languageCode
        _state.value = _state.value.copy(
            selectedLanguage = languageCode,
            statusMessage = "Language set to $langName"
        )
    }

    private fun setLanguageInternal(languageCode: String) {
        textToSpeech?.let { tts ->
            val locale = when (languageCode) {
                "en-IN" -> Locale("en", "IN")
                "hi-IN" -> Locale("hi", "IN")
                "kn-IN" -> Locale("kn", "IN")
                "ta-IN" -> Locale("ta", "IN")
                "te-IN" -> Locale("te", "IN")
                else -> Locale("en", "IN")
            }

            val result = tts.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                updateStatus("Warning: Language $languageCode not fully supported, using fallback")
                tts.language = Locale("en", "IN")
                _state.value = _state.value.copy(selectedLanguage = "hi-IN")
            } else {
                _state.value = _state.value.copy(selectedLanguage = languageCode)
            }
        }
    }

    /**
     * Set speech rate
     */
    fun setSpeechRate(rate: Float) {
        _state.value = _state.value.copy(speechRate = rate)
    }

    /**
     * Set speech pitch
     */
    fun setPitch(pitch: Float) {
        _state.value = _state.value.copy(pitch = pitch)
    }

    fun injectCharacterImages(context: Context) {
        val boyBase64 = context.assets.open("images/boy.png").use {
            android.util.Base64.encodeToString(it.readBytes(), android.util.Base64.NO_WRAP)
        }
        val girlBase64 = context.assets.open("images/girl.png").use {
            android.util.Base64.encodeToString(it.readBytes(), android.util.Base64.NO_WRAP)
        }

        webView?.evaluateJavascript(
            """
        window.lipSync.setImageData({
            boy: "data:image/png;base64,$boyBase64",
            girl: "data:image/png;base64,$girlBase64"
        });
        """.trimIndent(),
            null
        )
    }
    /**
     * Switch character in lip sync animation
     */
    fun switchCharacter(character: String) {
        _state.value = _state.value.copy(selectedCharacter = character)

        webView?.post {
            webView?.evaluateJavascript(
                "window.AndroidLipSyncAPI.switchCharacter('$character')"
            ) { result ->
                Log.d("LipSync", "Character switch result: $result")
            }
        }
    }

    /**
     * Toggle debug mode in lip sync
     */
    fun toggleDebug() {
        val newDebugMode = !_state.value.debugMode
        _state.value = _state.value.copy(debugMode = newDebugMode)

        webView?.post {
            webView?.evaluateJavascript(
                "window.AndroidLipSyncAPI.toggleDebug($newDebugMode)"
            ) { result ->
                Log.d("LipSync", "Debug toggle result: $result")
            }
        }
    }

    /**
     * Test lip sync animation
     */
    fun testAnimation() {
        webView?.post {
            webView?.evaluateJavascript(
                "window.AndroidLipSyncAPI.testAnimation()"
            ) { result ->
                Log.d("LipSync", "Test animation result: $result")
            }
        }
    }

    /**
     * Start lip sync animation
     */
    private fun startLipSync(text: String) {
        val escapedText = text.replace("'", "\\'").replace("\n", "\\n")

        webView?.post {
            webView?.evaluateJavascript(
                "window.AndroidLipSyncAPI.startLipSync('$escapedText')"
            ) { result ->
                Log.d("LipSync", "Start lip sync result: $result")
            }
        }
    }

    /**
     * Stop lip sync animation
     */
    private fun stopLipSync() {
        webView?.post {
            webView?.evaluateJavascript(
                "window.AndroidLipSyncAPI.stopLipSync()"
            ) { result ->
                Log.d("LipSync", "Stop lip sync result: $result")
            }
        }
    }


    /**
     * Detect language from text
     */
    private fun detectLanguage(text: String): String {
        if (text.isBlank()) return "en-IN"

        // Check for Indic scripts
        for ((langCode, pattern) in languagePatterns) {
            if (pattern.find(text) != null) {
                return langCode
            }
        }

        // Default to English
        return "en-IN"
    }

    /**
     * Update status message
     */
    private fun updateStatus(message: String) {
        _state.value = _state.value.copy(statusMessage = message)
        Log.d("TTS", message)
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        textToSpeech?.let { tts ->
            tts.stop()
            tts.shutdown()
        }
        textToSpeech = null
        webView = null
        _state.value = _state.value.copy(
            isInitialized = false,
            isSpeaking = false,
            statusMessage = "Text-to-Speech resources released"
        )
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}