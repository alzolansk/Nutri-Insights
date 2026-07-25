package com.example.widgetfatsecret.fatsecret.domain

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** One weighing, as returned by `weights.get_month.v2` (date is an epoch-day). */
data class WeightEntry(val dateInt: Long, val weightKg: Double)

/**
 * The account profile fields relevant to weight, from `profile.get`.
 *
 * [goalWeightKg] is null when the user has not set a goal (the API sends 0 or
 * omits it), which the widget renders as an explicit "sem meta" state rather
 * than as a goal of zero.
 */
data class WeightProfile(
    val goalWeightKg: Double? = null,
    val lastWeightKg: Double? = null,
    val lastWeightDateInt: Long? = null,
    /** The unit the user reads weights in; `weight_kg` is always metric. */
    val usesPounds: Boolean = false,
) {
    companion object { val EMPTY = WeightProfile() }
}

/** Direction of travel. Deliberately neutral: no value judgement attached. */
enum class WeightTrend { LOSING, GAINING, STABLE, UNKNOWN }

/**
 * Everything the weight widget renders, derived once so the widget itself stays
 * free of arithmetic. Every field is nullable exactly where the underlying data
 * may be missing, so the widget can branch on "no records" / "no goal" / "only
 * one weighing" without inventing numbers.
 */
data class WeightStats(
    val latest: WeightEntry? = null,
    val previous: WeightEntry? = null,
    /**
     * The oldest weighing the app knows about — the discovered all-time baseline
     * when one has been persisted, otherwise the oldest one inside the fetched
     * window. This is the anchor for [totalDelta] and [goalProgress].
     */
    val first: WeightEntry? = null,
    /** Oldest weighing inside the fetched window; anchors the recent rate. */
    val windowFirst: WeightEntry? = null,
    /** Change against the previous weighing, in kg. Null with fewer than two. */
    val deltaFromPrevious: Double? = null,
    /** Change since [first], in kg. Null when only one weighing is known. */
    val totalDelta: Double? = null,
    /** Average change per week over the fetched window, in kg. */
    val weeklyAverage: Double? = null,
    val trend: WeightTrend = WeightTrend.UNKNOWN,
    val goalKg: Double? = null,
    /** 0f..1f from the first weighing towards the goal. Null without a goal. */
    val goalProgress: Float? = null,
    /** Signed kg still to go to reach the goal. Null without a goal. */
    val remainingToGoal: Double? = null,
    val daysSinceLast: Int? = null,
    /** True when the last change moved closer to the goal. Null without a goal. */
    val movingTowardGoal: Boolean? = null,
) {
    val hasData: Boolean get() = latest != null
    val hasHistory: Boolean get() = deltaFromPrevious != null
}

/**
 * Pure arithmetic over a weight history. No Android, no locale, no I/O — the
 * same split [NutritionCalculator] uses, so all the edge cases (empty history, a
 * single weighing, a missing goal, a goal already reached) are unit-testable.
 */
object WeightCalculator {

    /**
     * Below this many kg/week the trend reads as "mantendo". Day-to-day scale
     * noise is easily a few hundred grams, so a deadband keeps the widget from
     * flip-flopping between "perdendo" and "ganhando" on water weight alone.
     */
    private const val STABLE_BAND_KG_PER_WEEK = 0.1

