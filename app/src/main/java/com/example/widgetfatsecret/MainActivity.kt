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
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    // Compose-observable so both onCreate and onNewIntent trigger handling.
    private val callbackUri: MutableState<Uri?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        callbackUri.value = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data

        setContent {
            WidgetFatSecretTheme {
                val vm: FatSecretViewModel = viewModel()
                AppRoot(vm, callbackUri)
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
private fun AppRoot(vm: FatSecretViewModel, callbackUri: MutableState<Uri?>) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsStateWithLifecycle()
    val connecting by vm.isConnecting.collectAsStateWithLifecycle()
    val syncing by vm.isSyncing.collectAsStateWithLifecycle()
    val startWeight by vm.startWeight.collectAsStateWithLifecycle()
    val discoveredStart by vm.discoveredStartWeight.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    // Consume a deep-link callback (oauth_verifier) exactly once.
    LaunchedEffect(callbackUri.value) {
        callbackUri.value?.let {
            vm.handleCallback(it)
            callbackUri.value = null
        }
    }

    // One-shot events: open the authorize URL, or show a message.
    LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is UiEvent.OpenBrowser -> runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, event.url.toUri()))
                }
                is UiEvent.Message ->
                    Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
            }
        }
    }

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
