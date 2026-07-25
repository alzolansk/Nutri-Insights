package com.example.widgetfatsecret.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.data.NutritionUiState
import com.example.widgetfatsecret.fatsecret.data.SyncStatus as DataSyncStatus
import com.example.widgetfatsecret.fatsecret.domain.DailyNutrition
import com.example.widgetfatsecret.fatsecret.domain.MealTotal
import com.example.widgetfatsecret.fatsecret.domain.NutritionCalculator
import com.example.widgetfatsecret.fatsecret.domain.NutritionFormat
import com.example.widgetfatsecret.fatsecret.domain.NutritionGoals
import com.example.widgetfatsecret.ui.design.EmptyState
import com.example.widgetfatsecret.ui.design.GoalRing
import com.example.widgetfatsecret.ui.design.NutriSpacing
import com.example.widgetfatsecret.ui.design.StatCard
import com.example.widgetfatsecret.ui.design.SyncStatus
import com.example.widgetfatsecret.ui.design.SyncStatusChip
import com.example.widgetfatsecret.ui.theme.MonoText
import com.example.widgetfatsecret.ui.theme.nutriColors
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Route entry point wired into [com.example.widgetfatsecret.ui.navigation.AppShell]
 * for [com.example.widgetfatsecret.ui.navigation.Route.Hoje] (planning.md §9,
 * Etapa 4).
 */
@Composable
fun TodayRoute(modifier: Modifier = Modifier, viewModel: TodayViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(state = state, modifier = modifier)
}

/**
 * "Hoje": goal ring with the remaining amount as a big number, macros with
 * consumed/goal + percent, today's meal distribution, and a "Leitura do dia"
 * with at most two insights — the FatSecret arithmetic, restated, never a
 * judgement (planning.md's negative scope).
 */
@Composable
fun TodayScreen(state: NutritionUiState, modifier: Modifier = Modifier) {
    val snapshot = state.snapshot
    val goals = state.goals
    val daily = snapshot.daily

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NutriSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(NutriSpacing.lg),
    ) {
        SyncStatusChip(
            status = snapshot.toChipStatus(),
            detail = snapshot.lastSyncMillis.takeIf { it > 0 }?.let { NutritionFormat.timeAgo(it) },
        )

        when {
            !snapshot.connected -> EmptyState(
                icon = "🔌",
                title = "Conta desconectada",
                description = "Conecte sua conta na aba Metas e conta para ver os dados de hoje.",
            )
            !snapshot.hasValidData -> EmptyState(
                icon = "⏳",
                title = "Aguardando sincronização",
                description = "Assim que a primeira sincronização terminar, seus dados aparecem aqui.",
            )
            else -> {
                CaloriesCard(daily, goals)
                MacrosCard(daily, goals)
                MealsCard(snapshot.mealBreakdown)
                InsightsCard(daily, goals, snapshot.mealBreakdown)
            }
        }
    }
}

private fun NutritionSnapshot.toChipStatus(): SyncStatus = when {
    !connected -> SyncStatus.DISCONNECTED
    syncStatus == DataSyncStatus.LOADING -> SyncStatus.SYNCING
    syncStatus == DataSyncStatus.ERROR && hasValidData -> SyncStatus.OFFLINE
    syncStatus == DataSyncStatus.ERROR -> SyncStatus.ERROR
    else -> SyncStatus.SYNCED
}

@Composable
private fun CaloriesCard(daily: DailyNutrition, goals: NutritionGoals) {
    val remaining = NutritionCalculator.remaining(daily.calories, goals.caloriesKcal)
    val over = remaining < 0
    val progress = NutritionCalculator.progressFraction(daily.calories, goals.caloriesKcal)
    val centerValue = NutritionFormat.int(abs(remaining))
    StatCard(
        title = "Hoje",
        meta = "${NutritionFormat.ratio(daily.calories, goals.caloriesKcal)} kcal",
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GoalRing(
                progress = progress,
                centerValue = if (over) "+$centerValue" else centerValue,
                centerLabel = if (over) "kcal acima da meta" else "kcal restantes",
            )
        }
    }
}

@Composable
private fun MacrosCard(daily: DailyNutrition, goals: NutritionGoals) {
    val colors = MaterialTheme.nutriColors
    StatCard(title = "Macronutrientes") {
        MacroRow("Proteína", daily.protein, goals.proteinG, colors.cyan)
        MacroRow("Carboidratos", daily.carbs, goals.carbsG, colors.amber)
        MacroRow("Gorduras", daily.fat, goals.fatG, colors.violet)
    }
}

@Composable
private fun MacroRow(label: String, consumed: Double, goal: Int, color: Color) {
    val colors = MaterialTheme.nutriColors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NutriSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.text2)
            Text(
                "${NutritionFormat.ratio(consumed, goal)} g  •  ${NutritionFormat.percentText(consumed, goal)}",
                style = MonoText.meta,
                color = colors.text,
            )
        }
        LinearProgressIndicator(
            progress = { NutritionCalculator.progressFraction(consumed, goal) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = colors.surface2,
        )
    }
}

@Composable
private fun MealsCard(meals: List<MealTotal>) {
    val colors = MaterialTheme.nutriColors
    val total = meals.sumOf { it.calories }
    StatCard(title = "Refeições") {
        if (meals.isEmpty()) {
            Text(
                "Nenhuma refeição registrada hoje.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text2,
            )
        } else {
            meals.forEach { meal ->
                val share = if (total > 0.0) (meal.calories / total * 100.0).roundToInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        NutritionFormat.mealLabel(meal.meal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text,
                    )
                    Text(
                        "${NutritionFormat.int(meal.calories)} kcal  •  $share%",
                        style = MonoText.meta,
                        color = colors.text2,
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsCard(daily: DailyNutrition, goals: NutritionGoals, meals: List<MealTotal>) {
    val colors = MaterialTheme.nutriColors
    val primary = NutritionFormat.insightText(NutritionCalculator.buildInsight(daily, goals))
    val secondary = NutritionCalculator.dominantMealShare(meals)?.let { NutritionFormat.mealShareText(it) }
    StatCard(title = "Leitura do dia") {
        Text(primary, style = MaterialTheme.typography.bodyMedium, color = colors.text)
        if (secondary != null) {
            Text(secondary, style = MaterialTheme.typography.bodyMedium, color = colors.text2)
        }
    }
}
