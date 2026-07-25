package com.example.widgetfatsecret.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.widgetfatsecret.fatsecret.data.AppContainer
import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.data.NutritionUiState
import com.example.widgetfatsecret.fatsecret.domain.NutritionGoals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Read-only view of today's nutrition for the "Hoje" tab (planning.md §9,
 * Etapa 4).
 *
 * Deliberately triggers no sync of its own: the app-open sync is already owned
 * by [com.example.widgetfatsecret.ui.account.AccountViewModel] and the periodic
 * sync by `SyncWorker`, both funneling through `AppContainer.syncAndRefresh()`. A
 * second sync trigger here would be exactly the risk planning.md §10 (R5)
 * warns about once more tabs get their own ViewModel — one sync source only.
 */
class TodayViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppContainer.get(application).repository

    val uiState: StateFlow<NutritionUiState> = repo.uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NutritionUiState(NutritionSnapshot(), NutritionGoals.DEFAULT),
    )
}
