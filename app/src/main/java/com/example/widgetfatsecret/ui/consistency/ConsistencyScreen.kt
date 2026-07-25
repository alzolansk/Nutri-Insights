package com.example.widgetfatsecret.ui.consistency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.widgetfatsecret.fatsecret.domain.NutritionFormat
import com.example.widgetfatsecret.fatsecret.domain.history.ConsistencyDayState
import com.example.widgetfatsecret.fatsecret.domain.history.ConsistencySummary
import com.example.widgetfatsecret.ui.design.CalendarGrid
import com.example.widgetfatsecret.ui.design.EmptyState
import com.example.widgetfatsecret.ui.design.MetricValue
import com.example.widgetfatsecret.ui.design.NutriSpacing
import com.example.widgetfatsecret.ui.design.ScreenSkeleton
import com.example.widgetfatsecret.ui.design.StatCard
import com.example.widgetfatsecret.ui.design.SyncStatusChip
import com.example.widgetfatsecret.ui.ContentState
import com.example.widgetfatsecret.ui.toChipStatus
import com.example.widgetfatsecret.ui.toContentState
import com.example.widgetfatsecret.ui.toUserMessage
import com.example.widgetfatsecret.ui.theme.MonoText
import com.example.widgetfatsecret.ui.theme.nutriColors
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun ConsistencyRoute(
    modifier: Modifier = Modifier,
    viewModel: ConsistencyViewModel = viewModel(),
    onSync: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConsistencyScreen(
        state = state,
        onSync = {
            viewModel.refresh()
            onSync?.invoke()
        },
        modifier = modifier,
    )
}

@Composable
fun ConsistencyScreen(
    state: ConsistencyUiState,
    modifier: Modifier = Modifier,
    onSync: (() -> Unit)? = null,
) {
    val snapshot = state.snapshot
    val contentState = snapshot.toContentState(
        hasUsableData = state.historyAvailable,
        hasRecords = state.historyAvailable,
        loading = state.isRefreshing && !state.historyAvailable,
    )
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

        when (contentState) {
            ContentState.DISCONNECTED -> EmptyState(
                title = "Conta desconectada",
                description = "Conecte sua conta em Metas e conta para ver os dias registrados.",
            )
            ContentState.LOADING -> ScreenSkeleton(consistencySkeletonHeights)
            ContentState.NOT_SYNCED -> {
                EmptyState(
                    title = "Histórico ainda não sincronizado",
                    description = "Nenhum mês foi trazido para este aparelho ainda.",
                    actionLabel = "Sincronizar agora",
                    onAction = onSync,
                )
                ScreenSkeleton(consistencySkeletonHeights)
            }
            ContentState.SYNC_ERROR -> EmptyState(
                title = "Falha na sincronização",
                description = snapshot.errorType?.toUserMessage()
                    ?: "Não foi possível trazer o histórico. Tente sincronizar novamente.",
                actionLabel = "Tentar novamente",
                onAction = onSync,
            )
            ContentState.EMPTY,
            ContentState.CONTENT,
            -> {
                MonthCard(month = state.month, summary = state.monthSummary)
                StreakCard(state.rollingSummary)
                ThirtyDayCard(state.rollingSummary)
            }
        }
    }
}

private val consistencySkeletonHeights = listOf(390.dp, 160.dp, 160.dp)

@Composable
private fun MonthCard(month: YearMonth, summary: ConsistencySummary) {
    StatCard(
        title = monthTitle(month),
        meta = "${summary.daysRecorded} dias registrados",
    ) {
        CalendarGrid(summary.days)
        CalendarLegend()
        Text(
            text = "Sem entradas e não sincronizado são estados diferentes. Dias futuros não entram nos cálculos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.nutriColors.text3,
        )
    }
}

@Composable
private fun CalendarLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(NutriSpacing.sm)) {
        LegendRow(ConsistencyDayState.RECORDED, "Registrado")
        LegendRow(ConsistencyDayState.NO_ENTRIES, "Sem entradas")
        LegendRow(ConsistencyDayState.NOT_SYNCED, "Não sincronizado")
        LegendRow(ConsistencyDayState.FUTURE, "Futuro")
    }
}

@Composable
private fun LegendRow(state: ConsistencyDayState, label: String) {
    val colors = MaterialTheme.nutriColors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NutriSpacing.sm)) {
        val base = Modifier.size(14.dp)
        val marker = when (state) {
            ConsistencyDayState.RECORDED -> base.background(colors.mint, RoundedCornerShape(4.dp))
            ConsistencyDayState.NO_ENTRIES -> base
                .background(colors.surface2, RoundedCornerShape(4.dp))
                .border(1.dp, colors.line2, RoundedCornerShape(4.dp))
            ConsistencyDayState.NOT_SYNCED -> base.dashedLegendBorder(colors.amber)
            ConsistencyDayState.FUTURE -> base.border(
                1.dp,
                colors.line.copy(alpha = 0.45f),
                RoundedCornerShape(4.dp),
            )
        }
        Box(modifier = marker)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.text2)
    }
}

private fun Modifier.dashedLegendBorder(color: Color): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
        ),
    )
}

@Composable
private fun StreakCard(summary: ConsistencySummary) {
    val colors = MaterialTheme.nutriColors
    StatCard(title = "Sequências", meta = "janela de 30 dias") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StreakMetric("Atual", summary.currentStreak)
            StreakMetric("Maior", summary.longestStreak)
        }
        Text(
            text = "Uma sequência conta dias consecutivos com registros. Uma lacuna encerra a sequência; " +
                "um dia não sincronizado permanece identificado como dado desconhecido.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.text3,
        )
    }
}

@Composable
private fun StreakMetric(label: String, days: Int) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label.uppercase(), style = MonoText.meta, color = MaterialTheme.nutriColors.text3)
        MetricValue(value = days.toString(), unit = if (days == 1) "dia" else "dias")
    }
}

@Composable
private fun ThirtyDayCard(summary: ConsistencySummary) {
    val colors = MaterialTheme.nutriColors
    StatCard(
        title = "Últimos 30 dias",
        meta = "${summary.synchronizedDays} de ${summary.windowDays} sincronizados",
    ) {
        if (summary.synchronizedDays == 0) {
            EmptyState(
                title = "Dados insuficientes",
                description = "É necessário ao menos 1 dia sincronizado para calcular o percentual.",
                contentPadding = 0.dp,
            )
        } else {
            MetricValue(value = "${(summary.recordedPercent * 100).roundToInt()}", unit = "% registrados")
            Text(
                text = "${summary.daysRecorded} de ${summary.synchronizedDays} dias sincronizados tiveram entradas. " +
                    "Dias sem sincronização ficam fora do percentual.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text2,
            )
        }
    }
}

private fun monthTitle(month: YearMonth): String {
    val names = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
    )
    return "${names[month.monthValue - 1]} de ${month.year}"
}
