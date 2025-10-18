package com.ragnar.mindmaplearningapp.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GroqLLMClient(
    private val apiKey: String,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val url = "https://api.groq.com/openai/v1/chat/completions"
    /**
     * Main query method that returns the complete API response
     */
    suspend fun queryLLM(userQuery: String): String = withContext(Dispatchers.IO) {
        try {
            // System + user prompt messages
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", """
                        You are an intelligent and structured AI tutor for a Class  student.
                        You must respond strictly in the following format and hierarchy — no markdown, no extra text, and no explanations outside this structure.
                    
                        [ANSWER]
                        Provide a clear, natural, and age-appropriate explanation for the student's query in less than  words.
                        Keep the tone educational and concise.
                    
                        [CONCEPT_MAP_JSON]
                        Provide ONLY a valid JSON object describing a hierarchical concept map for the same topic.
                        Use this exact structure:
                        {
                          "visualization_type": "Concept Map",
                          "main_concept": "Main idea or topic",
                          "nodes": [
                            {"id": "A", "label": "Main Concept", "category": "Main"},
                            {"id": "B", "label": "Sub Concept 1", "category": "Secondary"},
                            {"id": "C", "label": "Sub Concept 2", "category": "Secondary"},
                            {"id": "D", "label": "Leaf Concept 1", "category": "Leaf"},
                            ...
                          ],
                          "edges": [
                            {"from": "A", "to": "B", "label": "relation"},
                            {"from": "A", "to": "C", "label": "relation"},
                            {"from": "B", "to": "D", "label": "relation"},
                            ...
                          ]
                        }
                    
                        Hierarchy Rules:
                        - There must be exactly **1 Main node** (root concept).
                        - Include **2–3 Secondary nodes** connected directly to the main node.
                        - Remaining nodes should be **Leaf nodes**, connected to secondary nodes.
                        - Include approximately total nodes.
                        - All nodes and edges must form a clear hierarchical structure.
                        - JSON must be valid, syntactically correct, and directly parsable (no markdown or code block).
                    
                        Do not add any explanations, comments, or text after the JSON.
                        """.trimIndent()
                    )


                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userQuery)
                })
            }

            val jsonBody = JSONObject().apply {
                put("model", "meta-llama/llama-4-scout-17b-16e-instruct")
                put("messages", messages)
                put("temperature", 0.7)
                put("max_tokens", 1024)
            }

            Log.d("AIChatUtils", "Sending request to: $url")
            Log.d("AIChatUtils", "Request body: $jsonBody")

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            Log.d("AIChatUtils", "Response code: ${response.code}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e("AIChatUtils", "Error: ${response.code} - ${response.message}")
                Log.e("AIChatUtils", "Error body: $errorBody")
                response.close()
                return@withContext "Error: ${response.code} - ${response.message}"
            }

            // Read response body
            val responseBody = response.body?.string() ?: ""
            response.close()

            Log.d("AIChatUtils", "Response body: $responseBody")
            return@withContext responseBody

        } catch (e: Exception) {
            Log.e("AIChatUtils", "Exception during API call: ${e.message}", e)
            return@withContext "Error: ${e.message}"
        }
    }

    /**
     * Extracts the concept map JSON from the AI response
     */
    fun extractConceptMapJSON(fullResponse: String): String {
        try {
            // Parse the full API response
            val jsonResponse = JSONObject(fullResponse)
            val choices = jsonResponse.getJSONArray("choices")

            if (choices.length() > 0) {
                val message = choices.getJSONObject(0).getJSONObject("message")
                val content = message.getString("content")

                Log.d("AIChatUtils", "Extracting concept map from content of length: ${content.length}")

                // Look for any JSON object that contains "visualization_type": "Concept Map"
                var currentPos = 0

                while (currentPos < content.length) {
                    val startPos = content.indexOf("{", currentPos)
                    if (startPos == -1) break

                    // Find the matching closing brace
                    var braceCount = 0
                    var endIndex = startPos

                    for (i in startPos until content.length) {
                        when (content[i]) {
                            '{' -> braceCount++
                            '}' -> {
                                braceCount--
                                if (braceCount == 0) {
                                    endIndex = i + 1
                                    break
                                }
                            }
                        }
                    }

                    if (endIndex > startPos) {
                        // Try to parse this JSON object
                        val candidateJson = content.substring(startPos, endIndex)

                        try {
                            val testObj = JSONObject(candidateJson)
                            // Check if it's a concept map JSON
                            if (testObj.has("visualization_type") &&
                                testObj.has("main_concept") &&
                                testObj.has("nodes") &&
                                testObj.has("edges")) {

                                Log.d("AIChatUtils", "Successfully extracted concept map JSON")
                                return candidateJson
                            }
                        } catch (e: Exception) {
                            // Not a valid JSON or not a concept map, continue searching
                            Log.e("GroqLLMClient", "Not a valid JSON or not a concept map, continue searching :\n ${e.message}")
                        }
                    }

                    currentPos = endIndex
                }
            }

            Log.e("AIChatUtils", "Could not extract concept map JSON from response")
            return getDefaultConceptMapJSON()

        } catch (e: Exception) {
            Log.e("AIChatUtils", "Error extracting concept map JSON: ${e.message}", e)
            return getDefaultConceptMapJSON()
        }
    }

    /**
     * Extracts the answer text from the AI response
     */
    fun extractAnswer(fullResponse: String): String {
        try {
            val jsonResponse = JSONObject(fullResponse)
            val choices = jsonResponse.getJSONArray("choices")

            if (choices.length() > 0) {
                val message = choices.getJSONObject(0).getJSONObject("message")
                val content = message.getString("content")

                // Try to find [ANSWER] marker
                val answerStart = content.indexOf("[ANSWER]")
                if (answerStart != -1) {
                    val afterAnswer = content.substring(answerStart + "[ANSWER]".length).trim()

                    // Find where the JSON starts (to exclude it)
                    val jsonStart = afterAnswer.indexOf("{")

                    // Also find [CONCEPT_MAP_JSON] marker as alternative boundary
                    val conceptMapMarker = afterAnswer.indexOf("[CONCEPT_MAP_JSON]")

                    // Determine where to cut the answer
                    val cutPosition = when {
                        conceptMapMarker != -1 -> conceptMapMarker
                        jsonStart != -1 -> jsonStart
                        else -> afterAnswer.length
                    }

                    val rawAnswer = if (cutPosition != afterAnswer.length) {
                        afterAnswer.substring(0, cutPosition).trim()
                    } else {
                        afterAnswer
                    }

                    // Clean up the answer by removing any remaining bracket markers
                    var cleanedAnswer = rawAnswer

                    // Remove patterns like [COW], [Concept Map], [ANYTHING] at the end or in middle
                    cleanedAnswer = cleanedAnswer.replace(Regex("\\[.*?\\]"), "").trim()

                    // Remove any trailing markers or labels before returning
                    return cleanedAnswer.trim()
                }

                // No [ANSWER] marker, try to extract text before JSON or markers
                var rawContent = content

                // Find first JSON occurrence
                val firstBrace = rawContent.indexOf("{")
                if (firstBrace > 0) {
                    rawContent = rawContent.substring(0, firstBrace).trim()
                }

                // Find [CONCEPT_MAP_JSON] marker
                val conceptMapMarker = rawContent.indexOf("[CONCEPT_MAP_JSON]")
                if (conceptMapMarker != -1) {
                    rawContent = rawContent.substring(0, conceptMapMarker).trim()
                }

                // Clean up any bracket markers
                rawContent = rawContent.replace(Regex("\\[.*?\\]"), "").trim()

                return if (rawContent.isNotBlank()) rawContent else content.trim()
            }

            return "I encountered an error processing the response."

        } catch (e: Exception) {
            Log.e("AIChatUtils", "Error extracting answer: ${e.message}", e)
            return "I encountered an error processing the response."
        }
    }

    /**
     * Returns a default concept map JSON when extraction fails
     */
    private fun getDefaultConceptMapJSON(): String {
        return """
        {
          "visualization_type": "Concept Map",
          "main_concept": "Loading...",
          "nodes": [
            {"id": "A", "label": "Concept", "category": "Core"}
          ],
          "edges": []
        }
        """.trimIndent()
    }
}