package com.example.widgetfatsecret.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.widgetfatsecret.fatsecret.data.AppContainer
import com.example.widgetfatsecret.fatsecret.data.MissingCredentialsException
import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.data.NutritionUiState
import com.example.widgetfatsecret.fatsecret.data.SyncErrorType
import com.example.widgetfatsecret.fatsecret.data.SyncResult
import com.example.widgetfatsecret.fatsecret.domain.NutritionGoals
import com.example.widgetfatsecret.fatsecret.widget.NutritionWidget
import com.example.widgetfatsecret.fatsecret.widget.WeightWidget
import com.example.widgetfatsecret.fatsecret.work.SyncScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-shot UI signals (navigation / transient messages). */
sealed interface UiEvent {
    data class OpenBrowser(val url: String) : UiEvent
    data class Message(val text: String) : UiEvent
}

class FatSecretViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext
    private val container = AppContainer.get(app)
    private val repo = container.repository

    val uiState = repo.uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NutritionUiState(NutritionSnapshot(), NutritionGoals.DEFAULT),
    )

    /** The manually set starting weight, and the diary's oldest weighing as a hint. */
    val startWeight = repo.startWeightFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )
    val discoveredStartWeight = repo.discoveredStartWeightFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val isConnecting = MutableStateFlow(false)

    /** Drives the button's disabled/"Sincronizando…" state. */
    val isSyncing = MutableStateFlow(false)

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    init {
        // Keep the persisted "connected" flag in sync with the token store, and
        // make sure the periodic worker is scheduled whenever we're connected.
        viewModelScope.launch {
            val connected = repo.isConnected()
            container.cacheStore.setConnected(connected)
            if (connected) {
                SyncScheduler.ensurePeriodic(appContext)
                syncNow()
            }
        }
    }

    /** Begins the OAuth handshake and asks the UI to open the authorize URL. */
    fun connect() {
        if (isConnecting.value) return
        viewModelScope.launch {
            isConnecting.value = true
            try {
                val url = repo.beginConnect()
                _events.emit(UiEvent.OpenBrowser(url))
            } catch (e: MissingCredentialsException) {
                _events.emit(UiEvent.Message("Preencha as credenciais em local.properties (veja o README)."))
            } catch (e: Exception) {
                _events.emit(UiEvent.Message("Falha ao iniciar a conexão. Verifique a rede e as credenciais."))
            } finally {
                isConnecting.value = false
            }
        }
    }

    /** Called from the deep-link callback: exchanges the verifier for tokens. */
    fun handleCallback(uri: Uri) {
        val verifier = uri.getQueryParameter("oauth_verifier")
        if (verifier.isNullOrBlank()) {
            viewModelScope.launch {
                _events.emit(UiEvent.Message("Autorização negada ou callback inválido."))
            }
            return
        }
        viewModelScope.launch {
            when (val result = repo.completeConnect(verifier)) {
                is SyncResult.Success -> {
                    SyncScheduler.ensurePeriodic(appContext)
                    updateWidgets()
                    _events.emit(UiEvent.Message("Conectado ao FatSecret!"))
                }
                is SyncResult.Failure -> {
                    updateWidgets()
                    _events.emit(UiEvent.Message(messageFor(result.type)))
                }
            }
        }
    }

    /**
     * Manual sync (also runs on app open when connected).
     *
     * The work itself runs on the container's application scope, not here: the
     * sequence is fetch → persist → refresh widgets, and if this coroutine were
     * cancelled between the last two steps (leaving the screen, rotation, the
     * ViewModel clearing) the data would land on disk with the widgets never
     * told about it. This scope only *observes* the shared run so the button can
     * show progress; cancelling the observation never cancels the sync.
     */
    fun syncNow() {
        viewModelScope.launch {
            isSyncing.value = true
            try {
                container.syncAndRefresh().await()
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            // Documented decision: clear the cache on disconnect so the widget
            // never shows stale data from a disconnected account.
            repo.disconnect(clearCache = true)
            SyncScheduler.cancelAll(appContext)
            updateWidgets()
            _events.emit(UiEvent.Message("Desconectado."))
        }
    }

    /**
     * Persists everything the settings screen owns, then refreshes the widgets
     * ONCE.
     *
     * Both writes live in a single coroutine on purpose: when they were two
     * independent launches with an [updateWidgets] each, the two Glance sessions
     * raced and the winning composition could be one that started before the
     * second write committed — the widget then kept the old value until the next
     * sync. One write sequence, one refresh, no race.
     */
    fun saveGoals(goals: NutritionGoals, startWeightKg: Double?) {
        viewModelScope.launch {
            repo.saveGoals(goals)
            repo.saveStartWeight(startWeightKg)
            updateWidgets()
            // Recompute against the new goals right away.
            if (repo.isConnected()) syncNow()
        }
    }

    /**
     * Both widgets read the same caches, so every state change that can affect
     * either one refreshes both. Kept in one place so a new widget cannot be
     * forgotten at one of the call sites.
     */
    private suspend fun updateWidgets() {
        NutritionWidget.updateAll(appContext)
        WeightWidget.updateAll(appContext)
    }

    private fun messageFor(type: SyncErrorType): String = when (type) {
        SyncErrorType.NO_CREDENTIALS -> "Credenciais ausentes. Preencha local.properties."
        SyncErrorType.NOT_CONNECTED -> "Conta não conectada."
        SyncErrorType.NETWORK -> "Sem conexão. Tentaremos novamente automaticamente."
        SyncErrorType.RATE_LIMIT -> "Limite de requisições atingido. Aguarde um pouco."
        SyncErrorType.SERVER -> "Falha temporária do servidor FatSecret."
        SyncErrorType.AUTH_INVALID -> "Autorização inválida ou expirada. Reconecte a conta."
        SyncErrorType.EMPTY -> "Resposta vazia do servidor."
        SyncErrorType.UNKNOWN -> "Erro inesperado ao sincronizar."
    }

    fun messageForPublic(type: SyncErrorType): String = messageFor(type)
}
