package com.example.widgetfatsecret.ui.consistency

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.widgetfatsecret.fatsecret.data.AppContainer
import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.domain.FatSecretDate
import com.example.widgetfatsecret.fatsecret.domain.history.ConsistencyCalculator
import com.example.widgetfatsecret.fatsecret.domain.history.ConsistencySummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

const val CONSISTENCY_WINDOW_DAYS = 30

private val EMPTY_SUMMARY = ConsistencyCalculator.summarize(
    history = emptyList(),
    syncedDays = emptySet(),
    windowDays = CONSISTENCY_WINDOW_DAYS,
    today = 0L,
)

data class ConsistencyUiState(
    val snapshot: NutritionSnapshot = NutritionSnapshot(),
    val month: YearMonth = YearMonth.from(LocalDate.ofEpochDay(0L)),
    val monthSummary: ConsistencySummary = EMPTY_SUMMARY,
    val rollingSummary: ConsistencySummary = EMPTY_SUMMARY,
)

class ConsistencyViewModel(application: Application) : AndroidViewModel(application) {

    private val container = AppContainer.get(application)
    private val repo = container.repository
    private val historyRepo = container.historyRepository

    init {
        viewModelScope.launch { historyRepo.refresh() }
    }

    val uiState: StateFlow<ConsistencyUiState> =
        combine(repo.uiState, historyRepo.historyFlow) { ui, history ->
            val today = FatSecretDate.today()
            val todayDate = LocalDate.ofEpochDay(today)
            val month = YearMonth.from(todayDate)
            val monthStart = month.atDay(1).toEpochDay()
            val monthEnd = month.atEndOfMonth().toEpochDay()
            val rollingStart = today - CONSISTENCY_WINDOW_DAYS + 1
            ConsistencyUiState(
                snapshot = ui.snapshot,
                month = month,
                monthSummary = ConsistencyCalculator.summarize(
                    history = history.days,
                    syncedDays = history.syncedDaysBetween(monthStart, monthEnd),
                    windowStart = monthStart,
                    windowEnd = monthEnd,
                    today = today,
                ),
                rollingSummary = ConsistencyCalculator.summarize(
                    history = history.days,
                    syncedDays = history.syncedDaysBetween(rollingStart, today),
                    windowDays = CONSISTENCY_WINDOW_DAYS,
                    today = today,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConsistencyUiState(),
        )
}
