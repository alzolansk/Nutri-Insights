package com.example.widgetfatsecret.fatsecret.domain

/**
 * Pure calculations over daily nutrition — summing, percentages and insight
 * selection. No Android dependencies, fully unit-testable.
 *
 * Insights are arithmetic only: they never make medical judgements or
 * recommendations, they only restate the relationship between what was consumed
 * and the user's own configured goals.
 */
object NutritionCalculator {

    /** Sums every entry into a single [DailyNutrition]. Empty list -> EMPTY. */
    fun sum(entries: List<FoodEntry>): DailyNutrition {
        if (entries.isEmpty()) return DailyNutrition.EMPTY
        var cal = 0.0
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0
        for (e in entries) {
            cal += e.calories
            protein += e.protein
            carbs += e.carbohydrate
            fat += e.fat
        }
        return DailyNutrition(
            calories = cal,
            protein = protein,
            carbs = carbs,
            fat = fat,
            entryCount = entries.size,
        )
    }

    /**
     * Percentage of a goal reached (can exceed 100). Returns 0 when the goal is
     * 0 or negative so a mis-configured goal never divides by zero.
     */
    fun percent(consumed: Double, goal: Int): Double =
        if (goal <= 0) 0.0 else consumed / goal * 100.0

    /** Progress fraction clamped to [0,1] for progress bars. */
    fun progressFraction(consumed: Double, goal: Int): Float =
        if (goal <= 0) 0f else (consumed / goal).coerceIn(0.0, 1.0).toFloat()

    /** Remaining amount toward a goal (negative means over the goal). */
    fun remaining(consumed: Double, goal: Int): Double = goal - consumed

    /**
     * Picks exactly ONE insight, by priority:
     *
     *  1. no entries at all;
     *  2. fat over the goal (a notable overshoot);
     *  3. already at/over the calorie budget -> show the % of the daily goal;
     *  4. protein goal reached (a positive milestone, while still under calories);
     *  5. carbs near the goal (within 90–100%);
     *  6. protein still missing;
     *  7. calories still remaining (when no protein goal is in play);
     *  8. neutral percent-of-day fallback (e.g. all goals set to zero).
     *
     * Every branch guards against a zero/negative goal so a mis-configured goal
     * never divides by zero and never produces a misleading message.
     */
    fun buildInsight(daily: DailyNutrition, goals: NutritionGoals): Insight {
        if (daily.entryCount == 0) return Insight(InsightType.NO_ENTRIES)

        if (goals.fatG > 0 && daily.fat > goals.fatG) {
            return Insight(InsightType.FAT_OVER, daily.fat - goals.fatG)
        }
        if (goals.caloriesKcal > 0 && daily.calories >= goals.caloriesKcal) {
            return Insight(InsightType.PERCENT_OF_DAILY, percent(daily.calories, goals.caloriesKcal))
        }
        if (goals.proteinG > 0 && daily.protein >= goals.proteinG) {
            return Insight(InsightType.PROTEIN_GOAL_REACHED)
        }
        if (goals.carbsG > 0) {
            val pct = daily.carbs / goals.carbsG
            if (pct in 0.9..1.0) return Insight(InsightType.CARBS_NEAR_GOAL)
        }
        if (goals.proteinG > 0 && daily.protein < goals.proteinG) {
            return Insight(InsightType.PROTEIN_REMAINING, goals.proteinG - daily.protein)
        }
        if (goals.caloriesKcal > 0 && daily.calories < goals.caloriesKcal) {
            return Insight(InsightType.CALORIES_REMAINING, goals.caloriesKcal - daily.calories)
        }
        return Insight(InsightType.PERCENT_OF_DAILY, percent(daily.calories, goals.caloriesKcal))
    }
}
