package com.example.widgetfatsecret.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.widgetfatsecret.ui.design.EmptyState
import com.example.widgetfatsecret.ui.theme.nutriColors
import com.example.widgetfatsecret.ui.today.TodayRoute

/**
 * Casca de navegação do "Nutri Insights" (planning.md §9, Etapa 3): Scaffold +
 * barra de navegação com as 5 abas + rota de Metas/Conta aberta pelo avatar no
 * topo. `Hoje` já tem conteúdo real desde a Etapa 4 ([TodayRoute]); as demais
 * rotas mostram apenas um placeholder até as Etapas 5-9. O NavHost não sabe
 * nada sobre OAuth: o deep link continua tratado pela Activity (planning.md
 * §6, item 4).
 */
@Composable
fun AppShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onMetasConta = currentDestination?.hierarchy?.any { it.hasRoute<Route.MetasConta>() } == true

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                onAvatarClick = {
                    navController.navigate(Route.MetasConta) { launchSingleTop = true }
                },
            )
        },
        bottomBar = {
            if (!onMetasConta) {
                AppBottomBar(navController = navController, currentDestination = currentDestination)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Hoje,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Route.Hoje> {
                TodayRoute()
            }
            composable<Route.Tendencias> {
                PlaceholderScreen(
                    title = "Tendências",
                    description = "Médias de 7/14/30 dias e o gráfico de calorias chegam na Etapa 6.",
                )
            }
            composable<Route.Padroes> {
                PlaceholderScreen(
                    title = "Padrões",
                    description = "Padrões por dia da semana e a folha de metodologia chegam na Etapa 7.",
                )
            }
            composable<Route.Consistencia> {
                PlaceholderScreen(
                    title = "Consistência",
                    description = "O calendário mensal e as sequências chegam na Etapa 8.",
                )
            }
            composable<Route.Peso> {
                PlaceholderScreen(
                    title = "Peso",
                    description = "Peso atual, delta e evolução chegam na Etapa 9.",
                )
            }
            composable<Route.MetasConta> {
                PlaceholderScreen(
                    title = "Metas e conta",
                    description = "Metas, conectar/desconectar e sincronização manual chegam na Etapa 5.",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(onAvatarClick: () -> Unit) {
    val colors = MaterialTheme.nutriColors
    TopAppBar(
        title = { Text("Nutri Insights") },
        actions = {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.surface2, CircleShape)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "⚙", color = colors.text)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.bg,
            titleContentColor = colors.text,
        ),
    )
}

@Composable
private fun AppBottomBar(navController: NavHostController, currentDestination: androidx.navigation.NavDestination?) {
    val colors = MaterialTheme.nutriColors
    NavigationBar(containerColor = colors.surface) {
        bottomTabs.forEach { (route, label) ->
            val selected = when (route) {
                Route.Hoje -> currentDestination?.hierarchy?.any { it.hasRoute<Route.Hoje>() } == true
                Route.Tendencias -> currentDestination?.hierarchy?.any { it.hasRoute<Route.Tendencias>() } == true
                Route.Padroes -> currentDestination?.hierarchy?.any { it.hasRoute<Route.Padroes>() } == true
                Route.Consistencia -> currentDestination?.hierarchy?.any { it.hasRoute<Route.Consistencia>() } == true
                Route.Peso -> currentDestination?.hierarchy?.any { it.hasRoute<Route.Peso>() } == true
                else -> false
            }
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Text(text = label.take(1), color = if (selected) colors.mint else colors.text2) },
                label = { Text(text = label) },
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(title = title, description = description, icon = "🛠")
    }
}
