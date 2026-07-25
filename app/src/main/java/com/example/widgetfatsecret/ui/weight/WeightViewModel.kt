package com.example.widgetfatsecret.ui.weight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.widgetfatsecret.fatsecret.data.AppContainer
import com.example.widgetfatsecret.fatsecret.data.DayNutrition
import com.example.widgetfatsecret.fatsecret.data.WeightSnapshot
import com.example.widgetfatsecret.fatsecret.data.SyncStatus
import com.example.widgetfatsecret.fatsecret.domain.FatSecretDate
import com.example.widgetfatsecret.fatsecret.domain.WeightEntry
import com.example.widgetfatsecret.fatsecret.domain.WeightStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val WEIGHT_TIMELINE_DAYS = 30

data class WeightTimelinePoint(
    val dateInt: Long,
    val weightKg: Double? = null,
    val movingAverageKg: Double? = null,
    val calories: Double? = null,
)

data class WeightUiState(
    val snapshot: WeightSnapshot = WeightSnapshot(),
    val stats: WeightStats = WeightStats(),
    val timeline: List<WeightTimelinePoint> = emptyList(),
    val isSyncing: Boolean = false,
    val accountConnected: Boolean = false,
)

/**
 * Pure alignment used by the Peso chart. Missing days remain null: neither a
 * missing weighing nor an absent food diary is converted to zero. The moving
 * average uses weighings from the preceding seven calendar days and is emitted
 * only on days that actually contain a weighing.
 */
internal object WeightTimelineCalculator {
    fun align(
        weights: List<WeightEntry>,
        nutrition: List<DayNutrition>,
        today: Long,
        windowDays: Int = WEIGHT_TIMELINE_DAYS,
    ): List<WeightTimelinePoint> {
        if (windowDays <= 0) return emptyList()
        val start = today - windowDays + 1
        val byWeightDay = weights
            .filter { it.dateInt in start..today }
            .associateBy { it.dateInt }
        val byNutritionDay = nutrition
            .filter { it.dateInt in start..today }
            .associateBy { it.dateInt }

        return (start..today).map { day ->
            val weight = byWeightDay[day]
            val average = if (weight == null) {
                null
            } else {
                byWeightDay.values
                    .filter { it.dateInt in (day - 6)..day }
                    .map { it.weightKg }
                    .average()
            }
            WeightTimelinePoint(
                dateInt = day,
                weightKg = weight?.weightKg,
                movingAverageKg = average,
                calories = byNutritionDay[day]?.calories,
            )
        }
    }
}

/**
 * Peso tab (planning.md Etapa 9). Weight figures come from the exact same
 * repository state consumed by WeightWidget; this ViewModel never recomputes
 * deltas, trend, baseline or goal progress.
 */
class WeightViewModel(application: Application) : AndroidViewModel(application) {

    private val container = AppContainer.get(application)
    private val repository = container.repository
    private val historyRepository = container.historyRepository

    init {
        // Calories share the chart's time axis. This remains isolated from the
        // widgets and from AppContainer.syncAndRefresh(), as in Etapas 6-8.
        refreshHistory()
    }

    fun refreshHistory() {
        viewModelScope.launch { historyRepository.refresh() }
    }

    val uiState: StateFlow<WeightUiState> =
        combine(repository.weightState, historyRepository.daysFlow, repository.uiState) { weight, nutrition, nutritionState ->
            WeightUiState(
                snapshot = weight.snapshot,
                stats = weight.stats,
                timeline = WeightTimelineCalculator.align(
                    weights = weight.snapshot.entries,
                    nutrition = nutrition,
                    today = FatSecretDate.today(),
                ),
                isSyncing = nutritionState.snapshot.syncStatus == SyncStatus.LOADING,
                accountConnected = nutritionState.snapshot.connected,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeightUiState(),
        )
}
