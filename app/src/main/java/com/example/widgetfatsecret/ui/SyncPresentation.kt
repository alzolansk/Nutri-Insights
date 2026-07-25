package com.example.widgetfatsecret.ui

import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.data.SyncErrorType
import com.example.widgetfatsecret.fatsecret.data.SyncStatus as DataSyncStatus
import com.example.widgetfatsecret.fatsecret.data.WeightSnapshot
import com.example.widgetfatsecret.ui.design.SyncStatus

/**
 * The six data states from slide 10, plus the account-disconnected state that
 * already existed before Etapa 10. A failed refresh with valid cached data is
 * intentionally [CONTENT]: the dated status chip communicates the failure while
 * the screen keeps rendering the last successful snapshot.
 */
enum class ContentState {
    DISCONNECTED,
    LOADING,
    NOT_SYNCED,
    SYNC_ERROR,
    EMPTY,
    CONTENT,
}

fun resolveContentState(
    connected: Boolean,
    syncStatus: DataSyncStatus,
    hasUsableData: Boolean,
    hasRecords: Boolean,
): ContentState = when {
    !connected -> ContentState.DISCONNECTED
    syncStatus == DataSyncStatus.LOADING && !hasUsableData -> ContentState.LOADING
    syncStatus == DataSyncStatus.ERROR && !hasUsableData -> ContentState.SYNC_ERROR
    !hasUsableData -> ContentState.NOT_SYNCED
    !hasRecords -> ContentState.EMPTY
    else -> ContentState.CONTENT
}

fun NutritionSnapshot.toContentState(
    hasUsableData: Boolean = hasValidData,
    hasRecords: Boolean = hasEntries,
    loading: Boolean = syncStatus == DataSyncStatus.LOADING,
): ContentState = resolveContentState(
    connected = connected,
    syncStatus = if (loading) DataSyncStatus.LOADING else syncStatus,
    hasUsableData = hasUsableData,
    hasRecords = hasRecords,
)

fun WeightSnapshot.toContentState(
    connected: Boolean = this.connected,
    loading: Boolean = syncStatus == DataSyncStatus.LOADING,
): ContentState =
    resolveContentState(
        connected = connected,
        syncStatus = if (loading) DataSyncStatus.LOADING else syncStatus,
        hasUsableData = hasValidData,
        hasRecords = entries.isNotEmpty(),
    )

fun NutritionSnapshot.toChipStatus(): SyncStatus = when {
    !connected -> SyncStatus.DISCONNECTED
    syncStatus == DataSyncStatus.LOADING -> SyncStatus.SYNCING
    syncStatus == DataSyncStatus.ERROR && hasValidData -> SyncStatus.OFFLINE
    syncStatus == DataSyncStatus.ERROR -> SyncStatus.ERROR
    else -> SyncStatus.SYNCED
}

fun WeightSnapshot.toChipStatus(
    connected: Boolean = this.connected,
    loading: Boolean = syncStatus == DataSyncStatus.LOADING,
): SyncStatus = when {
    !connected -> SyncStatus.DISCONNECTED
    loading -> SyncStatus.SYNCING
    syncStatus == DataSyncStatus.ERROR && hasValidData -> SyncStatus.OFFLINE
    syncStatus == DataSyncStatus.ERROR -> SyncStatus.ERROR
    else -> SyncStatus.SYNCED
}

fun SyncErrorType.toUserMessage(): String = when (this) {
    SyncErrorType.NO_CREDENTIALS -> "Credenciais ausentes. Preencha local.properties."
    SyncErrorType.NOT_CONNECTED -> "Conta não conectada."
    SyncErrorType.NETWORK -> "Sem conexão. Tentaremos novamente automaticamente."
    SyncErrorType.RATE_LIMIT -> "Limite de requisições atingido. Aguarde um pouco."
    SyncErrorType.SERVER -> "Falha temporária do servidor FatSecret."
    SyncErrorType.AUTH_INVALID -> "Autorização inválida ou expirada. Reconecte a conta."
    SyncErrorType.EMPTY -> "O servidor retornou uma resposta vazia."
    SyncErrorType.UNKNOWN -> "Erro inesperado ao sincronizar."
}
