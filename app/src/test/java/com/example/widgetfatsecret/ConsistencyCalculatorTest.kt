package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.data.DayNutrition
import com.example.widgetfatsecret.fatsecret.domain.history.ConsistencyCalculator
import com.example.widgetfatsecret.fatsecret.domain.history.ConsistencyDayState
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsistencyCalculatorTest {

    private fun day(dateInt: Long) =
        DayNutrition(dateInt, calories = 2000.0, protein = 0.0, carbs = 0.0, fat = 0.0)

    @Test
    fun emptyHistoryIsAllNotSyncedWithZeroStreaks() {
        val s = ConsistencyCalculator.summarize(
            history = emptyList(),
            syncedDays = emptySet(),
            windowDays = 5,
            today = 104,
        )
        assertEquals(5, s.days.size)
        assertEquals(
            setOf(ConsistencyDayState.NOT_SYNCED),
            s.days.map { it.state }.toSet(),
        )
        assertEquals(0, s.daysRecorded)
        assertEquals(0, s.synchronizedDays)
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.longestStreak)
        assertEquals(0f, s.recordedPercent, 0.001f)
    }

    @Test
    fun futureDaysAreNeverMissingAndDoNotBreakTheCurrentStreak() {
        // A month-grid style window that extends past today.
        val s = ConsistencyCalculator.summarize(
            history = listOf(day(100), day(101)),
            syncedDays = (99L..103L).toSet(),
            windowStart = 99,
            windowEnd = 103,
            today = 101,
        )
        val byDate = s.days.associateBy { it.dateInt }
        assertEquals(ConsistencyDayState.NO_ENTRIES, byDate.getValue(99).state)
        assertEquals(ConsistencyDayState.RECORDED, byDate.getValue(100).state)
        assertEquals(ConsistencyDayState.RECORDED, byDate.getValue(101).state)
        assertEquals(ConsistencyDayState.FUTURE, byDate.getValue(102).state)
        assertEquals(ConsistencyDayState.FUTURE, byDate.getValue(103).state)
        assertEquals(2, s.currentStreak) // future days are skipped, not counted as breaks
    }

    @Test
    fun aGapInTheMiddleBreaksBothStreaksCorrectly() {
        // Recorded 96-97, missing 98, recorded 99-100.
        val history = listOf(day(96), day(97), day(99), day(100))
        val s = ConsistencyCalculator.summarize(
            history = history,
            syncedDays = (96L..100L).toSet(),
            windowDays = 5,
            today = 100,
        )
        assertEquals(2, s.currentStreak) // 99,100
        assertEquals(2, s.longestStreak) // either run is length 2
        assertEquals(4, s.daysRecorded)
    }

    @Test
    fun aMonthBoundaryDoesNotAffectStreakMath() {
        // Epoch days straddling a month rollover are just consecutive integers.
        val history = listOf(day(30), day(31), day(32))
        val s = ConsistencyCalculator.summarize(
            history = history,
            syncedDays = (30L..32L).toSet(),
            windowDays = 3,
            today = 32,
        )
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.longestStreak)
        assertEquals(1f, s.recordedPercent, 0.001f)
    }

    @Test
    fun fourCalendarStatesRemainDistinctInAPartialMonth() {
        val s = ConsistencyCalculator.summarize(
            history = listOf(day(101)),
            syncedDays = setOf(100L, 101L),
            windowStart = 100,
            windowEnd = 104,
            today = 103,
        )

        assertEquals(
            listOf(
                ConsistencyDayState.NO_ENTRIES,
                ConsistencyDayState.RECORDED,
                ConsistencyDayState.NOT_SYNCED,
                ConsistencyDayState.NOT_SYNCED,
                ConsistencyDayState.FUTURE,
            ),
            s.days.map { it.state },
        )
    }

    @Test
    fun unsyncedDaysStayOutsideTheThirtyDayPercentage() {
        val s = ConsistencyCalculator.summarize(
            history = listOf(day(98), day(100)),
            syncedDays = setOf(97L, 98L, 100L),
            windowDays = 5,
            today = 100,
        )

        assertEquals(2, s.daysRecorded)
        assertEquals(3, s.synchronizedDays)
        assertEquals(2f / 3f, s.recordedPercent, 0.001f)
    }
}
