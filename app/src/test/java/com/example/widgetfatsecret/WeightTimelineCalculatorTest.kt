package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.data.DayNutrition
import com.example.widgetfatsecret.fatsecret.domain.WeightEntry
import com.example.widgetfatsecret.ui.weight.WeightTimelineCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightTimelineCalculatorTest {

    @Test
    fun alignsThirtyCalendarDaysWithoutTurningAbsenceIntoZero() {
        val result = WeightTimelineCalculator.align(
            weights = listOf(WeightEntry(81, 80.0), WeightEntry(100, 78.0)),
            nutrition = listOf(DayNutrition(100, 1800.0, 0.0, 0.0, 0.0)),
            today = 100,
        )

        assertEquals(30, result.size)
        assertEquals(71L, result.first().dateInt)
        assertEquals(100L, result.last().dateInt)
        assertNull(result.first().weightKg)
        assertNull(result.first().calories)
        assertEquals(78.0, result.last().weightKg!!, 0.001)
        assertEquals(1800.0, result.last().calories!!, 0.001)
    }

    @Test
    fun movingAverageUsesOnlyWeighingsInPreviousSevenCalendarDays() {
        val result = WeightTimelineCalculator.align(
            weights = listOf(
                WeightEntry(90, 90.0),
                WeightEntry(94, 88.0),
                WeightEntry(100, 86.0),
            ),
            nutrition = emptyList(),
            today = 100,
        )

        assertEquals(90.0, result.first { it.dateInt == 90L }.movingAverageKg!!, 0.001)
        assertEquals(89.0, result.first { it.dateInt == 94L }.movingAverageKg!!, 0.001)
        // Day 90 is outside 94..100; only days 94 and 100 enter the average.
        assertEquals(87.0, result.first { it.dateInt == 100L }.movingAverageKg!!, 0.001)
        assertNull(result.first { it.dateInt == 99L }.movingAverageKg)
    }

    @Test
    fun ignoresRecordsOutsideWindowAndHandlesNonPositiveWindow() {
        val result = WeightTimelineCalculator.align(
            weights = listOf(WeightEntry(70, 95.0), WeightEntry(100, 85.0)),
            nutrition = listOf(DayNutrition(70, 2000.0, 0.0, 0.0, 0.0)),
            today = 100,
            windowDays = 7,
        )

        assertEquals(7, result.size)
        assertEquals(1, result.count { it.weightKg != null })
        assertEquals(0, result.count { it.calories != null })
        assertEquals(
            emptyList<Any>(),
            WeightTimelineCalculator.align(emptyList(), emptyList(), today = 100, windowDays = 0),
        )
    }
}
