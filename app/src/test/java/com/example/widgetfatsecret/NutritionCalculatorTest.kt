package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.domain.DailyNutrition
import com.example.widgetfatsecret.fatsecret.domain.FoodEntry
import com.example.widgetfatsecret.fatsecret.domain.InsightType
import com.example.widgetfatsecret.fatsecret.domain.NutritionCalculator
import com.example.widgetfatsecret.fatsecret.domain.NutritionGoals
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionCalculatorTest {

    private val goals = NutritionGoals(caloriesKcal = 2000, proteinG = 150, carbsG = 200, fatG = 65)

    @Test
    fun sumsMultipleEntries() {
        val entries = listOf(
            FoodEntry(calories = 200.0, protein = 10.0, carbohydrate = 20.0, fat = 5.0),
            FoodEntry(calories = 300.5, protein = 15.5, carbohydrate = 10.0, fat = 2.0),
        )
        val daily = NutritionCalculator.sum(entries)
        assertEquals(500.5, daily.calories, 0.001)
        assertEquals(25.5, daily.protein, 0.001)
        assertEquals(30.0, daily.carbs, 0.001)
        assertEquals(7.0, daily.fat, 0.001)
        assertEquals(2, daily.entryCount)
    }

    @Test
    fun emptyListSumsToEmpty() {
        assertEquals(DailyNutrition.EMPTY, NutritionCalculator.sum(emptyList()))
    }

    @Test
    fun percentHandlesZeroGoal() {
        assertEquals(0.0, NutritionCalculator.percent(123.0, 0), 0.0)
    }

    @Test
    fun percentCanExceedOneHundred() {
        assertEquals(150.0, NutritionCalculator.percent(300.0, 200), 0.001)
    }

    @Test
    fun progressFractionClampsAtOne() {
        assertEquals(1.0f, NutritionCalculator.progressFraction(4000.0, 2000), 0.0001f)
        assertEquals(0.5f, NutritionCalculator.progressFraction(1000.0, 2000), 0.0001f)
        assertEquals(0.0f, NutritionCalculator.progressFraction(1000.0, 0), 0.0001f)
    }

    @Test
    fun insightIsNoEntriesWhenEmpty() {
        val insight = NutritionCalculator.buildInsight(DailyNutrition.EMPTY, goals)
        assertEquals(InsightType.NO_ENTRIES, insight.type)
    }

    @Test
    fun insightReportsFatOver() {
        val daily = DailyNutrition(calories = 1000.0, protein = 50.0, carbs = 100.0, fat = 80.0, entryCount = 3)
        val insight = NutritionCalculator.buildInsight(daily, goals)
        assertEquals(InsightType.FAT_OVER, insight.type)
        assertEquals(15.0, insight.value, 0.001) // 80 - 65
    }

    @Test
    fun insightReportsProteinGoalReached() {
        val daily = DailyNutrition(calories = 1000.0, protein = 160.0, carbs = 100.0, fat = 30.0, entryCount = 4)
        val insight = NutritionCalculator.buildInsight(daily, goals)
        assertEquals(InsightType.PROTEIN_GOAL_REACHED, insight.type)
    }

    @Test
    fun insightReportsProteinRemaining() {
        val daily = DailyNutrition(calories = 800.0, protein = 100.0, carbs = 50.0, fat = 20.0, entryCount = 2)
        val insight = NutritionCalculator.buildInsight(daily, goals)
        assertEquals(InsightType.PROTEIN_REMAINING, insight.type)
        assertEquals(50.0, insight.value, 0.001)
    }

    @Test
    fun insightFallsBackToPercentWhenGoalsMet() {
        val daily = DailyNutrition(calories = 2200.0, protein = 160.0, carbs = 210.0, fat = 60.0, entryCount = 5)
        val insight = NutritionCalculator.buildInsight(daily, goals)
        assertEquals(InsightType.PERCENT_OF_DAILY, insight.type)
        assertEquals(110.0, insight.value, 0.001)
    }

    @Test
    fun insightWithAllZeroGoalsDoesNotCrash() {
        val zeroGoals = NutritionGoals(0, 0, 0, 0)
        val daily = DailyNutrition(calories = 500.0, protein = 10.0, carbs = 10.0, fat = 10.0, entryCount = 1)
        val insight = NutritionCalculator.buildInsight(daily, zeroGoals)
        assertEquals(InsightType.PERCENT_OF_DAILY, insight.type)
        assertEquals(0.0, insight.value, 0.0)
    }
}
