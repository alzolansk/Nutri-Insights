package com.example.widgetfatsecret.ui.patterns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.data.SyncStatus as DataSyncStatus
import com.example.widgetfatsecret.fatsecret.domain.NutritionFormat
import com.example.widgetfatsecret.fatsecret.domain.history.GoalFrequency
import com.example.widgetfatsecret.fatsecret.domain.history.PatternMetric
import com.example.widgetfatsecret.fatsecret.domain.history.PatternSummary
import com.example.widgetfatsecret.fatsecret.domain.history.WeekdayAverage
import com.example.widgetfatsecret.ui.design.EmptyState
import com.example.widgetfatsecret.ui.design.MetaChip
import com.example.widgetfatsecret.ui.design.NutriSpacing
import com.example.widgetfatsecret.ui.design.StatCard
import com.example.widgetfatsecret.ui.design.SyncStatus
import com.example.widgetfatsecret.ui.design.SyncStatusChip
import com.example.widgetfatsecret.ui.theme.MonoText
import com.example.widgetfatsecret.ui.theme.nutriColors
import java.time.DayOfWeek
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PatternsRoute(modifier: Modifier = Modifier, viewModel: PatternsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PatternsScreen(state = state, modifier = modifier)
}

/**
 * Patterns observed over a fixed 28-day window. Every calculated statement is
 * auditable through [MethodologySheet]; unavailable meal history is described
 * explicitly instead of being inferred from today's meal breakdown.
 */
@Composable
fun PatternsScreen(state: PatternsUiState, modifier: Modifier = Modifier) {
    var selectedMethodology by remember { mutableStateOf<Methodology?>(null) }
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

        if (!snapshot.connected) {
            EmptyState(
                title = "Conta desconectada",
                description = "Conecte sua conta em Metas e conta para observar padrões do histórico.",
            )
        } else {
            WeekdayAveragesCard(state.pattern)

            if (!state.pattern.hasEnoughData) {
                StatCard(
                    title = "Padrões observados",
                    meta = sampleText(state.pattern.daysRecorded),
                ) {
                    Text(
                        text = "Dados insuficientes — são necessários ao menos " +
                            "${PatternSummary.MIN_RECORDED_DAYS} dias registrados na janela.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.nutriColors.text2,
                    )
                }
            } else {
                DayPatternInsight(state.pattern, onOpenMethodology = { selectedMethodology = it })
                CyclePatternInsight(state, onOpenMethodology = { selectedMethodology = it })
                GoalFrequencyInsight(
                    frequency = state.calorieFrequency,
                    title = "Frequência em relação à meta",
                    sentence = calorieFrequencySentence(state.calorieFrequency),
                    calculation = "Cada dia registrado foi comparado com a meta local de calorias. " +
                        "A faixa próxima à meta vai de 95% a 105%; o resultado conta os dias abaixo " +
                        "ou acima dessa faixa.",
                    limitation = commonGoalLimitation,
                    onOpenMethodology = { selectedMethodology = it },
                )
                MacroPatternInsight(state, onOpenMethodology = { selectedMethodology = it })
            }

            MealHistoryState()
        }
    }

    selectedMethodology?.let { methodology ->
        MethodologySheet(
            methodology = methodology,
            onDismiss = { selectedMethodology = null },
        )
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
private fun WeekdayAveragesCard(summary: PatternSummary) {
    val colors = MaterialTheme.nutriColors
    StatCard(
        title = "Média por dia da semana",
        meta = "${summary.daysRecorded} de ${summary.windowDays} dias",
    ) {
        summary.byWeekday.forEachIndexed { index, average ->
            WeekdayRow(average)
            if (index != summary.byWeekday.lastIndex) {
                HorizontalDivider(color = colors.line)
            }
        }
        Text(
            text = "Dias sem registro ficam fora das médias.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.text3,
        )
    }
}

@Composable
private fun WeekdayRow(average: WeekdayAverage) {
    val colors = MaterialTheme.nutriColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = weekdayName(average.dayOfWeek),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
        )
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(
                text = average.averageCalories?.let { "${NutritionFormat.int(it)} kcal" } ?: "—",
                style = MonoText.meta,
                color = if (average.averageCalories == null) colors.text3 else colors.cyan,
            )
            Text(
                text = sampleText(average.daysRecorded),
                style = MaterialTheme.typography.bodySmall,
                color = colors.text3,
            )
        }
    }
}

@Composable
private fun DayPatternInsight(summary: PatternSummary, onOpenMethodology: (Methodology) -> Unit) {
    val highest = summary.byWeekday
        .filter { it.averageCalories != null }
        .maxByOrNull { it.averageCalories!! }
        ?: return
    val sentence = "Entre os dias registrados, ${weekdayName(highest.dayOfWeek).lowercase()} teve a maior " +
        "média: ${NutritionFormat.int(highest.averageCalories!!)} kcal (${sampleText(highest.daysRecorded)})."
    InsightCard(
        title = "Padrão por dia",
        sentence = sentence,
        onClick = {
            onOpenMethodology(
                Methodology(
                    title = "Maior média por dia da semana",
                    window = "${summary.windowDays} dias",
                    daysRecorded = summary.daysRecorded,
                    calculation = "Somamos as calorias dos dias registrados de cada dia da semana, " +
                        "dividimos pela amostra daquele dia e destacamos a maior média.",
                    limitation = "Dias sem registro não entram no cálculo. Cada dia da semana pode ter " +
                        "uma amostra diferente; esta comparação descreve apenas a janela analisada.",
                ),
            )
        },
    )
}

