package com.example.widgetfatsecret.fatsecret.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.widgetfatsecret.fatsecret.domain.DailyNutrition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Sync lifecycle of the cached daily data. */
enum class SyncStatus { IDLE, LOADING, SUCCESS, ERROR }

/** The last-known widget/app state, persisted across process death and reboots. */
data class NutritionSnapshot(
    val daily: DailyNutrition = DailyNutrition.EMPTY,
    val lastSyncMillis: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val errorType: SyncErrorType? = null,
    val connected: Boolean = false,
    /** True once at least one successful sync has populated the cache. */
    val hasValidData: Boolean = false,
    /**
     * Daily calorie totals for the last few days, oldest first and ending on the
     * sync day (up to 7 values). A day with no record is stored as 0.0. Empty
     * until a sync has populated it; used only by the tall widget's weekly chart.
     */
    val weeklyCalories: List<Double> = emptyList(),
) {
    val hasEntries: Boolean get() = daily.entryCount > 0
}

/**
 * Small on-device cache of the latest valid daily totals plus sync metadata.
 * Uses DataStore Preferences (Room would be overkill for one row). Crucially,
 * an error NEVER overwrites the cached macros with zeros — only the status and
 * error fields change, so the widget keeps showing the last good data.
 */
class NutritionCacheStore(private val context: Context) {

    val snapshotFlow: Flow<NutritionSnapshot> = context.cacheDataStore.data.map { p ->
        NutritionSnapshot(
            daily = DailyNutrition(
                calories = p[KEY_CALORIES] ?: 0.0,
                protein = p[KEY_PROTEIN] ?: 0.0,
                carbs = p[KEY_CARBS] ?: 0.0,
                fat = p[KEY_FAT] ?: 0.0,
                entryCount = p[KEY_ENTRY_COUNT] ?: 0,
            ),
            lastSyncMillis = p[KEY_LAST_SYNC] ?: 0L,
            syncStatus = (p[KEY_STATUS]?.let { runCatching { SyncStatus.valueOf(it) }.getOrNull() })
                ?: SyncStatus.IDLE,
            errorType = p[KEY_ERROR]?.let { runCatching { SyncErrorType.valueOf(it) }.getOrNull() },
            connected = p[KEY_CONNECTED] ?: false,
            hasValidData = p[KEY_HAS_DATA] ?: false,
            weeklyCalories = p[KEY_WEEKLY]
                ?.split(',')
                ?.mapNotNull { it.toDoubleOrNull() }
                ?: emptyList(),
        )
    }

    suspend fun setLoading() {
        context.cacheDataStore.edit { it[KEY_STATUS] = SyncStatus.LOADING.name }
    }

    suspend fun setConnected(connected: Boolean) {
        context.cacheDataStore.edit { it[KEY_CONNECTED] = connected }
    }

    /**
     * Records a successful sync. This is the only path that writes macro values.
     *
     * [weekly] is the last few days' calorie totals for the tall widget's chart.
     * When it is null (the secondary weekly fetch failed) the previously stored
     * history is left untouched, so a hiccup never blanks the chart.
     */
    suspend fun saveSuccess(daily: DailyNutrition, syncMillis: Long, weekly: List<Double>? = null) {
        context.cacheDataStore.edit { p ->
            p[KEY_CALORIES] = daily.calories
            p[KEY_PROTEIN] = daily.protein
            p[KEY_CARBS] = daily.carbs
            p[KEY_FAT] = daily.fat
            p[KEY_ENTRY_COUNT] = daily.entryCount
            p[KEY_LAST_SYNC] = syncMillis
            p[KEY_STATUS] = SyncStatus.SUCCESS.name
            p[KEY_CONNECTED] = true
            p[KEY_HAS_DATA] = true
            if (weekly != null) p[KEY_WEEKLY] = weekly.joinToString(",")
            p.remove(KEY_ERROR)
        }
    }

    /** Records a failure without touching the cached macros or last-sync time. */
    suspend fun saveError(type: SyncErrorType) {
        context.cacheDataStore.edit { p ->
            p[KEY_STATUS] = SyncStatus.ERROR.name
            p[KEY_ERROR] = type.name
        }
    }

    /** Wipes cached nutrition (used on disconnect when clearing the cache). */
    suspend fun clearAll() {
        context.cacheDataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_CALORIES = doublePreferencesKey("calories")
        val KEY_PROTEIN = doublePreferencesKey("protein")
        val KEY_CARBS = doublePreferencesKey("carbs")
        val KEY_FAT = doublePreferencesKey("fat")
        val KEY_ENTRY_COUNT = intPreferencesKey("entry_count")
        val KEY_LAST_SYNC = longPreferencesKey("last_sync")
        val KEY_STATUS = stringPreferencesKey("status")
        val KEY_ERROR = stringPreferencesKey("error")
        val KEY_CONNECTED = booleanPreferencesKey("connected")
        val KEY_HAS_DATA = booleanPreferencesKey("has_data")
        val KEY_WEEKLY = stringPreferencesKey("weekly_calories")
    }
}

private val Context.cacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "nutrition_cache")
