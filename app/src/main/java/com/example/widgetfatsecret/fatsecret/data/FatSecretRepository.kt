package com.example.widgetfatsecret.fatsecret.data

import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretAuthClient
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretConfig
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretFoodClient
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretWeightClient
import com.example.widgetfatsecret.fatsecret.domain.DailyNutrition
import com.example.widgetfatsecret.fatsecret.domain.FatSecretDate
import com.example.widgetfatsecret.fatsecret.domain.NutritionCalculator
import com.example.widgetfatsecret.fatsecret.domain.NutritionGoals
import com.example.widgetfatsecret.fatsecret.domain.WeightCalculator
import com.example.widgetfatsecret.fatsecret.domain.WeightEntry
import com.example.widgetfatsecret.fatsecret.domain.WeightStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Combined state the UI/widget renders from. */
data class NutritionUiState(
    val snapshot: NutritionSnapshot,
    val goals: NutritionGoals,
)

/**
 * What the weight widget renders: the raw cache (for connected / empty / stale
 * states) plus the derived figures. The arithmetic lives in [WeightCalculator]
 * and is applied here, so the widget never recomputes it.
 */
data class WeightUiState(
    val snapshot: WeightSnapshot,
    val stats: WeightStats,
)

/** Outcome of a sync attempt. */
sealed interface SyncResult {
    data object Success : SyncResult
    data class Failure(val type: SyncErrorType) : SyncResult
}

/**
 * Orchestrates the whole FatSecret flow: the 3-legged OAuth handshake, the daily
 * sync, monthly insights, goal persistence and cache management.
 *
 * Never logs tokens, secrets or signatures.
 */
