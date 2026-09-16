package com.example.data.gemini

import com.example.data.model.BeverageType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.2f,
    @Json(name = "responseMimeType") val responseMimeType: String? = "application/json"
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiAssistantAction(
    @Json(name = "action") val action: String = "LOG_WATER", // "LOG_WATER", "QUERY", "CHAT"
    @Json(name = "amountMl") val amountMl: Int? = 250,
    @Json(name = "beverage") val beverage: String = "WATER",
    @Json(name = "note") val note: String = "",
    @Json(name = "assistantReply") val assistantReply: String = "Logged water!"
) {
    fun toBeverageType(): BeverageType {
        return BeverageType.fromName(beverage)
    }
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val loggedAmountMl: Int? = null,
    val beverageType: BeverageType? = null
)

enum class MessageSender {
    USER, ASSISTANT, SYSTEM
}
