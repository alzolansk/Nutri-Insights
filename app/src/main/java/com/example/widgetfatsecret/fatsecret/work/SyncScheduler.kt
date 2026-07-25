package com.example.widgetfatsecret.fatsecret.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules FatSecret syncs responsibly: one periodic job (every 30 min, only
 * when online, with exponential backoff) and an on-demand "sync now" job.
 * No permanent services, no tight loops.
 */
object SyncScheduler {

    private const val PERIODIC_WORK = "fatsecret_periodic_sync"
    private const val ONESHOT_WORK = "fatsecret_sync_now"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Ensures the periodic sync is scheduled (idempotent). */
    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Runs a sync as soon as possible. Currently unreferenced: the widget's
     * refresh button was removed in favour of a quieter layout, and the app
     * syncs directly through the repository on open. Kept because it is the
     * constraint-aware, retrying way to trigger a sync from outside a
     * coroutine scope — use it instead of hand-rolling one.
     */
    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONESHOT_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /** Cancels all account-related work (used on disconnect). */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(ONESHOT_WORK)
    }
}
