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

    fun trend(windowDays: Int, today: Long = FatSecretDate.today()): Flow<TrendSummary> =
        daysFlow.map { TrendCalculator.summarize(it, windowDays, today) }

    fun pattern(windowDays: Int = 28, today: Long = FatSecretDate.today()): Flow<PatternSummary> =
        daysFlow.map { PatternCalculator.summarize(it, windowDays, today) }

    fun consistency(windowDays: Int = 30, today: Long = FatSecretDate.today()): Flow<ConsistencySummary> =
        daysFlow.map { ConsistencyCalculator.summarize(it, windowDays, today) }

    /**
     * Fetches the calendar month containing [today] plus [monthsBack] prior
     * months via `get_month` (1 request per month — see risco R6 in
     * planning.md) and merges the result into the store. Must only be called
     * from an explicit refresh once a history-consuming screen exists — never
     * from a ViewModel's `init {}`, and never from [AppContainer.syncAndRefresh].
     */
    suspend fun refresh(today: Long = FatSecretDate.today(), monthsBack: Int = 1) {
        for (offset in 0..monthsBack) {
            // get_month resolves the calendar month containing `date`;
            // stepping back ~30 days per offset is enough to land a step in
            // the previous month regardless of which day of the month it is.
            val probeDay = today - offset * 30
            val fetched = runCatching { foodClient.getMonth(probeDay) }.getOrNull() ?: continue
            store.merge(fetched)
        }
    }
}
