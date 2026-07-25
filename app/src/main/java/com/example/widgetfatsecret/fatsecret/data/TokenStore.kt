package com.example.widgetfatsecret.fatsecret.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores OAuth tokens encrypted at rest via [EncryptedSharedPreferences]
 * (backed by the Android Keystore). Nothing here is ever logged.
 *
 * Holds two kinds of material:
 *  - the transient request token + secret used during the 3-legged handshake;
 *  - the long-lived access token + secret used to sign API calls.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val appCtx = context.applicationContext
        val masterKey = MasterKey.Builder(appCtx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appCtx,
            "fatsecret_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // --- access token ---------------------------------------------------------

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        private set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var accessTokenSecret: String?
        get() = prefs.getString(KEY_ACCESS_SECRET, null)
        private set(value) = prefs.edit().putString(KEY_ACCESS_SECRET, value).apply()

    val isConnected: Boolean
        get() = !accessToken.isNullOrEmpty() && !accessTokenSecret.isNullOrEmpty()

    fun saveAccessToken(token: String, secret: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_ACCESS_SECRET, secret)
            .apply()
    }

    fun clearAccessToken() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_ACCESS_SECRET)
            .apply()
    }

    // --- request token (transient) --------------------------------------------

    fun saveRequestToken(token: String, secret: String) {
        prefs.edit()
            .putString(KEY_REQUEST_TOKEN, token)
            .putString(KEY_REQUEST_SECRET, secret)
            .apply()
    }

    fun requestToken(): String? = prefs.getString(KEY_REQUEST_TOKEN, null)
    fun requestTokenSecret(): String? = prefs.getString(KEY_REQUEST_SECRET, null)

    fun clearRequestToken() {
        prefs.edit()
            .remove(KEY_REQUEST_TOKEN)
            .remove(KEY_REQUEST_SECRET)
            .apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_ACCESS_SECRET = "access_token_secret"
        const val KEY_REQUEST_TOKEN = "request_token"
        const val KEY_REQUEST_SECRET = "request_token_secret"
    }
}
