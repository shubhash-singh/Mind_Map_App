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

class LLMClient(
    private val apiKey: String,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val url = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun queryLLM(userQuery: String): String = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", """
    You are an advanced AI tutor specializing in Computer Science Engineering for final year students.
    You must respond in a structured format with two distinct sections. Follow this format precisely.

    [ANSWER]
    Provide a comprehensive, technically detailed explanation suitable for a final year engineering student.
    
    Requirements for the answer:
    - Length: 200-300 words for focused explanations
    - Depth: Include technical details, real-world applications, and implementation considerations
    - Structure: Use clear progression from fundamentals to advanced concepts
    - Tone: Professional yet accessible, assuming strong technical background
    - Include: Practical examples, use cases, advantages/disadvantages where relevant
    - Add: Industry context and how the concept applies in modern software engineering
    
    For complex topics:
    - Break down into logical components
    - Explain underlying principles and mechanisms
    - Connect to related CS concepts the student should know
    - Mention common challenges and best practices

    [CONCEPT_MAP_JSON]
    Generate a comprehensive, multi-level hierarchical concept map with minimum 25 nodes.
    Output ONLY valid JSON with this exact structure:
    
    {
      "visualization_type": "Concept Map",
      "main_concept": "Core Topic Name",
      "nodes": [
        {"id": "A", "label": "Main Concept", "category": "Main"},
        {"id": "B1", "label": "Primary Category 1", "category": "Primary"},
        {"id": "B2", "label": "Primary Category 2", "category": "Primary"},
        {"id": "C1", "label": "Secondary Concept 1.1", "category": "Secondary"},
        {"id": "C2", "label": "Secondary Concept 1.2", "category": "Secondary"},
        {"id": "D1", "label": "Tertiary Detail 1.1.1", "category": "Tertiary"},
        {"id": "E1", "label": "Implementation Detail", "category": "Leaf"},
        ...
      ],
      "edges": [
        {"from": "A", "to": "B1", "label": "consists of"},
        {"from": "A", "to": "B2", "label": "includes"},
        {"from": "B1", "to": "C1", "label": "implements"},
        {"from": "C1", "to": "D1", "label": "uses"},
        {"from": "D1", "to": "E1", "label": "requires"},
        ...
      ]
    }

    Hierarchical Structure Requirements:
    
    Level 1 - Main (1 node):
    - The root concept or central topic
    
    Level 2 - Primary (3-4 nodes):
    - Major categories or fundamental components
    - Connected directly to the Main node
    
    Level 3 - Secondary (6-8 nodes):
    - Sub-components, key principles, or methodologies
    - Connected to Primary nodes
    
    Level 4 - Tertiary (8-10 nodes):
    - Specific techniques, algorithms, or implementation details
    - Connected to Secondary nodes
    
    Level 5 - Leaf (7-10 nodes):
    - Concrete examples, specific technologies, or detailed mechanisms
    - Connected to Tertiary nodes
    
    Total nodes: Minimum 25, ideally 30-35 for comprehensive coverage
    
    Edge Labels:
    Use meaningful relationship descriptors like:
    - "consists of", "includes", "requires", "implements"
    - "uses", "applies", "extends", "depends on"
    - "leads to", "produces", "enables", "supports"
    
    Quality Requirements:
    - Every node must be meaningfully connected (no orphans)
    - Create a balanced tree structure (avoid single long chains)
    - Include both theoretical concepts and practical implementations
    - Cover different aspects: fundamentals, applications, tools, best practices
    - JSON must be syntactically perfect and directly parsable
    - No markdown formatting, code blocks, or extra text after JSON
    
    For technical topics, include nodes covering:
    - Core algorithms/data structures
    - Time/space complexity considerations
    - Real-world applications
    - Related technologies/frameworks
    - Common pitfalls and solutions
    - Performance optimization techniques
    
    Output the JSON immediately after this section with no additional text.
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
                put("max_tokens", 2048)
            }

            Log.d("AIChatUtils", "Sending request to: $url")

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

            val responseBody = response.body?.string() ?: ""
            response.close()

            Log.d("AIChatUtils", "Response received, length: ${responseBody.length}")
            return@withContext responseBody

        } catch (e: Exception) {
            Log.e("AIChatUtils", "Exception during API call: ${e.message}", e)
            return@withContext "Error: ${e.message}"
        }
    }

    suspend fun extractConceptMapJSON(fullResponse: String): String = withContext(Dispatchers.Default) {
        try {
            val jsonResponse = JSONObject(fullResponse)
            val choices = jsonResponse.getJSONArray("choices")

            if (choices.length() > 0) {
                val message = choices.getJSONObject(0).getJSONObject("message")
                val content = message.getString("content")

                Log.d("AIChatUtils", "Extracting concept map from content of length: ${content.length}")

                // OPTIMIZED: Use regex to find JSON blocks instead of character-by-character iteration
                val jsonPattern = Regex("""\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""", RegexOption.DOT_MATCHES_ALL)
                val matches = jsonPattern.findAll(content)

                for (match in matches) {
                    val candidateJson = match.value

                    try {
                        val testObj = JSONObject(candidateJson)

                        // Check if it's a concept map JSON
                        if (testObj.has("visualization_type") &&
                            testObj.has("main_concept") &&
                            testObj.has("nodes") &&
                            testObj.has("edges")) {

                            Log.d("AIChatUtils", "Successfully extracted concept map JSON")
                            return@withContext candidateJson
                        }
                    } catch (e: Exception) {
                        // Not valid JSON, continue to next match
                        continue
                    }
                }

                // Fallback: Try to find JSON between markers
                val jsonStartMarker = "[CONCEPT_MAP_JSON]"
                val markerIndex = content.indexOf(jsonStartMarker)

                if (markerIndex != -1) {
                    val afterMarker = content.substring(markerIndex + jsonStartMarker.length).trim()
                    val firstBrace = afterMarker.indexOf('{')

                    if (firstBrace != -1) {
                        // Find matching closing brace using stack-based approach
                        var braceCount = 0
                        var endIndex = firstBrace

                        for (i in firstBrace until afterMarker.length) {
                            when (afterMarker[i]) {
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

                        if (endIndex > firstBrace) {
                            val extractedJson = afterMarker.substring(firstBrace, endIndex)

                            try {
                                // Validate it's proper JSON
                                val testObj = JSONObject(extractedJson)
                                if (testObj.has("nodes") && testObj.has("edges")) {
                                    return@withContext extractedJson
                                }
                            } catch (e: Exception) {
                                Log.e("AIChatUtils", "Invalid JSON after marker: ${e.message}")
                            }
                        }
                    }
                }
            }

            Log.e("AIChatUtils", "Could not extract concept map JSON from response")
            return@withContext getDefaultConceptMapJSON()

        } catch (e: Exception) {
            Log.e("AIChatUtils", "Error extracting concept map JSON: ${e.message}", e)
            return@withContext getDefaultConceptMapJSON()
        }
    }

    /**
     * Extracts the answer text from the AI response
     */
    suspend fun extractAnswer(fullResponse: String): String = withContext(Dispatchers.Default) {
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

                    // Find where the next section starts
                    val conceptMapMarker = afterAnswer.indexOf("[CONCEPT_MAP_JSON]")
                    val jsonStart = afterAnswer.indexOf("{")

                    val cutPosition = when {
                        conceptMapMarker != -1 -> conceptMapMarker
                        jsonStart != -1 -> jsonStart
                        else -> afterAnswer.length
                    }

                    val rawAnswer = afterAnswer.substring(0, cutPosition).trim()

                    // Clean up any remaining bracket markers
                    val cleanedAnswer = rawAnswer.replace(Regex("\\[.*?\\]"), "").trim()

                    return@withContext cleanedAnswer
                }

                // Fallback: extract text before JSON
                var rawContent = content
                val firstBrace = rawContent.indexOf("{")
                if (firstBrace > 0) {
                    rawContent = rawContent.substring(0, firstBrace).trim()
                }

                val conceptMapMarker = rawContent.indexOf("[CONCEPT_MAP_JSON]")
                if (conceptMapMarker != -1) {
                    rawContent = rawContent.substring(0, conceptMapMarker).trim()
                }

                rawContent = rawContent.replace(Regex("\\[.*?\\]"), "").trim()

                return@withContext if (rawContent.isNotBlank()) rawContent else content.trim()
            }

            return@withContext "I encountered an error processing the response."

        } catch (e: Exception) {
            Log.e("AIChatUtils", "Error extracting answer: ${e.message}", e)
            return@withContext "I encountered an error processing the response."
        }
    }

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