package com.example.widgetfatsecret

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.widgetfatsecret.ui.FatSecretViewModel
import com.example.widgetfatsecret.ui.GoalsSettingsScreen
import com.example.widgetfatsecret.ui.MainScreen
import com.example.widgetfatsecret.ui.UiEvent
import com.example.widgetfatsecret.ui.account.AccountViewModel
import com.example.widgetfatsecret.ui.navigation.AppShell
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Convivência entre a UI antiga (`AppRoot`) e a nova casca de navegação
 * (`AppShell`, planning.md §9 Etapa 3), enquanto as Etapas 4-11 migram tela
 * por tela. `false` = a nova UI é a padrão; alterne para `true` (e recompile)
 * para voltar à UI antiga como rede de segurança. Ver planning.md §6, item 6.
 */
private const val USE_LEGACY_UI = false

class MainActivity : ComponentActivity() {

    // Compose-observable so both onCreate and onNewIntent trigger handling.
    private val callbackUri: MutableState<Uri?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        callbackUri.value = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data

        setContent {
            WidgetFatSecretTheme {
                // Tratamento do deep link OAuth e dos eventos do ViewModel vivem
                // aqui, fora de AppRoot/AppShell, para continuar funcionando
                // identicamente não importa qual UI está visível (planning.md §6).
                // Só UM dos dois ViewModels é instanciado por processo — nunca os
                // dois — para que o sync de abertura não dispare duas vezes
                // (planning.md §7/§10, risco R5): o legado usa FatSecretViewModel
                // até a Etapa 11 remover a UI antiga; a UI nova usa AccountViewModel
                // desde a Etapa 5.
                if (USE_LEGACY_UI) {
                    val vm: FatSecretViewModel = viewModel()
                    OAuthCallbackAndEventEffects(vm.events, vm::handleCallback, callbackUri)
                    AppRoot(vm)
                } else {
                    val vm: AccountViewModel = viewModel()
                    OAuthCallbackAndEventEffects(vm.events, vm::handleCallback, callbackUri)
                    AppShell(accountViewModel = vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            callbackUri.value = intent.data
        }
    }
}

@Composable
private fun OAuthCallbackAndEventEffects(
    events: Flow<UiEvent>,
    onCallback: (Uri) -> Unit,
    callbackUri: MutableState<Uri?>,
) {
    val context = LocalContext.current

    // Consume a deep-link callback (oauth_verifier) exactly once.
    LaunchedEffect(callbackUri.value) {
        callbackUri.value?.let {
            onCallback(it)
            callbackUri.value = null
        }
    }

    // One-shot events: open the authorize URL, or show a message.
    LaunchedEffect(Unit) {
        events.collectLatest { event ->
            when (event) {
                is UiEvent.OpenBrowser -> runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, event.url.toUri()))
                }
                is UiEvent.Message ->
                    Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
private fun AppRoot(vm: FatSecretViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val connecting by vm.isConnecting.collectAsStateWithLifecycle()
    val syncing by vm.isSyncing.collectAsStateWithLifecycle()
    val startWeight by vm.startWeight.collectAsStateWithLifecycle()
    val discoveredStart by vm.discoveredStartWeight.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (showSettings) {
            GoalsSettingsScreen(
                goals = state.goals,
                startWeightKg = startWeight,
                discoveredStartWeightKg = discoveredStart,
                onSave = vm::saveGoals,
                onBack = { showSettings = false },
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            MainScreen(
                state = state,
                connecting = connecting,
                syncing = syncing,
                onConnect = vm::connect,
                onDisconnect = vm::disconnect,
                onSync = vm::syncNow,
                onOpenSettings = { showSettings = true },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
