package com.ragnar.mindmaplearningapp.core

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragnar.mindmaplearningapp.data.model.ChatMessageModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ChatViewModel(apiKey: String) : ViewModel() {


    private val llmClient = LLMClient(apiKey)

    // Chat messages
    private val _messages = MutableStateFlow<List<ChatMessageModel>>(emptyList())
    val messages: StateFlow<List<ChatMessageModel>> = _messages

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Typing animation state
    private val _typingText = MutableStateFlow("")
    val typingText: StateFlow<String> = _typingText

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    // Concept map with progressive drawing
    private val _conceptMapJSON = MutableStateFlow("""{"visualization_type":"None","main_concept":"Chat for a Concept Map","nodes":[],"edges":[]}""")
    val conceptMapJSON: StateFlow<String> = _conceptMapJSON

    // TTS trigger
    private val _shouldStartTTS = MutableStateFlow(false)
    val shouldStartTTS: StateFlow<Boolean> = _shouldStartTTS

    private val _fullTextForTTS = MutableStateFlow("")
    val fullTextForTTS: StateFlow<String> = _fullTextForTTS

    // Jobs for animation control
    private var typingJob: Job? = null
    private var conceptMapJob: Job? = null

    private var lastUserMessage: String = ""

    private val _ttsProgress = MutableStateFlow(0f)
    val ttsProgress: StateFlow<Float> = _ttsProgress

    fun sendMessage(userMessage: String) {
        lastUserMessage = userMessage

        // Add user message to chat
        val userMsg = ChatMessageModel(content = userMessage, sender = "user")
        _messages.value += userMsg
        _isLoading.value = true

        // Cancel any ongoing animations
        cancelAnimations()

        viewModelScope.launch {
            try {
                val response = llmClient.queryLLM(userMessage)

                val answerText = llmClient.extractAnswer(response)
                val conceptMapJson = llmClient.extractConceptMapJSON(response)

                // Store full text for TTS
                _fullTextForTTS.value = answerText

                // Start typing animation
                startTypingAnimation(answerText)

                // Update concept map progressively
                startProgressiveConceptMap(conceptMapJson)

                Log.i("ChatViewModel", "Success: Answer and concept map updated")

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message: ${e.message}", e)
                _messages.value += ChatMessageModel(
                    content = "Sorry, I encountered an error: ${e.message}",
                    sender = "ai",
                    canRetry = true
                )
                _isTyping.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startTypingAnimation(fullText: String) {
        typingJob?.cancel()

        typingJob = viewModelScope.launch {
            _isTyping.value = true
            _typingText.value = ""

            // Trigger TTS to start immediately
            _shouldStartTTS.value = true
            delay(100)
            _shouldStartTTS.value = false

            // Use word-by-word for better performance
            val words = fullText.split(" ")

            words.forEachIndexed { index, word ->
                _typingText.value += if (index == 0) word else " $word"

                // Dynamic delay: 200-300ms per word (matches natural TTS speed)
                val baseDelay = 250L
                val pauseMs = when {
                    word.endsWith(".") || word.endsWith("!") || word.endsWith("?") -> baseDelay + 150L
                    word.endsWith(",") || word.endsWith(";") -> baseDelay + 80L
                    else -> baseDelay
                }

                delay(pauseMs)
            }

            // Animation complete
            _messages.value += ChatMessageModel(
                content = fullText,
                sender = "ai"
            )

            _isTyping.value = false
            _typingText.value = ""
        }
    }

    private fun startProgressiveConceptMap(conceptMapJson: String) {
        conceptMapJob?.cancel()

        conceptMapJob = viewModelScope.launch {
            try {
                val jsonObj = JSONObject(conceptMapJson)
                val nodesArray = jsonObj.getJSONArray("nodes")
                val edgesArray = jsonObj.getJSONArray("edges")

                val totalNodes = nodesArray.length()
                val totalEdges = edgesArray.length()

                if (totalNodes == 0) {
                    _conceptMapJSON.value = conceptMapJson
                    return@launch
                }

                // Start with empty concept map (but keep main_concept)
                val progressiveNodes = JSONArray()
                val progressiveEdges = JSONArray()

                // Add nodes one by one with delay
                for (i in 0 until totalNodes) {
                    progressiveNodes.put(nodesArray.getJSONObject(i))

                    val progressMap = JSONObject().apply {
                        put("visualization_type", jsonObj.getString("visualization_type"))
                        put("main_concept", jsonObj.getString("main_concept"))
                        put("nodes", progressiveNodes)
                        put("edges", JSONArray()) // No edges yet
                    }

                    _conceptMapJSON.value = progressMap.toString()

                    // Delay between nodes (adjust for smooth animation)
                    delay(400L)
                }

                // Now add edges one by one
                for (i in 0 until totalEdges) {
                    progressiveEdges.put(edgesArray.getJSONObject(i))

                    val progressMap = JSONObject().apply {
                        put("visualization_type", jsonObj.getString("visualization_type"))
                        put("main_concept", jsonObj.getString("main_concept"))
                        put("nodes", progressiveNodes)
                        put("edges", progressiveEdges)
                    }

                    _conceptMapJSON.value = progressMap.toString()

                    // Delay between edges
                    delay(300L)
                }

                Log.i("ChatViewModel", "Concept map animation complete")

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error animating concept map: ${e.message}", e)
                // Fallback: show complete map
                _conceptMapJSON.value = conceptMapJson
            }
        }
    }

    private fun cancelAnimations() {
        typingJob?.cancel()
        conceptMapJob?.cancel()
        _isTyping.value = false
        _typingText.value = ""
    }

    fun retryLastMessage() {
        if (lastUserMessage.isNotBlank()) {
            _messages.value = _messages.value.filter { !(it.sender == "ai" && it.isError) }
            sendMessage(lastUserMessage)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelAnimations()
    }
}