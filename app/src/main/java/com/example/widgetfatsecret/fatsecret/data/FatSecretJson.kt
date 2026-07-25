package com.example.widgetfatsecret.fatsecret.data

import com.example.widgetfatsecret.fatsecret.domain.FoodEntry
import com.example.widgetfatsecret.fatsecret.domain.WeightEntry
import com.example.widgetfatsecret.fatsecret.domain.WeightProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** One day's totals coming from the monthly endpoint. */
data class DayNutrition(
    val dateInt: Long,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

/**
 * Tolerant parsing of FatSecret food-diary JSON. Handles every shape the API
 * throws at us:
 *  - `food_entry` as a list, or a single object, or absent;
 *  - numeric fields delivered as JSON strings;
 *  - missing / null nutrient fields;
 *  - an empty diary (`food_entries` is null, "", {} or missing);
 *  - an `error` object instead of data.
 *
 * Only [kotlinx.serialization.json] is used, so this is pure-JVM and fully
 * unit-testable.
 */
object FatSecretJson {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** Parses a `food_entries.get.v2` response body into a list of entries. */
    fun parseDailyEntries(body: String): List<FoodEntry> {
        val root = parseObject(body) ?: return emptyList()
        checkForError(root)
        val entriesEl = root["food_entries"] ?: return emptyList()
        return extractEntries(entriesEl)
    }

    /** Parses a `food_entries.get_month.v2` response body into per-day totals. */
    fun parseMonth(body: String): List<DayNutrition> =
        monthDays(body).mapNotNull { o ->
            val dateInt = long(o["date_int"]) ?: return@mapNotNull null
            DayNutrition(
                dateInt = dateInt,
                calories = num(o["calories"]),
                protein = num(o["protein"]),
                carbs = num(o["carbohydrate"]),
                fat = num(o["fat"]),
            )
        }

    /**
     * Parses a `weights.get_month.v2` response body. Verified against the live
     * API: it uses the very same `month.day` envelope as the food endpoint, so
     * the traversal is shared rather than restated.
     */
    fun parseWeightMonth(body: String): List<WeightEntry> =
        monthDays(body).mapNotNull { o ->
            val dateInt = long(o["date_int"]) ?: return@mapNotNull null
            val kg = num(o["weight_kg"])
            if (kg <= 0.0) null else WeightEntry(dateInt = dateInt, weightKg = kg)
        }

    /**
     * Parses a `profile.get` response body. A goal of 0 (or an absent field)
     * means "no goal set", which is surfaced as null rather than as a real
     * target of zero kilos.
     */
    fun parseProfile(body: String): WeightProfile {
        val root = parseObject(body) ?: return WeightProfile.EMPTY
        checkForError(root)
        val p = root["profile"] as? JsonObject ?: return WeightProfile.EMPTY
        val goal = num(p["goal_weight_kg"]).takeIf { it > 0.0 }
        val last = num(p["last_weight_kg"]).takeIf { it > 0.0 }
        return WeightProfile(
            goalWeightKg = goal,
            lastWeightKg = last,
            lastWeightDateInt = long(p["last_weight_date_int"]),
            usesPounds = str(p["weight_measure"]).equals("Lb", ignoreCase = true),
        )
    }

    /** The shared `{"month":{"day":[...]}}` traversal used by both endpoints. */
    private fun monthDays(body: String): List<JsonObject> {
        val root = parseObject(body) ?: return emptyList()
        checkForError(root)
        val month = root["month"] as? JsonObject ?: return emptyList()
        val dayEl = month["day"] ?: return emptyList()
        return asObjectList(dayEl)
    }

    // --- internals -----------------------------------------------------------

    private fun parseObject(body: String): JsonObject? {
        if (body.isBlank()) return null
        val el = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return null
        return el as? JsonObject
    }

    private fun checkForError(root: JsonObject) {
        val err = root["error"] as? JsonObject ?: return
        val code = long(err["code"])?.toInt() ?: -1
        val message = (err["message"] as? JsonPrimitive)?.contentOrNull ?: "Unknown error"
        throw FatSecretApiException(code, message)
    }

    private fun extractEntries(el: JsonElement): List<FoodEntry> {
        val container = el as? JsonObject ?: return emptyList() // null / "" -> no entries
        val feEl = container["food_entry"] ?: return emptyList()
        return asObjectList(feEl).map { parseEntry(it) }
    }

    /** Normalizes an element that may be a single object or an array into a list. */
    private fun asObjectList(el: JsonElement): List<JsonObject> = when (el) {
        is JsonArray -> el.mapNotNull { it as? JsonObject }
        is JsonObject -> listOf(el)
        else -> emptyList()
    }

    private fun parseEntry(o: JsonObject): FoodEntry = FoodEntry(
        name = str(o["food_entry_name"]),
        meal = str(o["meal"]),
        numberOfUnits = num(o["number_of_units"]),
        servingDescription = str(o["serving_description"]),
        calories = num(o["calories"]),
        carbohydrate = num(o["carbohydrate"]),
        protein = num(o["protein"]),
        fat = num(o["fat"]),
    )

    private fun str(el: JsonElement?): String =
        (el as? JsonPrimitive)?.contentOrNull ?: ""

    /** Reads a numeric field that may be a JSON number, a JSON string, or null. */
    private fun num(el: JsonElement?): Double {
        val prim = el as? JsonPrimitive ?: return 0.0
        val content = prim.contentOrNull ?: return 0.0
        return content.trim().toDoubleOrNull() ?: 0.0
    }

    private fun long(el: JsonElement?): Long? {
        val prim = el as? JsonPrimitive ?: return null
        return prim.contentOrNull?.trim()?.toLongOrNull()
    }
}
