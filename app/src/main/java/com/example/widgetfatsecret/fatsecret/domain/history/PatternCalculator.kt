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
) {
    val hasEnoughData: Boolean get() = daysRecorded >= MIN_RECORDED_DAYS

    companion object {
        const val MIN_RECORDED_DAYS = 4
    }
}

data class CycleAverage(
    val averageCalories: Double?,
    val daysRecorded: Int,
)

data class WeeklyCycleSummary(
    val weekdays: CycleAverage,
    val weekend: CycleAverage,
) {
    val differenceCalories: Double?
        get() = if (weekdays.averageCalories != null && weekend.averageCalories != null) {
            weekend.averageCalories - weekdays.averageCalories
        } else {
            null
        }

    val hasEnoughData: Boolean
        get() = weekdays.daysRecorded >= MIN_RECORDED_DAYS_PER_PART &&
            weekend.daysRecorded >= MIN_RECORDED_DAYS_PER_PART

    companion object {
        const val MIN_RECORDED_DAYS_PER_PART = 2
    }
}

enum class PatternMetric {
    CALORIES,
    PROTEIN,
    CARBS,
    FAT,
}

data class GoalFrequency(
    val metric: PatternMetric,
    val below: Int,
    val near: Int,
    val above: Int,
) {
    val daysRecorded: Int get() = below + near + above
    val outside: Int get() = below + above
}

/**
 * Pure grouping over a nutrition history for weekday patterns (e.g. "fins de
 * semana rodam mais alto"). Deliberately stops at the day-of-week level — no
 * per-food or micronutrient analysis, which is explicit negative scope
 * (planning.md §0).
 */
object PatternCalculator {

    fun summarize(history: List<DayNutrition>, windowDays: Int, today: Long): PatternSummary {
        val inWindow = history.inWindow(windowDays, today)
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

    /**
     * Compares the same 28-day history as two auditable parts of the weekly
     * cycle: Monday-Friday and Saturday-Sunday. Each average is weighted by
     * recorded days, never by weekday buckets, and missing days stay absent.
     */
    fun weeklyCycle(history: List<DayNutrition>, windowDays: Int, today: Long): WeeklyCycleSummary {
        val (weekendDays, weekdays) = history.inWindow(windowDays, today).partition { day ->
            LocalDate.ofEpochDay(day.dateInt).dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        }
        return WeeklyCycleSummary(
            weekdays = weekdays.toCycleAverage(),
            weekend = weekendDays.toCycleAverage(),
        )
    }

    /**
     * Counts recorded days below/near/above a local goal. `Near` is the same
     * explicit +/-5% band used by Tendencias; the caller can override it.
     * Non-positive goals intentionally produce no classification.
     */
    fun goalFrequency(
        history: List<DayNutrition>,
        windowDays: Int,
        today: Long,
        metric: PatternMetric,
        goal: Double,
        tolerance: Double = 0.05,
    ): GoalFrequency {
        if (goal <= 0.0) return GoalFrequency(metric, below = 0, near = 0, above = 0)

        val safeTolerance = tolerance.coerceAtLeast(0.0)
        val lower = goal * (1.0 - safeTolerance)
        val upper = goal * (1.0 + safeTolerance)
        var below = 0
        var near = 0
        var above = 0

        history.inWindow(windowDays, today).forEach { day ->
            val value = when (metric) {
                PatternMetric.CALORIES -> day.calories
                PatternMetric.PROTEIN -> day.protein
                PatternMetric.CARBS -> day.carbs
                PatternMetric.FAT -> day.fat
            }
            when {
                value < lower -> below++
                value > upper -> above++
                else -> near++
            }
        }
        return GoalFrequency(metric, below = below, near = near, above = above)
    }

    private fun List<DayNutrition>.inWindow(windowDays: Int, today: Long): List<DayNutrition> {
        if (windowDays <= 0) return emptyList()
        val windowStart = today - windowDays + 1
        return filter { it.dateInt in windowStart..today }
    }

    private fun List<DayNutrition>.toCycleAverage(): CycleAverage = CycleAverage(
        averageCalories = takeIf { it.isNotEmpty() }?.map { it.calories }?.average(),
        daysRecorded = size,
    )
}
