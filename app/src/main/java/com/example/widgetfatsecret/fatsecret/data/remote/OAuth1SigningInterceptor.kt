package com.example.widgetfatsecret.fatsecret.data.remote

import com.example.widgetfatsecret.fatsecret.oauth.OAuth1Signer
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * OkHttp interceptor that signs every outgoing request to the FatSecret platform
 * API with OAuth 1.0a (HMAC-SHA1), using the currently stored access token.
 *
 * All request query parameters are folded into the signature base string, and
 * the oauth_* parameters are appended to the URL query string. FatSecret only
 * reads the OAuth protocol parameters from the query string (an Authorization
 * header is ignored, yielding "Missing required parameter"). The token is
 * fetched lazily per request via [tokenProvider] so it always reflects the
 * latest stored credentials.
 */
class OAuth1SigningInterceptor(
    private val consumerKey: String,
    private val consumerSecret: String,
    private val tokenProvider: () -> Pair<String, String>?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val (token, tokenSecret) = tokenProvider()
            ?: throw IOException("no_access_token")

        val url = original.url
        val baseUrl = "${url.scheme}://${url.host}${url.encodedPath}"

        val params = HashMap<String, String>()
        for (i in 0 until url.querySize) {
            val name = url.queryParameterName(i)
            val value = url.queryParameterValue(i) ?: ""
            params[name] = value
        }

        val oauth = OAuth1Signer.buildOAuthParams(
            method = original.method,
            baseUrl = baseUrl,
            requestParams = params,
            consumerKey = consumerKey,
            consumerSecret = consumerSecret,
            token = token,
            tokenSecret = tokenSecret,
        )

        // Deliver the oauth_* params (including oauth_signature) in the query
        // string, encoded with the same RFC 3986 rules used for the base string.
        val signedUrl = url.newBuilder().apply {
            oauth.forEach { (k, v) ->
                addEncodedQueryParameter(
                    OAuth1Signer.percentEncode(k),
                    OAuth1Signer.percentEncode(v),
                )
            }
        }.build()

        val signed = original.newBuilder()
            .url(signedUrl)
            .build()
        return chain.proceed(signed)
    }
}
