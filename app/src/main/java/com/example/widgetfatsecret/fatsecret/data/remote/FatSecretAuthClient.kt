package com.example.widgetfatsecret.fatsecret.data.remote

import com.example.widgetfatsecret.fatsecret.oauth.OAuth1Signer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLDecoder

data class RequestToken(
    val token: String,
    val tokenSecret: String,
    val callbackConfirmed: Boolean,
)

data class AccessToken(
    val token: String,
    val tokenSecret: String,
)

/**
 * Implements the two server-to-server legs of the FatSecret 3-legged OAuth 1.0a
 * handshake (request_token and access_token). The browser authorization leg is
 * handled by [authorizeUrl] + the app's deep-link callback.
 *
 * These calls are signed manually (not via the shared interceptor) because they
 * use transient tokens and special protocol parameters (oauth_callback,
 * oauth_verifier). Secrets are never logged.
 */
class FatSecretAuthClient(
    private val client: OkHttpClient,
    private val consumerKey: String,
    private val consumerSecret: String,
) {

    /** Leg 1: obtain a request token. [callbackUrl] must be the app deep link. */
    suspend fun fetchRequestToken(callbackUrl: String): RequestToken = withContext(Dispatchers.IO) {
        // FatSecret's request_token endpoint must be called with GET: the HTTP
        // method is part of the OAuth signature base string, and the server
        // validates the signature as a GET. Signing/sending as POST yields
        // "Invalid signature" even with correct credentials.
        val oauth = OAuth1Signer.buildOAuthParams(
            method = "GET",
            baseUrl = FatSecretConfig.REQUEST_TOKEN_URL,
            requestParams = emptyMap(),
            consumerKey = consumerKey,
            consumerSecret = consumerSecret,
            token = null,
            tokenSecret = null,
            extraOAuth = mapOf("oauth_callback" to callbackUrl),
        )
        // FatSecret only reads the OAuth protocol parameters from the query
        // string, NOT from an Authorization header (a header yields "Missing
        // required parameter: oauth_consumer_key"). Each param is encoded with
        // the same RFC 3986 rules used to build the signature base string so the
        // transmitted value matches what was signed.
        val request = Request.Builder()
            .url(FatSecretConfig.REQUEST_TOKEN_URL.withOAuthQuery(oauth))
            .get()
            .build()

        val form = executeForm(request)
        RequestToken(
            token = form["oauth_token"].orEmpty(),
            tokenSecret = form["oauth_token_secret"].orEmpty(),
            callbackConfirmed = form["oauth_callback_confirmed"] == "true",
        ).also {
            if (it.token.isEmpty() || it.tokenSecret.isEmpty()) {
                throw IOException("request_token response missing token")
            }
        }
    }

    /** Builds the browser URL the user visits to authorize the request token. */
    fun authorizeUrl(requestToken: String): String =
        "${FatSecretConfig.AUTHORIZE_URL}?oauth_token=${OAuth1Signer.percentEncode(requestToken)}"

    /** Leg 3: exchange the authorized request token + verifier for an access token. */
    suspend fun fetchAccessToken(
        requestToken: String,
        requestTokenSecret: String,
        verifier: String,
    ): AccessToken = withContext(Dispatchers.IO) {
        val oauth = OAuth1Signer.buildOAuthParams(
            method = "GET",
            baseUrl = FatSecretConfig.ACCESS_TOKEN_URL,
            requestParams = emptyMap(),
            consumerKey = consumerKey,
            consumerSecret = consumerSecret,
            token = requestToken,
            tokenSecret = requestTokenSecret,
            extraOAuth = mapOf("oauth_verifier" to verifier),
        )
        val request = Request.Builder()
            .url(FatSecretConfig.ACCESS_TOKEN_URL.withOAuthQuery(oauth))
            .get()
            .build()

        val form = executeForm(request)
        AccessToken(
            token = form["oauth_token"].orEmpty(),
            tokenSecret = form["oauth_token_secret"].orEmpty(),
        ).also {
            if (it.token.isEmpty() || it.tokenSecret.isEmpty()) {
                throw IOException("access_token response missing token")
            }
        }
    }

    /**
     * Appends the given oauth_* parameters (including oauth_signature) to [this]
     * base URL as query parameters, percent-encoded exactly like the signature
     * base string so the transmitted values match what was signed.
     */
    private fun String.withOAuthQuery(oauth: Map<String, String>): okhttp3.HttpUrl =
        toHttpUrl().newBuilder().apply {
            oauth.forEach { (k, v) ->
                addEncodedQueryParameter(
                    OAuth1Signer.percentEncode(k),
                    OAuth1Signer.percentEncode(v),
                )
            }
        }.build()

    private fun executeForm(request: Request): Map<String, String> {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // Do not include the body verbatim (may echo signed params); code only.
                throw IOException("OAuth endpoint HTTP ${response.code}")
            }
            return parseFormEncoded(body)
        }
    }

    private fun parseFormEncoded(body: String): Map<String, String> {
        if (body.isBlank()) return emptyMap()
        return body.split("&").mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val k = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
            val v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
            k to v
        }.toMap()
    }
}
