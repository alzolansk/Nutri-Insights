package com.example.widgetfatsecret.ui.trends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.widgetfatsecret.fatsecret.data.AppContainer
import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.domain.FatSecretDate
import com.example.widgetfatsecret.fatsecret.domain.NutritionGoals
import com.example.widgetfatsecret.fatsecret.domain.history.CalorieDistribution
import com.example.widgetfatsecret.fatsecret.domain.history.TrendCalculator
import com.example.widgetfatsecret.fatsecret.domain.history.TrendSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val EMPTY_TREND = TrendCalculator.summarize(emptyList(), windowDays = 7, today = 0L)

data class TrendsUiState(
    val snapshot: NutritionSnapshot = NutritionSnapshot(),
    val goals: NutritionGoals = NutritionGoals.DEFAULT,
    val trend7: TrendSummary = EMPTY_TREND,
    val trend14: TrendSummary = EMPTY_TREND,
    val trend30: TrendSummary = EMPTY_TREND,
    val distribution: CalorieDistribution = CalorieDistribution(0, 0, 0),
)

/**
 * "Tendências" tab (planning.md §9, Etapa 6): 7/14/30-day averages, change vs.
 * the previous period, the 30-day calorie chart, and the above/near/below
 * distribution against the goal — all derived from [AppContainer.historyRepository],
 * which Etapa 1 built without any caller yet.
 *
 * This is the designated first caller of [com.example.widgetfatsecret.fatsecret.data.history.HistoryRepository.refresh],
 * per that class's own doc comment. It runs once per ViewModel instance (this
 * screen's own scope, not the widgets' `AppContainer.syncAndRefresh()`), costs
 * extra quota (risco R6) and never touches `NutritionCacheStore` or the widgets.
 */
class TrendsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = AppContainer.get(application)
    private val repo = container.repository
    private val historyRepo = container.historyRepository

    init {
        viewModelScope.launch { historyRepo.refresh() }
    }

    val uiState: StateFlow<TrendsUiState> = combine(repo.uiState, historyRepo.daysFlow) { ui, days ->
        val today = FatSecretDate.today()
        val trend30 = TrendCalculator.summarize(days, windowDays = 30, today = today)
        TrendsUiState(
            snapshot = ui.snapshot,
            goals = ui.goals,
            trend7 = TrendCalculator.summarize(days, windowDays = 7, today = today),
            trend14 = TrendCalculator.summarize(days, windowDays = 14, today = today),
            trend30 = trend30,
            distribution = TrendCalculator.distribution(trend30.days, ui.goals.caloriesKcal.toDouble()),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrendsUiState(),
    )
}
