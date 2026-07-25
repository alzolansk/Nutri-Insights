package com.example.widgetfatsecret.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.widgetfatsecret.fatsecret.data.NutritionUiState
import com.example.widgetfatsecret.fatsecret.data.SyncStatus as DataSyncStatus
import com.example.widgetfatsecret.fatsecret.domain.NutritionFormat
import com.example.widgetfatsecret.fatsecret.domain.NutritionGoals
import com.example.widgetfatsecret.fatsecret.domain.WeightFormat
import com.example.widgetfatsecret.ui.design.NutriPrimaryButton
import com.example.widgetfatsecret.ui.design.NutriSecondaryButton
import com.example.widgetfatsecret.ui.design.NutriSpacing
import com.example.widgetfatsecret.ui.design.StatCard
import com.example.widgetfatsecret.ui.design.SyncStatusChip
import com.example.widgetfatsecret.ui.toChipStatus
import com.example.widgetfatsecret.ui.toUserMessage
import com.example.widgetfatsecret.ui.theme.nutriColors

/**
 * Route entry point wired into [com.example.widgetfatsecret.ui.navigation.AppShell]
 * for [com.example.widgetfatsecret.ui.navigation.Route.MetasConta] (planning.md
 * §9, Etapa 5). Unlike [com.example.widgetfatsecret.ui.today.TodayViewModel],
 * [viewModel] here must be the SAME instance MainActivity created (it owns the
 * OAuth callback and the app-open sync) — so the caller passes it in explicitly
 * instead of resolving a fresh one scoped to this nav-graph entry.
 */
@Composable
fun GoalsAccountRoute(viewModel: AccountViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val syncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val startWeight by viewModel.startWeight.collectAsStateWithLifecycle()
    val discoveredStart by viewModel.discoveredStartWeight.collectAsStateWithLifecycle()

    GoalsAccountScreen(
        state = state,
        connecting = connecting,
        syncing = syncing,
        startWeightKg = startWeight,
        discoveredStartWeightKg = discoveredStart,
        onConnect = viewModel::connect,
        onDisconnect = viewModel::disconnect,
        onSync = viewModel::syncNow,
        onSave = viewModel::saveGoals,
        modifier = modifier,
    )
}

/**
 * "Metas e conta": conectar/desconectar, sincronização manual, metas diárias e
 * peso inicial — regras preservadas da interface original mais as ações de
 * conta (planning.md §9, Etapa 5). Não existe design de
 * referência para o card de conta no protótipo (ver planning.md §0, "Achado
 * importante"), então a casca visual é desenhada ad-hoc com os tokens do
 * design system (Etapa 2) — os textos e o comportamento são preservados
 * literalmente.
 */
@Composable
fun GoalsAccountScreen(
    state: NutritionUiState,
    connecting: Boolean,
    syncing: Boolean,
    startWeightKg: Double?,
    discoveredStartWeightKg: Double?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
    onSave: (NutritionGoals, Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = state.snapshot
    val goals = state.goals

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
            .padding(NutriSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(NutriSpacing.lg),
    ) {
        SyncStatusChip(
            status = snapshot.toChipStatus(),
            detail = snapshot.lastSyncMillis.takeIf { it > 0 }?.let { NutritionFormat.timeAgo(it) },
        )

        if (snapshot.connected && snapshot.syncStatus == DataSyncStatus.ERROR) {
            StatCard(title = "Última sincronização") {
                Text(
                    text = if (snapshot.hasValidData) {
                        "A atualização falhou, mas os últimos dados válidos continuam visíveis nas telas. " +
                            (snapshot.errorType?.toUserMessage() ?: "Tente novamente mais tarde.")
                    } else {
                        snapshot.errorType?.toUserMessage()
                            ?: "Não foi possível trazer os dados. Tente sincronizar novamente."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.nutriColors.text2,
                )
            }
        }

        AccountCard(
            connected = snapshot.connected,
            connecting = connecting,
            syncing = syncing,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
            onSync = onSync,
        )

        GoalsCard(
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            onCaloriesChange = { calories = it },
            onProteinChange = { protein = it },
            onCarbsChange = { carbs = it },
            onFatChange = { fat = it },
        )

        StartWeightCard(
            value = startWeight,
            discoveredStartWeightKg = discoveredStartWeightKg,
            onChange = { startWeight = it },
        )

        NutriPrimaryButton(
            text = "Salvar",
            modifier = Modifier.fillMaxWidth(),
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
                    // Blank clears the override; a comma is accepted because the
                    // pt-BR keyboard produces one.
                    startWeight.replace(',', '.').toDoubleOrNull(),
                )
            },
        )
    }
}

@Composable
private fun AccountCard(
    connected: Boolean,
    connecting: Boolean,
    syncing: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
) {
    val colors = MaterialTheme.nutriColors
    StatCard(title = "Conta FatSecret") {
        Text(
            text = if (connected) "Conectado" else "Desconectado",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text2,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NutriSpacing.md),
        ) {
            if (connected) {
                NutriPrimaryButton(
                    text = if (syncing) "Sincronizando…" else "Sincronizar",
                    enabled = !syncing,
                    onClick = onSync,
                    modifier = Modifier.weight(1f),
                )
                NutriSecondaryButton(
                    text = "Desconectar",
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                )
            } else {
                NutriPrimaryButton(
                    text = if (connecting) "Conectando…" else "Conectar ao FatSecret",
                    enabled = !connecting,
                    onClick = onConnect,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GoalsCard(
    calories: String,
    protein: String,
    carbs: String,
    fat: String,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
) {
    val colors = MaterialTheme.nutriColors
    StatCard(title = "Metas diárias") {
        // The API genuinely has no endpoint for the account's calorie/macro
        // targets (only goal_weight_kg, in profile.get), so these numbers cannot
        // be imported. Saying so here avoids the "why didn't it sync my goals?"
        // confusion. See README section 4. Metas de macros são locais (slide 12).
        Text(
            "A API do FatSecret não disponibiliza as metas da sua conta, apenas o " +
                "que foi consumido. Por isso as metas ficam salvas neste aparelho — " +
                "se você alterá-las no FatSecret, reajuste aqui.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.text2,
        )
        NumberField("Calorias (kcal)", calories, onCaloriesChange)
        NumberField("Proteína (g)", protein, onProteinChange)
        NumberField("Carboidratos (g)", carbs, onCarbsChange)
        NumberField("Gorduras (g)", fat, onFatChange)
    }
}

@Composable
private fun StartWeightCard(
    value: String,
    discoveredStartWeightKg: Double?,
    onChange: (String) -> Unit,
) {
    val colors = MaterialTheme.nutriColors
    StatCard(title = "Peso inicial") {
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
            color = colors.text2,
        )
        DecimalField("Peso inicial (kg)", value, onChange)
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
        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth(),
    )
}
