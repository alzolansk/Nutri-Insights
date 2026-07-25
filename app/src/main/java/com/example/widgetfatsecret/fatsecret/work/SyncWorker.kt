package com.example.widgetfatsecret.fatsecret.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.widgetfatsecret.fatsecret.data.AppContainer
import com.example.widgetfatsecret.fatsecret.data.SyncErrorType
import com.example.widgetfatsecret.fatsecret.data.SyncResult

/**
 * Performs one FatSecret sync and refreshes the widget. Used both for the
 * periodic background sync and for on-demand ("sync now") requests.
 *
 * Retry policy: transient failures (network/server/rate-limit) return
 * [Result.retry] so WorkManager applies exponential backoff. Terminal failures
 * (not connected / no credentials / auth invalid) return [Result.success] — the
 * widget already shows the correct state and retrying would not help.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer.get(applicationContext)
        // Same entry point the UI uses, so a periodic run and a manual tap share
        // one sync instead of racing each other's writes. It already performs
        // the widget refresh after persisting, for both widgets.
        val result = container.syncAndRefresh().await()

        return when (result) {
            is SyncResult.Success -> Result.success()
            is SyncResult.Failure -> when (result.type) {
                SyncErrorType.NETWORK,
                SyncErrorType.SERVER,
                SyncErrorType.RATE_LIMIT,
                SyncErrorType.UNKNOWN -> Result.retry()
                else -> Result.success()
            }
        }
    }
}