    /**
     * Builds the stats. [entries] may arrive unsorted and with duplicate days;
     * [profile]'s last weighing is folded in so a user whose only record predates
     * the fetched window still gets a current weight instead of an empty state.
     *
     * [baseline] is the all-time first weighing, discovered separately because
     * [entries] only covers a recent window. Without it "total" and "progress"
     * would silently mean "since the window started", which is a much smaller
     * (and, to the user, plainly wrong) number. It is ignored when it is not
     * actually older than what the window holds.
     *
     * [startOverrideKg] is the user's manually entered starting weight and wins
     * over [baseline]: FatSecret's own "Peso Inicial" is not necessarily the
     * oldest logged weighing, and `profile.get` does not expose it, so walking
     * the diary alone cannot reproduce the number shown in that app.
     */
    fun stats(
        entries: List<WeightEntry>,
        profile: WeightProfile,
        today: Long,
        baseline: WeightEntry? = null,
        startOverrideKg: Double? = null,
    ): WeightStats {
        val merged = merge(entries, profile)
        if (merged.isEmpty()) {
            return WeightStats(goalKg = profile.goalWeightKg, first = baseline)
        }

        val latest = merged.last()
        val windowFirst = merged.first()
        val discovered = baseline?.takeIf { it.dateInt < windowFirst.dateInt } ?: windowFirst
        val override = startOverrideKg?.takeIf { it > 0.0 }
        // The override keeps the discovered date: only the weight is disputed,
        // and no date is stored alongside a manually typed starting weight.
        val first = override?.let { WeightEntry(discovered.dateInt, it) } ?: discovered
        val previous = merged.getOrNull(merged.size - 2)
        val goal = profile.goalWeightKg

        val deltaFromPrevious = previous?.let { latest.weightKg - it.weightKg }
        // Anchored on the starting weight when we have one, so "total" means
        // what the FatSecret app calls "perdeu até agora".
        val totalDelta = if (override != null || first.dateInt != latest.dateInt) {
            latest.weightKg - first.weightKg
        } else {
            null
        }

        // The rate stays deliberately window-scoped: it answers "how fast am I
        // moving lately", so it must not be diluted by a year of older history.
        // Measured over elapsed days, not the number of weighings, so an
        // irregular logging cadence does not distort it.
        val spanDays = latest.dateInt - windowFirst.dateInt
        val weeklyAverage = if (merged.size >= 2 && spanDays > 0) {
            (latest.weightKg - windowFirst.weightKg) / spanDays * 7.0
        } else {
            null
        }

        val trend = when {
            weeklyAverage == null -> WeightTrend.UNKNOWN
            abs(weeklyAverage) < STABLE_BAND_KG_PER_WEEK -> WeightTrend.STABLE
            weeklyAverage < 0 -> WeightTrend.LOSING
            else -> WeightTrend.GAINING
        }

        // Progress runs from the first known weighing towards the goal, so it
        // works identically whether the goal is below (cutting) or above
        // (gaining) the starting weight — both numerator and denominator flip.
        val goalProgress = if (goal != null && first.weightKg != goal) {
            ((first.weightKg - latest.weightKg) / (first.weightKg - goal))
                .toFloat().coerceIn(0f, 1f)
        } else if (goal != null) {
            1f // started exactly at the goal
        } else {
            null
        }

        val movingTowardGoal = if (goal != null && previous != null) {
            abs(latest.weightKg - goal) < abs(previous.weightKg - goal)
        } else {
            null
        }

        return WeightStats(
            latest = latest,
            previous = previous,
            first = first,
            windowFirst = windowFirst,
            deltaFromPrevious = deltaFromPrevious,
            totalDelta = totalDelta,
            weeklyAverage = weeklyAverage,
            trend = trend,
            goalKg = goal,
            goalProgress = goalProgress,
            remainingToGoal = goal?.let { latest.weightKg - it },
            daysSinceLast = (today - latest.dateInt).toInt().coerceAtLeast(0),
            movingTowardGoal = movingTowardGoal,
        )
    }

    /**
     * Sorted ascending, one entry per day (latest wins), with the profile's last
     * weighing folded in when the history does not already cover that day.
     */
    private fun merge(entries: List<WeightEntry>, profile: WeightProfile): List<WeightEntry> {
        val byDay = LinkedHashMap<Long, Double>()
        entries.forEach { byDay[it.dateInt] = it.weightKg }
        val pd = profile.lastWeightDateInt
        val pw = profile.lastWeightKg
        if (pd != null && pw != null && pw > 0.0 && !byDay.containsKey(pd)) {
            byDay[pd] = pw
        }
        return byDay.entries.sortedBy { it.key }.map { WeightEntry(it.key, it.value) }
    }
}

/** pt-BR presentation for weights. Locale concerns stay out of the calculator. */
object WeightFormat {

    private const val KG_PER_LB = 2.2046226218
    private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

    private val oneDecimal: NumberFormat = NumberFormat.getNumberInstance(ptBr).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }

    fun unit(usesPounds: Boolean): String = if (usesPounds) "lb" else "kg"

    private fun convert(kg: Double, usesPounds: Boolean): Double =
        if (usesPounds) kg * KG_PER_LB else kg

    /** "104,9 kg" */
    fun weight(kg: Double, usesPounds: Boolean): String =
        "${oneDecimal.format(convert(kg, usesPounds))} ${unit(usesPounds)}"

    /** Just the number, for the hero line where the unit is drawn separately. */
    fun weightValue(kg: Double, usesPounds: Boolean): String =
        oneDecimal.format(convert(kg, usesPounds))

    /**
     * Always sign-prefixed, e.g. "-0,7 kg" / "+0,3 kg" / "0,0 kg". The sign is
     * the whole point of a delta, so it is never dropped for small values.
     */
    fun delta(kg: Double, usesPounds: Boolean): String {
        val v = convert(kg, usesPounds)
        val sign = if (v > 0) "+" else if (v < 0) "−" else ""
        return "$sign${oneDecimal.format(abs(v))} ${unit(usesPounds)}"
    }

    /** "−0,4 kg/sem" */
    fun perWeek(kg: Double, usesPounds: Boolean): String =
        "${delta(kg, usesPounds)}/sem"

    /**
     * Neutral, factual phrasing — the widget describes what the numbers do, it
     * does not praise or admonish the person reading it.
     */
    fun trendLabel(trend: WeightTrend): String = when (trend) {
        WeightTrend.LOSING -> "Perdendo"
        WeightTrend.GAINING -> "Ganhando"
        WeightTrend.STABLE -> "Mantendo"
        WeightTrend.UNKNOWN -> "—"
    }

    /** A discreet direction glyph; no icon assets, so it never fails to load. */
    fun trendArrow(trend: WeightTrend): String = when (trend) {
        WeightTrend.LOSING -> "↓"
        WeightTrend.GAINING -> "↑"
        WeightTrend.STABLE -> "→"
        WeightTrend.UNKNOWN -> "·"
    }

    /** "hoje" / "ontem" / "há 5 dias". */
    fun sinceLast(days: Int): String = when {
        days <= 0 -> "hoje"
        days == 1 -> "ontem"
        else -> "há $days dias"
    }
}
