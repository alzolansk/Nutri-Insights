package com.example.widgetfatsecret.fatsecret.data

import android.content.Context
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretAuthClient
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretConfig
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretFoodClient
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretService
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretWeightClient
import com.example.widgetfatsecret.fatsecret.data.remote.OAuth1SigningInterceptor
import com.example.widgetfatsecret.fatsecret.widget.NutritionWidget
import com.example.widgetfatsecret.fatsecret.widget.WeightWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Tiny hand-rolled dependency container (no DI framework needed for a personal
 * app). One instance is shared across the Activity, the WorkManager worker and
 * the Glance widget via [get].
 */
class AppContainer private constructor(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Scope for work that must finish even if the screen that started it goes
     * away. A sync writes to disk and THEN refreshes the widgets; running that
     * on `viewModelScope` meant closing the app mid-sync could cancel the job
     * between those two steps, leaving fresh data persisted but every widget
     * still showing the old numbers.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val syncLock = Any()
    private var inFlightSync: Deferred<SyncResult>? = null

    val tokenStore = TokenStore(context)
    val goalsStore = GoalsStore(context)
    val cacheStore = NutritionCacheStore(context)
    val weightCacheStore = WeightCacheStore(context)

    // Plain client for the OAuth handshake legs (signed manually).
    private val authHttp = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val authClient = FatSecretAuthClient(
        client = authHttp,
        consumerKey = FatSecretConfig.consumerKey,
        consumerSecret = FatSecretConfig.consumerSecret,
    )

    // API client: every request is signed with the stored access token.
    private val apiHttp = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(
            OAuth1SigningInterceptor(
                consumerKey = FatSecretConfig.consumerKey,
                consumerSecret = FatSecretConfig.consumerSecret,
                tokenProvider = {
                    val t = tokenStore.accessToken
                    val s = tokenStore.accessTokenSecret
                    if (t.isNullOrEmpty() || s.isNullOrEmpty()) null else t to s
                },
            )
        )
        .build()

    private val service: FatSecretService = Retrofit.Builder()
        .baseUrl(FatSecretConfig.PLATFORM_BASE_URL)
        .client(apiHttp)
        .build()
        .create(FatSecretService::class.java)

    private val foodClient = FatSecretFoodClient(service)
    private val weightClient = FatSecretWeightClient(service)

    val repository = FatSecretRepository(
        authClient = authClient,
        foodClient = foodClient,
        weightClient = weightClient,
        tokenStore = tokenStore,
        goalsStore = goalsStore,
        cacheStore = cacheStore,
        weightCacheStore = weightCacheStore,
    )

    /**
     * The one place the whole sync sequence lives: fetch → persist → refresh
     * every widget instance, in that order, on a scope no screen can cancel.
     *
     * Single-flight on purpose. The button had no reentrancy guard and
     * `init {}` also syncs on open, so taps could overlap: two syncs would each
     * fetch and each write, and whichever finished LAST won — so a slow sync
     * could land on top of a newer one's results. Callers now share one run and
     * all observe the same outcome, which is what makes repeated taps
     * idempotent instead of racy (and stops burning quota on an API that is
     * IP-whitelisted and rate-limited).
     */
    fun syncAndRefresh(): Deferred<SyncResult> = synchronized(syncLock) {
        inFlightSync?.takeIf { it.isActive }?.let { return@synchronized it }

        lateinit var started: Deferred<SyncResult>
        started = appScope.async {
            try {
                val result = repository.sync()
                // Only after the cache is committed, so the widgets always
                // recompose from the data this sync just persisted.
                NutritionWidget.updateAll(appContext)
                WeightWidget.updateAll(appContext)
                result
            } finally {
                // Identity check: never clear a NEWER run's handle.
                synchronized(syncLock) {
                    if (inFlightSync === started) inFlightSync = null
                }
            }
        }
        inFlightSync = started
        started
    }

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}
