package com.example.widgetfatsecret.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Rotas tipadas do [AppShell] (planning.md §9, Etapa 3). As 5 abas da barra de
 * navegação mais a rota de Metas/Conta (aberta pelo avatar no topo, fora da
 * barra). Nenhuma delas navega para fora do NavHost — o deep link OAuth
 * continua tratado pela Activity, não por estas rotas (planning.md §6).
 */
sealed interface Route {
    @Serializable
    data object Hoje : Route

    @Serializable
    data object Tendencias : Route

    @Serializable
    data object Padroes : Route

    @Serializable
    data object Consistencia : Route

    @Serializable
    data object Peso : Route

    @Serializable
    data object MetasConta : Route
}

/** As 5 abas visíveis na [androidx.compose.material3.NavigationBar], em ordem. */
val bottomTabs: List<Pair<Route, String>> = listOf(
    Route.Hoje to "Hoje",
    Route.Tendencias to "Tendências",
    Route.Padroes to "Padrões",
    Route.Consistencia to "Consistência",
    Route.Peso to "Peso",
)
