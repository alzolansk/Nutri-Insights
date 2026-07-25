package com.example.widgetfatsecret.ui.patterns

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.widgetfatsecret.fatsecret.data.AppContainer
import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.domain.FatSecretDate
import com.example.widgetfatsecret.fatsecret.domain.NutritionGoals
import com.example.widgetfatsecret.fatsecret.domain.history.GoalFrequency
import com.example.widgetfatsecret.fatsecret.domain.history.PatternCalculator
import com.example.widgetfatsecret.fatsecret.domain.history.PatternMetric
import com.example.widgetfatsecret.fatsecret.domain.history.PatternSummary
import com.example.widgetfatsecret.fatsecret.domain.history.WeeklyCycleSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val PATTERN_WINDOW_DAYS = 28

private val EMPTY_PATTERN = PatternCalculator.summarize(
    history = emptyList(),
    windowDays = PATTERN_WINDOW_DAYS,
    today = 0L,
)
private val EMPTY_CYCLE = PatternCalculator.weeklyCycle(
    history = emptyList(),
    windowDays = PATTERN_WINDOW_DAYS,
    today = 0L,
)

data class PatternsUiState(
    val snapshot: NutritionSnapshot = NutritionSnapshot(),
    val goals: NutritionGoals = NutritionGoals.DEFAULT,
    val pattern: PatternSummary = EMPTY_PATTERN,
    val cycle: WeeklyCycleSummary = EMPTY_CYCLE,
    val calorieFrequency: GoalFrequency = GoalFrequency(PatternMetric.CALORIES, 0, 0, 0),
    val macroFrequencies: List<GoalFrequency> = emptyList(),
    val historyAvailable: Boolean = false,
    val isRefreshing: Boolean = true,
)

/**
 * Read model for Etapa 7. All claims come from the same fixed 28-day window,
 * exclude missing days, and retain the sample counts needed by the methodology
 * sheet. The history refresh remains independent from widget synchronization.
 */
class PatternsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = AppContainer.get(application)
    private val repo = container.repository
    private val historyRepo = container.historyRepository
    private val isRefreshing = MutableStateFlow(false)

    init {
        refresh()
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                historyRepo.refresh()
            } finally {
                isRefreshing.value = false
            }
        }
    }

    val uiState: StateFlow<PatternsUiState> = combine(
        repo.uiState,
        historyRepo.historyFlow,
        isRefreshing,
    ) { ui, history, refreshing ->
        val days = history.days
        val today = FatSecretDate.today()
        val goals = ui.goals
        PatternsUiState(
            snapshot = ui.snapshot,
            goals = goals,
            pattern = PatternCalculator.summarize(days, PATTERN_WINDOW_DAYS, today),
            cycle = PatternCalculator.weeklyCycle(days, PATTERN_WINDOW_DAYS, today),
            calorieFrequency = PatternCalculator.goalFrequency(
                days,
                PATTERN_WINDOW_DAYS,
                today,
                PatternMetric.CALORIES,
                goals.caloriesKcal.toDouble(),
            ),
            macroFrequencies = listOf(
                PatternCalculator.goalFrequency(
                    days,
                    PATTERN_WINDOW_DAYS,
                    today,
                    PatternMetric.PROTEIN,
                    goals.proteinG.toDouble(),
                ),
                PatternCalculator.goalFrequency(
                    days,
                    PATTERN_WINDOW_DAYS,
                    today,
                    PatternMetric.CARBS,
                    goals.carbsG.toDouble(),
                ),
                PatternCalculator.goalFrequency(
                    days,
                    PATTERN_WINDOW_DAYS,
                    today,
                    PatternMetric.FAT,
                    goals.fatG.toDouble(),
                ),
            ),
            historyAvailable = history.syncedMonths.isNotEmpty() || days.isNotEmpty(),
            isRefreshing = refreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PatternsUiState(),
    )
}
