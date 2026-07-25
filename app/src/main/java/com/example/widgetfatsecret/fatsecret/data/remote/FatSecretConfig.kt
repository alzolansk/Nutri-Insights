package com.example.widgetfatsecret.fatsecret.data.remote

import com.example.widgetfatsecret.BuildConfig

/**
 * Single source of truth for the FatSecret credentials, read from BuildConfig
 * (which is populated from the git-ignored local.properties). No real secret is
 * ever written in Kotlin source.
 */
object FatSecretConfig {
    val consumerKey: String get() = BuildConfig.FATSECRET_CONSUMER_KEY
    val consumerSecret: String get() = BuildConfig.FATSECRET_CONSUMER_SECRET
    val callbackUrl: String get() = BuildConfig.FATSECRET_CALLBACK_URL

    val hasCredentials: Boolean
        get() = consumerKey.isNotBlank() && consumerSecret.isNotBlank()

    // Endpoints
    const val REQUEST_TOKEN_URL = "https://authentication.fatsecret.com/oauth/request_token"
    const val AUTHORIZE_URL = "https://authentication.fatsecret.com/oauth/authorize"
    const val ACCESS_TOKEN_URL = "https://authentication.fatsecret.com/oauth/access_token"
    const val PLATFORM_BASE_URL = "https://platform.fatsecret.com/"
}
