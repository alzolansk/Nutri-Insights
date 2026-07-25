package com.example.widgetfatsecret.fatsecret.domain.history

import com.example.widgetfatsecret.fatsecret.data.DayNutrition

/** One day's calories in a trend window, or `null` when the day has no record. */
data class TrendDay(val dateInt: Long, val calories: Double?)

data class TrendSummary(
    val windowDays: Int,
    val daysRecorded: Int,
    val averageCalories: Double?,
    val previousAverageCalories: Double?,
    val changeVsPreviousCalories: Double?,
    /** One entry per calendar day in the window, oldest first — gaps included. */
    val days: List<TrendDay>,
) {
    /** Below this, planning.md's rule is "dados insuficientes", not a summary. */
    val hasEnoughData: Boolean get() = daysRecorded >= MIN_RECORDED_DAYS

    companion object {
        const val MIN_RECORDED_DAYS = 4
    }
}

/** Where a recorded day's calories fell relative to the goal, within [distribution]'s tolerance band. */
enum class GoalBand { ABOVE, NEAR, BELOW }

data class CalorieDistribution(val above: Int, val near: Int, val below: Int) {
    val recordedTotal: Int get() = above + near + below
}

/**
 * Pure aggregation over a nutrition history (no Android, no I/O). A day
 * without a record is an absence, never a zero: it is excluded from every
 * average and carried into [TrendSummary.days] as a `null`, so the UI can
 * render it as a gap instead of a dip.
 */
object TrendCalculator {

    /**
     * Buckets recorded [days] as above / near / below [goalCalories], within a
     * [tolerance] fraction (default 5%) counted as "near". Days without a
     * record and a non-positive goal are excluded, never forced into a bucket.
     */
    fun distribution(days: List<TrendDay>, goalCalories: Double, tolerance: Double = 0.05): CalorieDistribution {
        if (goalCalories <= 0.0) return CalorieDistribution(0, 0, 0)
        var above = 0
        var near = 0
        var below = 0
        for (day in days) {
            val calories = day.calories ?: continue
            when {
                calories > goalCalories * (1 + tolerance) -> above++
                calories < goalCalories * (1 - tolerance) -> below++
                else -> near++
            }
        }
        return CalorieDistribution(above, near, below)
    }

    fun summarize(history: List<DayNutrition>, windowDays: Int, today: Long): TrendSummary {
        val byDay = history.associateBy { it.dateInt }
        val windowStart = today - windowDays + 1
        val days = (windowStart..today).map { d -> TrendDay(d, byDay[d]?.calories) }
        val recorded = days.mapNotNull { it.calories }

        val previousStart = windowStart - windowDays
        val previousEnd = windowStart - 1
        val previousRecorded = (previousStart..previousEnd).mapNotNull { byDay[it]?.calories }

        val average = recorded.takeIf { it.isNotEmpty() }?.average()
        val previousAverage = previousRecorded.takeIf { it.isNotEmpty() }?.average()
        val change = if (average != null && previousAverage != null) average - previousAverage else null

        return TrendSummary(
            windowDays = windowDays,
            daysRecorded = recorded.size,
            averageCalories = average,
            previousAverageCalories = previousAverage,
            changeVsPreviousCalories = change,
            days = days,
        )
    }
}
