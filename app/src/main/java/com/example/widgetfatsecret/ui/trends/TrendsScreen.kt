package com.example.widgetfatsecret.ui.trends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.data.SyncStatus as DataSyncStatus
import com.example.widgetfatsecret.fatsecret.domain.NutritionFormat
import com.example.widgetfatsecret.fatsecret.domain.history.CalorieDistribution
import com.example.widgetfatsecret.fatsecret.domain.history.TrendSummary
import com.example.widgetfatsecret.ui.design.BarChart
import com.example.widgetfatsecret.ui.design.BarDatum
import com.example.widgetfatsecret.ui.design.EmptyState
import com.example.widgetfatsecret.ui.design.MetaChip
import com.example.widgetfatsecret.ui.design.MetricValue
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
 * for [com.example.widgetfatsecret.ui.navigation.Route.Tendencias] (planning.md §9,
 * Etapa 6).
 */
@Composable
fun TrendsRoute(modifier: Modifier = Modifier, viewModel: TrendsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrendsScreen(state = state, modifier = modifier)
}

/**
 * "Tendências": 7/14/30-day average calories with the change vs. the previous
 * period, a 30-day calorie chart with the goal line, and how many recorded
 * days fell above/near/below the goal — all purely descriptive (planning.md's
 * "padrão mensurável, nunca julgamento").
 */
@Composable
fun TrendsScreen(state: TrendsUiState, modifier: Modifier = Modifier) {
    val snapshot = state.snapshot

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
                description = "Conecte sua conta na aba Metas e conta para ver as tendências de calorias.",
            )
            else -> {
                WindowCard(title = "7 dias", summary = state.trend7)
                WindowCard(title = "14 dias", summary = state.trend14)
                WindowCard(title = "30 dias", summary = state.trend30)
                ChartCard(summary = state.trend30, goalCalories = state.goals.caloriesKcal)
                DistributionCard(distribution = state.distribution)
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
private fun WindowCard(title: String, summary: TrendSummary) {
    val colors = MaterialTheme.nutriColors
    StatCard(
        title = "Média — $title",
        meta = "${summary.daysRecorded} de ${summary.windowDays} dias",
    ) {
        if (!summary.hasEnoughData) {
            Text(
                "Dados insuficientes — são necessários ao menos ${TrendSummary.MIN_RECORDED_DAYS} dias " +
                    "registrados nesta janela.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text2,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricValue(value = NutritionFormat.int(summary.averageCalories!!), unit = "kcal")
                changeText(summary.changeVsPreviousCalories)?.let { text ->
                    MetaChip(text = text)
                }
            }
        }
    }
}

private fun changeText(change: Double?): String? {
    if (change == null) return null
    val arrow = if (change >= 0) "↑" else "↓"
    return "$arrow ${NutritionFormat.int(abs(change))} kcal vs. período anterior"
}

@Composable
private fun ChartCard(summary: TrendSummary, goalCalories: Int) {
    val colors = MaterialTheme.nutriColors
    StatCard(
        title = "Calorias por dia",
        meta = "${summary.daysRecorded} de ${summary.windowDays} dias",
    ) {
        if (summary.daysRecorded == 0) {
            Text(
                "Sem histórico de calorias disponível ainda.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text2,
            )
        } else {
            BarChart(
                data = summary.days.mapIndexed { index, day ->
                    BarDatum(
                        value = day.calories?.toFloat(),
                        highlighted = index == summary.days.lastIndex,
                    )
                },
                goal = goalCalories.toFloat(),
            )
        }
    }
}

@Composable
private fun DistributionCard(distribution: CalorieDistribution) {
    val colors = MaterialTheme.nutriColors
    StatCard(title = "Distribuição vs. meta") {
        if (distribution.recordedTotal == 0) {
            Text(
                "Sem dias registrados suficientes para comparar com a meta.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text2,
            )
        } else {
            DistributionRow("Acima da meta", distribution.above, distribution.recordedTotal, colors.coral)
            DistributionRow("Próximo da meta", distribution.near, distribution.recordedTotal, colors.mint)
            DistributionRow("Abaixo da meta", distribution.below, distribution.recordedTotal, colors.text3)
        }
    }
}

@Composable
private fun DistributionRow(label: String, count: Int, total: Int, dotColor: androidx.compose.ui.graphics.Color) {
    val colors = MaterialTheme.nutriColors
    val percent = if (total > 0) (count.toDouble() / total * 100.0).roundToInt() else 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MetaChip(text = label, dotColor = dotColor)
        Text(
            "$count de $total dias  •  $percent%",
            style = MonoText.meta,
            color = colors.text,
        )
    }
}
