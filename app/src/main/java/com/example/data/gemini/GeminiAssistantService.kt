package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.BeverageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.regex.Pattern

class GeminiAssistantService {
    private val TAG = "GeminiAssistantService"
    private val apiService = GeminiRetrofitClient.service
    private val moshi = GeminiRetrofitClient.moshi
    private val actionAdapter = moshi.adapter(GeminiAssistantAction::class.java)

    suspend fun processUserSpeechOrText(
        input: String,
        currentTotalMl: Int,
        targetMl: Int,
        streakDays: Int
    ): GeminiAssistantAction = withContext(Dispatchers.IO) {
        val trimmedInput = input.trim()
        if (trimmedInput.isBlank()) {
            return@withContext GeminiAssistantAction(
                action = "CHAT",
                amountMl = null,
                beverage = "WATER",
                note = "",
                assistantReply = "I'm listening! Tell me what you drank (e.g., 'I drank a glass of water')."
            )
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val systemPrompt = """
                    You are the Gemini Hydration Assistant for an Android Water Tracker app.
                    Current User Context:
                    - Today's current intake: $currentTotalMl ml
                    - Daily goal: $targetMl ml
                    - Current streak: $streakDays days
                    
                    Your task is to analyze user voice/text commands and return a strict JSON object.
                    
                    JSON Output Schema:
                    {
                      "action": "LOG_WATER" | "QUERY" | "CHAT",
                      "amountMl": integer or null (e.g., 250 for standard glass, 150 for cup, 500 for bottle, 330 for can, 750 for flask, or exact ml from text),
                      "beverage": "WATER" | "ELECTROLYTE" | "TEA" | "COFFEE" | "JUICE" | "COCONUT_WATER",
                      "note": string (e.g. "Glass of water", "Post gym workout"),
                      "assistantReply": string (a warm, encouraging 1-2 sentence response confirming the logged water, remaining amount to goal, or answering their question)
                    }
                    
                    Standard Volume Assumptions:
                    - "glass" or "glass of water" = 250 ml
                    - "cup" / "cup of tea" = 200 ml (or 150ml for espresso/small cup)
                    - "bottle" = 500 ml
                    - "can" = 330 ml
                    - "flask" / "tumbler" = 750 ml
                    - "mug" = 350 ml
                    - "pint" = 473 ml
                    - "liter" / "litre" = 1000 ml
                    - "gallon" = 3785 ml
                    - "oz" or "ounces" = multiply by 29.57 and round to nearest int
                    - If number only like "drank 300" -> 300 ml
                    
                    If user says "I drank a glass of water" or "i drunk a glass of water", action MUST be "LOG_WATER", amountMl MUST be 250, beverage MUST be "WATER".
                    Always respond with valid JSON ONLY. No markdown wrapping.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = trimmedInput)),
                            role = "user"
                        )
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = systemPrompt))
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.1f,
                        responseMimeType = "application/json"
                    )
                )

                val response = apiService.generateContent(apiKey, request)
                val jsonText = response.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text

                if (!jsonText.isNullOrBlank()) {
                    val cleanJson = jsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val parsedAction = actionAdapter.fromJson(cleanJson)
                    if (parsedAction != null) {
                        return@withContext parsedAction
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API call failed, falling back to smart local NLP parser", e)
            }
        }

        // Fallback: Smart local NLP rule-based parser for offline / instant resilience
        return@withContext parseWithLocalNlp(trimmedInput, currentTotalMl, targetMl)
    }

    private fun parseWithLocalNlp(
        input: String,
        currentTotalMl: Int,
        targetMl: Int
    ): GeminiAssistantAction {
        val lower = input.lowercase(Locale.ROOT)

        // Detect Beverage Type
        val beverage = when {
            lower.contains("electrolyte") || lower.contains("lemon") || lower.contains("gatorade") || lower.contains("powerade") -> "ELECTROLYTE"
            lower.contains("tea") || lower.contains("matcha") || lower.contains("chai") || lower.contains("infusion") -> "TEA"
            lower.contains("coffee") || lower.contains("espresso") || lower.contains("latte") || lower.contains("cappuccino") || lower.contains("americano") -> "COFFEE"
            lower.contains("juice") || lower.contains("smoothie") || lower.contains("orange") || lower.contains("apple juice") -> "JUICE"
            lower.contains("coconut") -> "COCONUT_WATER"
            else -> "WATER"
        }

        val bevDisplayName = BeverageType.fromName(beverage).displayName

        // Check for queries
        if (lower.startsWith("how much") || lower.contains("status") || lower.contains("progress") || lower.contains("how many")) {
            val remaining = (targetMl - currentTotalMl).coerceAtLeast(0)
            val pct = if (targetMl > 0) (currentTotalMl * 100 / targetMl) else 0
            return GeminiAssistantAction(
                action = "QUERY",
                amountMl = null,
                beverage = beverage,
                note = "",
                assistantReply = "You have logged $currentTotalMl ml so far today ($pct% of your $targetMl ml goal). You have $remaining ml remaining! 💧"
            )
        }

        // Detect Amount
        var amountMl: Int? = null

        // 1. Explicit numbers (e.g. 500 ml, 250ml, 300, 1.5 l, 16 oz)
        val mlRegex = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(ml|milliliter|millilitre|l|liter|litre|oz|ounce)?", Pattern.CASE_INSENSITIVE)
        val mlMatcher = mlRegex.matcher(lower)

        // Find matches in context of intake
        while (mlMatcher.find()) {
            val valueStr = mlMatcher.group(1) ?: continue
            val unit = mlMatcher.group(2)?.lowercase(Locale.ROOT)
            val num = valueStr.toFloatOrNull() ?: continue

            when (unit) {
                "l", "liter", "litre" -> {
                    amountMl = (num * 1000).toInt()
                    break
                }
                "oz", "ounce" -> {
                    amountMl = (num * 29.57f).toInt()
                    break
                }
                "ml", "milliliter", "millilitre" -> {
                    amountMl = num.toInt()
                    break
                }
                else -> {
                    // Plain number
                    if (num >= 20 && num <= 5000) {
                        amountMl = num.toInt()
                    }
                }
            }
        }

        // 2. Container words heuristics if no explicit volume
        if (amountMl == null) {
            // Count multipliers: "two glasses", "2 cups", "half a glass"
            val countMultiplier = when {
                lower.contains("half") || lower.contains("1/2") -> 0.5f
                lower.contains("two") || lower.contains("2 ") || lower.contains("2x") || lower.contains("couple") -> 2f
                lower.contains("three") || lower.contains("3 ") || lower.contains("3x") -> 3f
                lower.contains("four") || lower.contains("4 ") -> 4f
                else -> 1f
            }

            val baseAmount = when {
                lower.contains("glass") -> 250
                lower.contains("cup") -> if (beverage == "COFFEE") 180 else 200
                lower.contains("bottle") -> if (lower.contains("big") || lower.contains("large")) 750 else 500
                lower.contains("can") -> 330
                lower.contains("flask") || lower.contains("tumbler") -> 750
                lower.contains("mug") -> 350
                lower.contains("pint") -> 473
                lower.contains("sip") || lower.contains("gulp") -> 50
                lower.contains("espresso") || lower.contains("shot") -> 50
                lower.contains("water") || lower.contains("drink") || lower.contains("drank") || lower.contains("drunk") -> 250 // Default standard glass
                else -> 250
            }

            amountMl = (baseAmount * countMultiplier).toInt()
        }

        val safeAmount = amountMl.coerceIn(20, 5000)
        val newTotal = currentTotalMl + safeAmount
        val newPct = if (targetMl > 0) (newTotal * 100 / targetMl) else 0

        val reply = if (newTotal >= targetMl) {
            "Logged +${safeAmount} ml of $bevDisplayName! 🎉 Awesome! You just reached your daily hydration goal of $targetMl ml ($newPct%)!"
        } else {
            "Logged +${safeAmount} ml of $bevDisplayName! 💧 You're now at $newTotal ml ($newPct% of your daily goal)."
        }

        return GeminiAssistantAction(
            action = "LOG_WATER",
            amountMl = safeAmount,
            beverage = beverage,
            note = input.take(40),
            assistantReply = reply
        )
    }
}
