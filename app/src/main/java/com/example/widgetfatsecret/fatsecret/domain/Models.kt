package com.example.widgetfatsecret.fatsecret.domain

/**
 * User-configured daily targets. These are plain, user-editable numbers — NOT
 * nutritional recommendations and NOT fetched from FatSecret.
 */
data class NutritionGoals(
    val caloriesKcal: Int = DEFAULT_CALORIES,
    val proteinG: Int = DEFAULT_PROTEIN,
    val carbsG: Int = DEFAULT_CARBS,
    val fatG: Int = DEFAULT_FAT,
) {
    companion object {
        const val DEFAULT_CALORIES = 2000
        const val DEFAULT_PROTEIN = 150
        const val DEFAULT_CARBS = 200
        const val DEFAULT_FAT = 65
        val DEFAULT = NutritionGoals()
    }
}

/** A single food diary entry (subset of FatSecret's food_entry we care about). */
data class FoodEntry(
    val name: String = "",
    val meal: String = "",
    val numberOfUnits: Double = 0.0,
    val servingDescription: String = "",
    val calories: Double = 0.0,
    val carbohydrate: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
)

/** Aggregated totals for a single day. */
data class DailyNutrition(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val entryCount: Int = 0,
) {
    companion object {
        val EMPTY = DailyNutrition()
    }
}

/** A short, locally-computed insight. Never a medical/diagnostic statement. */
enum class InsightType {
    NO_ENTRIES,
    FAT_OVER,
    PROTEIN_GOAL_REACHED,
    CARBS_NEAR_GOAL,
    PROTEIN_REMAINING,
    CALORIES_REMAINING,
    PERCENT_OF_DAILY,
}

data class Insight(val type: InsightType, val value: Double = 0.0)

/** Calorie total for one meal slot (breakfast/lunch/dinner/other), today only. */
data class MealTotal(val meal: String, val calories: Double)

/** How much of today's calories the single largest meal accounts for. */
data class MealShareInsight(val meal: String, val percent: Double)
