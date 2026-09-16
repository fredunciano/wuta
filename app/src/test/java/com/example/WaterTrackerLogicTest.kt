package com.example

import com.example.data.gemini.GeminiAssistantService
import com.example.data.model.BeverageType
import com.example.data.model.HydrationSummary
import com.example.ui.util.ShareUtil
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterTrackerLogicTest {

    @Test
    fun beverageEffectiveHydrationCalculation() {
        val water = BeverageType.WATER
        val coffee = BeverageType.COFFEE
        val electrolyte = BeverageType.ELECTROLYTE

        assertEquals(250, (250 * water.hydrationFactor).toInt())
        assertEquals(200, (250 * coffee.hydrationFactor).toInt())
        assertEquals(262, (250 * electrolyte.hydrationFactor).toInt())
    }

    @Test
    fun shareMessageGenerationFormat() {
        val summary = HydrationSummary(
            todayTotalMl = 2000,
            todayEffectiveMl = 2000,
            dailyTargetMl = 2500,
            currentStreakDays = 5,
            bestStreakDays = 12
        )

        val message = ShareUtil.generateShareMessage(summary)
        assertTrue(message.contains("2000 / 2500 ml"))
        assertTrue(message.contains("80%"))
        assertTrue(message.contains("Streak: 5 days"))
        assertTrue(message.contains("#WaterTracker"))
    }

    @Test
    fun geminiAssistantNaturalLanguageParsingFallback() = runBlocking {
        val service = GeminiAssistantService()

        // 1. Test "i drunk a glass of water"
        val action1 = service.parseWithLocalNlp("i drunk a glass of water", 1000, 2500, 3)
        assertEquals("LOG_WATER", action1.action)
        assertEquals(250, action1.amountMl)
        assertEquals("WATER", action1.beverageType)
        assertTrue(action1.assistantReply.contains("250ml"))

        // 2. Test "had 500ml water after workout"
        val action2 = service.parseWithLocalNlp("had 500ml water after workout", 1250, 2500, 3)
        assertEquals("LOG_WATER", action2.action)
        assertEquals(500, action2.amountMl)

        // 3. Test "drank a cup of green tea"
        val action3 = service.parseWithLocalNlp("drank a cup of green tea", 1750, 2500, 3)
        assertEquals("LOG_WATER", action3.action)
        assertEquals(200, action3.amountMl)
        assertEquals("TEA", action3.beverageType)

        // 4. Test "how much water have i drank today?"
        val action4 = service.parseWithLocalNlp("how much water have i drank today?", 1950, 2500, 3)
        assertEquals("QUERY", action4.action)
        assertTrue(action4.assistantReply.contains("1950ml"))

        // 5. Test Google Assistant typical phrase: "Log a glass of water"
        val action5 = service.parseWithLocalNlp("log a glass of water", 500, 2500, 1)
        assertEquals("LOG_WATER", action5.action)
        assertEquals(250, action5.amountMl)
        assertEquals("WATER", action5.beverageType)

        // 6. Test Google Assistant typical phrase: "Log 750ml water bottle"
        val action6 = service.parseWithLocalNlp("log 750ml water bottle", 750, 2500, 1)
        assertEquals("LOG_WATER", action6.action)
        assertEquals(750, action6.amountMl)
    }
}
