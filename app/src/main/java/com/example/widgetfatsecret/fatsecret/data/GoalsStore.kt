package com.example.widgetfatsecret.fatsecret.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.widgetfatsecret.fatsecret.domain.NutritionGoals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.goalsDataStore: DataStore<Preferences> by preferencesDataStore(name = "nutrition_goals")

/** Persists the user's editable daily targets with DataStore Preferences. */
class GoalsStore(private val context: Context) {

    val goalsFlow: Flow<NutritionGoals> = context.goalsDataStore.data.map { p ->
        NutritionGoals(
            caloriesKcal = p[KEY_CALORIES] ?: NutritionGoals.DEFAULT_CALORIES,
            proteinG = p[KEY_PROTEIN] ?: NutritionGoals.DEFAULT_PROTEIN,
            carbsG = p[KEY_CARBS] ?: NutritionGoals.DEFAULT_CARBS,
            fatG = p[KEY_FAT] ?: NutritionGoals.DEFAULT_FAT,
        )
    }

    /**
     * The user's own starting weight, in kg, or null when they have not set one.
     *
     * FatSecret shows a "Peso Inicial" that is NOT the oldest weighing in the
     * diary and is not exposed by `profile.get` (verified against the live API),
     * so it cannot be imported — same situation as the calorie/macro targets.
     * When set, it anchors "total" and "progresso" so the widget agrees with the
     * FatSecret app; when absent, the oldest weighing found in the diary is used.
     */
    val startWeightFlow: Flow<Double?> = context.goalsDataStore.data.map { p ->
        p[KEY_START_WEIGHT]?.takeIf { it > 0.0 }
    }

    /** Passing null (or a non-positive value) clears the override. */
    suspend fun saveStartWeight(kg: Double?) {
        context.goalsDataStore.edit { p ->
            if (kg != null && kg > 0.0) p[KEY_START_WEIGHT] = kg else p.remove(KEY_START_WEIGHT)
        }
    }

    suspend fun save(goals: NutritionGoals) {
        context.goalsDataStore.edit { p ->
            p[KEY_CALORIES] = goals.caloriesKcal
            p[KEY_PROTEIN] = goals.proteinG
            p[KEY_CARBS] = goals.carbsG
            p[KEY_FAT] = goals.fatG
        }
    }

    private companion object {
        val KEY_CALORIES = intPreferencesKey("calories")
        val KEY_PROTEIN = intPreferencesKey("protein")
        val KEY_CARBS = intPreferencesKey("carbs")
        val KEY_FAT = intPreferencesKey("fat")
        val KEY_START_WEIGHT = doublePreferencesKey("start_weight_kg")
    }
}
