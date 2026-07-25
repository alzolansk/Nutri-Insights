package com.example.widgetfatsecret.fatsecret.domain.history

import com.example.widgetfatsecret.fatsecret.data.DayNutrition

enum class ConsistencyDayState { RECORDED, NO_ENTRIES, NOT_SYNCED, FUTURE }

data class ConsistencyDay(val dateInt: Long, val state: ConsistencyDayState)

data class ConsistencySummary(
    val windowDays: Int,
    /** One entry per calendar day in the window, oldest first. */
    val days: List<ConsistencyDay>,
    val daysRecorded: Int,
    val synchronizedDays: Int,
    val currentStreak: Int,
    val longestStreak: Int,
) {
    val recordedPercent: Float
        get() = if (synchronizedDays == 0) 0f else daysRecorded.toFloat() / synchronizedDays
}

/**
 * Pure calendar aggregation over a nutrition history. [windowEnd] may be
 * later than [today] — a month grid shows the whole month, including days
 * that haven't happened yet — and any such day is [ConsistencyDayState.FUTURE],
 * never [ConsistencyDayState.NO_ENTRIES]: it must not count against a streak or
 * a percentage just because it hasn't happened.
 */
object ConsistencyCalculator {

    /** Convenience overload for a rolling window of [windowDays] ending today. */
    fun summarize(
        history: List<DayNutrition>,
        syncedDays: Set<Long>,
        windowDays: Int,
        today: Long,
    ): ConsistencySummary = summarize(
        history = history,
        syncedDays = syncedDays,
        windowStart = today - windowDays + 1,
        windowEnd = today,
        today = today,
    )

    fun summarize(
        history: List<DayNutrition>,
        syncedDays: Set<Long>,
        windowStart: Long,
        windowEnd: Long,
        today: Long,
    ): ConsistencySummary {
        require(windowEnd >= windowStart) { "windowEnd must not precede windowStart" }
        val recordedDays = history.map { it.dateInt }.toSet()
        val windowDays = (windowEnd - windowStart + 1).toInt()
        val days = (windowStart..windowEnd).map { d ->
            val state = when {
                d > today -> ConsistencyDayState.FUTURE
                d in recordedDays -> ConsistencyDayState.RECORDED
                d in syncedDays -> ConsistencyDayState.NO_ENTRIES
                else -> ConsistencyDayState.NOT_SYNCED
            }
            ConsistencyDay(d, state)
        }

        var currentStreak = 0
        for (day in days.reversed()) {
            if (day.state == ConsistencyDayState.FUTURE) continue
            if (day.state == ConsistencyDayState.RECORDED) currentStreak++ else break
        }

        var longestStreak = 0
        var running = 0
        for (day in days) {
            when (day.state) {
                ConsistencyDayState.RECORDED -> {
                    running++
                    longestStreak = maxOf(longestStreak, running)
                }
                ConsistencyDayState.NO_ENTRIES,
                ConsistencyDayState.NOT_SYNCED,
                -> running = 0
                ConsistencyDayState.FUTURE -> Unit
            }
        }

        return ConsistencySummary(
            windowDays = windowDays,
            days = days,
            daysRecorded = days.count { it.state == ConsistencyDayState.RECORDED },
            synchronizedDays = days.count {
                it.state == ConsistencyDayState.RECORDED || it.state == ConsistencyDayState.NO_ENTRIES
            },
            currentStreak = currentStreak,
            longestStreak = longestStreak,
        )
    }
}
