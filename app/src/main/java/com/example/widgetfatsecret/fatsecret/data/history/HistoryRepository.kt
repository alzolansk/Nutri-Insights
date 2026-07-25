package com.example.widgetfatsecret.fatsecret.data.history

import com.example.widgetfatsecret.fatsecret.data.DayNutrition
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretFoodClient
import com.example.widgetfatsecret.fatsecret.domain.FatSecretDate
import com.example.widgetfatsecret.fatsecret.domain.history.ConsistencyCalculator
import com.example.widgetfatsecret.fatsecret.domain.history.ConsistencySummary
import com.example.widgetfatsecret.fatsecret.domain.history.PatternCalculator
import com.example.widgetfatsecret.fatsecret.domain.history.PatternSummary
import com.example.widgetfatsecret.fatsecret.domain.history.TrendCalculator
import com.example.widgetfatsecret.fatsecret.domain.history.TrendSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/**
 * Read side (Flows derived from the stored history) plus the one write path
 * (fetch via `get_month` and merge). Etapa 1 only lays this groundwork — no
 * screen consumes it yet, and nothing here is called from
 * [com.example.widgetfatsecret.fatsecret.data.FatSecretRepository.sync].
 */
class HistoryRepository(
    private val foodClient: FatSecretFoodClient,
    private val store: NutritionHistoryStore,
) {

    val daysFlow: Flow<List<DayNutrition>> = store.daysFlow
    val historyFlow: Flow<NutritionHistory> = store.historyFlow

    fun trend(windowDays: Int, today: Long = FatSecretDate.today()): Flow<TrendSummary> =
        daysFlow.map { TrendCalculator.summarize(it, windowDays, today) }

    fun pattern(windowDays: Int = 28, today: Long = FatSecretDate.today()): Flow<PatternSummary> =
        daysFlow.map { PatternCalculator.summarize(it, windowDays, today) }

    fun consistency(windowDays: Int = 30, today: Long = FatSecretDate.today()): Flow<ConsistencySummary> =
        historyFlow.map { history ->
            val start = today - windowDays + 1
            ConsistencyCalculator.summarize(
                history = history.days,
                syncedDays = history.syncedDaysBetween(start, today),
                windowDays = windowDays,
                today = today,
            )
        }

    /**
     * Fetches the calendar month containing [today] plus [monthsBack] prior
     * months via `get_month` (1 request per month — see risco R6 in
     * planning.md) and merges the result into the store. Must only be called
     * from an explicit refresh once a history-consuming screen exists — never
     * from a ViewModel's `init {}`, and never from [AppContainer.syncAndRefresh].
     */
    suspend fun refresh(today: Long = FatSecretDate.today(), monthsBack: Int = 1) {
        for (offset in 0..monthsBack) {
            // Calendar arithmetic matters here: subtracting 30 epoch days on
            // the 31st can still land in the same month and skip the previous
            // one, leaving a false "nao sincronizado" gap at the rollover.
            val probeDay = YearMonth.from(LocalDate.ofEpochDay(today))
                .minusMonths(offset.toLong())
                .atDay(1)
                .toEpochDay()
            val fetched = runCatching { foodClient.getMonth(probeDay) }.getOrNull() ?: continue
            store.mergeSyncedMonth(probeDay, fetched)
        }
    }
}
