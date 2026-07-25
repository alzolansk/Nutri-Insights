package com.example.widgetfatsecret.fatsecret.domain.history

import com.example.widgetfatsecret.fatsecret.data.DayNutrition
import java.time.DayOfWeek
import java.time.LocalDate

data class WeekdayAverage(
    val dayOfWeek: DayOfWeek,
    val averageCalories: Double?,
    val daysRecorded: Int,
)

data class PatternSummary(
    val windowDays: Int,
    val daysRecorded: Int,
    /** Always 7 entries, Monday through Sunday, regardless of what was recorded. */
    val byWeekday: List<WeekdayAverage>,
)

/**
 * Pure grouping over a nutrition history for weekday patterns (e.g. "fins de
 * semana rodam mais alto"). Deliberately stops at the day-of-week level — no
 * per-food or micronutrient analysis, which is explicit negative scope
 * (planning.md §0).
 */
object PatternCalculator {

    fun summarize(history: List<DayNutrition>, windowDays: Int, today: Long): PatternSummary {
        val windowStart = today - windowDays + 1
        val inWindow = history.filter { it.dateInt in windowStart..today }
        val byWeekday = DayOfWeek.entries.map { dow ->
            val calories = inWindow
                .filter { LocalDate.ofEpochDay(it.dateInt).dayOfWeek == dow }
                .map { it.calories }
            WeekdayAverage(
                dayOfWeek = dow,
                averageCalories = calories.takeIf { it.isNotEmpty() }?.average(),
                daysRecorded = calories.size,
            )
        }
        return PatternSummary(
            windowDays = windowDays,
            daysRecorded = inWindow.size,
            byWeekday = byWeekday,
        )
    }
}
