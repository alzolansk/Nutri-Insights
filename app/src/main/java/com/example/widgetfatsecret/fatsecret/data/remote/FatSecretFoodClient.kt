package com.example.widgetfatsecret.fatsecret.data.remote

import com.example.widgetfatsecret.fatsecret.data.DayNutrition
import com.example.widgetfatsecret.fatsecret.data.FatSecretJson
import com.example.widgetfatsecret.fatsecret.domain.FoodEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the food diary using the method-based Platform API.
 *
 *  - [getDailyEntries]  -> method=food_entries.get.v2       (a single day)
 *  - [getMonth]         -> method=food_entries.get_month.v2 (a month of totals)
 *
 * The `date` parameter is the integer number of days since 1970-01-01 (NOT a
 * millisecond timestamp) — see [com.example.widgetfatsecret.fatsecret.domain
 * .FatSecretDate].
 */
class FatSecretFoodClient(
    private val service: FatSecretService,
) {

    /** Fetches and sums-ready list of entries for the given epoch-day. */
    suspend fun getDailyEntries(daysSinceEpoch: Long): List<FoodEntry> =
        withContext(Dispatchers.IO) {
            val body = service.serverApi(
                mapOf(
                    "method" to "food_entries.get.v2",
                    "format" to "json",
                    "date" to daysSinceEpoch.toString(),
                )
            ).string()
            FatSecretJson.parseDailyEntries(body)
        }

    /** Fetches per-day totals for the month containing [dayInMonth] (epoch-day). */
    suspend fun getMonth(dayInMonth: Long): List<DayNutrition> =
        withContext(Dispatchers.IO) {
            val body = service.serverApi(
                mapOf(
                    "method" to "food_entries.get_month.v2",
                    "format" to "json",
                    "date" to dayInMonth.toString(),
                )
            ).string()
            FatSecretJson.parseMonth(body)
        }
}
