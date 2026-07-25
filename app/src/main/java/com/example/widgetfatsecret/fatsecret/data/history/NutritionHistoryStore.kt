package com.example.widgetfatsecret.fatsecret.data.history

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.widgetfatsecret.fatsecret.data.DayNutrition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

data class NutritionHistory(
    val days: List<DayNutrition> = emptyList(),
    val syncedMonths: Set<YearMonth> = emptySet(),
) {
    fun syncedDaysBetween(start: Long, end: Long): Set<Long> =
        if (end < start) emptySet() else (start..end).filterTo(mutableSetOf()) { epochDay ->
            YearMonth.from(LocalDate.ofEpochDay(epochDay)) in syncedMonths
        }
}

/**
 * Daily nutrition history, deliberately a separate DataStore file from
 * [com.example.widgetfatsecret.fatsecret.data.NutritionCacheStore] (which only
 * keeps today + 7 days of calories for the widget). Feeds Tendências/Padrões/
 * Consistência once those screens exist; nothing reads this yet.
 *
 * Stored as compact `dateInt:cal:protein:carbs:fat` CSV, same pattern as
 * WeightCacheStore. Bounded to [KEEP_DAYS] so the file doesn't grow unbounded
 * across years of use.
 */
class NutritionHistoryStore(private val context: Context) {

    val daysFlow: Flow<List<DayNutrition>> = context.historyDataStore.data.map { p ->
        decode(p[KEY_DAYS]).sortedBy { it.dateInt }
    }

    val syncedMonthsFlow: Flow<Set<YearMonth>> = context.historyDataStore.data.map { p ->
        decodeMonths(p[KEY_SYNCED_MONTHS])
    }

    val historyFlow: Flow<NutritionHistory> = combine(daysFlow, syncedMonthsFlow) { days, months ->
        NutritionHistory(days = days, syncedMonths = months)
    }

    /**
     * Merges [days] into the stored history, keyed by `dateInt` (new values
     * win on conflict), then trims to the most recent [KEEP_DAYS]. Never drops
     * a stored day absent from [days] — a partial month fetch must not erase
     * older history.
     */
    suspend fun merge(days: List<DayNutrition>) {
        if (days.isEmpty()) return
        context.historyDataStore.edit { p ->
            val byDay = LinkedHashMap<Long, DayNutrition>()
            decode(p[KEY_DAYS]).forEach { byDay[it.dateInt] = it }
            days.forEach { byDay[it.dateInt] = it }
            val trimmed = byDay.values.sortedBy { it.dateInt }.takeLast(KEEP_DAYS)
            p[KEY_DAYS] = encode(trimmed)
        }
    }

    /**
     * Stores a successful full-month response and records its coverage even
     * when the API returned no diary entries. This metadata is what lets the
     * Consistencia screen distinguish "sem entradas" from "nao sincronizado".
     */
    suspend fun mergeSyncedMonth(dayInMonth: Long, days: List<DayNutrition>) {
        val month = YearMonth.from(LocalDate.ofEpochDay(dayInMonth))
        context.historyDataStore.edit { p ->
            val byDay = LinkedHashMap<Long, DayNutrition>()
            decode(p[KEY_DAYS]).forEach { byDay[it.dateInt] = it }
            days.forEach { byDay[it.dateInt] = it }
            p[KEY_DAYS] = encode(byDay.values.sortedBy { it.dateInt }.takeLast(KEEP_DAYS))
            p[KEY_SYNCED_MONTHS] = encodeMonths(decodeMonths(p[KEY_SYNCED_MONTHS]) + month)
        }
    }

    /** Wipes cached history (used on disconnect when clearing the cache). */
    suspend fun clearAll() {
        context.historyDataStore.edit { it.clear() }
    }

    private fun encode(days: List<DayNutrition>): String =
        days.joinToString(",") { "${it.dateInt}:${it.calories}:${it.protein}:${it.carbs}:${it.fat}" }

    private fun decode(raw: String?): List<DayNutrition> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',').mapNotNull { entry ->
            val parts = entry.split(':')
            if (parts.size != 5) return@mapNotNull null
            val dateInt = parts[0].toLongOrNull() ?: return@mapNotNull null
            val calories = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            val protein = parts[2].toDoubleOrNull() ?: return@mapNotNull null
            val carbs = parts[3].toDoubleOrNull() ?: return@mapNotNull null
            val fat = parts[4].toDoubleOrNull() ?: return@mapNotNull null
            DayNutrition(dateInt, calories, protein, carbs, fat)
        }
    }

    private fun encodeMonths(months: Set<YearMonth>): String =
        months.sorted().takeLast(KEEP_MONTHS).joinToString(",")

    private fun decodeMonths(raw: String?): Set<YearMonth> =
        raw.orEmpty().split(',').mapNotNull { value ->
            runCatching { YearMonth.parse(value) }.getOrNull()
        }.toSet()

    private companion object {
        val KEY_DAYS = stringPreferencesKey("days")
        val KEY_SYNCED_MONTHS = stringPreferencesKey("synced_months")
        const val KEEP_DAYS = 400
        const val KEEP_MONTHS = 14
    }
}

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "nutrition_history")
