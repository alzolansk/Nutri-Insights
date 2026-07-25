package com.example.widgetfatsecret.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.widgetfatsecret.fatsecret.data.SyncStatus as DataSyncStatus
import com.example.widgetfatsecret.fatsecret.data.WeightSnapshot
import com.example.widgetfatsecret.fatsecret.domain.NutritionFormat
import com.example.widgetfatsecret.fatsecret.domain.WeightEntry
import com.example.widgetfatsecret.fatsecret.domain.WeightFormat
import com.example.widgetfatsecret.fatsecret.domain.WeightStats
import com.example.widgetfatsecret.fatsecret.domain.WeightTrend
import com.example.widgetfatsecret.ui.design.EmptyState
import com.example.widgetfatsecret.ui.design.GoalRing
import com.example.widgetfatsecret.ui.design.MetaChip
import com.example.widgetfatsecret.ui.design.MetricValue
import com.example.widgetfatsecret.ui.design.NutriSpacing
import com.example.widgetfatsecret.ui.design.StatCard
import com.example.widgetfatsecret.ui.design.SyncStatus
import com.example.widgetfatsecret.ui.design.SyncStatusChip
import com.example.widgetfatsecret.ui.theme.MonoText
import com.example.widgetfatsecret.ui.theme.nutriColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun WeightRoute(modifier: Modifier = Modifier, viewModel: WeightViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WeightScreen(state = state, modifier = modifier)
}

@Composable
fun WeightScreen(state: WeightUiState, modifier: Modifier = Modifier) {
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
                title = "Conta desconectada",
                description = "Conecte sua conta em Metas e conta para ver seu histórico de peso.",
            )
            !state.stats.hasData -> EmptyState(
                title = "Sem pesagens",
                description = "Ainda não há pesagens disponíveis na conta sincronizada.",
            )
            else -> WeightContent(state)
        }
    }
}

private fun WeightSnapshot.toChipStatus(): SyncStatus = when {
    !connected -> SyncStatus.DISCONNECTED
    syncStatus == DataSyncStatus.LOADING -> SyncStatus.SYNCING
    syncStatus == DataSyncStatus.ERROR && hasValidData -> SyncStatus.OFFLINE
    syncStatus == DataSyncStatus.ERROR -> SyncStatus.ERROR
    else -> SyncStatus.SYNCED
}

@Composable
private fun WeightContent(state: WeightUiState) {
    val stats = state.stats
    val pounds = state.snapshot.profile.usesPounds
    CurrentWeightCard(stats, pounds)
    GoalCard(stats, pounds)
    TimelineCard(state.timeline, pounds)
    RecentWeighingsCard(state.snapshot.entries, pounds)
}

@Composable
private fun CurrentWeightCard(stats: WeightStats, pounds: Boolean) {
    val colors = MaterialTheme.nutriColors
    val latest = requireNotNull(stats.latest)
    StatCard(title = "Peso atual", meta = stats.daysSinceLast?.let(WeightFormat::sinceLast)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(NutriSpacing.sm)) {
            Text(
                text = WeightFormat.weightValue(latest.weightKg, pounds),
                style = MonoText.metricLarge,
                color = colors.text,
            )
            Text(
                text = WeightFormat.unit(pounds),
                style = MaterialTheme.typography.labelMedium,
                color = colors.text3,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Text(
            text = previousDeltaText(stats, pounds),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text2,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            WeightStat("Tendência", trendText(stats))
            WeightStat("Média", stats.weeklyAverage?.let { WeightFormat.perWeek(it, pounds) } ?: "—")
            WeightStat("Total", stats.totalDelta?.let { WeightFormat.delta(it, pounds) } ?: "—")
        }
    }
}

@Composable
private fun WeightStat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(NutriSpacing.xs)) {
        Text(label.uppercase(), style = MonoText.meta, color = MaterialTheme.nutriColors.text3)
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.nutriColors.text)
    }
}

private fun previousDeltaText(stats: WeightStats, pounds: Boolean): String {
    val delta = stats.deltaFromPrevious
        ?: return "Primeira pesagem" + (stats.daysSinceLast?.let { " · ${WeightFormat.sinceLast(it)}" } ?: "")
    val direction = when {
        delta < 0 -> WeightTrend.LOSING
        delta > 0 -> WeightTrend.GAINING
        else -> WeightTrend.STABLE
    }
    val since = stats.daysSinceLast?.let { " · ${WeightFormat.sinceLast(it)}" }.orEmpty()
    return "${WeightFormat.trendArrow(direction)} ${WeightFormat.delta(delta, pounds)} desde a pesagem anterior$since"
}

private fun trendText(stats: WeightStats): String = if (stats.trend == WeightTrend.UNKNOWN) {
    "—"
} else {
    "${WeightFormat.trendArrow(stats.trend)} ${WeightFormat.trendLabel(stats.trend)}"
}

@Composable
private fun GoalCard(stats: WeightStats, pounds: Boolean) {
    val colors = MaterialTheme.nutriColors
    StatCard(title = "Meta do FatSecret") {
        val goal = stats.goalKg
        val progress = stats.goalProgress
        if (goal == null || progress == null) {
            Text("Sem meta definida no FatSecret.", style = MaterialTheme.typography.bodyMedium, color = colors.text2)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NutriSpacing.xl),
            ) {
                GoalRing(
                    progress = progress,
                    centerValue = "${(progress * 100).toInt()}%",
                    centerLabel = "do percurso",
                    ringSize = 116.dp,
                    strokeWidth = 10.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(NutriSpacing.sm)) {
                    MetricValue(
                        value = WeightFormat.weightValue(goal, pounds),
                        unit = WeightFormat.unit(pounds),
                    )
                    Text(
                        text = remainingText(stats, pounds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text2,
                    )
                }
            }
        }
    }
}