@Composable
private fun CyclePatternInsight(state: PatternsUiState, onOpenMethodology: (Methodology) -> Unit) {
    val cycle = state.cycle
    val total = cycle.weekdays.daysRecorded + cycle.weekend.daysRecorded
    if (!cycle.hasEnoughData || cycle.differenceCalories == null) {
        StatCard(
            title = "Ciclo semanal",
            meta = sampleText(total),
        ) {
            Text(
                text = "Dados insuficientes — são necessários ao menos 2 dias registrados " +
                    "nos dias úteis e 2 no fim de semana.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.nutriColors.text2,
            )
        }
        return
    }

    val difference = cycle.differenceCalories ?: return
    val sentence = when {
        abs(difference) < 0.5 -> "A média de calorias foi igual nos dias úteis e no fim de semana."
        difference > 0 -> "No fim de semana, a média ficou ${NutritionFormat.int(abs(difference))} kcal " +
            "acima da média dos dias úteis."
        else -> "No fim de semana, a média ficou ${NutritionFormat.int(abs(difference))} kcal " +
            "abaixo da média dos dias úteis."
    }
    InsightCard(
        title = "Ciclo semanal",
        sentence = sentence,
        meta = "${cycle.weekdays.daysRecorded} úteis · ${cycle.weekend.daysRecorded} fim de semana",
        onClick = {
            onOpenMethodology(
                Methodology(
                    title = "Dias úteis e fim de semana",
                    window = "$PATTERN_WINDOW_DAYS dias",
                    daysRecorded = total,
                    calculation = "Calculamos uma média com todos os dias registrados de segunda a " +
                        "sexta e outra com sábado e domingo; a frase mostra a diferença entre elas.",
                    limitation = "Dias sem registro ficam fora das duas médias. Feriados não são " +
                        "tratados separadamente, e a diferença não indica causa.",
                ),
            )
        },
    )
}

@Composable
private fun GoalFrequencyInsight(
    frequency: GoalFrequency,
    title: String,
    sentence: String,
    calculation: String,
    limitation: String,
    onOpenMethodology: (Methodology) -> Unit,
) {
    if (frequency.daysRecorded == 0) return
    InsightCard(
        title = title,
        sentence = sentence,
        onClick = {
            onOpenMethodology(
                Methodology(
                    title = title,
                    window = "$PATTERN_WINDOW_DAYS dias",
                    daysRecorded = frequency.daysRecorded,
                    calculation = calculation,
                    limitation = limitation,
                ),
            )
        },
    )
}

@Composable
private fun MacroPatternInsight(state: PatternsUiState, onOpenMethodology: (Methodology) -> Unit) {
    val selected = state.macroFrequencies
        .filter { it.daysRecorded > 0 }
        .maxByOrNull { maxOf(it.below, it.above) }
        ?: return
    val belowIsDominant = selected.below >= selected.above
    val count = if (belowIsDominant) selected.below else selected.above
    val direction = if (belowIsDominant) "abaixo" else "acima"
    val percent = percent(count, selected.daysRecorded)
    val metric = metricName(selected.metric)
    GoalFrequencyInsight(
        frequency = selected,
        title = "Padrão de macro",
        sentence = "$metric ficou $direction da faixa da meta em $count de " +
            "${selected.daysRecorded} dias registrados ($percent%).",
        calculation = "Para proteína, carboidratos e gorduras, cada dia registrado foi comparado " +
            "com a meta local e sua faixa de ±5%. Mostramos a direção mais frequente do macro com " +
            "maior contagem direcional.",
        limitation = commonGoalLimitation + " Este destaque compara frequências, não a importância " +
            "nutricional de cada macro.",
        onOpenMethodology = onOpenMethodology,
    )
}

@Composable
private fun InsightCard(
    title: String,
    sentence: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.nutriColors
    StatCard(
        modifier = modifier.clickable(onClick = onClick),
        title = title,
        meta = meta,
    ) {
        Text(
            text = sentence,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.text,
        )
        MetaChip(text = "Ver metodologia ›")
    }
}

@Composable
private fun MealHistoryState() {
    val colors = MaterialTheme.nutriColors
    StatCard(
        title = "Padrões por refeição",
        meta = "DADO NÃO DISPONÍVEL",
    ) {
        Text(
            text = "O histórico atual não guarda refeições por dia. Esta análise só aparecerá " +
                "quando houver uma sincronização detalhada; os dados de hoje não são projetados " +
                "sobre as semanas anteriores.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text2,
        )
    }
}

private fun calorieFrequencySentence(frequency: GoalFrequency): String {
    val percent = percent(frequency.outside, frequency.daysRecorded)
    return "As calorias ficaram fora da faixa de ±5% da meta em ${frequency.outside} de " +
        "${frequency.daysRecorded} dias registrados ($percent%)."
}

private fun percent(count: Int, total: Int): Int =
    if (total == 0) 0 else (count.toDouble() / total * 100.0).roundToInt()

private fun sampleText(count: Int): String =
    if (count == 1) "1 dia registrado" else "$count dias registrados"

private fun metricName(metric: PatternMetric): String = when (metric) {
    PatternMetric.CALORIES -> "Calorias"
    PatternMetric.PROTEIN -> "Proteína"
    PatternMetric.CARBS -> "Carboidratos"
    PatternMetric.FAT -> "Gorduras"
}

private fun weekdayName(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "Segunda-feira"
    DayOfWeek.TUESDAY -> "Terça-feira"
    DayOfWeek.WEDNESDAY -> "Quarta-feira"
    DayOfWeek.THURSDAY -> "Quinta-feira"
    DayOfWeek.FRIDAY -> "Sexta-feira"
    DayOfWeek.SATURDAY -> "Sábado"
    DayOfWeek.SUNDAY -> "Domingo"
}

private const val commonGoalLimitation =
    "As metas são configuradas localmente. Dias sem registro ficam fora da amostra, e a comparação " +
        "não avalia alimentos, micronutrientes ou adequação nutricional."
