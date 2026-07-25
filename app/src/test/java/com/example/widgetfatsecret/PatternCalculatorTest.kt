package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.data.DayNutrition
import com.example.widgetfatsecret.fatsecret.domain.history.PatternCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatternCalculatorTest {

    private fun day(dateInt: Long, calories: Double) =
        DayNutrition(dateInt, calories, protein = 0.0, carbs = 0.0, fat = 0.0)

    @Test
    fun emptyHistoryYieldsSevenWeekdaysWithNoAverage() {
        val s = PatternCalculator.summarize(emptyList(), windowDays = 28, today = 100)
        assertEquals(7, s.byWeekday.size)
        assertEquals(0, s.daysRecorded)
        s.byWeekday.forEach { assertNull(it.averageCalories) }
    }

    @Test
    fun groupsCaloriesByDayOfWeekWithinTheWindow() {
        // Two epoch days that land on the same weekday, one week apart.
        val base = 20000L
        val baseDow = LocalDate.ofEpochDay(base).dayOfWeek
        val history = listOf(day(base, 2000.0), day(base + 7, 2200.0), day(base + 1, 1800.0))

        val s = PatternCalculator.summarize(history, windowDays = 10, today = base + 7)

        val grouped = s.byWeekday.first { it.dayOfWeek == baseDow }
        assertEquals(2, grouped.daysRecorded)
        assertEquals(2100.0, grouped.averageCalories!!, 0.001) // (2000+2200)/2

        val nextDow = LocalDate.ofEpochDay(base + 1).dayOfWeek
        val single = s.byWeekday.first { it.dayOfWeek == nextDow }
        assertEquals(1, single.daysRecorded)
        assertEquals(1800.0, single.averageCalories!!, 0.001)
    }

    @Test
    fun daysOutsideTheWindowAreExcluded() {
        val history = listOf(day(50, 2000.0), day(100, 2200.0))
        val s = PatternCalculator.summarize(history, windowDays = 7, today = 100)
        assertEquals(1, s.daysRecorded) // day 50 is far outside a 7-day window ending at 100
    }
}
