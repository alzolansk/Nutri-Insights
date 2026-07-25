package com.example.widgetfatsecret.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.fatsecret.data.NutritionUiState
import com.example.widgetfatsecret.fatsecret.data.SyncStatus
import com.example.widgetfatsecret.fatsecret.domain.NutritionCalculator
import com.example.widgetfatsecret.fatsecret.domain.NutritionFormat
import com.example.widgetfatsecret.fatsecret.domain.NutritionGoals
import com.example.widgetfatsecret.fatsecret.domain.WeightFormat

@Composable
fun MainScreen(
    state: NutritionUiState,
    connecting: Boolean,
    syncing: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connected = state.snapshot.connected
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("FatSecret", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            if (connected) "Conta conectada" else "Conta desconectada",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        TodaySummaryCard(state)

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (connected) {
                Button(
                    onClick = onSync,
                    enabled = !syncing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (syncing) "Sincronizando…" else "Sincronizar")
                }
                OutlinedButton(onClick = onDisconnect, modifier = Modifier.weight(1f)) {
                    Text("Desconectar")
                }
            } else {
                Button(
                    onClick = onConnect,
                    enabled = !connecting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (connecting) "Conectando…" else "Conectar ao FatSecret")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Configurar metas")
        }
    }
}

@Composable
private fun TodaySummaryCard(state: NutritionUiState) {
    val daily = state.snapshot.daily
    val goals = state.goals
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Hoje", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Text(
                "${NutritionFormat.ratio(daily.calories, goals.caloriesKcal)} kcal",
                style = MaterialTheme.typography.headlineSmall,
            )
            LinearProgressIndicator(
                progress = { NutritionCalculator.progressFraction(daily.calories, goals.caloriesKcal) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            MacroRow("Proteína", daily.protein, goals.proteinG)
            MacroRow("Carboidratos", daily.carbs, goals.carbsG)
            MacroRow("Gorduras", daily.fat, goals.fatG)

            Spacer(Modifier.height(8.dp))
            Text(
                NutritionFormat.insightText(NutritionCalculator.buildInsight(daily, goals)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            val statusText = when (state.snapshot.syncStatus) {
                SyncStatus.LOADING -> "Sincronizando…"
                SyncStatus.ERROR -> "Última sincronização falhou"
                else -> "Registros hoje: ${daily.entryCount}"
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MacroRow(label: String, consumed: Double, goal: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${NutritionFormat.int(consumed)}/${NutritionFormat.int(goal)} g  •  ${
                NutritionFormat.percentText(consumed, goal)
            }",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun GoalsSettingsScreen(
    goals: NutritionGoals,
    startWeightKg: Double?,
    discoveredStartWeightKg: Double?,
    onSave: (NutritionGoals, Double?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var calories by remember(goals) { mutableStateOf(goals.caloriesKcal.toString()) }
    var protein by remember(goals) { mutableStateOf(goals.proteinG.toString()) }
    var carbs by remember(goals) { mutableStateOf(goals.carbsG.toString()) }
    var fat by remember(goals) { mutableStateOf(goals.fatG.toString()) }
    var startWeight by remember(startWeightKg) {
        mutableStateOf(startWeightKg?.let { WeightFormat.weightValue(it, usesPounds = false) } ?: "")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Metas diárias", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Valores configuráveis por você — não são recomendações nutricionais.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        // The API genuinely has no endpoint for the account's calorie/macro
        // targets (only goal_weight_kg, in profile.get), so these numbers cannot
        // be imported. Saying so here avoids the "why didn't it sync my goals?"
        // confusion. See README section 4.
        Text(
            "A API do FatSecret não disponibiliza as metas da sua conta, apenas o " +
                "que foi consumido. Por isso as metas ficam salvas neste aparelho — " +
                "se você alterá-las no FatSecret, reajuste aqui.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        NumberField("Calorias (kcal)", calories) { calories = it }
        NumberField("Proteína (g)", protein) { protein = it }
        NumberField("Carboidratos (g)", carbs) { carbs = it }
        NumberField("Gorduras (g)", fat) { fat = it }

        Spacer(Modifier.height(20.dp))
        Text("Peso inicial", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        // The "Peso Inicial" the FatSecret app shows is not the oldest weighing
        // in the diary and is not exposed by the API, so it has to be typed in
        // once for the widget's "total" and "progresso" to match that app.
        Text(
            "O FatSecret não disponibiliza o seu peso inicial pela API. Informe-o " +
                "aqui para que o total perdido e o progresso da meta batam com o " +
                "aplicativo. Em branco, é usada a pesagem mais antiga do diário" +
                (discoveredStartWeightKg?.let {
                    " (${WeightFormat.weight(it, usesPounds = false)})"
                } ?: "") + ".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        DecimalField("Peso inicial (kg)", startWeight) { startWeight = it }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Voltar") }
            Button(
                onClick = {
                    onSave(
                        NutritionGoals(
                            caloriesKcal = calories.toIntOrNull()?.coerceAtLeast(0)
                                ?: NutritionGoals.DEFAULT_CALORIES,
                            proteinG = protein.toIntOrNull()?.coerceAtLeast(0)
                                ?: NutritionGoals.DEFAULT_PROTEIN,
                            carbsG = carbs.toIntOrNull()?.coerceAtLeast(0)
                                ?: NutritionGoals.DEFAULT_CARBS,
                            fatG = fat.toIntOrNull()?.coerceAtLeast(0)
                                ?: NutritionGoals.DEFAULT_FAT,
                        ),
                        // Blank clears the override; a comma is accepted because
                        // the pt-BR keyboard produces one.
                        startWeight.replace(',', '.').toDoubleOrNull(),
                    )
                    onBack()
                },
                modifier = Modifier.weight(1f),
            ) { Text("Salvar") }
        }
    }
}

/** Like [NumberField], but keeps a single decimal separator (`,` or `.`). */
@Composable
private fun DecimalField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            val cleaned = buildString {
                var seenSeparator = false
                new.forEach { c ->
                    when {
                        c.isDigit() -> append(c)
                        (c == ',' || c == '.') && !seenSeparator && isNotEmpty() -> {
                            seenSeparator = true
                            append(',')
                        }
                    }
                }
            }
            onChange(cleaned.take(6))
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onChange(new.filter { it.isDigit() }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}
