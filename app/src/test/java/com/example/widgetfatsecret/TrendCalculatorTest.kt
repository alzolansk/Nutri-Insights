package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.data.DayNutrition
import com.example.widgetfatsecret.fatsecret.domain.history.TrendCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendCalculatorTest {

    private fun day(dateInt: Long, calories: Double) =
        DayNutrition(dateInt, calories, protein = 0.0, carbs = 0.0, fat = 0.0)

    @Test
    fun emptyHistoryYieldsNoAverageAndAllGaps() {
        val s = TrendCalculator.summarize(emptyList(), windowDays = 7, today = 100)
        assertEquals(0, s.daysRecorded)
        assertNull(s.averageCalories)
        assertNull(s.previousAverageCalories)
        assertNull(s.changeVsPreviousCalories)
        assertEquals(7, s.days.size)
        assertTrue(s.days.all { it.calories == null })
        assertFalse(s.hasEnoughData)
    }

    @Test
    fun partialWindowAveragesOnlyRecordedDaysAndKeepsGapsAsNull() {
        val history = listOf(day(96, 2000.0), day(98, 1800.0), day(100, 2200.0))
        val s = TrendCalculator.summarize(history, windowDays = 7, today = 100)

        assertEquals(3, s.daysRecorded)
        assertEquals(2000.0, s.averageCalories!!, 0.001) // (2000+1800+2200)/3, not /7
        assertNull(s.days.first { it.dateInt == 97L }.calories)
        assertEquals(2000.0, s.days.first { it.dateInt == 96L }.calories!!, 0.001)
    }

    @Test
    fun changeVsPreviousComparesAdjacentNonOverlappingWindows() {
        // Previous window: days 90-96 avg 2000; current window: days 97-100+ avg 2400.
        val history = listOf(
            day(90, 2000.0), day(93, 2000.0), day(96, 2000.0),
            day(97, 2400.0), day(99, 2400.0), day(100, 2400.0),
        )
        val s = TrendCalculator.summarize(history, windowDays = 4, today = 100)
        assertEquals(2400.0, s.averageCalories!!, 0.001)
        assertEquals(2000.0, s.previousAverageCalories!!, 0.001)
        assertEquals(400.0, s.changeVsPreviousCalories!!, 0.001)
    }

    @Test
    fun fewerThanFourRecordedDaysIsInsufficientData() {
        val s = TrendCalculator.summarize(listOf(day(98, 2000.0), day(100, 2000.0)), windowDays = 7, today = 100)
        assertFalse(s.hasEnoughData)
    }
}