class FatSecretRepository(
    private val authClient: FatSecretAuthClient,
    private val foodClient: FatSecretFoodClient,
    private val weightClient: FatSecretWeightClient,
    private val tokenStore: TokenStore,
    private val goalsStore: GoalsStore,
    private val cacheStore: NutritionCacheStore,
    private val weightCacheStore: WeightCacheStore,
) {

    /** Guards the fetch→persist sequence against overlapping syncs. */
    private val syncMutex = Mutex()

    val uiState: Flow<NutritionUiState> =
        combine(cacheStore.snapshotFlow, goalsStore.goalsFlow) { snap, goals ->
            NutritionUiState(snap, goals)
        }

    /** Weight cache with the derived stats already applied. */
    val weightState: Flow<WeightUiState> =
        combine(weightCacheStore.snapshotFlow, goalsStore.startWeightFlow) { snap, startKg ->
            WeightUiState(
                snap,
                WeightCalculator.stats(
                    snap.entries,
                    snap.profile,
                    FatSecretDate.today(),
                    snap.baseline,
                    startKg,
                ),
            )
        }

    /** The manual starting weight, and the one discovered in the diary as a hint. */
    val startWeightFlow: Flow<Double?> = goalsStore.startWeightFlow
    val discoveredStartWeightFlow: Flow<Double?> =
        weightCacheStore.snapshotFlow.map { it.baseline?.weightKg }

    suspend fun saveStartWeight(kg: Double?) = goalsStore.saveStartWeight(kg)

    val goalsFlow: Flow<NutritionGoals> = goalsStore.goalsFlow

    fun isConnected(): Boolean = tokenStore.isConnected

    // --- OAuth handshake ------------------------------------------------------

    /**
     * Leg 1+2: fetches a request token and returns the browser authorization URL.
     * Throws [MissingCredentialsException] if credentials are not configured.
     */
    suspend fun beginConnect(): String {
        if (!FatSecretConfig.hasCredentials) throw MissingCredentialsException()
        val rt = authClient.fetchRequestToken(FatSecretConfig.callbackUrl)
        tokenStore.saveRequestToken(rt.token, rt.tokenSecret)
        return authClient.authorizeUrl(rt.token)
    }

    /**
     * Leg 3: exchanges the stored request token + [verifier] for an access token,
     * persists it, and performs an initial sync.
     */
    suspend fun completeConnect(verifier: String): SyncResult {
        val requestToken = tokenStore.requestToken()
        val requestSecret = tokenStore.requestTokenSecret()
        if (requestToken.isNullOrEmpty() || requestSecret.isNullOrEmpty()) {
            cacheStore.saveError(SyncErrorType.AUTH_INVALID)
            return SyncResult.Failure(SyncErrorType.AUTH_INVALID)
        }
        return try {
            val access = authClient.fetchAccessToken(requestToken, requestSecret, verifier)
            tokenStore.saveAccessToken(access.token, access.tokenSecret)
            tokenStore.clearRequestToken()
            cacheStore.setConnected(true)
            sync()
        } catch (e: Exception) {
            val type = mapError(e)
            cacheStore.saveError(type)
            SyncResult.Failure(type)
        }
    }

    /**
     * Removes the access token and marks the widget disconnected. Cache handling
     * is explicit: by default we CLEAR the cached nutrition on disconnect so the
     * widget does not keep showing another account's data. Pass [clearCache] =
     * false to keep the last totals visible until the next connect.
     */
    suspend fun disconnect(clearCache: Boolean = true) {
        tokenStore.clearAccessToken()
        tokenStore.clearRequestToken()
        if (clearCache) {
            cacheStore.clearAll()
            weightCacheStore.clearAll()
        } else {
            cacheStore.setConnected(false)
            weightCacheStore.setConnected(false)
        }
    }

    // --- Sync -----------------------------------------------------------------

    /**
     * Fetches today's diary, sums it and updates the cache.
     *
     * Serialized: the periodic worker and an on-demand sync can be triggered
     * independently, and two runs interleaving their writes is what let an older
     * response overwrite a newer one. The mutex makes the fetch→persist sequence
     * atomic with respect to other syncs; it does not replace the caller-level
     * de-duplication in [AppContainer.syncAndRefresh], it backstops it.
     */
    suspend fun sync(): SyncResult = syncMutex.withLock { syncLocked() }

    private suspend fun syncLocked(): SyncResult {
        if (!FatSecretConfig.hasCredentials) {
            cacheStore.saveError(SyncErrorType.NO_CREDENTIALS)
            return SyncResult.Failure(SyncErrorType.NO_CREDENTIALS)
        }
        if (!tokenStore.isConnected) {
            cacheStore.setConnected(false)
            cacheStore.saveError(SyncErrorType.NOT_CONNECTED)
            return SyncResult.Failure(SyncErrorType.NOT_CONNECTED)
        }

        cacheStore.setLoading()
        return try {
            val today = FatSecretDate.today()
            val entries = foodClient.getDailyEntries(today)
            val daily: DailyNutrition = NutritionCalculator.sum(entries)
            // Same fetch as `daily`, no extra request: today's calories grouped
            // by meal, for the "Hoje" tab's distribution card (planning.md §9,
            // Etapa 4).
            val meals = NutritionCalculator.mealBreakdown(entries)
            // Secondary, best-effort: the last 7 days of calories for the tall
            // widget's chart. A failure here must not fail the whole sync, so it
            // is caught separately and simply leaves the previous history intact.
            val weekly = runCatching { fetchWeeklyCalories(today, daily.calories) }.getOrNull()
            cacheStore.saveSuccess(daily, System.currentTimeMillis(), weekly, meals)
            // The weight widget rides the same sync cycle (app open, goal change,
            // and the 30-minute worker), so a new weighing logged in FatSecret
            // reaches the home screen without any extra scheduling. It is fully
            // independent: a weight failure must not fail the nutrition sync.
            syncWeight(today)
            SyncResult.Success
        } catch (e: Exception) {
            val type = mapError(e)
            // Never overwrite cached macros with zeros on failure.
            cacheStore.saveError(type)
            SyncResult.Failure(type)
        }
    }

    /**
     * The last 7 days of calorie totals, oldest first and ending today. Built
     * from the monthly endpoint (one call, or two when the window straddles a
     * month boundary). Today's slot is overwritten with [todayCalories] — the
     * freshly-summed value — because the monthly totals can lag the live diary.
     * Days the API omits become 0.0 (rendered as an empty bar).
     */
    private suspend fun fetchWeeklyCalories(today: Long, todayCalories: Double): List<Double> {
        val window = 7
        val startDay = today - (window - 1)
        val monthDays = buildList {
            addAll(foodClient.getMonth(today))
            val startMonth = FatSecretDate.fromDaysSinceEpoch(startDay).withDayOfMonth(1)
            val todayMonth = FatSecretDate.fromDaysSinceEpoch(today).withDayOfMonth(1)
            if (startMonth != todayMonth) addAll(foodClient.getMonth(startDay))
        }
        val caloriesByDay = monthDays.associate { it.dateInt to it.calories }
        return (startDay..today).map { day ->
            if (day == today) todayCalories else (caloriesByDay[day] ?: 0.0)
        }
    }

    // --- Weight ---------------------------------------------------------------

    /**
     * Refreshes the weight history and profile. Best-effort by contract: it
     * swallows its own failures into the weight cache's error flag so the
     * caller's (nutrition) sync result is unaffected, and a failed fetch leaves
     * the previously cached weighings on screen rather than blanking them.
     */
    private suspend fun syncWeight(today: Long) {
        val window = runCatching {
            val profile = weightClient.getProfile()
            val entries = fetchWeightWindow(today)
            weightCacheStore.saveSuccess(entries, profile, System.currentTimeMillis())
            entries
        }.onFailure {
            weightCacheStore.saveError()
        }.getOrNull()
        // Deliberately after the window is already persisted: this can cost a
        // dozen requests the first time, and the widget should not wait on it.
        // Its own failure is swallowed — a missing baseline degrades "total" and
        // "progresso", it does not invalidate the sync.
        runCatching { ensureBaseline(today, window?.minByOrNull { it.dateInt }) }
    }

    /**
     * Finds and stores the all-time first weighing.
     *
     * `profile.get` exposes only the LAST weighing — there is no starting-weight
     * field (verified against the live API), so the only way to get the same
     * number the FatSecret app shows as "Peso Inicial" is to walk
     * `weights.get_month.v2` backwards until the history runs out.
     *
     * Runs once and then stays cached: it re-walks only when nothing is stored,
     * or when the earliest entry in the current window is somehow older than the
     * stored baseline (which means the stored one is wrong). Because it is
     * bounded by both a month cap and a run of empty months, the worst case is a
     * few dozen requests, exactly once.
     */
    private suspend fun ensureBaseline(today: Long, windowOldest: WeightEntry?) {
        val stored = weightCacheStore.baseline()
        val stale = stored != null && windowOldest != null && windowOldest.dateInt < stored.dateInt
        if (stored != null && !stale) return
        val found = walkBackForFirstWeighing(today) ?: return
        weightCacheStore.saveBaseline(found)
    }

    /**
     * Walks month by month towards the past, keeping the oldest weighing seen.
     * Stops after [MAX_EMPTY_MONTHS] consecutive months with no weighings (a
     * tolerance for real gaps in logging) or after [MAX_MONTHS_BACK] months.
     */
    private suspend fun walkBackForFirstWeighing(today: Long): WeightEntry? {
        var cursor = FatSecretDate.fromDaysSinceEpoch(today).withDayOfMonth(1)
        var oldest: WeightEntry? = null
        var emptyStreak = 0
        repeat(MAX_MONTHS_BACK) {
            val month = weightClient.getMonth(FatSecretDate.daysSinceEpoch(cursor))
            val monthOldest = month.minByOrNull { it.dateInt }
            if (monthOldest == null) {
                emptyStreak++
                if (emptyStreak >= MAX_EMPTY_MONTHS) return oldest
            } else {
                emptyStreak = 0
                if (oldest == null || monthOldest.dateInt < oldest!!.dateInt) oldest = monthOldest
            }
            cursor = cursor.minusMonths(1)
        }
        return oldest
    }

    /**
     * Weighings covering the last [WEIGHT_WINDOW_DAYS] days. The endpoint returns
     * whole months, so the previous month is fetched too whenever the window
     * reaches back into it. Returned oldest-first; days without a weighing are
     * simply absent (weight is not logged daily, and interpolating would invent
     * measurements the user never took).
     */
    private suspend fun fetchWeightWindow(today: Long): List<WeightEntry> {
        val startDay = today - (WEIGHT_WINDOW_DAYS - 1)
        val all = buildList {
            addAll(weightClient.getMonth(today))
            val startMonth = FatSecretDate.fromDaysSinceEpoch(startDay).withDayOfMonth(1)
            val todayMonth = FatSecretDate.fromDaysSinceEpoch(today).withDayOfMonth(1)
            if (startMonth != todayMonth) addAll(weightClient.getMonth(startDay))
        }
        return all.filter { it.dateInt in startDay..today }
            .distinctBy { it.dateInt }
            .sortedBy { it.dateInt }
    }

    // --- Monthly insights (optional) -----------------------------------------

    /**
     * Average calories over the last [window] days that actually have records.
     * Days omitted by the API are treated as "no record", NOT as zero consumption.
     * Returns null if there aren't any recorded days.
     */
    suspend fun recentDailyCalorieAverage(window: Int = 7): Double? {
        if (!tokenStore.isConnected) return null
        val today = FatSecretDate.today()
        val month = runCatching { foodClient.getMonth(today) }.getOrNull() ?: return null
        val recorded = month
            .filter { it.dateInt <= today }
            .sortedByDescending { it.dateInt }
            .take(window)
        if (recorded.isEmpty()) return null
        return recorded.sumOf { it.calories } / recorded.size
    }

    /** Number of days in the current month that have at least one record. */
    suspend fun daysRecordedThisMonth(): Int {
        if (!tokenStore.isConnected) return 0
        val today = FatSecretDate.today()
        val month = runCatching { foodClient.getMonth(today) }.getOrNull() ?: return 0
        return month.count()
    }

    // --- Goals ----------------------------------------------------------------

    suspend fun saveGoals(goals: NutritionGoals) = goalsStore.save(goals)

    // --- Error mapping --------------------------------------------------------

    private companion object {
        /** Matches the largest chart the widget draws (30 days). */
        const val WEIGHT_WINDOW_DAYS = 30

        /** Hard stop for the baseline walk: five years of monthly requests. */
        const val MAX_MONTHS_BACK = 60

        /**
         * Consecutive empty months that end the walk. A full year, because
         * people do stop weighing themselves for long stretches and stopping
         * early would silently anchor "total" on the wrong weighing.
         */
        const val MAX_EMPTY_MONTHS = 12
    }

    private fun mapError(e: Throwable): SyncErrorType = when (e) {
        is FatSecretApiException -> e.toSyncErrorType()
        is NotConnectedException -> SyncErrorType.NOT_CONNECTED
        is MissingCredentialsException -> SyncErrorType.NO_CREDENTIALS
        is UnknownHostException, is SocketTimeoutException -> SyncErrorType.NETWORK
        is IOException -> if (e.message == "no_access_token") SyncErrorType.NOT_CONNECTED
        else SyncErrorType.NETWORK
        else -> SyncErrorType.UNKNOWN
    }
}
