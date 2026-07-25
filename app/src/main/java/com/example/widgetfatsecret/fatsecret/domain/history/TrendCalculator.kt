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

/**
 * Pure aggregation over a nutrition history (no Android, no I/O). A day
 * without a record is an absence, never a zero: it is excluded from every
 * average and carried into [TrendSummary.days] as a `null`, so the UI can
 * render it as a gap instead of a dip.
 */
object TrendCalculator {

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
