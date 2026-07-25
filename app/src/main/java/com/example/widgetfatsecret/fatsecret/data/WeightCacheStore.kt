package com.example.widgetfatsecret.fatsecret.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.widgetfatsecret.fatsecret.domain.WeightEntry
import com.example.widgetfatsecret.fatsecret.domain.WeightProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** The last-known weight history + profile, persisted for the widget to render. */
data class WeightSnapshot(
    /** Weighings, oldest first. Empty until a sync has populated it. */
    val entries: List<WeightEntry> = emptyList(),
    val profile: WeightProfile = WeightProfile.EMPTY,
    /**
     * The all-time first weighing, found by walking the monthly endpoint
     * backwards. Null until that discovery has run (or if the account has no
     * weighings at all). Persisted separately from [entries] because it usually
     * sits far outside the fetched window.
     */
    val baseline: WeightEntry? = null,
    val lastSyncMillis: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val connected: Boolean = false,
    /** True once at least one successful weight sync has populated the cache. */
    val hasValidData: Boolean = false,
)

/**
 * On-device cache for the weight widget, deliberately a separate DataStore file
 * from [NutritionCacheStore]: the two widgets sync independently and a failure
 * on one must never blank the other.
 *
 * Same guarantee as the nutrition cache: an error NEVER overwrites the stored
 * history, so the widget keeps showing the last good data instead of an empty
 * state whenever the network hiccups.
 *
 * The history is stored as a compact `dateInt:kg` CSV. It is a bounded, flat
 * list of scalars (a month or two of weighings), so Preferences stays a better
 * fit than pulling Room in for one table.
 */
class WeightCacheStore(private val context: Context) {

    val snapshotFlow: Flow<WeightSnapshot> = context.weightDataStore.data.map { p ->
        WeightSnapshot(
            entries = decode(p[KEY_ENTRIES]),
            profile = WeightProfile(
                goalWeightKg = p[KEY_GOAL]?.takeIf { it > 0.0 },
                lastWeightKg = p[KEY_LAST_KG]?.takeIf { it > 0.0 },
                lastWeightDateInt = p[KEY_LAST_DATE],
                usesPounds = p[KEY_POUNDS] ?: false,
            ),
            baseline = readBaseline(p),
            lastSyncMillis = p[KEY_LAST_SYNC] ?: 0L,
            syncStatus = (p[KEY_STATUS]?.let { runCatching { SyncStatus.valueOf(it) }.getOrNull() })
                ?: SyncStatus.IDLE,
            connected = p[KEY_CONNECTED] ?: false,
            hasValidData = p[KEY_HAS_DATA] ?: false,
        )
    }

    suspend fun setConnected(connected: Boolean) {
        context.weightDataStore.edit { it[KEY_CONNECTED] = connected }
    }

    /** Records a successful weight sync. The only path that writes history. */
    suspend fun saveSuccess(
        entries: List<WeightEntry>,
        profile: WeightProfile,
        syncMillis: Long,
    ) {
        context.weightDataStore.edit { p ->
            p[KEY_ENTRIES] = encode(entries)
            if (profile.goalWeightKg != null) p[KEY_GOAL] = profile.goalWeightKg
            else p.remove(KEY_GOAL)
            if (profile.lastWeightKg != null) p[KEY_LAST_KG] = profile.lastWeightKg
            else p.remove(KEY_LAST_KG)
            if (profile.lastWeightDateInt != null) p[KEY_LAST_DATE] = profile.lastWeightDateInt
            else p.remove(KEY_LAST_DATE)
            p[KEY_POUNDS] = profile.usesPounds
            p[KEY_LAST_SYNC] = syncMillis
            p[KEY_STATUS] = SyncStatus.SUCCESS.name
            p[KEY_CONNECTED] = true
            p[KEY_HAS_DATA] = true
        }
    }

    /**
     * Stores the all-time first weighing. Written only by the (one-off, then
     * occasional) backwards walk, never by the ordinary window sync, so a normal
     * sync can never shrink the baseline back into the window.
     */
    suspend fun saveBaseline(entry: WeightEntry) {
        context.weightDataStore.edit { p ->
            p[KEY_BASE_DATE] = entry.dateInt
            p[KEY_BASE_KG] = entry.weightKg
        }
    }

    /** The stored baseline, read once — the sync path needs a value, not a flow. */
    suspend fun baseline(): WeightEntry? = readBaseline(context.weightDataStore.data.first())

    private fun readBaseline(p: Preferences): WeightEntry? {
        val day = p[KEY_BASE_DATE] ?: return null
        val kg = p[KEY_BASE_KG]?.takeIf { it > 0.0 } ?: return null
        return WeightEntry(day, kg)
    }

    /** Records a failure without touching the cached history or profile. */
    suspend fun saveError() {
        context.weightDataStore.edit { it[KEY_STATUS] = SyncStatus.ERROR.name }
    }

    /** Wipes cached weights (used on disconnect when clearing the cache). */
    suspend fun clearAll() {
        context.weightDataStore.edit { it.clear() }
    }

    // --- encoding --------------------------------------------------------------

    /** "20640:107.7,20642:107.1" — oldest first, plain ASCII, locale-independent. */
    private fun encode(entries: List<WeightEntry>): String =
        entries.joinToString(",") { "${it.dateInt}:${it.weightKg}" }

    private fun decode(raw: String?): List<WeightEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',').mapNotNull { pair ->
            val parts = pair.split(':')
            if (parts.size != 2) return@mapNotNull null
            val day = parts[0].toLongOrNull() ?: return@mapNotNull null
            val kg = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            WeightEntry(day, kg)
        }
    }

    private companion object {
        val KEY_ENTRIES = stringPreferencesKey("entries")
        val KEY_GOAL = doublePreferencesKey("goal_kg")
        val KEY_LAST_KG = doublePreferencesKey("last_kg")
        val KEY_LAST_DATE = longPreferencesKey("last_date")
        val KEY_POUNDS = booleanPreferencesKey("pounds")
        val KEY_BASE_DATE = longPreferencesKey("baseline_date")
        val KEY_BASE_KG = doublePreferencesKey("baseline_kg")
        val KEY_LAST_SYNC = longPreferencesKey("last_sync")
        val KEY_STATUS = stringPreferencesKey("status")
        val KEY_CONNECTED = booleanPreferencesKey("connected")
        val KEY_HAS_DATA = booleanPreferencesKey("has_data")
    }
}

private val Context.weightDataStore: DataStore<Preferences> by preferencesDataStore(name = "weight_cache")
