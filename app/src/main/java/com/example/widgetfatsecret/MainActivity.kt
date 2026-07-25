package com.example.widgetfatsecret

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.widgetfatsecret.ui.UiEvent
import com.example.widgetfatsecret.ui.account.AccountViewModel
import com.example.widgetfatsecret.ui.navigation.AppShell
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import kotlinx.coroutines.flow.Flow
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
                val vm: AccountViewModel = viewModel()
                OAuthCallbackAndEventEffects(vm.events, vm::handleCallback, callbackUri)
                AppShell(accountViewModel = vm)
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