private fun remainingText(stats: WeightStats, pounds: Boolean): String {
    val remaining = stats.remainingToGoal ?: return ""
    return if (abs(remaining) < 0.05) {
        "Meta alcançada"
    } else {
        "Faltam ${WeightFormat.weight(abs(remaining), pounds)}"
    }
}

@Composable
private fun TimelineCard(points: List<WeightTimelinePoint>, pounds: Boolean) {
    val colors = MaterialTheme.nutriColors
    val weighings = points.count { it.weightKg != null }
    val recordedCalories = points.count { it.calories != null }
    StatCard(
        title = "Evolução",
        meta = "$weighings pesagens · 30 dias",
    ) {
        TimelineLegend()
        if (weighings == 0) {
            Text("Sem pesagens nos últimos 30 dias.", style = MaterialTheme.typography.bodyMedium, color = colors.text2)
        } else {
            WeightNutritionTimeline(points = points)
        }
        Text(
            text = "$recordedCalories de 30 dias têm calorias registradas. Dias ausentes permanecem sem valor.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.text3,
        )
        Text(
            text = "Peso e calorias aparecem no mesmo período apenas para leitura conjunta. O gráfico não mede correlação e não indica causa entre as séries.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.text2,
        )
        Text(
            text = "Média móvel: pesagens registradas nos 7 dias anteriores a cada pesagem. Unidade: ${WeightFormat.unit(pounds)}.",
            style = MonoText.meta,
            color = colors.text3,
        )
    }
}

@Composable
private fun TimelineLegend() {
    val colors = MaterialTheme.nutriColors
    Column(verticalArrangement = Arrangement.spacedBy(NutriSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(NutriSpacing.sm)) {
            MetaChip(text = "Pesagem", dotColor = colors.text2)
            MetaChip(text = "Média móvel 7 dias", dotColor = colors.cyan)
        }
        MetaChip(text = "Calorias", dotColor = colors.text3)
    }
}

@Composable
private fun WeightNutritionTimeline(
    points: List<WeightTimelinePoint>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nutriColors
    val rawWeights = points.mapNotNull { it.weightKg }
    val averages = points.mapNotNull { it.movingAverageKg }
    val weightValues = rawWeights + averages
    val minWeight = (weightValues.minOrNull() ?: 0.0).toFloat()
    val maxWeight = (weightValues.maxOrNull() ?: 1.0).toFloat()
    val weightPadding = ((maxWeight - minWeight) * 0.15f).coerceAtLeast(0.5f)
    val low = minWeight - weightPadding
    val high = maxWeight + weightPadding
    val maxCalories = points.mapNotNull { it.calories }.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f

    Canvas(modifier = modifier.fillMaxWidth().height(230.dp)) {
        if (points.isEmpty()) return@Canvas
        val count = points.size
        val slot = size.width / count
        val weightTop = 8.dp.toPx()
        val weightBottom = size.height * 0.62f
        val calorieTop = size.height * 0.72f
        val calorieBottom = size.height

        fun x(index: Int) = (index + 0.5f) * slot
        fun weightY(value: Double): Float = weightBottom -
            ((value.toFloat() - low) / (high - low).coerceAtLeast(0.001f)) * (weightBottom - weightTop)

        val averagePath = Path()
        var pathStarted = false
        points.forEachIndexed { index, point ->
            point.movingAverageKg?.let { value ->
                val px = x(index)
                val py = weightY(value)
                if (!pathStarted) {
                    averagePath.moveTo(px, py)
                    pathStarted = true
                } else {
                    averagePath.lineTo(px, py)
                }
            }
        }
        if (pathStarted) {
            drawPath(
                path = averagePath,
                color = colors.cyan,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        points.forEachIndexed { index, point ->
            point.weightKg?.let { value ->
                drawCircle(color = colors.text2, radius = 3.5.dp.toPx(), center = Offset(x(index), weightY(value)))
            }
        }

        val barWidth = (slot * 0.55f).coerceAtLeast(2.dp.toPx())
        points.forEachIndexed { index, point ->
            val left = x(index) - barWidth / 2f
            val calories = point.calories
            if (calories == null) {
                val stubTop = calorieBottom - (calorieBottom - calorieTop) * 0.28f
                drawRect(
                    color = colors.line2,
                    topLeft = Offset(left, stubTop),
                    size = androidx.compose.ui.geometry.Size(barWidth, calorieBottom - stubTop),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            } else {
                val barTop = calorieBottom - (calories.toFloat() / maxCalories) * (calorieBottom - calorieTop)
                drawRect(
                    color = colors.text3.copy(alpha = 0.75f),
                    topLeft = Offset(left, barTop),
                    size = androidx.compose.ui.geometry.Size(barWidth, calorieBottom - barTop),
                )
            }
        }
    }
}

@Composable
private fun RecentWeighingsCard(entries: List<WeightEntry>, pounds: Boolean) {
    val recent = entries.sortedByDescending { it.dateInt }.take(5)
    val colors = MaterialTheme.nutriColors
    StatCard(title = "Pesagens recentes", meta = "últimas ${recent.size}") {
        recent.forEachIndexed { index, entry ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDate(entry.dateInt), style = MaterialTheme.typography.bodyMedium, color = colors.text2)
                Text(WeightFormat.weight(entry.weightKg, pounds), style = MonoText.meta, color = colors.text)
            }
            if (index != recent.lastIndex) Spacer(Modifier.height(NutriSpacing.xs))
        }
    }
}

private val weightDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("pt-BR"))
private fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(weightDateFormatter)
