package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.domain.Insight
import com.example.widgetfatsecret.fatsecret.domain.InsightType
import com.example.widgetfatsecret.fatsecret.domain.MealShareInsight
import com.example.widgetfatsecret.fatsecret.domain.NutritionFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionFormatTest {

    @Test
    fun formatsIntegerWithoutDecimals() {
        // Rounding, grouping separator may vary by locale data; digits must match.
        assertEquals("1420", NutritionFormat.int(1420.4).filter { it.isDigit() })
        assertEquals("2000", NutritionFormat.int(2000).filter { it.isDigit() })
    }

    @Test
    fun percentTextRoundsToWholePercent() {
        assertEquals("71%", NutritionFormat.percentText(1420.0, 2000))
        assertEquals("150%", NutritionFormat.percentText(300.0, 200))
        assertEquals("0%", NutritionFormat.percentText(100.0, 0))
    }

    @Test
    fun insightTextForEachType() {
        assertEquals(
            "Nenhum alimento registrado hoje",
            NutritionFormat.insightText(Insight(InsightType.NO_ENTRIES)),
        )
        assertEquals(
            "Meta de proteína atingida",
            NutritionFormat.insightText(Insight(InsightType.PROTEIN_GOAL_REACHED)),
        )
        assertEquals(
            "Carboidratos próximos da meta",
            NutritionFormat.insightText(Insight(InsightType.CARBS_NEAR_GOAL)),
        )
        assertEquals(
            "Você consumiu 110% da meta diária",
            NutritionFormat.insightText(Insight(InsightType.PERCENT_OF_DAILY, 110.0)),
        )
    }

    @Test
    fun mealLabelTranslatesKnownFatSecretMeals() {
        assertEquals("Café da manhã", NutritionFormat.mealLabel("Breakfast"))
        assertEquals("Almoço", NutritionFormat.mealLabel("Lunch"))
        assertEquals("Jantar", NutritionFormat.mealLabel("Dinner"))
        assertEquals("Lanches", NutritionFormat.mealLabel("Other"))
    }

    @Test
    fun mealLabelPassesThroughUnknownValues() {
        assertEquals("Ceia", NutritionFormat.mealLabel("Ceia"))
    }

    @Test
    fun mealShareTextDescribesDominantMeal() {
        assertEquals(
            "Jantar concentra 43% das calorias de hoje",
            NutritionFormat.mealShareText(MealShareInsight("Dinner", 43.0)),
        )
    }

    @Test
    fun timeAgoBucketsElapsedTime() {
        val now = 1_000_000_000L
        assertEquals("—", NutritionFormat.timeAgo(0L, now))
        assertEquals("agora", NutritionFormat.timeAgo(now - 30_000L, now))
        assertEquals("há 5 min", NutritionFormat.timeAgo(now - 5 * 60_000L, now))
        assertEquals("há 2 h", NutritionFormat.timeAgo(now - 2 * 60 * 60_000L, now))
        assertEquals("há 3 d", NutritionFormat.timeAgo(now - 3 * 24 * 60 * 60_000L, now))
    }
}
